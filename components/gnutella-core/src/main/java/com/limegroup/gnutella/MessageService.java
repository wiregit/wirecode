package com.limegroup.gnutella;

import com.limegroup.gnutella.settings.BooleanSetting;

/**
 * Implementation of the <tt>MessageService</tt> interface for displaying 
 * messages to the user.
 */
public class MessageService {

    /**
     * Variable for the <tt>MessageCallback</tt> implementation to use for 
     * displaying messages.
     */
    private static MessageCallback _callback = new ShellMessageService();
    
    /**
     * Private constructor to ensure that this class cannot be instantiated.
     */
    private MessageService() {}

    /**
     * Sets the class to use for making callbacks to the user.
     * 
     * @param callback the <tt>MessageCallback</tt> instance to use
     */
    public static void setCallback(MessageCallback callback) {
        _callback = callback;
    }
    
    /**
     * Shows a locale-specific message to the user using the specified key to
     * look up the message in the resource bundles.
     * 
     * @param messageKey the key for looking up the message to display in the
     *  resource bundles
     */
    public static void showError(String messageKey) {
        _callback.showError(messageKey);  
    }

    /**
     * Shows a locale-specific message to the user using the specified key to
     * look up the message in the resource bundles if the BooleanSetting
     * indicates to do so.
     * 
     * @param messageKey the key for looking up the message to display in the
     *  resource bundles
     */
    public static void showError(String messageKey, BooleanSetting ignore) {
        _callback.showError(messageKey, ignore);
    }

    /**
     * Shows a locale-specific message to the user using the specified key to
     * look up the message in the resource bundles.  Also appends a second,
     * non-locale-specific string to this message, such as a file name.
     * 
     * @param messageKey the key for looking up the message to display in the
     *  resource bundles
     * @param message a non-locale-specific message that will be appended as-is
     *  to the message displayed to the user, such as a file name
     */
    public static void showError(String messageKey, String message) {
        _callback.showError(messageKey, message);
    }

    /**
     * Shows a locale-specific message to the user using the specified key to
     * look up the message in the resource bundles if the BooleanSetting
     * indicates to do so.  Also appends a second,
     * non-locale-specific string to this message, such as a file name.
     * 
     * @param messageKey the key for looking up the message to display in the
     *  resource bundles
     * @param message a non-locale-specific message that will be appended as-is
     *  to the message displayed to the user, such as a file name
     */
    public static void showError(String messageKey,
                                 String message,
                                 BooleanSetting ignore) {
        _callback.showError(messageKey, message, ignore);
    }

    /**
     * Shows a locale-specific message to the user using the specified key to
     * look up the message in the resource bundles.
     * 
     * @param messageKey the key for looking up the message to display in the
     *  resource bundles
     */
    public static void showMessage(String messageKey) {
        _callback.showMessage(messageKey);
    }

    /**
     * Shows a locale-specific message to the user using the specified key to
     * look up the message in the resource bundles if the BooleanSetting
     * indicates to do so.
     * 
     * @param messageKey the key for looking up the message to display in the
     *  resource bundles
     */
    public static void showMessage(String messageKey, BooleanSetting ignore) {
        _callback.showMessage(messageKey, ignore);
    }
    
    /**
     * Default messaging class that simply displays messages in the console.
     */
    private static final class ShellMessageService implements MessageCallback {

        // Inherit doc comment.
        public void showError(String messageKey) {
            System.out.println("error key: "+messageKey);
        }

        // Inherit doc domment.        
        public void showError(String messageKey, BooleanSetting ignore) {
            System.out.println("error key: "+messageKey);
        }

        // Inherit doc comment.
        public void showError(String messageKey, String message) {
            System.out.println("error key: "+messageKey+" extra message: "+
                message);
        }

        // Inherit doc comment.
        public void showError(String messageKey,
                              String message,
                              BooleanSetting ignore) {
            System.out.println("error key: "+messageKey+" extra message: "+
                message);
        }

        // Inherit doc comment.
        public void showMessage(String messageKey) {
            System.out.println("message key: "+messageKey); 
        }

        // Inherit doc comment.
        public void showMessage(String messageKey, BooleanSetting ignore) {
            System.out.println("message key: "+messageKey); 
        }
        
    }

}
