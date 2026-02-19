package fr.kainovaii.obsidian.http.controller;

import fr.kainovaii.obsidian.core.Obsidian;
import fr.kainovaii.obsidian.security.csrf.CsrfProtection;
import fr.kainovaii.obsidian.security.user.UserDetails;
import fr.kainovaii.obsidian.security.user.UserDetailsService;
import fr.kainovaii.obsidian.security.user.UserDetailsServiceImpl;
import fr.kainovaii.obsidian.di.Container;
import fr.kainovaii.obsidian.error.ErrorHandler;
import fr.kainovaii.obsidian.template.TemplateManager;
import org.mindrot.jbcrypt.BCrypt;
import spark.*;
import java.util.*;

import static spark.Spark.halt;

/**
 * Base controller providing authentication, flash messages, CSRF protection and template rendering.
 * All application controllers should extend this class.
 */
public class BaseController
{
    /** User details service instance */
    private static UserDetailsService userService;

    /**
     * Gets or initializes UserDetailsService.
     * Auto-detects implementation if not registered in container.
     *
     * @return UserDetailsService instance
     */
    protected static UserDetailsService getUserService()
    {
        if (userService == null) {
            try {
                userService = Container.resolve(UserDetailsService.class);
            } catch (Exception e) {
                userService = autoDetectUserDetailsService();
            }
        }
        return userService;
    }

