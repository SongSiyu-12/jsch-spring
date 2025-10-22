package com.yu.jsch;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Global session pool properties controlling whether commons-pool2-backed session pooling is enabled
 * and the pool sizing/validation settings.
 */
@Validated
@ConfigurationProperties(prefix = "ssh.session-pool")
public class SessionPoolProperties {

    /**
     * Enable pooling of SSH sessions using commons-pool2.
     */
    private boolean enabled = false;

    /**
     * Maximum total sessions across all pooled instances per host context.
     */
    @Positive
    private int maxTotal = 8;

    /**
     * Maximum number of idle sessions retained.
     */
    @Min(0)
    private int maxIdle = 8;

    /**
     * Minimum number of idle sessions to maintain.
     */
    @Min(0)
    private int minIdle = 0;

    /**
     * Validate a session before handing it out from the pool.
     */
    private boolean validateOnBorrow = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxTotal() {
        return maxTotal;
    }

    public void setMaxTotal(int maxTotal) {
        this.maxTotal = maxTotal;
    }

    public int getMaxIdle() {
        return maxIdle;
    }

    public void setMaxIdle(int maxIdle) {
        this.maxIdle = maxIdle;
    }

    public int getMinIdle() {
        return minIdle;
    }

    public void setMinIdle(int minIdle) {
        this.minIdle = minIdle;
    }

    public boolean isValidateOnBorrow() {
        return validateOnBorrow;
    }

    public void setValidateOnBorrow(boolean validateOnBorrow) {
        this.validateOnBorrow = validateOnBorrow;
    }
}
