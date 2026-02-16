package fr.kainovaii.obsidian.livecomponents.session;

import fr.kainovaii.obsidian.http.middleware.Middleware;
import spark.Request;
import spark.Response;

/**
 * Middleware that sets session in SessionContext for current request.
 * Enables LiveComponents to access session via SessionContext.get().
 */
public class SessionMiddleware implements Middleware
{
    /**
     * Handles request by setting session in context.
     *
     * @param req HTTP request
     * @param res HTTP response
     * @throws Exception if processing fails
     */
    @Override
    public void handle(Request req, Response res) throws Exception {
        try {
            SessionContext.set(req.session(true));
        } catch (Exception e)
        {
            SessionContext.set(null);
        }
    }
}