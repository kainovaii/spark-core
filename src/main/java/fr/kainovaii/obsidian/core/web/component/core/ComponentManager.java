package fr.kainovaii.obsidian.core.web.component.core;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import fr.kainovaii.obsidian.core.web.component.ComponentException;
import io.pebbletemplates.pebble.PebbleEngine;
import spark.Session;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ComponentManager
{
    private final PebbleEngine pebbleEngine;
    private final Map<String, Class<? extends LiveComponent>> registeredComponents = new ConcurrentHashMap<>();

    private final Cache<String, LiveComponent> activeComponents = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .maximumSize(10_000)
            .build();

    public ComponentManager(PebbleEngine pebbleEngine) {
        this.pebbleEngine = pebbleEngine;
    }

    public void register(String name, Class<? extends LiveComponent> componentClass) {
        registeredComponents.put(name, componentClass);
    }

    public String mount(String componentName, Session session) {
        try {
            Class<? extends LiveComponent> componentClass = registeredComponents.get(componentName);
            if (componentClass == null) {
                throw new ComponentException.ComponentNotFoundException(componentName);
            }

            LiveComponent instance = componentClass.getDeclaredConstructor().newInstance();

            String sessionId = session != null ? session.id() : "anonymous";
            String key = sessionId + ":" + instance.getId();

            activeComponents.put(key, instance);
            instance.captureState();
            return instance.render(pebbleEngine);

        } catch (ComponentException e) {
            throw e;
        } catch (Exception e) {
            throw new ComponentException("Failed to mount component: " + componentName, e);
        }
    }

    public ComponentResponse handleAction(ComponentRequest request, Session session) {
        try {
            String sessionId = session != null ? session.id() : "anonymous";
            String key = sessionId + ":" + request.getComponentId();

            LiveComponent component = activeComponents.getIfPresent(key);
            if (component == null) {
                return ComponentResponse.error("Component expired or not found. Please refresh the page.");
            }

            // Synchronized to avoid race conditions on count++
            synchronized (component) {
                component.hydrate(request.getState());
                executeAction(component, request.getAction(), request.getParams());
                component.captureState();

                String html = component.render(pebbleEngine);
                return ComponentResponse.success(html, component.getStateSnapshot());
            }

        } catch (Exception e) {
            return ComponentResponse.error("Action failed: " + e.getMessage());
        }
    }

    private void executeAction(LiveComponent component, String actionName, List<Object> params) {
        try {
            // Special handling for live:poll refresh
            if ("__refresh".equals(actionName)) {
                return;
            }

            // Special handling for live:model updates
            if (actionName.startsWith("updateField_")) {
                String fieldName = actionName.substring("updateField_".length());
                Object value = (params != null && !params.isEmpty()) ? params.get(0) : null;
                component.updateField(fieldName, value);
                return;
            }

            Method method = findMethod(component.getClass(), actionName, params != null ? params.size() : 0);
            if (method == null) {
                throw new ComponentException.ActionNotFoundException(
                        component.getClass().getSimpleName(),
                        actionName
                );
            }

            method.setAccessible(true);

            if (method.getParameterCount() == 0) {
                method.invoke(component);
            } else {
                // Convert parameters to correct types
                Object[] convertedParams = new Object[method.getParameterCount()];
                Class<?>[] paramTypes = method.getParameterTypes();

                for (int i = 0; i < method.getParameterCount(); i++) {
                    if (params != null && i < params.size()) {
                        convertedParams[i] = convertValue(params.get(i), paramTypes[i]);
                    } else {
                        convertedParams[i] = null;
                    }
                }

                method.invoke(component, convertedParams);
            }
        } catch (ComponentException e) {
            throw e;
        } catch (Exception e) {
            throw new ComponentException("Failed to execute action: " + actionName, e);
        }
    }

    private Method findMethod(Class<?> clazz, String methodName, int paramCount) {
        while (clazz != null && clazz != Object.class) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.getName().equals(methodName) && method.getParameterCount() == paramCount) {
                    return method;
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null || targetType.isInstance(value)) return value;

        if (targetType == Integer.class || targetType == int.class) {
            return value instanceof Number ? ((Number) value).intValue() : Integer.parseInt(value.toString());
        }
        if (targetType == Long.class || targetType == long.class) {
            return value instanceof Number ? ((Number) value).longValue() : Long.parseLong(value.toString());
        }
        if (targetType == Double.class || targetType == double.class) {
            return value instanceof Number ? ((Number) value).doubleValue() : Double.parseDouble(value.toString());
        }
        if (targetType == Boolean.class || targetType == boolean.class) {
            return value instanceof Boolean ? value : Boolean.parseBoolean(value.toString());
        }
        if (targetType == String.class) {
            return value.toString();
        }
        return value;
    }

    public int getActiveComponentCount() {
        return (int) activeComponents.estimatedSize();
    }

    public PebbleEngine getPebbleEngine() {
        return pebbleEngine;
    }

    public void clearSession(Session session) {
        if (session == null) return;
        String prefix = session.id() + ":";
        activeComponents.asMap().keySet()
                .removeIf(key -> key.startsWith(prefix));
    }
}