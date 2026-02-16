package fr.kainovaii.obsidian.livecomponents.pebble;

import fr.kainovaii.obsidian.livecomponents.core.ComponentManager;
import fr.kainovaii.obsidian.livecomponents.session.SessionContext;
import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.extension.Function;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pebble extension for LiveComponents.
 * Registers the component() function for mounting components in templates.
 */
public class ComponentExtension extends AbstractExtension
{
    /** Component manager instance */
    private final ComponentManager componentManager;

    /**
     * Constructor.
     *
     * @param componentManager Component manager
     */
    public ComponentExtension(ComponentManager componentManager) {
        this.componentManager = componentManager;
    }

    /**
     * Registers component function.
     *
     * @return Map of function name to implementation
     */
    @Override
    public Map<String, Function> getFunctions()
    {
        Map<String, Function> functions = new HashMap<>();
        functions.put("component", new ComponentFunction(componentManager));
        return functions;
    }

    /**
     * Pebble function for mounting LiveComponents.
     * Usage: {{ component('counter') }}
     */
    private static class ComponentFunction implements Function
    {
        /** Component manager instance */
        private final ComponentManager componentManager;

        /**
         * Constructor.
         *
         * @param componentManager Component manager
         */
        public ComponentFunction(ComponentManager componentManager) {
            this.componentManager = componentManager;
        }

        /**
         * Executes component mounting.
         *
         * @param args Function arguments (component name)
         * @param self Template instance
         * @param context Evaluation context
         * @param lineNumber Line number in template
         * @return Rendered component HTML or error comment
         */
        @Override
        public Object execute(Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber)
        {
            String componentName = (String) args.get("0");
            if (componentName == null || componentName.isEmpty()) {
                return "<!-- Error: component name required -->";
            }

            try {
                spark.Session session = SessionContext.get();
                return componentManager.mount(componentName, session);
            } catch (Exception e) {
                return "<!-- Error loading component '" + componentName + "': " + e.getMessage() + " -->";
            }
        }

        /**
         * Returns argument names for function.
         *
         * @return List containing "componentName"
         */
        @Override
        public List<String> getArgumentNames() {
            List<String> names = new ArrayList<>();
            names.add("componentName");
            return names;
        }
    }
}