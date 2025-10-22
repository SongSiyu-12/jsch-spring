package com.yu.jsch;

/**
 * Known hosts verification behavior.
 */
public enum KnownHostsMode {
    /**
     * Fail for unknown hosts; only known hosts are allowed.
     */
    STRICT,
    /**
     * Accept and add new hosts to the known_hosts store.
     */
    ACCEPT_NEW,
    /**
     * Do not verify hosts.
     */
    OFF
}
