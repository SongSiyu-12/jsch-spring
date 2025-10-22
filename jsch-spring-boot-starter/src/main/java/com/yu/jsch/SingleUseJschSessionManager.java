package com.yu.jsch;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.yu.jsch.observability.ObservabilityConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * SessionManager implementation that creates a new session for each execution and closes it afterwards.
 */
public class SingleUseJschSessionManager implements JschSessionManager {

    private static final Logger log = LoggerFactory.getLogger(SingleUseJschSessionManager.class);

    private final JschSessionFactory sessionFactory;
    private final ObservabilityConfig observability;
    private final String hostAlias;

    public SingleUseJschSessionManager(JschSessionFactory sessionFactory) {
        this(sessionFactory, ObservabilityConfig.disabled(), null);
    }

    public SingleUseJschSessionManager(JschSessionFactory sessionFactory, ObservabilityConfig observability, String hostAlias) {
        this.sessionFactory = Objects.requireNonNull(sessionFactory, "sessionFactory");
        this.observability = observability != null ? observability : ObservabilityConfig.disabled();
        this.hostAlias = hostAlias;
    }

    @Override
    public <T> T execute(SessionCallback<T> callback) throws Exception {
        Objects.requireNonNull(callback, "callback");
        Session session = null;
        Instant start = Instant.now();
        if (observability.isLoggingEnabled()) {
            log.atInfo()
                    .addKeyValue("event", "start")
                    .addKeyValue("metric", observability.sessionConnectMetric())
                    .addKeyValue("alias", hostAlias)
                    .addKeyValue("host", sessionFactory.getHost())
                    .addKeyValue("port", sessionFactory.getPort())
                    .addKeyValue("username", sessionFactory.getUsername())
                    .log("ssh session connect");
        }
        try {
            session = sessionFactory.createAndConnect();
            if (!isValid(session)) {
                throw new JSchException("Session is not connected");
            }
            if (observability.isLoggingEnabled()) {
                long durationMs = Duration.between(start, Instant.now()).toMillis();
                log.atInfo()
                        .addKeyValue("event", "finish")
                        .addKeyValue("metric", observability.sessionConnectMetric())
                        .addKeyValue("alias", hostAlias)
                        .addKeyValue("host", sessionFactory.getHost())
                        .addKeyValue("port", sessionFactory.getPort())
                        .addKeyValue("username", sessionFactory.getUsername())
                        .addKeyValue("duration_ms", durationMs)
                        .log("ssh session connected");
            }
            return callback.doInSession(session);
        } catch (Throwable ex) {
            if (observability.isLoggingEnabled()) {
                long durationMs = Duration.between(start, Instant.now()).toMillis();
                log.atWarn()
                        .addKeyValue("event", "failure")
                        .addKeyValue("metric", observability.sessionConnectMetric())
                        .addKeyValue("alias", hostAlias)
                        .addKeyValue("host", sessionFactory.getHost())
                        .addKeyValue("port", sessionFactory.getPort())
                        .addKeyValue("username", sessionFactory.getUsername())
                        .addKeyValue("duration_ms", durationMs)
                        .addKeyValue("error", ex.getClass().getSimpleName())
                        .addKeyValue("message", ex.getMessage())
                        .setCause(ex)
                        .log("ssh session connect failed");
            }
            if (ex instanceof Exception e) {
                throw e;
            }
            throw new RuntimeException(ex);
        } finally {
            close(session);
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
}
