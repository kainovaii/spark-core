package fr.kainovaii.obsidian.security.role;

import fr.kainovaii.obsidian.core.Obsidian;
import fr.kainovaii.obsidian.http.controller.BaseController;
import spark.Request;
import spark.Response;

import java.util.HashMap;
import java.util.Map;

import static spark.Spark.halt;

/**
 * Role-based access control checker.
 * Manages route-to-role mappings and enforces role requirements.
 */
public class RoleChecker extends BaseController
{
    /** Map of route patterns to required roles */
    private static final Map<String, String> pathToRole = new HashMap<>();

    /**
     * Registers a route with its required role.
     *
     * @param path Route path pattern
     * @param role Required role name
     */
    public static void registerPathWithRole(String path, String role) {
        pathToRole.put(path, role);
    }

    /**
     * Checks if user has required role for current route.
     * Redirects if access denied.
     *
     * @param req HTTP request
     * @param res HTTP response
     */
    public static void checkAccess(Request req, Response res)
    {
        String matchedPattern = req.matchedPath();
        String requiredRole = pathToRole.get(matchedPattern);

        if (requiredRole == null) { return; }

        requireLogin(req, res);

        if (!requiredRole.equals("DEFAULT"))
        {
            String userRole = getLoggedUser(req).getRole();
            if (userRole == null || !userRole.equals(requiredRole)) {
                redirectWithFlash(req, res, "error", "Access denied - Role required : " + requiredRole, Obsidian.loadConfigAndEnv().get("SITE_URL"));
                halt();
            }
        }
    }
}