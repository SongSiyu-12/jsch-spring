package com.yu.jsch.sftp;

/**
 * Options for SFTP transfers.
 */
public final class TransferOptions {
    /**
     * Upload using temp file and atomic rename.
     */
    private final boolean atomic;
    /**
     * Overwrite destination if it exists. For atomic=true this may delete the target before rename.
     */
    private final boolean overwrite;
    /**
     * Optional POSIX permission mask to apply (e.g., 0644 decimal 420). Null to skip.
     */
    private final Integer permissions;
    /**
     * Connect timeout in milliseconds for SFTP channel connection.
     */
    private final int connectTimeoutMillis;
    /**
     * Placeholder for text mode; SFTP is binary by default.
     */
    private final boolean textMode;

    private TransferOptions(Builder b) {
        this.atomic = b.atomic;
        this.overwrite = b.overwrite;
        this.permissions = b.permissions;
        this.connectTimeoutMillis = b.connectTimeoutMillis;
        this.textMode = b.textMode;
    }

    public boolean isAtomic() {
        return atomic;
    }

    public boolean isOverwrite() {
        return overwrite;
    }

    public Integer getPermissions() {
        return permissions;
    }

    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public boolean isTextMode() {
        return textMode;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private boolean atomic = true;
        private boolean overwrite = true;
        private Integer permissions = null;
        private int connectTimeoutMillis = 0;
        private boolean textMode = false;

        public Builder atomic(boolean atomic) {
            this.atomic = atomic;
            return this;
        }

        public Builder overwrite(boolean overwrite) {
            this.overwrite = overwrite;
            return this;
        }

        public Builder permissions(Integer permissions) {
            this.permissions = permissions;
            return this;
        }

        public Builder connectTimeoutMillis(int connectTimeoutMillis) {
            this.connectTimeoutMillis = connectTimeoutMillis;
            return this;
        }

        public Builder textMode(boolean textMode) {
            this.textMode = textMode;
            return this;
        }

        public TransferOptions build() {
            return new TransferOptions(this);
        }
    }

    public static TransferOptions defaults() {
        return builder().build();
    }
}
