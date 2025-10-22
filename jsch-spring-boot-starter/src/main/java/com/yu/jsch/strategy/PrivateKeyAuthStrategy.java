package com.yu.jsch.strategy;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.yu.jsch.HostDefinition;
import com.yu.jsch.util.ResourceUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Private key-based authentication strategy, supports inline key data or external path and optional passphrase.
 */
public class PrivateKeyAuthStrategy implements AuthStrategy {

    private final byte[] privateKey;
    private final byte[] passphrase;
    private final String identityName;

    public PrivateKeyAuthStrategy(byte[] privateKey, String passphrase) {
        this("ssh-identity", privateKey, passphrase);
    }

    public PrivateKeyAuthStrategy(String identityName, byte[] privateKey, String passphrase) {
        this.identityName = Objects.requireNonNull(identityName, "identityName");
        this.privateKey = Objects.requireNonNull(privateKey, "privateKey must not be null");
        this.passphrase = passphrase != null ? passphrase.getBytes(StandardCharsets.UTF_8) : null;
    }

    public static PrivateKeyAuthStrategy fromInline(String privateKey, String passphrase) {
        return new PrivateKeyAuthStrategy(privateKey.getBytes(StandardCharsets.UTF_8), passphrase);
    }

    public static PrivateKeyAuthStrategy fromPath(String path, String passphrase) throws IOException {
        return new PrivateKeyAuthStrategy(ResourceUtils.readAllBytes(path), passphrase);
    }

    @Override
    public void configure(JSch jsch, Session session, HostDefinition host) throws JSchException {
        jsch.addIdentity(identityName, this.privateKey, null, this.passphrase);
    }
}
