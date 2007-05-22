package org.limewire.store.storeserver.core;

/**
 * A general place for constants.
 * 
 * @author jpalm
 */
public class Constants {

    /**
     * Various levels for messages.
     */
    public enum Level {
        MESSAGE, WARNING, ERROR, FATAL,
    }

    /**
     * The length of the public and private keys generated.
     */
    public static final int KEY_LENGTH = 10; // TODO

    public final static String NEWLINE = "\r\n";

    /**
     * The quote used to surround callbacks.  We need to escape this in the
     * strings that we pass back to the callback.
     */
    public static final char CALLBACK_QUOTE = '\'';

    /**
     * The String version of {@link #CALLBACK_QUOTE}.
     */
    public static final String CALLBACK_QUOTE_STRING = String.valueOf(CALLBACK_QUOTE);

    /**
     * The callback in which error messages are wrapped.
     */
    public static final String ERROR_CALLBACK = "error";

    /**
     * The string that separates arguments in the {@link Parameters#MSG} argument
     * when the command {@link Parameters#COMMAND} parameter is <tt>Msg</tt>.  This is
     * the urlencoded version of <tt>&</tt>, which is <tt>%26</tt>. 
     */
    public static final String ARGUMENT_SEPARATOR = "%26";

}