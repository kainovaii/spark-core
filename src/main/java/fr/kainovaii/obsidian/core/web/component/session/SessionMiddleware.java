package fr.kainovaii.obsidian.core.web.component.session;

import fr.kainovaii.obsidian.core.web.middleware.Middleware;
import spark.Request;
import spark.Response;

public class SessionMiddleware implements Middleware
{
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