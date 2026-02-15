package fr.kainovaii.obsidian.core.web.component.core;

import fr.kainovaii.obsidian.core.web.component.annotations.State;
import fr.kainovaii.obsidian.core.web.component.ComponentException;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class LiveComponent
{
    @State
    protected String _id;

    protected transient Map<String, Object> stateSnapshot = new HashMap<>();

    public LiveComponent() {
        this._id = UUID.randomUUID().toString();
    }

    public abstract String template();

    public void captureState() {
        stateSnapshot.clear();
        for (Field field : getAllFields(this.getClass())) {
            if (field.isAnnotationPresent(State.class)) {
                field.setAccessible(true);
                try {
                    stateSnapshot.put(field.getName(), field.get(this));
                } catch (IllegalAccessException e) {
                    throw new ComponentException("Cannot capture state field: " + field.getName(), e);
                }
            }
        }
    }

    public void hydrate(Map<String, Object> state) {
        for (Field field : getAllFields(this.getClass())) {
            if (field.isAnnotationPresent(State.class)) {
                field.setAccessible(true);
                try {
                    Object value = state.get(field.getName());
                    if (value != null) {
                        field.set(this, convertValue(value, field.getType()));
                    }
                } catch (IllegalAccessException e) {
                    throw new ComponentException.StateHydrationException(field.getName(), e);
                }
            }
        }
    }

    public String render(PebbleEngine pebble) {
        try {
            PebbleTemplate template = pebble.getTemplate(template());
            Map<String, Object> context = new HashMap<>();
            context.put("_id", _id);

            // Add @State fields
            for (Field field : getAllFields(this.getClass())) {
                if (field.isAnnotationPresent(State.class)) {
                    field.setAccessible(true);
                    context.put(field.getName(), field.get(this));
                }
            }

            // Add getters
            for (java.lang.reflect.Method method : this.getClass().getMethods()) {
                String name = method.getName();
                if ((name.startsWith("get") || name.startsWith("is")) && method.getParameterCount() == 0 && !name.equals("getClass")) {
                    String propertyName = name.startsWith("is") ?
                            Character.toLowerCase(name.charAt(2)) + name.substring(3) :
                            Character.toLowerCase(name.charAt(3)) + name.substring(4);
                    context.put(propertyName, method.invoke(this));
                }
            }

            StringWriter writer = new StringWriter();
            template.evaluate(writer, context);
            return writer.toString();

        } catch (io.pebbletemplates.pebble.error.LoaderException e) {
            throw new ComponentException.TemplateNotFoundException(template(), e);
        } catch (Exception e) {
            throw new ComponentException("Failed to render component: " + this.getClass().getSimpleName(), e);
        }
    }

    private Field[] getAllFields(Class<?> clazz) {
        java.util.List<Field> fields = new java.util.ArrayList<>();
        while (clazz != null && clazz != Object.class) {
            fields.addAll(java.util.Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        return fields.toArray(new Field[0]);
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

    public void updateField(String fieldName, Object value) {
        try {
            Field field = findField(this.getClass(), fieldName);
            if (field != null && field.isAnnotationPresent(State.class)) {
                field.setAccessible(true);
                field.set(this, convertValue(value, field.getType()));
            } else {
                throw new ComponentException("Field '" + fieldName + "' not found or not marked with @State");
            }
        } catch (IllegalAccessException e) {
            throw new ComponentException("Cannot update field: " + fieldName, e);
        }
    }

    private Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null && clazz != Object.class) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    public String getId() { return _id; }
    public Map<String, Object> getStateSnapshot() { return stateSnapshot; }
}