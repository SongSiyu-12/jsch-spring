package com.yu.jsch;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.yu.jsch.auth.PasswordAuthStrategy;
import com.yu.jsch.auth.PublicKeyAuthStrategy;
import com.yu.jsch.host.HostConfig;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * SessionManager implementation that manages a pool per HostConfig stable key (host:port:username).
 * Allows invalidation of individual pools or all pools, and optionally enforces version matching.
 */
public class HostConfigJschSessionManager implements JschSessionManager {

    private final Supplier<JSch> jschSupplier;
    private final SessionPoolProperties poolProps;

    private final Map<String, PoolHolder> pools = new ConcurrentHashMap<>();

    public HostConfigJschSessionManager(Supplier<JSch> jschSupplier, SessionPoolProperties poolProps) {
        this.jschSupplier = Objects.requireNonNull(jschSupplier, "jschSupplier");
        this.poolProps = poolProps != null ? poolProps : new SessionPoolProperties();
    }

    @Override
    public <T> T execute(HostConfig hostConfig, SessionCallback<T> callback) throws Exception {
        Objects.requireNonNull(hostConfig, "hostConfig");
        Objects.requireNonNull(callback, "callback");
        String key = hostConfig.stableKey();
        PoolHolder holder = pools.compute(key, (k, existing) -> ensurePool(existing, hostConfig));
        GenericObjectPool<Session> pool = holder.pool;
        Session session = null;
        boolean returnedOrInvalidated = false;
        try {
            session = pool.borrowObject();
            if (!isValid(session)) {
                pool.invalidateObject(session);
                returnedOrInvalidated = true;
                throw new IllegalStateException("Borrowed session is not connected");
            }
            T result = callback.doInSession(session);
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
                    if (isValid(session)) pool.returnObject(session);
                    else pool.invalidateObject(session);
                } catch (Exception ignore) {
                }
            }
            if (ex instanceof Exception e) throw e;
            throw new RuntimeException(ex);
        }
    }

    private PoolHolder ensurePool(PoolHolder existing, HostConfig cfg) {
        Long ver = cfg.getVersion();
        if (existing == null) {
            return new PoolHolder(createPool(cfg), ver);
        }
        // If version provided and does not match, recreate pool (treat null as different)
        if (ver != null && !ver.equals(existing.version)) {
            try {
                existing.pool.close();
            } catch (Exception ignore) {
            }
            return new PoolHolder(createPool(cfg), ver);
        }
        return existing;
    }

    private GenericObjectPool<Session> createPool(HostConfig cfg) {
        GenericObjectPoolConfig<Session> cfgPool = new GenericObjectPoolConfig<>();
        cfgPool.setMaxTotal(poolProps.getMaxTotal());
        cfgPool.setMaxIdle(poolProps.getMaxIdle());
        cfgPool.setMinIdle(poolProps.getMinIdle());
        cfgPool.setBlockWhenExhausted(true);
        cfgPool.setTestOnBorrow(poolProps.isValidateOnBorrow());
        JschSessionFactory sessionFactory = buildSessionFactory(cfg);
        return new GenericObjectPool<>(new Factory(sessionFactory), cfgPool);
    }

    private JschSessionFactory buildSessionFactory(HostConfig cfg) {
        JschSessionFactory.Builder b = JschSessionFactory.builder()
                .jsch(jschSupplier)
                .host(cfg.getHost())
                .port(cfg.getPort())
                .username(cfg.getUsername())
                .connectTimeoutMillis(cfg.getConnectTimeoutMillis())
                .socketTimeoutMillis(cfg.getReadTimeoutMillis())
                .knownHostsMode(cfg.getKnownHosts() != null ? cfg.getKnownHosts().getMode() : null)
                .knownHostsPath(cfg.getKnownHosts() != null ? cfg.getKnownHosts().getPath() : null);
        HostConfig.Auth a = cfg.getAuth();
        if (a != null) {
            if (a.getType() == AuthType.PASSWORD) {
                b.authStrategy(new PasswordAuthStrategy(a.getPassword() != null ? new String(a.getPassword()) : null));
            } else if (a.getType() == AuthType.PUBLIC_KEY) {
                b.authStrategy(new PublicKeyAuthStrategy(a.getPrivateKeyPath(), a.getPrivateKey(), a.getPassphrase() != null ? new String(a.getPassphrase()) : null));
            }
        }
        // Clear sensitive material from memory as soon as we've built the factory
        cfg.clearSensitive();
        return b.build();
    }

    @Override
    public <T> T execute(SessionCallback<T> callback) throws Exception {
        throw new UnsupportedOperationException("Use execute(HostConfig, ...) for HostConfigSessionManager");
    }

    @Override
    public boolean isValid(Session session) {
        return session != null && session.isConnected();
    }

    @Override
    public void close(Session session) {
        if (session != null) try {
            session.disconnect();
        } catch (Throwable ignore) {
        }
    }

    @Override
    public void invalidate(String hostKey) {
        PoolHolder holder = pools.remove(hostKey);
        if (holder != null && holder.pool != null) {
            try {
                holder.pool.close();
            } catch (Exception ignore) {
            }
        }
    }

    @Override
    public void invalidateAll() {
        for (Map.Entry<String, PoolHolder> e : pools.entrySet()) {
            try {
                e.getValue().pool.close();
            } catch (Exception ignore) {
            }
        }
        pools.clear();
    }

    public static String hostKeyOf(HostConfig cfg) {
        return cfg != null ? cfg.stableKey() : null;
    }

    private static class Factory implements PooledObjectFactory<Session> {
        private final JschSessionFactory sessionFactory;

        Factory(JschSessionFactory sessionFactory) {
            this.sessionFactory = sessionFactory;
        }

        @Override
        public PooledObject<Session> makeObject() throws Exception {
            return new DefaultPooledObject<>(sessionFactory.createAndConnect());
        }

        @Override
        public void destroyObject(PooledObject<Session> p) {
            Session s = p.getObject();
            if (s != null) try {
                s.disconnect();
            } catch (Throwable ignore) {
            }
        }

        @Override
        public boolean validateObject(PooledObject<Session> p) {
            Session s = p.getObject();
            return s != null && s.isConnected();
        }

        @Override
        public void activateObject(PooledObject<Session> p) {
        }

        @Override
        public void passivateObject(PooledObject<Session> p) {
        }
    }

    private record PoolHolder(GenericObjectPool<Session> pool, Long version) {
    }
}
