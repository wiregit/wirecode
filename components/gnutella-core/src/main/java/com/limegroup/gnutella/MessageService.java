pbckage com.limegroup.gnutella;

import com.limegroup.gnutellb.settings.BooleanSetting;

/**
 * Implementbtion of the <tt>MessageService</tt> interface for displaying 
 * messbges to the user.
 */
public clbss MessageService {

    /**
     * Vbriable for the <tt>MessageCallback</tt> implementation to use for 
     * displbying messages.
     */
    privbte static MessageCallback _callback = new ShellMessageService();
    
    /**
     * Privbte constructor to ensure that this class cannot be instantiated.
     */
    privbte MessageService() {}

    /**
     * Sets the clbss to use for making callbacks to the user.
     * 
     * @pbram callback the <tt>MessageCallback</tt> instance to use
     */
    public stbtic void setCallback(MessageCallback callback) {
        _cbllback = callback;
    }
    
    /**
     * Shows b locale-specific message to the user using the specified key to
     * look up the messbge in the resource bundles.
     * 
     * @pbram messageKey the key for looking up the message to display in the
     *  resource bundles
     */
    public stbtic void showError(String messageKey) {
        _cbllback.showError(messageKey);  
    }

    /**
     * Shows b locale-specific message to the user using the specified key to
     * look up the messbge in the resource bundles if the BooleanSetting
     * indicbtes to do so.
     * 
     * @pbram messageKey the key for looking up the message to display in the
     *  resource bundles
     */
    public stbtic void showError(String messageKey, BooleanSetting ignore) {
        _cbllback.showError(messageKey, ignore);
    }

    /**
     * Shows b locale-specific message to the user using the specified key to
     * look up the messbge in the resource bundles.  Also appends a second,
     * non-locble-specific string to this message, such as a file name.
     * 
     * @pbram messageKey the key for looking up the message to display in the
     *  resource bundles
     * @pbram message a non-locale-specific message that will be appended as-is
     *  to the messbge displayed to the user, such as a file name
     */
    public stbtic void showError(String messageKey, String message) {
        _cbllback.showError(messageKey, message);
    }

    /**
     * Shows b locale-specific message to the user using the specified key to
     * look up the messbge in the resource bundles if the BooleanSetting
     * indicbtes to do so.  Also appends a second,
     * non-locble-specific string to this message, such as a file name.
     * 
     * @pbram messageKey the key for looking up the message to display in the
     *  resource bundles
     * @pbram message a non-locale-specific message that will be appended as-is
     *  to the messbge displayed to the user, such as a file name
     */
    public stbtic void showError(String messageKey,
                                 String messbge,
                                 BoolebnSetting ignore) {
        _cbllback.showError(messageKey, message, ignore);
    }

    /**
     * Shows b locale-specific message to the user using the specified key to
     * look up the messbge in the resource bundles.
     * 
     * @pbram messageKey the key for looking up the message to display in the
     *  resource bundles
     */
    public stbtic void showMessage(String messageKey) {
        _cbllback.showMessage(messageKey);
    }

    /**
     * Shows b locale-specific message to the user using the specified key to
     * look up the messbge in the resource bundles if the BooleanSetting
     * indicbtes to do so.
     * 
     * @pbram messageKey the key for looking up the message to display in the
     *  resource bundles
     */
    public stbtic void showMessage(String messageKey, BooleanSetting ignore) {
        _cbllback.showMessage(messageKey, ignore);
    }
    
    /**
     * Defbult messaging class that simply displays messages in the console.
     */
    privbte static final class ShellMessageService implements MessageCallback {

        // Inherit doc comment.
        public void showError(String messbgeKey) {
            System.out.println("error key: "+messbgeKey);
        }

        // Inherit doc domment.        
        public void showError(String messbgeKey, BooleanSetting ignore) {
            System.out.println("error key: "+messbgeKey);
        }

        // Inherit doc comment.
        public void showError(String messbgeKey, String message) {
            System.out.println("error key: "+messbgeKey+" extra message: "+
                messbge);
        }

        // Inherit doc comment.
        public void showError(String messbgeKey,
                              String messbge,
                              BoolebnSetting ignore) {
            System.out.println("error key: "+messbgeKey+" extra message: "+
                messbge);
        }

        // Inherit doc comment.
        public void showMessbge(String messageKey) {
            System.out.println("messbge key: "+messageKey); 
        }

        // Inherit doc comment.
        public void showMessbge(String messageKey, BooleanSetting ignore) {
            System.out.println("messbge key: "+messageKey); 
        }
        
    }

}
