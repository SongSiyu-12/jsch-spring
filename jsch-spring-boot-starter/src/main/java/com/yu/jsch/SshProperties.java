package com.yu.jsch;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * SSH configuration properties bound from the "ssh" prefix.
 * Supports global defaults and a map of named hosts.
 */
@Validated
@ConfigurationProperties(prefix = "ssh")
public class SshProperties {

    /**
     * Default settings applied to each host unless explicitly overridden.
     */
    @NotNull
    @Valid
    @NestedConfigurationProperty
    private Defaults defaults = new Defaults();

    /**
     * Map of host aliases to host configuration.
     */
    @NotNull
    private Map<String, @Valid Host> hosts = new LinkedHashMap<>();

    /**
     * Observability and logging configuration.
     */
    @NotNull
    @Valid
    @NestedConfigurationProperty
    private ObservabilityProperties observability = new ObservabilityProperties();

    public Defaults getDefaults() {
        return defaults;
    }

    public void setDefaults(Defaults defaults) {
        this.defaults = defaults;
    }

    public Map<String, Host> getHosts() {
        return hosts;
    }

    public void setHosts(Map<String, Host> hosts) {
        this.hosts = hosts;
    }

    public ObservabilityProperties getObservability() {
        return observability;
    }

    public void setObservability(ObservabilityProperties observability) {
        this.observability = Objects.requireNonNullElseGet(observability, ObservabilityProperties::new);
    }

    /**
     * Build an immutable HostDefinition by merging host-specific settings with defaults.
     *
     * @param alias name of the host in the map
     * @return merged HostDefinition
     * @throws IllegalArgumentException if the host alias is unknown or required fields are missing
     */
    public HostDefinition buildHostDefinition(String alias) {
        Host host = this.hosts.get(alias);
        if (host == null) {
            throw new IllegalArgumentException("Unknown SSH host alias: " + alias);
        }

        Defaults d = this.defaults != null ? this.defaults : new Defaults();

        String hostname = firstNonBlank(host.getHost(), d.getHost());
        if (hostname == null || hostname.isBlank()) {
            throw new IllegalArgumentException("Host is required for SSH host '" + alias + "'");
        }

        Integer port = firstNonNull(host.getPort(), d.getPort());
        if (port == null) {
            port = 22;
        }

        String username = firstNonBlank(host.getUsername(), d.getUsername());

        // Merge authentication
        AuthenticationProperties hostAuth = host.getAuthentication();
        AuthenticationProperties defAuth = d.getAuthentication();
        AuthType authType = firstNonNull(hostAuth.getType(), defAuth.getType());
        String password = firstNonBlank(hostAuth.getPassword(), defAuth.getPassword());
        String privateKeyPath = firstNonBlank(hostAuth.getPrivateKeyPath(), defAuth.getPrivateKeyPath());
        String privateKey = firstNonBlank(hostAuth.getPrivateKey(), defAuth.getPrivateKey());
        String passphrase = firstNonBlank(hostAuth.getPassphrase(), defAuth.getPassphrase());

        // Merge known hosts
        KnownHostsProperties hostKh = host.getKnownHosts();
        KnownHostsProperties defKh = d.getKnownHosts();
        KnownHostsMode khMode = firstNonNull(hostKh.getMode(), defKh.getMode());
        String khPath = firstNonBlank(hostKh.getPath(), defKh.getPath());

        // Merge retry
        RetryProperties hostRetry = host.getRetry();
        RetryProperties defRetry = d.getRetry();
        boolean retryEnabled = firstNonNull(hostRetry.isEnabled(), defRetry.isEnabled());
        Integer maxAttempts = firstNonNull(hostRetry.getMaxAttempts(), defRetry.getMaxAttempts());
        Duration retryDelay = firstNonNull(hostRetry.getDelay(), defRetry.getDelay());

        // Merge pool
        PoolProperties hostPool = host.getPool();
        PoolProperties defPool = d.getPool();
        boolean poolEnabled = firstNonNull(hostPool.isEnabled(), defPool.isEnabled());
        Integer maxTotal = firstNonNull(hostPool.getMaxTotal(), defPool.getMaxTotal());
        Integer maxIdle = firstNonNull(hostPool.getMaxIdle(), defPool.getMaxIdle());
        Integer minIdle = firstNonNull(hostPool.getMinIdle(), defPool.getMinIdle());
        Duration maxWait = firstNonNull(hostPool.getMaxWait(), defPool.getMaxWait());

        // Merge timeouts
        TimeoutsProperties hostTimeouts = host.getTimeouts();
        TimeoutsProperties defTimeouts = d.getTimeouts();
        Duration connectTimeout = firstNonNull(hostTimeouts.getConnect(), defTimeouts.getConnect());
        Duration authenticationTimeout = firstNonNull(hostTimeouts.getAuthentication(), defTimeouts.getAuthentication());
        Duration sessionTimeout = firstNonNull(hostTimeouts.getSession(), defTimeouts.getSession());
        Duration readTimeout = firstNonNull(hostTimeouts.getRead(), defTimeouts.getRead());

        return HostDefinition.builder()
                .alias(alias)
                .host(hostname)
                .port(port)
                .username(username)
                .authentication(HostDefinition.Authentication.builder()
                        .type(authType)
                        .password(password)
                        .privateKeyPath(privateKeyPath)
                        .privateKey(privateKey)
                        .passphrase(passphrase)
                        .build())
                .knownHosts(HostDefinition.KnownHosts.builder()
                        .mode(khMode)
                        .path(khPath)
                        .build())
                .retry(HostDefinition.Retry.builder()
                        .enabled(retryEnabled)
                        .maxAttempts(maxAttempts)
                        .delay(retryDelay)
                        .build())
                .pool(HostDefinition.Pool.builder()
                        .enabled(poolEnabled)
                        .maxTotal(maxTotal)
                        .maxIdle(maxIdle)
                        .minIdle(minIdle)
                        .maxWait(maxWait)
                        .build())
                .timeouts(HostDefinition.Timeouts.builder()
                        .connect(connectTimeout)
                        .authentication(authenticationTimeout)
                        .session(sessionTimeout)
                        .read(readTimeout)
                        .build())
                .build();
    }

