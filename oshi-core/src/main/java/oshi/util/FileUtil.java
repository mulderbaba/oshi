/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2017 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/oshi/oshi/graphs/contributors
 */
package oshi.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * File reading methods
 *
 * @author widdis[at]gmail[dot]com
 */
public class FileUtil {
    private static final Logger LOG = Logger.getLogger(FileUtil.class.getName());

    private FileUtil() {
    }

    /**
     * Read an entire file at one time. Intended primarily for Linux /proc
     * filesystem to avoid recalculating file contents on iterative reads.
     *
     * @param filename
     *            The file to read
     *
     * @return A list of Strings representing each line of the file, or an empty
     *         list if file could not be read or is empty
     */
    public static List<String> readFile(String filename) {
        return readFile(filename, true);
    }

    /**
     * Read an entire file at one time. Intended primarily for Linux /proc
     * filesystem to avoid recalculating file contents on iterative reads.
     *
     * @param filename
     *            The file to read
     * @param reportError
     *            Whether to log errors reading the file
     *
     * @return A list of Strings representing each line of the file, or an empty
     *         list if file could not be read or is empty
     */
    public static List<String> readFile(String filename, boolean reportError) {
        if (new File(filename).exists()) {
            LOG.log(Level.FINE, MessageFormat.format("Reading file {0}", filename));
            try {
                return Files.readAllLines(Paths.get(filename), StandardCharsets.UTF_8);
            } catch (IOException e) {
                if (reportError) {
                    LOG.log(Level.SEVERE, MessageFormat.format("Error reading file {0}. {1}", filename, e));
                }
            }
        } else if (reportError) {
            LOG.log(Level.WARNING, MessageFormat.format("File not found: {0}", filename));
        }
        return new ArrayList<>();
    }

    /**
     * Read a file and return the long value contained therein. Intended
     * primarily for Linux /sys filesystem
     *
     * @param filename
     *            The file to read
     * @return The value contained in the file, if any; otherwise zero
     */
    public static long getLongFromFile(String filename) {
        LOG.log(Level.FINE, MessageFormat.format("Reading file {0}", filename));
        List<String> read = FileUtil.readFile(filename, false);
        if (!read.isEmpty()) {
            LOG.log(Level.FINEST, MessageFormat.format("Read {0}", read.get(0)));
            return ParseUtil.parseLongOrDefault(read.get(0), 0L);
        }
        return 0L;
    }

    /**
     * Read a file and return the int value contained therein. Intended
     * primarily for Linux /sys filesystem
     *
     * @param filename
     *            The file to read
     * @return The value contained in the file, if any; otherwise zero
     */
    public static int getIntFromFile(String filename) {
        LOG.log(Level.FINE, MessageFormat.format("Reading file {0}", filename));
        try {
            List<String> read = FileUtil.readFile(filename, false);
            if (!read.isEmpty()) {
                LOG.log(Level.FINEST, MessageFormat.format("Read {0}", read.get(0)));
                return Integer.parseInt(read.get(0));
            }
        } catch (NumberFormatException ex) {
            LOG.log(Level.FINE, MessageFormat.format("Unable to read value from {0}. {1}", filename, ex));
        }
        return 0;
    }

    /**
     * Read a file and return the String value contained therein. Intended
     * primarily for Linux /sys filesystem
     *
     * @param filename
     *            The file to read
     * @return The value contained in the file, if any; otherwise empty string
     */
    public static String getStringFromFile(String filename) {
        LOG.log(Level.FINE, MessageFormat.format("Reading file {0}", filename));
        List<String> read = FileUtil.readFile(filename, false);
        if (!read.isEmpty()) {
            LOG.log(Level.FINEST, MessageFormat.format("Read {0}", read.get(0)));
            return read.get(0);
        }
        return "";
    }

    /**
     * Read a file and return an array of whitespace-delimited string values
     * contained therein. Intended primarily for Linux /proc
     *
     * @param filename
     *            The file to read
     * @return An array of strings containing delimited values
     */
    public static String[] getSplitFromFile(String filename) {
        LOG.log(Level.FINE, MessageFormat.format("Reading file {0}", filename));
        List<String> read = FileUtil.readFile(filename, false);
        if (!read.isEmpty()) {
            LOG.log(Level.FINEST, MessageFormat.format("Read {0}", read.get(0)));
            return read.get(0).split("\\s+");
        }
        return new String[0];
    }

    /**
     * Read a file and return a map of string keys to string values contained
     * therein. Intended primarily for Linux /proc/[pid]/io
     *
     * @param filename
     *            The file to read
     * @param separator
     *            Characters in each line of the file that separate the key and
     *            the value
     * @return The map contained in the file, if any; otherwise empty map
     */
    public static Map<String, String> getKeyValueMapFromFile(String filename, String separator) {
        Map<String, String> map = new HashMap<>();
        LOG.log(Level.FINE, MessageFormat.format("Reading file {0}", filename));
        List<String> lines = FileUtil.readFile(filename, false);
        for (String line : lines) {
            String[] parts = line.split(separator);
            if (parts.length == 2) {
                map.put(parts[0], parts[1].trim());
            }
        }
        return map;
    }
}
