package fr.kainovaii.obsidian.routing.pebble;

import fr.kainovaii.obsidian.routing.Route;
import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.extension.Function;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pebble extension for route URL generation.
 * Registers the route() function for generating URLs from named routes.
 */
public class RouteExtension extends AbstractExtension
{
    /**
     * Registers route function.
     *
     * @return Map of function name to implementation
     */
    @Override
    public Map<String, Function> getFunctions()
    {
        Map<String, Function> functions = new HashMap<>();
        functions.put("route", new RouteFunction());
        return functions;
    }
}