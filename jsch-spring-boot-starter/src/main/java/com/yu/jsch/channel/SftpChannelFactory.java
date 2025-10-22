package com.yu.jsch.channel;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * Factory to open and configure SFTP channels.
 */
public class SftpChannelFactory {

    /**
     * Open an SFTP channel for the provided session, connect it, and return it.
     * Ensures the channel is disconnected if an error occurs while connecting.
     */
    public ChannelSftp open(Session session, int connectTimeoutMillis) throws JSchException {
        Channel channel = null;
        try {
            channel = session.openChannel("sftp");
            if (!(channel instanceof ChannelSftp sftp)) {
                if (channel != null) channel.disconnect();
                throw new JSchException("Opened channel is not an sftp channel");
            }
            if (connectTimeoutMillis > 0) {
                sftp.connect(connectTimeoutMillis);
            } else {
                sftp.connect();
            }
            return sftp;
        } catch (JSchException e) {
            if (channel != null) {
                try {
                    channel.disconnect();
                } catch (Throwable ignore) {
                }
            }
            throw e;
        }
    }
}
