package fr.kainovaii.obsidian.core.web.component;

public class ComponentException extends RuntimeException
{
    public ComponentException(String message) {
        super(message);
    }

    public ComponentException(String message, Throwable cause) {
        super(message, cause);
    }

    public static class ComponentNotFoundException extends ComponentException {
        public ComponentNotFoundException(String componentName) {
            super("Component not found: " + componentName + ". Make sure it's registered with @LiveComponent annotation.");
        }
    }

    public static class TemplateNotFoundException extends ComponentException {
        public TemplateNotFoundException(String templatePath, Throwable cause) {
            super("Template not found: " + templatePath + ". Check that the file exists in src/main/resources/" + templatePath, cause);
        }
    }

    public static class ActionNotFoundException extends ComponentException {
        public ActionNotFoundException(String componentName, String actionName) {
            super("Action '" + actionName + "' not found in component '" + componentName + "'. Make sure the method exists and is public.");
        }
    }

    public static class StateHydrationException extends ComponentException {
        public StateHydrationException(String fieldName, Throwable cause) {
            super("Failed to hydrate state field: " + fieldName, cause);
        }
    }
}