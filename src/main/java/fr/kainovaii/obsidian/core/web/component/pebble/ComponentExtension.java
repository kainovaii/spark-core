package fr.kainovaii.obsidian.core.web.component.pebble;

import fr.kainovaii.obsidian.core.web.component.core.ComponentManager;
import fr.kainovaii.obsidian.core.web.component.session.SessionContext;
import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.extension.Function;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComponentExtension extends AbstractExtension
{
    private final ComponentManager componentManager;

    public ComponentExtension(ComponentManager componentManager) {
        this.componentManager = componentManager;
    }

    @Override
    public Map<String, Function> getFunctions()
    {
        Map<String, Function> functions = new HashMap<>();
        functions.put("component", new ComponentFunction(componentManager));
        return functions;
    }

    private static class ComponentFunction implements Function
    {
        private final ComponentManager componentManager;

        public ComponentFunction(ComponentManager componentManager) {
            this.componentManager = componentManager;
        }

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

        @Override
        public List<String> getArgumentNames() {
            List<String> names = new ArrayList<>();
            names.add("componentName");
            return names;
        }
    }
}