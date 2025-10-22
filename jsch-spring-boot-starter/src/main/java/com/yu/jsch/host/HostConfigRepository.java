package com.yu.jsch.host;

import java.util.Optional;

/**
 * Minimal repository abstraction to obtain HostConfig by id. Users can implement with JDBC/JPA.
 */
@FunctionalInterface
public interface HostConfigRepository {
    Optional<HostConfig> findById(String hostId);
}
