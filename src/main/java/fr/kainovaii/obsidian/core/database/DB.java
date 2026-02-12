package fr.kainovaii.obsidian.core.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.javalite.activejdbc.Base;

import javax.sql.DataSource;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

public class DB
{
    private static DB instance;
    private final Logger logger;
    private final String type;
    private final String dbPath;
    private HikariDataSource pool;

    public static DB initSQLite(String path, Logger logger) {
        instance = new DB("sqlite", path, null, 0, null, null, logger);
        return instance;
    }

    public static DB initMySQL(String host, int port, String database, String user, String password, Logger logger) {
        instance = new DB("mysql", database, host, port, user, password, logger);
        return instance;
    }

    public static DB initPostgreSQL(String host, int port, String database, String user, String password, Logger logger) {
        instance = new DB("postgresql", database, host, port, user, password, logger);
        return instance;
    }

    public static DB getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Database not initialized!");
        }
        return instance;
    }

    public static <T> T withConnection(Callable<T> task) {
        return getInstance().executeWithConnection(task);
    }

    public static <T> T withTransaction(Callable<T> task) {
        return getInstance().executeWithTransaction(task);
    }

    private DB(String type, String database, String host, int port, String user, String password, Logger logger)
    {
        this.type = type;
        this.logger = logger;
        this.dbPath = database;

        if (type.equals("sqlite")) {
            logger.info("SQLite database initialized: " + database);
        } else {
            setupConnectionPool(type, host, port, database, user, password);
        }
    }

    private void setupConnectionPool(String type, String host, int port, String database, String user, String password)
    {
        HikariConfig config = new HikariConfig();

        String url = switch (type) {
            case "mysql" -> String.format("jdbc:mysql://%s:%d/%s?useSSL=false", host, port, database);
            case "postgresql" -> String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
            default -> throw new IllegalArgumentException("Type non supporté: " + type);
        };

        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);

        pool = new HikariDataSource(config);
        logger.info("Connection pool initialized for " + type);
    }

    public <T> T executeWithConnection(Callable<T> task)
    {
        boolean created = false;
        try {
            if (!Base.hasConnection()) {
                connect();
                created = true;
            }
            return task.call();
        } catch (Exception e) {
            logger.severe("Erreur database: " + e.getMessage());
            throw new RuntimeException(e);
        } finally {
            if (created && Base.hasConnection()) {
                Base.close();
            }
        }
    }

    public <T> T executeWithTransaction(Callable<T> task)
    {
        boolean created = false;
        try {
            if (!Base.hasConnection()) {
                connect();
                created = true;
            }
            Base.openTransaction();
            T result = task.call();
            Base.commitTransaction();
            return result;
        } catch (Exception e) {
            if (Base.hasConnection()) {
                Base.rollbackTransaction();
            }
            logger.severe("Transaction échouée: " + e.getMessage());
            throw new RuntimeException(e);
        } finally {
            if (created && Base.hasConnection()) {
                Base.close();
            }
        }
    }

    public void close()
    {
        if (Base.hasConnection()) {
            Base.close();
        }
        if (pool != null) {
            pool.close();
            logger.info("Connection pool fermé");
        }
    }

    public String getType() {
        return type;
    }

    private void connect()
    {
        try {
            if (type.equals("sqlite")) {
                String url = "jdbc:sqlite:" + dbPath;
                Base.open("org.sqlite.JDBC", url, "", "");
            } else if (pool != null) {
                Base.open((DataSource) pool.getConnection());
            }
        } catch (Exception e) {
            logger.severe("Échec connexion: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}