package com.limegroup.gnutella;

/**
 * Interface for displaying messages to the user.
 */
public interface MessageCallback {

    /**
     * Displays an error to the user based on the provided message key.  This
     * appends the locale-specific string with another non-locale-specific
     * string, such as a file name.
     * 
     * @param messageKey the key for the locale-specific message to display
     */
    void showError(String messageKey);
    
    /**
     * Displays an error to the user based on the provided message key.  This
     * appends the locale-specific string with another non-locale-specific
     * string, such as a file name.
     * 
     * @param messageKey the key for the locale-specific message to display
     * @param message the string to append to the locale-specific message, such
     *  as a file name
     */
    void showError(String messageKey, String message);
    
    /**
     * Shows a locale-specific message to the user using the given message key.
     * 
     * @param messageKey the key for looking up the locale-specific message
     *  in the resource bundles
     */
    void showMessage(String messageKey);
}