    private static String firstNonBlank(String a, String b) {
        return (a != null && !a.isBlank()) ? a : (b != null && !b.isBlank() ? b : null);
    }

    private static <T> T firstNonNull(T a, T b) {
        return a != null ? a : b;
    }

    /**
     * Common SSH property subset for defaults and hosts.
     */
    public static abstract class CommonProperties {
        /**
         * Hostname or IP address.
         */
        private String host;

        /**
         * TCP port, defaults to 22 if not provided by defaults either.
         */
        @Min(1)
        @Max(65535)
        private Integer port;

        /**
         * Login username.
         */
        private String username;

        /**
         * Authentication settings.
         */
        @NotNull
        @Valid
        @NestedConfigurationProperty
        private AuthenticationProperties authentication = new AuthenticationProperties();

        /**
         * Known hosts handling.
         */
        @NotNull
        @Valid
        @NestedConfigurationProperty
        private KnownHostsProperties knownHosts = new KnownHostsProperties();

        /**
         * Retry options.
         */
        @NotNull
        @Valid
        @NestedConfigurationProperty
        private RetryProperties retry = new RetryProperties();

        /**
         * Session pooling settings.
         */
        @NotNull
        @Valid
        @NestedConfigurationProperty
        private PoolProperties pool = new PoolProperties();

        /**
         * Timeouts.
         */
        @NotNull
        @Valid
        @NestedConfigurationProperty
        private TimeoutsProperties timeouts = new TimeoutsProperties();

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public Integer getPort() {
            return port;
        }

