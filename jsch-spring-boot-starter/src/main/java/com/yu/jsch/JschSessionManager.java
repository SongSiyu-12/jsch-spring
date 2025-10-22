package com.yu.jsch;

import com.jcraft.jsch.Session;
import com.yu.jsch.host.HostConfig;

/**
 * Abstraction for obtaining, validating, and closing SSH sessions.
 */
public interface JschSessionManager {

    @FunctionalInterface
    interface SessionCallback<T> {
        T doInSession(Session session) throws Exception;
    }

    <T> T execute(SessionCallback<T> callback) throws Exception;

    /**
     * Execute a callback within a session resolved by the provided HostConfig.
     * Default implementation throws UnsupportedOperationException; managers supporting
     * dynamic host configs should override.
     */
    default <T> T execute(HostConfig hostConfig, SessionCallback<T> callback) throws Exception {
        throw new UnsupportedOperationException("HostConfig-aware execute is not supported by this SessionManager");
    }

    boolean isValid(Session session);

    void close(Session session);

    /**
     * Invalidate any pooled sessions associated with the given host key.
     * Default no-op for managers without pooling.
     */
    default void invalidate(String hostKey) {
    }

    /**
     * Invalidate all pooled sessions.
     * Default no-op for managers without pooling.
     */
    default void invalidateAll() {
    }
}
