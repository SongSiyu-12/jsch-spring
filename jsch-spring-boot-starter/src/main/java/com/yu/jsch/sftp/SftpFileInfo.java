package com.yu.jsch.sftp;

import java.time.Instant;

/**
 * Simple file info for SFTP directory listings.
 */
public final class SftpFileInfo {
    private final String name;
    private final boolean directory;
    private final long size;
    private final Instant modifiedTime;

    public SftpFileInfo(String name, boolean directory, long size, Instant modifiedTime) {
        this.name = name;
        this.directory = directory;
        this.size = size;
        this.modifiedTime = modifiedTime;
    }

    public String getName() {
        return name;
    }

    public boolean isDirectory() {
        return directory;
    }

    public long getSize() {
        return size;
    }

    public Instant getModifiedTime() {
        return modifiedTime;
    }
}
