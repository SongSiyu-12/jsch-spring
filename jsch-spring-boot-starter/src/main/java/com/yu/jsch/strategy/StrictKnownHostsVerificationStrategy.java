package com.yu.jsch.strategy;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.yu.jsch.HostDefinition;
import com.yu.jsch.util.ResourceUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * Strict known_hosts verification: only hosts present in the known_hosts file are allowed.
 */
public class StrictKnownHostsVerificationStrategy implements HostKeyVerificationStrategy {

    private final String knownHostsLocation;

    public StrictKnownHostsVerificationStrategy(String knownHostsLocation) {
        this.knownHostsLocation = Objects.requireNonNull(knownHostsLocation, "knownHostsLocation");
    }

    @Override
    public void apply(JSch jsch, HostDefinition host) throws JSchException {
        try {
            if (ResourceUtils.isClasspathLocation(knownHostsLocation)) {
                try (InputStream is = ResourceUtils.openStream(knownHostsLocation)) {
                    jsch.setKnownHosts(is);
                }
            } else {
                jsch.setKnownHosts(ResourceUtils.expandHome(knownHostsLocation));
            }
        } catch (IOException e) {
            throw new JSchException("Failed to load known_hosts from " + knownHostsLocation, e);
        }
    }
}
