package fr.kainovaii.obsidian.livecomponents.core;

import fr.kainovaii.obsidian.livecomponents.annotations.State;
import fr.kainovaii.obsidian.livecomponents.ComponentException;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Base class for LiveComponents - server-side reactive UI components.
 * Components maintain state, handle user interactions, and re-render automatically.
 */
public abstract class LiveComponent
{
    /** Unique component identifier */
    @State
    protected String _id;

    /** Snapshot of component state for diffing */
    protected transient Map<String, Object> stateSnapshot = new HashMap<>();

    /**
     * Constructor.
     * Generates unique component ID.
     */
    public LiveComponent() {
        this._id = UUID.randomUUID().toString();
    }

    /**
     * Returns template path for this component.
     *
     * @return Template path relative to classpath
     */
    public abstract String template();

    /**
     * Captures current state of all @State fields.
     * Used for state synchronization and diffing.
     */
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

    /**
     * Hydrates component state from map.
     * Used when restoring component from client state.
     *
     * @param state State map from client
     */
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

    /**
     * Renders component to HTML using Pebble template engine.
     * Includes all @State fields and getter methods in template context.
     *
     * @param pebble Pebble engine instance
     * @return Rendered HTML
     */
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

    /**
     * Gets all fields from class hierarchy.
     *
     * @param clazz Class to inspect
     * @return Array of all fields including inherited
     */
    private Field[] getAllFields(Class<?> clazz) {
        java.util.List<Field> fields = new java.util.ArrayList<>();
        while (clazz != null && clazz != Object.class) {
            fields.addAll(java.util.Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        return fields.toArray(new Field[0]);
    }

    /**
     * Converts value to target type.
     * Handles primitive types and String conversion.
     *
     * @param value Value to convert
     * @param targetType Target class
     * @return Converted value
     */
    private Object convertValue(Object value, Class<?> targetType)
    {
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

    /**
     * Updates a single @State field value.
     * Used for live:model bindings.
     *
     * @param fieldName Field name
     * @param value New value
     * @throws ComponentException if field not found or not marked with @State
     */
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

    /**
     * Finds field in class hierarchy.
     *
     * @param clazz Class to search
     * @param fieldName Field name
     * @return Field or null if not found
     */
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

    /**
     * Gets component ID.
     *
     * @return Component unique identifier
     */
    public String getId() { return _id; }

    /**
     * Gets state snapshot.
     *
     * @return Map of current state
     */
    public Map<String, Object> getStateSnapshot() { return stateSnapshot; }
}