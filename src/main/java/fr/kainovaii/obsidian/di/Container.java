package fr.kainovaii.obsidian.di;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * Dependency injection container.
 * Manages singleton instances and handles automatic dependency resolution.
 */
public class Container
{
    /** Singleton instances cache */
    private static final Map<Class<?>, Object> singletons = new HashMap<>();

    /** Interface to implementation bindings */
    private static final Map<Class<?>, Class<?>> bindings = new HashMap<>();

    /**
     * Registers a singleton instance.
     *
     * @param clazz Class type
     * @param instance Instance to register
     * @param <T> Type parameter
     */
    public static <T> void singleton(Class<T> clazz, T instance) {
        singletons.put(clazz, instance);
    }

    /**
     * Binds an interface to its implementation.
     *
     * @param abstraction Interface or abstract class
     * @param implementation Concrete implementation
     * @param <T> Type parameter
     */
    public static <T> void bind(Class<T> abstraction, Class<? extends T> implementation) {
        bindings.put(abstraction, implementation);
    }

    /**
     * Resolves a class instance with automatic dependency injection.
     * Returns existing singleton or creates new instance with constructor injection.
     *
     * @param clazz Class to resolve
     * @param <T> Type parameter
     * @return Resolved instance
     * @throws RuntimeException if dependency resolution fails
     */
    @SuppressWarnings("unchecked")
    public static <T> T resolve(Class<T> clazz)
    {
        if (singletons.containsKey(clazz)) {
            return (T) singletons.get(clazz);
        }

        Class<?> resolvedClass = bindings.getOrDefault(clazz, clazz);

        try {
            Constructor<?> constructor = resolvedClass.getDeclaredConstructors()[0];
            constructor.setAccessible(true);

            Class<?>[] paramTypes = constructor.getParameterTypes();
            Object[] params = new Object[paramTypes.length];

            for (int i = 0; i < paramTypes.length; i++) {
                params[i] = resolve(paramTypes[i]);
            }

            T instance = (T) constructor.newInstance(params);
            singletons.put(clazz, instance);

            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Cannot resolve dependency: " + clazz.getName(), e);
        }
    }

    /**
     * Clears all singletons and bindings.
     * Useful for testing or reinitialization.
     */
    public static void clear()
    {
        singletons.clear();
        bindings.clear();
    }
}