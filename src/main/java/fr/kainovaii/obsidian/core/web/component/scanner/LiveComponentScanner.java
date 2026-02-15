package fr.kainovaii.obsidian.core.web.component.scanner;

import fr.kainovaii.obsidian.core.web.component.core.ComponentManager;
import fr.kainovaii.obsidian.core.web.component.core.LiveComponent;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

import java.util.Set;
import java.util.logging.Logger;

public class LiveComponentScanner {
    
    private static final Logger logger = Logger.getLogger(LiveComponentScanner.class.getName());
    
    public static void scan(String basePackage, ComponentManager componentManager)
    {
        logger.info("Scanning for LiveComponents in package: " + basePackage);
        
        try {
            Reflections reflections = new Reflections(
                new ConfigurationBuilder()
                    .forPackage(basePackage)
                    .setScanners(Scanners.TypesAnnotated, Scanners.SubTypes)
            );
            
            Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(
                fr.kainovaii.obsidian.core.web.component.annotations.LiveComponent.class
            );
            
            for (Class<?> clazz : annotatedClasses) {
                if (LiveComponent.class.isAssignableFrom(clazz)) {
                    registerComponent(clazz, componentManager);
                } else {
                    logger.warning("Class " + clazz.getName() + " has @LiveComponent but doesn't extend LiveComponent");
                }
            }
            
            logger.info("Found and registered " + annotatedClasses.size() + " LiveComponents");
        } catch (Exception e) {
            logger.severe("Failed to scan for LiveComponents: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @SuppressWarnings("unchecked")
    private static void registerComponent(Class<?> clazz, ComponentManager componentManager)
    {
        fr.kainovaii.obsidian.core.web.component.annotations.LiveComponent annotation =  clazz.getAnnotation(fr.kainovaii.obsidian.core.web.component.annotations.LiveComponent.class);
        
        String componentName = (annotation.value() != null && !annotation.value().isEmpty()) ? annotation.value() : clazz.getSimpleName();
        
        componentManager.register(componentName, (Class<? extends LiveComponent>) clazz);
        logger.info("Registered LiveComponent: " + componentName + " (" + clazz.getName() + ")");
    }
}
