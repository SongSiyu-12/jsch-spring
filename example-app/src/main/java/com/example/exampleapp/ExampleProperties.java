package com.example.exampleapp;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "example")
public class ExampleProperties {

    /** Whether to run the demo on application startup. */
    private boolean runOnStartup = false;

    /** Host alias to use from ssh.hosts.* configuration. */
    private String host = "staging";

    /** Command to execute over SSH. */
    private String command = "echo Hello from SSH";

    /** Remote path to upload the example file to. Can be relative to user home. */
    private String remotePath = "example-app.txt";

    public boolean isRunOnStartup() {
        return runOnStartup;
    }

    public void setRunOnStartup(boolean runOnStartup) {
        this.runOnStartup = runOnStartup;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getRemotePath() {
        return remotePath;
    }

    public void setRemotePath(String remotePath) {
        this.remotePath = remotePath;
    }
}
