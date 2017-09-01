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
package oshi.json.util;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import oshi.util.ParseUtil;

/**
 * Convenience methods for manipulating properties files for JSON use
 *
 * @author widdis[at]gmail[dot]com
 */
public class PropertiesUtil {
    private static final Logger LOG = Logger.getLogger(PropertiesUtil.class.getName());

    private PropertiesUtil() {
    }

    /**
     * Loads Java Properties from a file on the class path
     *
     * @param propertiesFile
     *            File name
     * @return A Properties object from the loaded file
     */
    public static Properties loadProperties(String propertiesFile) {
        Properties props = new Properties();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            return props;
        }
        try (InputStream input = loader.getResourceAsStream(propertiesFile)) {
            if (input == null) {
                LOG.log(Level.SEVERE, MessageFormat.format("No properties file {0} on the classpath.", propertiesFile));
                return props;
            }
            props.load(input);
            LOG.log(Level.FINE, MessageFormat.format("Loaded properties: {0}", props));
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, MessageFormat.format("Error reading properties file {0}. {1}", propertiesFile, ex));
        }
        return props;
    }

    /**
     * Parses the (string) value of a property and determines whether it is true
     *
     * @param properties
     *            The properties to search
     * @param property
     *            The property to evaluate
     * @return False if the property is set to "false"; true otherwise
     */
    public static boolean getBoolean(Properties properties, String property) {
        return !"false".equalsIgnoreCase(properties.getProperty(property, "true"));
    }

    /**
     * Parses the (string) value of a property and determines its integer value
     *
     * @param properties
     *            The properties to search
     * @param property
     *            The property to evaluate
     * @param defaultInt
     *            The default if the property is missing
     * @return The integer value if parseable, or the default, otherwise
     */
    public static int getIntOrDefault(Properties properties, String property, int defaultInt) {
        return ParseUtil.parseIntOrDefault(properties.getProperty(property), defaultInt);
    }

    /**
     * Parses the (string) value of a property and determines its string value
     *
     * @param properties
     *            The properties to search
     * @param property
     *            The property to evaluate
     * @return The string value if parseable, or the string "null", otherwise
     */
    public static String getString(Properties properties, String property) {
        String s = properties.getProperty(property);
        return s == null ? "null" : s;
    }

    /**
     * Parses the (string) value of a property and determines its enum value
     *
     * @param properties
     *            The properties to search
     * @param property
     *            The property to evaluate
     * @param enumClass
     *            The class of enum to return
     * @param <T>
     *            An enum type
     * @return The enum value if parseable, or null, otherwise
     */
    public static <T extends Enum<T>> T getEnum(Properties properties, String property, Class<T> enumClass) {
        String s = properties.getProperty(property);
        if (enumClass != null && s != null) {
            try {
                return Enum.valueOf(enumClass, s.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                LOG.log(Level.SEVERE, MessageFormat.format("Property value {0} from property {1} " +
                                "does not match enum class {2}. {3}", s, property,
                        enumClass.getName(), ex));
            }
        }
        return null;
    }
}
