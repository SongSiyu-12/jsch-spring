package com.yu.jsch.client;

/**
 * Thrown when a host alias cannot be resolved to a configured host.
 */
public class HostNotFoundException extends RuntimeException {
    private final String alias;

    public HostNotFoundException(String alias) {
        super("Unknown SSH host alias: " + alias);
        this.alias = alias;
    }

    public String getAlias() {
        return alias;
    }
}
