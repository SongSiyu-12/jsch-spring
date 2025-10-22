package com.yu.jsch.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Simple resource utility for reading classpath or filesystem resources.
 */
public final class ResourceUtils {

    private ResourceUtils() {
    }

    public static boolean isClasspathLocation(String location) {
        return location != null && location.startsWith("classpath:");
    }

    public static String expandHome(String path) {
        if (path == null) return null;
        if (path.startsWith("~" + File.separator) || path.equals("~")) {
            String home = System.getProperty("user.home");
            if (path.length() == 1) {
                return home;
            }
            return home + path.substring(1);
        }
        return path;
    }

    /**
     * Opens an InputStream for the given location. Supports classpath: pseudo-scheme.
     */
    public static InputStream openStream(String location) throws IOException {
        if (isClasspathLocation(location)) {
            String resourcePath = location.substring("classpath:".length());
            if (resourcePath.startsWith("/")) {
                resourcePath = resourcePath.substring(1);
            }
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            InputStream is = cl.getResourceAsStream(resourcePath);
            if (is == null) {
                throw new FileNotFoundException("Classpath resource not found: " + resourcePath);
            }
            return is;
        }
        Path p = Path.of(expandHome(location));
        return Files.newInputStream(p);
    }

    /**
     * Read all bytes from the given location. Supports classpath: and filesystem paths, including ~ expansion.
     */
    public static byte[] readAllBytes(String location) throws IOException {
        try (InputStream is = openStream(location)) {
            return is.readAllBytes();
        }
    }

    public static String readAsString(String location) throws IOException {
        return new String(readAllBytes(location), StandardCharsets.UTF_8);
    }
}
