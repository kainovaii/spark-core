package fr.kainovaii.core.web.flash;


import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.extension.Function;

import java.util.HashMap;
import java.util.Map;

public class FlashExtension extends AbstractExtension
{
    @Override
    public Map<String, Function> getFunctions()
    {
        Map<String, Function> functions = new HashMap<>();
        functions.put("flash", new FlashFunction());
        return functions;
    }
}