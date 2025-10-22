package com.yu.jsch.strategy;

import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.yu.jsch.HostDefinition;

/**
 * Lenient host key acceptance: accepts any host key presented by the server.
 * This strategy is insecure and should only be used in trusted environments.
 */
public class LenientHostKeyVerificationStrategy implements HostKeyVerificationStrategy {

    private static final HostKeyRepository ACCEPT_ALL = new HostKeyRepository() {
        @Override
        public int check(String host, byte[] key) {
            return OK;
        }

        @Override
        public void add(HostKey hostkey, com.jcraft.jsch.UserInfo ui) {
            // no-op
        }

        @Override
        public void remove(String host, String type) {
            // no-op
        }

        @Override
        public void remove(String host, String type, byte[] key) {
            // no-op
        }

        @Override
        public String getKnownHostsRepositoryID() {
            return "accept-all";
        }

        @Override
        public HostKey[] getHostKey() {
            return new HostKey[0];
        }

        @Override
        public HostKey[] getHostKey(String host, String type) {
            return new HostKey[0];
        }
    };

    @Override
    public void apply(JSch jsch, HostDefinition host) throws JSchException {
        jsch.setHostKeyRepository(ACCEPT_ALL);
    }
}
