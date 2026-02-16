package fr.kainovaii.obsidian.routing;

import fr.kainovaii.obsidian.security.csrf.annotations.CsrfProtect;
import fr.kainovaii.obsidian.security.csrf.CsrfProtection;
import fr.kainovaii.obsidian.security.role.RoleChecker;
import fr.kainovaii.obsidian.di.Container;
import fr.kainovaii.obsidian.error.ErrorHandler;
import fr.kainovaii.obsidian.http.middleware.annotations.After;
import fr.kainovaii.obsidian.http.middleware.annotations.Before;
import fr.kainovaii.obsidian.http.middleware.MiddlewareManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * Creates Spark route handlers with middleware, CSRF protection, and error handling.
 * Handles method parameter injection and exception handling.
 */
public class RouteHandler
{
    /** Logger instance */
    private static final Logger logger = LoggerFactory.getLogger(RouteHandler.class);

    /**
     * Creates Spark route handler for controller method.
     * Wraps method with middleware, CSRF validation, and error handling.
     *
     * @param controller Controller instance
     * @param method Controller method
     * @return Spark route handler
     */
    public static spark.Route create(Object controller, Method method)
    {
        return (req, res) -> {
            try {
                RoleChecker.checkAccess(req, res);

                executeBeforeMiddleware(method, req, res);

                validateCsrf(controller, method, req, res);

                Object result = invokeMethod(controller, method, req, res);

                executeAfterMiddleware(method, req, res);

                return result;

            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                return ErrorHandler.handle(cause, req, res);
            } catch (Exception e) {
                return ErrorHandler.handle(e, req, res);
            }
        };
    }

    /**
     * Executes before middleware if present.
     *
     * @param method Controller method
     * @param req HTTP request
     * @param res HTTP response
     * @throws Exception if middleware execution fails
     */
    private static void executeBeforeMiddleware(Method method, Request req, Response res) throws Exception
    {
        if (method.isAnnotationPresent(Before.class)) {
            Before beforeAnnotation = method.getAnnotation(Before.class);
            MiddlewareManager.executeBefore(beforeAnnotation.value(), req, res);
        }
    }

    /**
     * Validates CSRF token if @CsrfProtect annotation present.
     *
     * @param controller Controller instance
     * @param method Controller method
     * @param req HTTP request
     * @param res HTTP response
     * @throws SecurityException if CSRF validation fails
     */
    private static void validateCsrf(Object controller, Method method, Request req, Response res)
    {
        if (method.isAnnotationPresent(CsrfProtect.class))
        {
            if (!CsrfProtection.validate(req)) {
                logger.warn("CSRF validation failed for {}.{}",
                        controller.getClass().getSimpleName(),
                        method.getName());

                if (req.session(false) != null) {
                    req.session().attribute("flash_error", "Invalid security token. Please try again.");
                }

                res.status(403);
                throw new SecurityException("CSRF token validation failed");
            }
        }
    }

    /**
     * Invokes controller method with resolved parameters.
     *
     * @param controller Controller instance
     * @param method Controller method
     * @param req HTTP request
     * @param res HTTP response
     * @return Method return value
     * @throws Exception if invocation fails
     */
    private static Object invokeMethod(Object controller, Method method, Request req, Response res) throws Exception
    {
        method.setAccessible(true);
        Object[] args = resolveMethodParameters(method, req, res);
        return method.invoke(controller, args);
    }

    /**
     * Resolves method parameters via dependency injection.
     * Injects Request, Response, or resolves from Container.
     *
     * @param method Controller method
     * @param req HTTP request
     * @param res HTTP response
     * @return Array of resolved parameters
     */
    private static Object[] resolveMethodParameters(Method method, Request req, Response res)
    {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Class<?> paramType = parameters[i].getType();

            if (paramType == Request.class) {
                args[i] = req;
            } else if (paramType == Response.class) {
                args[i] = res;
            } else {
                args[i] = Container.resolve(paramType);
            }
        }

        return args;
    }

    /**
     * Executes after middleware if present.
     *
     * @param method Controller method
     * @param req HTTP request
     * @param res HTTP response
     * @throws Exception if middleware execution fails
     */
    private static void executeAfterMiddleware(Method method, Request req, Response res) throws Exception
    {
        if (method.isAnnotationPresent(After.class)) {
            After afterAnnotation = method.getAnnotation(After.class);
            MiddlewareManager.executeAfter(afterAnnotation.value(), req, res);
        }
    }
}