package fr.kainovaii.obsidian.core.web.component.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.kainovaii.obsidian.core.security.csrf.CsrfProtect;
import fr.kainovaii.obsidian.core.web.component.core.ComponentManager;
import fr.kainovaii.obsidian.core.web.component.core.ComponentRequest;
import fr.kainovaii.obsidian.core.web.component.core.ComponentResponse;
import fr.kainovaii.obsidian.core.web.controller.Controller;
import fr.kainovaii.obsidian.core.web.middleware.Before;
import fr.kainovaii.obsidian.core.web.middleware.builtin.RateLimitMiddleware;
import fr.kainovaii.obsidian.core.web.route.methods.POST;
import spark.Request;
import spark.Response;

@Controller
public class LiveComponentController
{
    private final ObjectMapper objectMapper = new ObjectMapper();

    @CsrfProtect
    @Before(RateLimitMiddleware.class)
    @POST(value = "/obsidian/components", name = "obsidian.components.handle")
    public Object handleAction(Request req, Response res, ComponentManager componentManager)
    {
        res.type("application/json");
        try {
            ComponentRequest componentRequest = objectMapper.readValue(req.body(), ComponentRequest.class);
            ComponentResponse componentResponse = componentManager.handleAction( componentRequest, req.session(true) );
            return objectMapper.writeValueAsString(componentResponse);
        } catch (Exception e) {
            ComponentResponse errorResponse = ComponentResponse.error("Server error: " + e.getMessage());
            try {
                return objectMapper.writeValueAsString(errorResponse);
            } catch (Exception jsonError) {
                return "{\"success\":false,\"error\":\"Fatal error\"}";
            }
        }
    }
}