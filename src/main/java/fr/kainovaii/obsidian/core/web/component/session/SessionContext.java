package fr.kainovaii.obsidian.core.web.component.session;

import spark.Session;

public class SessionContext
{
    private static final ThreadLocal<Session> currentSession = new ThreadLocal<>();

    public static void set(Session session) {
        currentSession.set(session);
    }

    public static Session get() {
        return currentSession.get();
    }

    public static void clear() {
        currentSession.remove();
    }
}

