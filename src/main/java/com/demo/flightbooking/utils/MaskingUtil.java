package com.demo.flightbooking.utils;

/**
 * Utility class for masking sensitive data to prevent exposure in logs and reports.
 * This is critical for compliance with regulations like PCI DSS and GDPR.
 * The class is final to prevent extension, as it only contains static methods.
 */
public final class MaskingUtil {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private MaskingUtil() {}

    /**
     * Masks a credit card number, showing only the last 4 digits. The mask
     * character '*' is repeated to match the original length of the number.
     * It handles nulls and cleans non-digit characters before masking.
     * Example: "4111-1111 1111-1111" becomes "************4444"
     *
     * @param cardNumber The full credit card number, possibly with separators.
     * @return A masked card number, or null if the input is null.
     */
    public static String maskCardNumber(String cardNumber) {
        if (cardNumber == null) {
            return null;
        }

        // Normalize the input by removing all non-digit characters
        String digitsOnly = cardNumber.replaceAll("\\D", "");
        int len = digitsOnly.length();

        if (len <= 4) {
            // Return a generic mask for very short or invalid numbers
            return "************";
        }

        // Create a dynamic-length mask
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len - 4; i++) {
            sb.append('*');
        }
        sb.append(digitsOnly.substring(len - 4));

        return sb.toString();
    }

    /**
     * Masks any sensitive value for safe logging, showing only the last 4
     * characters.
     * Unlike {@link #maskCardNumber(String)}, this does NOT strip non-digit
     * characters,
     * making it suitable for passwords, CVVs, and any arbitrary sensitive text.
     * Example: "5555666677778888" becomes "************8888"
     * Example: "MyP@ssw0rd!" becomes "*******0rd!"
     *
     * @param value The sensitive value to mask.
     * @return A masked version of the value, or "****" if null/short.
     */
    public static String maskSensitiveValue(String value) {
        if (value == null || value.length() <= 4) {
            return "****";
        }
        return "*".repeat(value.length() - 4) + value.substring(value.length() - 4);
    }
}
