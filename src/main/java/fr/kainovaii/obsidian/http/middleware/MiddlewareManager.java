package fr.kainovaii.obsidian.http.middleware;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.util.HashMap;
import java.util.Map;

/**
 * Middleware execution manager.
 * Handles instantiation and execution of middleware classes for routes.
 */
public class MiddlewareManager
{
    /** Logger instance */
    private static final Logger logger = LoggerFactory.getLogger(MiddlewareManager.class);

    /** Singleton instances of middleware classes */
    private static final Map<Class<? extends Middleware>, Middleware> instances = new HashMap<>();

    /**
     * Executes before-route middleware chain.
     *
     * @param middlewareClasses Array of middleware classes to execute
     * @param req HTTP request
     * @param res HTTP response
     * @throws Exception if any middleware throws an exception
     */
    public static void executeBefore(Class<? extends Middleware>[] middlewareClasses, Request req, Response res) throws Exception
    {
        for (Class<? extends Middleware> middlewareClass : middlewareClasses) {
            Middleware middleware = getInstance(middlewareClass);
            logger.debug("Executing before middleware: {}", middlewareClass.getSimpleName());
            middleware.handle(req, res);
        }
    }

    /**
     * Executes after-route middleware chain.
     *
     * @param middlewareClasses Array of middleware classes to execute
     * @param req HTTP request
     * @param res HTTP response
     * @throws Exception if any middleware throws an exception
     */
    public static void executeAfter(Class<? extends Middleware>[] middlewareClasses, Request req, Response res) throws Exception
    {
        for (Class<? extends Middleware> middlewareClass : middlewareClasses) {
            Middleware middleware = getInstance(middlewareClass);
            logger.debug("Executing after middleware: {}", middlewareClass.getSimpleName());
            middleware.handle(req, res);
        }
    }

    /**
     * Gets or creates singleton instance of middleware.
     *
     * @param middlewareClass Middleware class
     * @return Middleware instance
     * @throws RuntimeException if instantiation fails
     */
    private static Middleware getInstance(Class<? extends Middleware> middlewareClass)
    {
        return instances.computeIfAbsent(middlewareClass, cls -> {
            try {
                return cls.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                logger.error("Failed to instantiate middleware: {}", cls.getName(), e);
                throw new RuntimeException("Could not instantiate middleware: " + cls.getName(), e);
            }
        });
    }
}