package fr.kainovaii.obsidian.core.web.di;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

public class Container
{
    private static final Map<Class<?>, Object> singletons = new HashMap<>();
    private static final Map<Class<?>, Class<?>> bindings = new HashMap<>();

    public static <T> void singleton(Class<T> clazz, T instance) { singletons.put(clazz, instance); }

    public static <T> void bind(Class<T> abstraction, Class<? extends T> implementation) { bindings.put(abstraction, implementation); }

    @SuppressWarnings("unchecked")
    public static <T> T resolve(Class<T> clazz)
    {
        if (singletons.containsKey(clazz)) { return (T) singletons.get(clazz);}

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

    public static void clear()
    {
        singletons.clear();
        bindings.clear();
    }
}