    /**
     * Auto-detects UserDetailsService implementation.
     * Searches for @UserDetailsServiceImpl annotated class.
     *
     * @return UserDetailsService instance
     * @throws RuntimeException if no implementation found
     */
    private static UserDetailsService autoDetectUserDetailsService()
    {
        try {
            org.reflections.Reflections reflections = new org.reflections.Reflections(Obsidian.getBasePackage());

            Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(UserDetailsServiceImpl.class);

            if (annotatedClasses.isEmpty()) {
                throw new RuntimeException("No class annotated with @UserDetailsServiceImpl found in " + Obsidian.getBasePackage());
            }

            Class<?> implClass = annotatedClasses.iterator().next();

            if (!UserDetailsService.class.isAssignableFrom(implClass)) {
                throw new RuntimeException(implClass.getName() + " is annotated with @UserDetailsServiceImpl but doesn't implement UserDetailsService");
            }

            try {
                return (UserDetailsService) implClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                java.lang.reflect.Constructor<?>[] constructors = implClass.getConstructors();
                if (constructors.length > 0) {
                    java.lang.reflect.Constructor<?> constructor = constructors[0];
                    java.lang.reflect.Parameter[] params = constructor.getParameters();
                    Object[] args = new Object[params.length];

                    for (int i = 0; i < params.length; i++) {
                        args[i] = Container.resolve(params[i].getType());
                    }
                    return (UserDetailsService) constructor.newInstance(args);
                }
                throw e;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to auto-detect UserDetailsService: " + e.getMessage(), e);
        }
    }

    /**
     * Authenticates user and creates session.
     *
     * @param username Username
     * @param password Plain text password
     * @param session HTTP session
     * @return true if login successful, false otherwise
     */
    protected static boolean login(String username, String password, Session session)
    {
        UserDetails user = getUserService().loadByUsername(username);
        if (user == null || !user.isEnabled()) return false;

        if (checkPassword(password, user.getPassword()))
        {
            session.attribute("logged", true);
            session.attribute("user_id", user.getId());
            session.attribute("username", user.getUsername());
            session.attribute("role", user.getRole());
            return true;
        }
        return false;
    }

    /**
     * Hashes a plain text password using BCrypt.
     *
     * @param password Plain text password to hash
     * @return BCrypt hashed password
     */
    protected static String hashPassword(String password)
    {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    /**
     * Verifies a plain text password against a BCrypt hash.
     *
     * @param password Plain text password to verify
     * @param hash     BCrypt hash to compare against
     * @return true if the password matches the hash, false otherwise
     */
    protected static boolean checkPassword(String password, String hash)
    {
        return BCrypt.checkpw(password, hash);
    }

    /**
     * Logs out user by invalidating session.
     *
     * @param session HTTP session
     */
    protected static void logout(Session session) {
        if (session != null) session.invalidate();
    }

    /**
     * Checks if user is logged in.
     *
     * @param req HTTP request
     * @return true if user is authenticated, false otherwise
     */
    public static boolean isLogged(Request req)
    {
        Session session = req.session(false);
        if (session == null) return false;

        Boolean logged = session.attribute("logged");
        return Boolean.TRUE.equals(logged);
    }

    /**
     * Gets currently logged in user.
     *
     * @param req HTTP request
     * @param <T> UserDetails type
     * @return User details or null if not logged in
     */
    @SuppressWarnings("unchecked")
    public static <T extends UserDetails> T getLoggedUser(Request req)
    {
        Session session = req.session(false);
        if (session == null) return null;

        Object userId = session.attribute("user_id");
        if (userId == null) return null;

        return (T) getUserService().loadById(userId);
    }

    /**
     * Checks if user has specific role.
     *
     * @param req HTTP request
     * @param role Role name to check
     * @return true if user has role, false otherwise
     */
    protected static boolean hasRole(Request req, String role)
    {
        UserDetails user = getLoggedUser(req);
        return user != null && role.equals(user.getRole());
    }

    /**
     * Requires user to be logged in or redirects to login page.
     *
     * @param req HTTP request
     * @param res HTTP response
     */
    protected static void requireLogin(Request req, Response res)
    {
        if (!isLogged(req)) {
            res.redirect("/users/login");
            halt();
        }
    }

    /**
     * Sets a flash message for next request.
     *
     * @param req HTTP request
     * @param type Message type (success, error, info, warning)
     * @param message Message text
     */
    protected static void setFlash(Request req, String type, String message)
    {
        Session session = req.session();
        @SuppressWarnings("unchecked")
        Map<String, String> flashes = (Map<String, String>) session.attribute("_flash_messages");

        if (flashes == null) {
            flashes = new HashMap<>();
            session.attribute("_flash_messages", flashes);
        }

        flashes.put(type, message);
    }

    /**
     * Collects and removes flash messages from session.
     *
     * @param req HTTP request
     * @return Map of flash messages
     */
    public static Map<String, String> collectFlashes(Request req)
    {
        Session session = req.session(false);
        if (session == null) return new HashMap<>();

        @SuppressWarnings("unchecked")
        Map<String, String> flashes = (Map<String, String>) session.attribute("_flash_messages");

        if (flashes == null) { return new HashMap<>(); }
        session.removeAttribute("_flash_messages");
        return flashes;
    }

    /**
     * Sets flash message and redirects.
     *
     * @param req HTTP request
     * @param res HTTP response
     * @param type Message type
     * @param message Message text
     * @param location Redirect location
     * @return null (never reached due to halt)
     */
    protected static Object redirectWithFlash(Request req, Response res, String type, String message, String location)
    {
        setFlash(req, type, message);
        res.redirect(location);
        halt();
        return null;
    }

    /**
     * Gets CSRF token for current request.
     *
     * @param req HTTP request
     * @return CSRF token
     */
    protected String csrfToken(Request req) {
        return CsrfProtection.getToken(req);
    }

    /**
     * Validates CSRF token from request.
     *
     * @param req HTTP request
     * @return true if valid, false otherwise
     */
    protected boolean validateCsrf(Request req) {
        return CsrfProtection.validate(req);
    }

    /**
     * Regenerates CSRF token.
     *
     * @param req HTTP request
     */
    protected void regenerateCsrfToken(Request req) {
        CsrfProtection.regenerateToken(req);
    }

    /**
     * Renders template with model data.
     *
     * @param template Template path (relative to view/)
     * @param model Template variables
     * @return Rendered HTML
     */
    protected String render(String template, Map<String, Object> model)
    {
        Map<String, Object> merged = new HashMap<>(TemplateManager.getGlobals());
        if (model != null) merged.putAll(model);

        try {
            return TemplateManager.get().render("view/" + template, merged);
        } catch (Exception exception) {
            Request req = (Request) merged.get("request");
            Response res = (Response) merged.get("response");
            return ErrorHandler.handle(exception, req, res);
        }
    }
}