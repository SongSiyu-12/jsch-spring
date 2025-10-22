package com.yu.jsch.channel;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * Factory to open and configure exec channels.
 */
public class ExecChannelFactory {

    /**
     * Open an exec channel for the provided session and return it without connecting.
     * Caller is responsible for configuring command, environment, PTY, and connecting.
     */
    public ChannelExec open(Session session) throws JSchException {
        Channel channel = session.openChannel("exec");
        if (!(channel instanceof ChannelExec exec)) {
            if (channel != null) channel.disconnect();
            throw new JSchException("Opened channel is not an exec channel");
        }
        return exec;
    }

    /**
     * Open an exec channel for the provided session and command, connect it, and return it.
     * Ensures the channel is disconnected if an error occurs while connecting.
     */
    public ChannelExec open(Session session, String command, int connectTimeoutMillis) throws JSchException {
        Channel channel = null;
        try {
            channel = session.openChannel("exec");
            if (!(channel instanceof ChannelExec exec)) {
                if (channel != null) channel.disconnect();
                throw new JSchException("Opened channel is not an exec channel");
            }
            exec.setCommand(command);
            if (connectTimeoutMillis > 0) {
                exec.connect(connectTimeoutMillis);
            } else {
                exec.connect();
            }
            return exec;
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