        public void setPort(Integer port) {
            this.port = port;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public AuthenticationProperties getAuthentication() {
            return authentication;
        }

        public void setAuthentication(AuthenticationProperties authentication) {
            this.authentication = Objects.requireNonNullElseGet(authentication, AuthenticationProperties::new);
        }

        public KnownHostsProperties getKnownHosts() {
            return knownHosts;
        }

        public void setKnownHosts(KnownHostsProperties knownHosts) {
            this.knownHosts = Objects.requireNonNullElseGet(knownHosts, KnownHostsProperties::new);
        }

        public RetryProperties getRetry() {
            return retry;
        }

        public void setRetry(RetryProperties retry) {
            this.retry = Objects.requireNonNullElseGet(retry, RetryProperties::new);
        }

        public PoolProperties getPool() {
            return pool;
        }

        public void setPool(PoolProperties pool) {
            this.pool = Objects.requireNonNullElseGet(pool, PoolProperties::new);
        }

        public TimeoutsProperties getTimeouts() {
            return timeouts;
        }

        public void setTimeouts(TimeoutsProperties timeouts) {
            this.timeouts = Objects.requireNonNullElseGet(timeouts, TimeoutsProperties::new);
        }
    }

    /**
     * Default SSH settings applied to all hosts if not overridden.
     */
    public static class Defaults extends CommonProperties {
        public Defaults() {
            // Provide sensible defaults
            setPort(22);
            getKnownHosts().setMode(KnownHostsMode.STRICT);
            getKnownHosts().setPath("~/.ssh/known_hosts");
            getRetry().setEnabled(true);
            getRetry().setMaxAttempts(3);
            getRetry().setDelay(Duration.ofMillis(200));
            getPool().setEnabled(false);
            getPool().setMaxTotal(8);
            getPool().setMaxIdle(8);
            getPool().setMinIdle(0);
            getPool().setMaxWait(Duration.ofSeconds(30));
            getTimeouts().setConnect(Duration.ofSeconds(5));
            getTimeouts().setAuthentication(Duration.ofSeconds(10));
            getTimeouts().setSession(Duration.ofMinutes(10));
            getTimeouts().setRead(Duration.ofSeconds(30));
        }
    }

    /**
     * Host-specific SSH settings. Host and/or username typically required (or inherited from defaults).
     */
    public static class Host extends CommonProperties {
        @Override
        @NotBlank(message = "Host is required for an SSH host entry")
        public String getHost() {
            return super.getHost();
        }

        @Override
        public void setHost(String host) {
            super.setHost(host);
        }
    }

    public static class AuthenticationProperties {
        /**
         * Authentication method.
         */
        private AuthType type;
        /**
         * Password for password-based auth.
         */
        private String password;
        /**
         * File path or classpath location of the private key.
         */
        private String privateKeyPath;
        /**
         * Inline private key PEM content.
         */
        private String privateKey;
        /**
         * Optional passphrase for private key.
         */
        private String passphrase;

        public AuthType getType() {
            return type;
        }

        public void setType(AuthType type) {
            this.type = type;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getPrivateKeyPath() {
            return privateKeyPath;
        }

        public void setPrivateKeyPath(String privateKeyPath) {
            this.privateKeyPath = privateKeyPath;
        }

        public String getPrivateKey() {
            return privateKey;
        }

        public void setPrivateKey(String privateKey) {
            this.privateKey = privateKey;
        }

        public String getPassphrase() {
            return passphrase;
        }

        public void setPassphrase(String passphrase) {
            this.passphrase = passphrase;
        }
    }

    public static class KnownHostsProperties {
        /**
         * Behavior for known_hosts checking.
         */
        private KnownHostsMode mode = KnownHostsMode.STRICT;
        /**
         * Path to the known_hosts file. Tilde (~) is allowed.
         */
        private String path = "~/.ssh/known_hosts";

        public KnownHostsMode getMode() {
            return mode;
        }

