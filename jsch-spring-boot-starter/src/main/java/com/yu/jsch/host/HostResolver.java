package com.yu.jsch.host;

import java.util.Optional;

/**
 * Host resolver capable of resolving host configuration by an identifier (alias/id).
 */
public interface HostResolver {
    Optional<HostConfig> resolve(String hostId);
}
