package com.limegroup.gnutella;

import com.limegroup.gnutella.settings.BooleanSetting;

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
     * The message is only displayed if the BooleanSetting indicates the user
     * has chosen to display the message.
     * 
     * @param messageKey the key for the locale-specific message to display
     * @param ignore the BooleanSetting that stores whether or not the user
     *        has chosen to receive future warnings of this message.
     */
    void showError(String messageKey, BooleanSetting ignore);
    
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
     * Displays an error to the user based on the provided message key.  This
     * appends the locale-specific string with another non-locale-specific
     * string, such as a file name.
     * The message is only displayed if the BooleanSetting indicates the user
     * has chosen to display the message.
     * 
     * @param messageKey the key for the locale-specific message to display
     * @param message the string to append to the locale-specific message, such
     *  as a file name
     */
    void showError(String messageKey, String message, BooleanSetting ignore);
    
    /**
     * Shows a locale-specific message to the user using the given message key.
     * 
     * @param messageKey the key for looking up the locale-specific message
     *  in the resource bundles
     */
    void showMessage(String messageKey);
    
    /**
     * Shows a locale-specific message to the user using the given message key.
     * The message is only displayed if the BooleanSetting indicates the user
     * has chosen to dispaly the message.
     * 
     * @param messageKey the key for looking up the locale-specific message
     *  in the resource bundles
     */
    void showMessage(String messageKey, BooleanSetting ignore);
}
