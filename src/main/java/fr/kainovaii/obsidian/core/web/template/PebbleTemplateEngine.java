package fr.kainovaii.obsidian.core.web.template;

import fr.kainovaii.obsidian.core.security.csrf.CsrfExtension;
import fr.kainovaii.obsidian.core.web.component.pebble.ComponentHelperExtension;
import fr.kainovaii.obsidian.core.web.route.RoutePebbleExtension;
import fr.kainovaii.obsidian.core.web.flash.FlashExtension;
import fr.kainovaii.obsidian.core.web.template.extension.StripTagsFilter;
import spark.ModelAndView;
import spark.TemplateEngine;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.loader.ClasspathLoader;

import java.io.StringWriter;
import java.util.Map;

public class PebbleTemplateEngine extends TemplateEngine
{
    private final PebbleEngine engine;

    public PebbleTemplateEngine()
    {
        ClasspathLoader loader = new ClasspathLoader();
        engine = new PebbleEngine.Builder()
            .loader(loader)
            .extension(new RoutePebbleExtension())
            .extension(new StripTagsFilter())
            .extension(new CsrfExtension())
            .extension(new FlashExtension())
            .extension(new ComponentHelperExtension())
            .cacheActive(true)
            .build();
    }

    @Override
    public String render(ModelAndView modelAndView)
    {
        try {
            var template = engine.getTemplate(modelAndView.getViewName());
            var writer = new StringWriter();
            template.evaluate(writer, (Map<String, Object>) modelAndView.getModel());
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String render(String templateName, Map<String, Object> model)
    {
        try {
            var template = engine.getTemplate(templateName);
            var writer = new StringWriter();
            template.evaluate(writer, model);
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}