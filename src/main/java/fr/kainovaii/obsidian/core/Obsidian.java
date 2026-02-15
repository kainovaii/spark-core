package fr.kainovaii.obsidian.core;

import fr.kainovaii.obsidian.core.database.DB;
import fr.kainovaii.obsidian.core.database.MigrationManager;
import fr.kainovaii.obsidian.core.web.di.ComponentScanner;
import fr.kainovaii.obsidian.core.web.di.Container;
import fr.kainovaii.obsidian.core.web.WebServer;
import fr.kainovaii.obsidian.core.web.component.core.ComponentManager;
import fr.kainovaii.obsidian.core.web.component.pebble.ComponentExtension;
import fr.kainovaii.obsidian.core.web.component.scanner.LiveComponentScanner;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.loader.ClasspathLoader;

import java.util.logging.Logger;

public class Obsidian
{
    public final static Logger logger = Logger.getLogger("Spark");
    private static String basePackage;
    private static ComponentManager componentManager;

    public Obsidian()
    {
        basePackage = "fr.kainovaii.obsidian.app";
    }

    public Obsidian(Class<?> mainClass)
    {
        basePackage = mainClass.getPackage().getName();
    }

    public Obsidian setBasePackage(String basePackage)
    {
        Obsidian.basePackage = basePackage;
        return this;
    }

    public static String getBasePackage()
    {
        return Obsidian.basePackage;
    }

    public void connectDatabase()
    {
        System.out.println("Loading database");
        EnvLoader env = loadConfigAndEnv();

        String dbType = env.get("DB_TYPE");
        if (dbType == null || dbType.isEmpty()) { dbType = "sqlite"; }

        switch (dbType.toLowerCase())
        {
            case "sqlite":
                String dbPath = env.get("DB_PATH");
                if (dbPath == null || dbPath.isEmpty()) {
                    dbPath = "Spark/data.db";
                }
                DB.initSQLite(dbPath, logger);
                break;
            case "mysql":
                String mysqlHost = env.get("DB_HOST");
                String mysqlPort = env.get("DB_PORT");
                DB.initMySQL(
                        mysqlHost != null ? mysqlHost : "localhost",
                        Integer.parseInt(mysqlPort != null ? mysqlPort : "3306"),
                        env.get("DB_NAME"),
                        env.get("DB_USER"),
                        env.get("DB_PASSWORD"),
                        logger
                );
                break;
            case "postgresql":
                String pgHost = env.get("DB_HOST");
                String pgPort = env.get("DB_PORT");
                DB.initPostgreSQL(
                        pgHost != null ? pgHost : "localhost",
                        Integer.parseInt(pgPort != null ? pgPort : "5432"),
                        env.get("DB_NAME"),
                        env.get("DB_USER"),
                        env.get("DB_PASSWORD"),
                        logger
                );
                break;
            default:
                throw new IllegalArgumentException("Unsupported database type: " + dbType);
        }
    }

    public void loadMigrations()
    {
        MigrationManager migrations = new MigrationManager(DB.getInstance(), logger);
        migrations.discover();
        migrations.migrate();
    }

    public void loadContainer()
    {
        ComponentScanner.scanPackage();
    }

    public void loadLiveComponents()
    {
        System.out.println("Loading LiveComponents...");

        try {
            ClasspathLoader loader = new ClasspathLoader();
            PebbleEngine componentPebble = new PebbleEngine.Builder()
                    .loader(loader)
                    .cacheActive(true)
                    .build();

            componentManager = new ComponentManager(componentPebble);

            LiveComponentScanner.scan(basePackage, componentManager);

            componentPebble = new PebbleEngine.Builder()
                    .loader(loader)
                    .extension(new ComponentExtension(componentManager))
                    .cacheActive(true)
                    .build();

            java.lang.reflect.Field field = ComponentManager.class.getDeclaredField("pebbleEngine");
            field.setAccessible(true);
            field.set(componentManager, componentPebble);

            // Enregistrer dans le container
            Container.singleton(ComponentManager.class, componentManager);

            System.out.println("LiveComponents loaded successfully!");
        } catch (Exception e) {
            logger.severe("Failed to load LiveComponents: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static EnvLoader loadConfigAndEnv()
    {
        EnvLoader env = new EnvLoader();
        env.load();
        return env;
    }

    public void startWebServer() { new WebServer().start(); }

    public static int getWebPort() { return Integer.parseInt(Obsidian.loadConfigAndEnv().get("PORT_WEB")); }

    public void registerMotd()
    {
        EnvLoader env = new EnvLoader();

        env.load();
        final String RESET = "\u001B[0m";
        final String CYAN = "\u001B[36m";
        final String YELLOW = "\u001B[33m";
        final String GREEN = "\u001B[32m";
        final String MAGENTA = "\u001B[35m";

        System.out.println(CYAN + "+--------------------------------------+" + RESET);
        System.out.println(CYAN + "|          Obsidian 1.0                |" + RESET);
        System.out.println(CYAN + "+--------------------------------------+" + RESET);
        System.out.println(GREEN + "| Developpeur       : KainoVaii        |" + RESET);
        System.out.println(GREEN + "| Version           : 1.0              |" + RESET);
        System.out.println(GREEN + "| Environnement     : " + env.get("ENVIRONMENT") + "              |" + RESET);
        System.out.println(CYAN + "+--------------------------------------+" + RESET);
        System.out.println(CYAN + "|      Chargement des modules...       |" + RESET);
        System.out.println(CYAN + "+--------------------------------------+" + RESET);
        System.out.println();
    }

    public void init()
    {
        registerMotd();
        loadConfigAndEnv();
        connectDatabase();
        loadMigrations();
        loadContainer();
        loadLiveComponents();
        startWebServer();
    }

    public static Obsidian run(Class<?> mainClass)
    {
        Obsidian app = new Obsidian(mainClass);
        app.init();
        return app;
    }

    public static ComponentManager getComponentManager() {
        return componentManager;
    }
}
