package com.yu.jsch.host;

import java.util.Objects;
import java.util.Optional;

/**
 * Example HostResolver implementation that retrieves HostConfig entries from a repository (e.g., JDBC/JPA).
 */
public class DatabaseHostResolver implements HostResolver {

    private final HostConfigRepository repository;

    public DatabaseHostResolver(HostConfigRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    public Optional<HostConfig> resolve(String hostId) {
        return repository.findById(hostId);
    }
}
