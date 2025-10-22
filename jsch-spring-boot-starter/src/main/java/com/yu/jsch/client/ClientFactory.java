package com.yu.jsch.client;

import com.jcraft.jsch.JSch;
import com.yu.jsch.*;
import com.yu.jsch.auth.PasswordAuthStrategy;
import com.yu.jsch.auth.PublicKeyAuthStrategy;
import com.yu.jsch.channel.ExecChannelFactory;
import com.yu.jsch.channel.SftpChannelFactory;
import com.yu.jsch.observability.ObservabilityConfig;
import com.yu.jsch.strategy.ExponentialBackoffRetryStrategy;
import com.yu.jsch.strategy.NoRetryStrategy;
import com.yu.jsch.strategy.RetryStrategy;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Factory/builder for creating SshClient and SftpClient facades from configuration.
 */
public final class ClientFactory {

    private ClientFactory() {
    }

    public static Builder builder(SshProperties properties) {
        return new Builder(properties);
    }

    public static class Builder {
        private final SshProperties properties;
        private Supplier<JSch> jschSupplier = JSch::new;
        private String defaultHostAlias;
        private SessionPoolProperties sessionPool = new SessionPoolProperties();

        public Builder(SshProperties properties) {
            this.properties = Objects.requireNonNull(properties, "properties");
        }

        public Builder jsch(Supplier<JSch> jschSupplier) {
            this.jschSupplier = Objects.requireNonNull(jschSupplier);
            return this;
        }

        public Builder defaultHost(String alias) {
            this.defaultHostAlias = alias;
            return this;
        }

        public Builder sessionPool(SessionPoolProperties poolProps) {
            this.sessionPool = poolProps != null ? poolProps : new SessionPoolProperties();
            return this;
        }

        public SshClient buildSshClient() {
            Map<String, DefaultSshClient.HostContext> map = new HashMap<>();
            String def = resolveDefaultHostAlias();
            ObservabilityConfig observability = ObservabilityConfig.fromProperties(properties.getObservability());
            for (Map.Entry<String, SshProperties.Host> e : properties.getHosts().entrySet()) {
                String alias = e.getKey();
                HostDefinition host = properties.buildHostDefinition(alias);
                RetryStrategy retry = buildRetry(host);
                JschSessionFactory sessionFactory = buildSessionFactory(host);
                JschSessionManager sm = buildSessionManager(sessionFactory, observability, alias);
                SshTemplate template = new SshTemplate(sm, new ExecChannelFactory(), retry, observability, alias);
                int connectTimeoutMillis = toMillis(host.getTimeouts().getConnect());
                map.put(alias, new DefaultSshClient.HostContext(template, connectTimeoutMillis));
            }
            return new DefaultSshClient(map, def);
        }

        public SftpClient buildSftpClient() {
            Map<String, DefaultSftpClient.HostContext> map = new HashMap<>();
            String def = resolveDefaultHostAlias();
            ObservabilityConfig observability = ObservabilityConfig.fromProperties(properties.getObservability());
            for (Map.Entry<String, SshProperties.Host> e : properties.getHosts().entrySet()) {
                String alias = e.getKey();
                HostDefinition host = properties.buildHostDefinition(alias);
                RetryStrategy retry = buildRetry(host);
                JschSessionFactory sessionFactory = buildSessionFactory(host);
                JschSessionManager sm = buildSessionManager(sessionFactory, observability, alias);
                SftpTemplate template = new SftpTemplate(sm, new SftpChannelFactory(), retry, observability, alias);
                int connectTimeoutMillis = toMillis(host.getTimeouts().getConnect());
                map.put(alias, new DefaultSftpClient.HostContext(template, connectTimeoutMillis));
            }
            return new DefaultSftpClient(map, def);
        }

        private JschSessionManager buildSessionManager(JschSessionFactory sessionFactory, ObservabilityConfig observability, String alias) {
            if (sessionPool != null && sessionPool.isEnabled()) {
                return new PooledJschSessionManager(sessionFactory, sessionPool);
            }
            return new SingleUseJschSessionManager(sessionFactory, observability, alias);
        }

        private String resolveDefaultHostAlias() {
            if (defaultHostAlias != null) {
                if (!properties.getHosts().containsKey(defaultHostAlias)) {
                    throw new HostNotFoundException(defaultHostAlias);
                }
                return defaultHostAlias;
            }
            if (properties.getHosts().size() == 1) {
                return properties.getHosts().keySet().iterator().next();
            }
            return null; // force caller to specify per-call
        }

        private RetryStrategy buildRetry(HostDefinition host) {
            HostDefinition.Retry r = host.getRetry();
            if (r.isEnabled() && r.getMaxAttempts() > 1) {
                long baseDelay = r.getDelay() != null ? r.getDelay().toMillis() : 200L;
                return ExponentialBackoffRetryStrategy.builder()
                        .maxAttempts(r.getMaxAttempts() - 1)
                        .baseDelayMillis(baseDelay)
                        .multiplier(2.0d)
                        .build();
            }
            return new NoRetryStrategy();
        }

        private JschSessionFactory buildSessionFactory(HostDefinition host) {
            JschSessionFactory.Builder b = JschSessionFactory.builder()
                    .jsch(jschSupplier)
                    .host(host.getHost())
                    .port(host.getPort())
                    .username(host.getUsername())
                    .connectTimeoutMillis(toMillis(host.getTimeouts().getConnect()))
                    .socketTimeoutMillis(toMillis(host.getTimeouts().getRead()))
                    .knownHostsMode(host.getKnownHosts().getMode())
                    .knownHostsPath(host.getKnownHosts().getPath());
            HostDefinition.Authentication a = host.getAuthentication();
            AuthType type = a.getType();
            if (type == null) {
                throw new IllegalArgumentException("ssh.hosts." + host.getAlias() + ".authentication.type must be set to 'password' or 'public_key'");
            }
            if (type == AuthType.PASSWORD) {
                if (a.getPassword() == null || a.getPassword().isBlank()) {
                    throw new IllegalArgumentException("ssh.hosts." + host.getAlias() + ": password is required when auth.type=password");
                }
                b.authStrategy(new PasswordAuthStrategy(a.getPassword()));
            } else if (type == AuthType.PUBLIC_KEY) {
                boolean hasPath = a.getPrivateKeyPath() != null && !a.getPrivateKeyPath().isBlank();
                boolean hasInline = a.getPrivateKey() != null && !a.getPrivateKey().isBlank();
                if (!hasPath && !hasInline) {
                    throw new IllegalArgumentException("ssh.hosts." + host.getAlias() + ": private-key-path or private-key must be provided when auth.type=public_key");
                }
                b.authStrategy(new PublicKeyAuthStrategy(a.getPrivateKeyPath(), a.getPrivateKey(), a.getPassphrase()));
            }
            return b.build();
        }

        private int toMillis(Duration d) {
            return d != null ? Math.toIntExact(d.toMillis()) : 0;
        }
    }
}
