package com.limegroup.gnutella;

import java.io.File;

/**
 *  Interface for connection events to be fed from the router into the
 *  visual world.
 */
public interface ActivityCallback
{
    /**
     *  Handle a new connection.
     */
    public void connectionInitializing(Connection c);

    /**
     *  Mark a connection as initialized
     */
    public void connectionInitialized(Connection c);

    /**
     *  Handle a removed connection.
     */
    public void connectionClosed(Connection c);

    /**
     *  Add a known host/port
     */
    public void knownHost(Endpoint e);

    /**
     *  Add a query reply to a query screen
     */
    public void handleQueryReply( QueryReply qr );

    /**
     *  Add a query string to the monitor screen
     */
    public void handleQueryString( String query );

    /** Add a file to the download window */
    public void addDownload(Downloader d);

    public void removeDownload(Downloader d);

    public void addUpload(Uploader u);

    public void removeUpload(Uploader u);

    /** Temp function until we rewrite HTTPManager for counting connections */
    public int getNumUploads();
    

    /**
     * Notifies the GUI that the given directory has been shared.  This method
     * is called exactly once per directory per change to the shared directory
     * and extension settings.  Note that the files in directory are not
     * necessarily yet indexed at the time of this call.
     *
     * @requires "directory" is a directory and "parent" is the parent
	 *  directory of that directory, or null if no parent exists. 
     */
    public void addSharedDirectory(final File directory, final File parent);

    /**
     * Notifies the GUI that the given file has been shared.  This method is
     * called exactly once per file per change to the shared directory and
     * extension settings.
     *
     * @requires f is a file, addSharedDirectory has been called with parent 
     *  as an argument, and parent contains f 
     */
    public void addSharedFile(final File file, final File parent);
           

    /**
     * Equivalent to error(errorCode, t).
     */
    public void error(int errorCode);

    /**
     * @requires errorCode is a number matching up with one of the
     * predefined error messages, e.g., "15" to show GUIStyles.E_15.
     *
     * @effects displays one the predefined error message
     *  corresponding to errorCode.  If t!=null, also displays the
     *  stack trace of t; in this case, the associated error message
     *  should explain the stack trace to the user.
     */
    public void error(int errorCode, Throwable t);


    /** Constants for the various errors
     *  See GUIStyles.java for the
     *  associated messages */

    //maybe use this array instead?  we'll see
    public static final int[] ERROR_MESSAGES = {0,1,2,3,4,
                                                5,6,7,8,9,
                                                10,11,12};

    /** list of errors reported in the GUI
     *  the associated strings for these
     *  errors can be found in the GUIStyles
     *  interface */
    /** error on port */
    public static final int ERROR_0  = 0;
    /** error on statistics gatherer start */
    public static final int ERROR_1  = 1;
    /** error while accepting incoming connections */
    public static final int ERROR_2  = 2;
    /** error on listening to socket*/
    public static final int ERROR_3  = 3;
    /** error on opening an inpute stream */
    public static final int ERROR_4  = 4;
    /** error on a bad url */
    public static final int ERROR_5  = 5;
    /** error opening connection */
    public static final int ERROR_6  = 6;
    /** error sending push request */
    public static final int ERROR_7  = 7;
    /** error writing to file */
    public static final int ERROR_8  = 8;
    /** error reading header information */
    public static final int ERROR_9  = 9;
    /** error reading gnutella.net file */
    public static final int ERROR_10 = 10;
    /** error creating file for statistics logs */
    public static final int ERROR_11 = 11;
    /** error writing data to log */
    public static final int ERROR_12 = 12;
    /** error finding the router host */
    public static final int ERROR_13 = 13;
    /** internal error */
    public static final int ERROR_20 = 20;

}
