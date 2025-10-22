package com.yu.jsch;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.util.Objects;

/**
 * SessionManager backed by an Apache Commons Pool2 GenericObjectPool of JSch Sessions.
 * Validates sessions before use (configurable) and returns/invalidates them depending on outcome.
 */
public class PooledJschSessionManager implements JschSessionManager {

    private final GenericObjectPool<Session> pool;

    public PooledJschSessionManager(JschSessionFactory sessionFactory, SessionPoolProperties props) {
        Objects.requireNonNull(sessionFactory, "sessionFactory");
        Objects.requireNonNull(props, "props");
        GenericObjectPoolConfig<Session> cfg = new GenericObjectPoolConfig<>();
        cfg.setMaxTotal(props.getMaxTotal());
        cfg.setMaxIdle(props.getMaxIdle());
        cfg.setMinIdle(props.getMinIdle());
        cfg.setTestOnBorrow(props.isValidateOnBorrow());
        cfg.setBlockWhenExhausted(true);
        this.pool = new GenericObjectPool<>(new SessionPooledObjectFactory(sessionFactory), cfg);
    }

    @Override
    public <T> T execute(SessionCallback<T> callback) throws Exception {
        Objects.requireNonNull(callback, "callback");
        Session session = null;
        boolean returnedOrInvalidated = false;
        try {
            session = pool.borrowObject();
            if (!isValid(session)) {
                pool.invalidateObject(session);
                returnedOrInvalidated = true;
                throw new JSchException("Borrowed session is not connected");
            }
            T result = callback.doInSession(session);
            // After use, decide whether to return or invalidate
            if (isValid(session)) {
                pool.returnObject(session);
            } else {
                pool.invalidateObject(session);
            }
            returnedOrInvalidated = true;
            return result;
        } catch (Throwable ex) {
            if (session != null && !returnedOrInvalidated) {
                try {
                    if (isValid(session)) {
                        pool.returnObject(session);
                    } else {
                        pool.invalidateObject(session);
                    }
                } catch (Exception ignore) {
                }
                returnedOrInvalidated = true;
            }
            if (ex instanceof Exception e) throw e;
            throw new RuntimeException(ex);
        } finally {
            if (session != null && !returnedOrInvalidated) {
                try {
                    pool.returnObject(session);
                } catch (Exception ignore) {
                }
            }
        }
    }

    @Override
    public boolean isValid(Session session) {
        return session != null && session.isConnected();
    }

    @Override
    public void close(Session session) {
        if (session != null) {
            try {
                session.disconnect();
            } catch (Throwable ignore) {
            }
        }
    }

    /**
     * Shutdown the connection pool and close all sessions.
     * Should be called when the SessionManager is no longer needed.
     */
    public void shutdown() {
        if (pool != null) {
            try {
                pool.close();
            } catch (Exception e) {
                // Log but don't throw - this is cleanup
            }
        }
    }

    /**
     * Get pool statistics for monitoring.
     */
    public PoolStats getPoolStats() {
        if (pool == null) {
            return new PoolStats(0, 0, 0, 0);
        }
        return new PoolStats(
                pool.getNumActive(),
                pool.getNumIdle(),
                pool.getMaxTotal(),
                pool.getMaxIdle()
        );
    }

    public static record PoolStats(int active, int idle, int maxTotal, int maxIdle) {
    }

    private static class SessionPooledObjectFactory extends BasePooledObjectFactory<Session> {
        private final JschSessionFactory sessionFactory;

        SessionPooledObjectFactory(JschSessionFactory sessionFactory) {
            this.sessionFactory = sessionFactory;
        }

        @Override
        public Session create() throws Exception {
            return sessionFactory.createAndConnect();
        }

        @Override
        public PooledObject<Session> wrap(Session session) {
            return new DefaultPooledObject<>(session);
        }

        @Override
        public boolean validateObject(PooledObject<Session> p) {
            Session s = p.getObject();
            return s != null && s.isConnected();
        }

        @Override
        public void destroyObject(PooledObject<Session> p) {
            Session s = p.getObject();
            if (s != null) {
                try {
                    s.disconnect();
                } catch (Throwable ignore) {
                }
            }
        }
    }
}