        public void setMode(KnownHostsMode mode) {
            this.mode = mode;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    public static class RetryProperties {
        /**
         * Whether to enable simple retry on connection failures.
         */
        private boolean enabled = true;
        /**
         * Maximum number of attempts (including the first).
         */
        @Min(1)
        private Integer maxAttempts = 3;
        /**
         * Fixed delay between attempts.
         */
        @NotNull
        private Duration delay = Duration.ofMillis(200);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Integer getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(Integer maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public Duration getDelay() {
            return delay;
        }

        public void setDelay(Duration delay) {
            this.delay = delay;
        }
    }

    public static class PoolProperties {
        /**
         * Whether to pool SSH sessions.
         */
        private boolean enabled = false;
        /**
         * Max total sessions in the pool.
         */
        @Positive
        private Integer maxTotal = 8;
        /**
         * Max idle sessions.
         */
        @Min(0)
        private Integer maxIdle = 8;
        /**
         * Min idle sessions.
         */
        @Min(0)
        private Integer minIdle = 0;
        /**
         * Max time to wait for a session to become available.
         */
        @NotNull
        private Duration maxWait = Duration.ofSeconds(30);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Integer getMaxTotal() {
            return maxTotal;
        }

        public void setMaxTotal(Integer maxTotal) {
            this.maxTotal = maxTotal;
        }

        public Integer getMaxIdle() {
            return maxIdle;
        }

        public void setMaxIdle(Integer maxIdle) {
            this.maxIdle = maxIdle;
        }

        public Integer getMinIdle() {
            return minIdle;
        }

        public void setMinIdle(Integer minIdle) {
            this.minIdle = minIdle;
        }

        public Duration getMaxWait() {
            return maxWait;
        }

        public void setMaxWait(Duration maxWait) {
            this.maxWait = maxWait;
        }
    }

    public static class TimeoutsProperties {
        /**
         * Connect timeout.
         */
        @NotNull
        private Duration connect = Duration.ofSeconds(5);
        /**
         * Authentication timeout.
         */
        @NotNull
        private Duration authentication = Duration.ofSeconds(10);
        /**
         * Session lifetime timeout.
         */
        @NotNull
        private Duration session = Duration.ofMinutes(10);
        /**
         * Read timeout.
         */
        @NotNull
        private Duration read = Duration.ofSeconds(30);

        public Duration getConnect() {
            return connect;
        }

        public void setConnect(Duration connect) {
            this.connect = connect;
        }

        public Duration getAuthentication() {
            return authentication;
        }

        public void setAuthentication(Duration authentication) {
            this.authentication = authentication;
        }

        public Duration getSession() {
            return session;
        }

        public void setSession(Duration session) {
            this.session = session;
        }

        public Duration getRead() {
            return read;
        }

        public void setRead(Duration read) {
            this.read = read;
        }
    }

    /**
     * Observability properties to control structured logging and metric names.
     */
    public static class ObservabilityProperties {
        /**
         * Enable or disable structured logging across the library.
         */
        private boolean enabled = true;
        /**
         * Metric name configuration for log enrichment.
         */
        @NotNull
        @Valid
        @NestedConfigurationProperty
        private MetricNames metricNames = new MetricNames();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public MetricNames getMetricNames() {
            return metricNames;
        }

        public void setMetricNames(MetricNames metricNames) {
            this.metricNames = Objects.requireNonNullElseGet(metricNames, MetricNames::new);
        }

        /**
         * Metric names used in structured logs.
         */
        public static class MetricNames {
            /**
             * Metric for session connect attempts.
             */
            @NotNull
            private String sessionConnect = "ssh.session.connect";
            /**
             * Metric for SSH exec attempts.
             */
            @NotNull
            private String sshExec = "ssh.exec";
            /**
             * Metric for generic SFTP operations.
             */
            @NotNull
            private String sftpOperation = "ssh.sftp";

            public String getSessionConnect() {
                return sessionConnect;
            }

            public void setSessionConnect(String sessionConnect) {
                this.sessionConnect = sessionConnect;
            }

            public String getSshExec() {
                return sshExec;
            }

            public void setSshExec(String sshExec) {
                this.sshExec = sshExec;
            }

            public String getSftpOperation() {
                return sftpOperation;
            }

            public void setSftpOperation(String sftpOperation) {
                this.sftpOperation = sftpOperation;
            }
        }
    }
}
