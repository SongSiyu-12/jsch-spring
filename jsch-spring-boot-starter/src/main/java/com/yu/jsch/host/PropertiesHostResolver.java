package com.yu.jsch.host;

import com.yu.jsch.AuthType;
import com.yu.jsch.HostDefinition;
import com.yu.jsch.SshProperties;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Default HostResolver that maps from Spring Boot SshProperties (ssh.hosts.*) into HostConfig.
 */
public class PropertiesHostResolver implements HostResolver {

    private final SshProperties properties;

    public PropertiesHostResolver(SshProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    @Override
    public Optional<HostConfig> resolve(String hostId) {
        if (hostId == null) return Optional.empty();
        SshProperties.Host host = properties.getHosts().get(hostId);
        if (host == null) return Optional.empty();
        HostDefinition def = properties.buildHostDefinition(hostId);
        HostConfig.Auth auth;
        AuthType type = def.getAuthentication().getType();
        if (type == AuthType.PASSWORD) {
            String pwd = def.getAuthentication().getPassword();
            auth = HostConfig.Auth.builder()
                    .type(AuthType.PASSWORD)
                    .password(pwd != null ? pwd.toCharArray() : null)
                    .build();
        } else {
            auth = HostConfig.Auth.builder()
                    .type(AuthType.PUBLIC_KEY)
                    .privateKeyPath(def.getAuthentication().getPrivateKeyPath())
                    .privateKey(def.getAuthentication().getPrivateKey())
                    .passphrase(def.getAuthentication().getPassphrase() != null ? def.getAuthentication().getPassphrase().toCharArray() : null)
                    .build();
        }
        SshProperties.TimeoutsProperties tp = host.getTimeouts();
        int connectMs = toMillis(tp.getConnect());
        int readMs = toMillis(tp.getRead());
        HostConfig.KnownHosts kh = HostConfig.KnownHosts.builder()
                .mode(def.getKnownHosts().getMode())
                .path(def.getKnownHosts().getPath())
                .build();
        HostConfig cfg = HostConfig.builder()
                .host(def.getHost())
                .port(def.getPort())
                .username(def.getUsername())
                .auth(auth)
                .knownHosts(kh)
                .connectTimeoutMillis(connectMs)
                .readTimeoutMillis(readMs)
                .build();
        return Optional.of(cfg);
    }

    private int toMillis(Duration d) {
        return d != null ? Math.toIntExact(d.toMillis()) : 0;
    }
}
