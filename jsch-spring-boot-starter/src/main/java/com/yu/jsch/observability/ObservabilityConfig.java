package com.yu.jsch.observability;

import com.yu.jsch.SshProperties;

import java.util.Objects;

/**
 * Internal observability configuration used to control structured logging and metric names.
 */
public final class ObservabilityConfig {

    private final boolean loggingEnabled;
    private final String sessionConnectMetric;
    private final String sshExecMetric;
    private final String sftpOperationMetric;

    private ObservabilityConfig(boolean loggingEnabled, String sessionConnectMetric, String sshExecMetric, String sftpOperationMetric) {
        this.loggingEnabled = loggingEnabled;
        this.sessionConnectMetric = Objects.requireNonNullElse(sessionConnectMetric, "ssh.session.connect");
        this.sshExecMetric = Objects.requireNonNullElse(sshExecMetric, "ssh.exec");
        this.sftpOperationMetric = Objects.requireNonNullElse(sftpOperationMetric, "ssh.sftp");
    }

    public static ObservabilityConfig fromProperties(SshProperties.ObservabilityProperties props) {
        if (props == null) {
            return defaults();
        }
        SshProperties.ObservabilityProperties.MetricNames names = props.getMetricNames();
        return new ObservabilityConfig(
                props.isEnabled(),
                names != null ? names.getSessionConnect() : null,
                names != null ? names.getSshExec() : null,
                names != null ? names.getSftpOperation() : null
        );
    }

    public static ObservabilityConfig defaults() {
        return new ObservabilityConfig(true, "ssh.session.connect", "ssh.exec", "ssh.sftp");
    }

    public static ObservabilityConfig disabled() {
        return new ObservabilityConfig(false, "ssh.session.connect", "ssh.exec", "ssh.sftp");
    }

    public boolean isLoggingEnabled() {
        return loggingEnabled;
    }

    public String sessionConnectMetric() {
        return sessionConnectMetric;
    }

    public String sshExecMetric() {
        return sshExecMetric;
    }

    public String sftpOperationMetric() {
        return sftpOperationMetric;
    }
}
