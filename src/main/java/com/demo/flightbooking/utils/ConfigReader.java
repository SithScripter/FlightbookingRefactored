package com.demo.flightbooking.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.demo.flightbooking.enums.EnvironmentType;

/**
 * A utility class to read configuration settings from the config.properties file.
 * It uses a static block to load the properties once, making it efficient.
 * This class centralizes all configuration management.
 */
public class ConfigReader {

    private static final Logger logger = LogManager.getLogger(ConfigReader.class);
    private static final Properties properties = new Properties();
    private static final String CONFIG_FILE = System.getProperty("configFile", "config/config.properties");
    
    /**
     * Static block to load the properties file when the class is initialized.
     * This ensures the properties are loaded only once during the test execution.
     */
    static {
        try (InputStream stream = ConfigReader.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (stream == null) {
                logger.error("Configuration file not found: {}", CONFIG_FILE);
                throw new RuntimeException("Configuration file not found: " + CONFIG_FILE);
            }
            properties.load(stream);
            logger.info("Configuration successfully loaded from: {}", CONFIG_FILE);
        } catch (IOException e) {
            logger.error("Failed to load configuration file: {}", CONFIG_FILE, e);
            throw new RuntimeException("Failed to load configuration", e);
        }
    }

    /**
     * Retrieves a property value by its key.
     *
     * @param key The key of the property to retrieve.
     * @return The property value as a String.
     */
    public static String getProperty(String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            logger.warn("Property not found: {}", key);
        }
        return value;
    }
    
    /**
     * Retrieves a property value and converts it to an integer.
     *
     * @param key The key of the property to retrieve.
     * @return The property value as an int.
     */
    public static int getPropertyAsInt(String key) {
        String value = getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                logger.error("Property '{}' value '{}' is not a valid integer.", key, value, e);
                return 0; // Return 0 or some default on format error
            }
        }
        return 0;
    }

    /**
     * Gets the application URL based on the 'env' system property (e.g., -Denv=QA).
     * Falls back to the default 'application.url' if 'env' is not specified.
     * @return The target application URL for the test run.
     */
    public static String getApplicationUrl() {
        String env = System.getProperty("env"); // Reads -Denv=QA from the command line
        if (env == null || env.trim().isEmpty()) {
            logger.info("No 'env' system property provided. Using default 'application.url'.");
            return getProperty("application.url");
        }

        EnvironmentType environmentType;
        try {
            environmentType = EnvironmentType.valueOf(env.toUpperCase().trim());
            logger.info("Running tests on environment: {}", environmentType);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid environment specified: '{}'. Please use one of: {}", env, (Object[]) EnvironmentType.values());
            throw new IllegalArgumentException("Invalid environment specified: " + env);
        }

        String propertyKey = environmentType.name().toLowerCase() + ".url";
        String url = getProperty(propertyKey);

        if (url == null || url.isEmpty()) {
            throw new RuntimeException("URL for environment '" + environmentType + "' not found in config.properties for key '" + propertyKey + "'");
        }
        return url;
    }
}