package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*;

/**
 *  A stub for ActivityCallback that does nothing except report error messages.
 */
public class ActivityCallbackStub implements ActivityCallback
{
    /**
     *  Handle a new connection.
     */
    public void connectionInitializing(Connection c) { }

    /**
     *  Mark a connection as initialized
     */
    public void connectionInitialized(Connection c) { }

    /**
     *  Handle a removed connection.
     */
    public void connectionClosed(Connection c) { }

    /**
     *  Add a known host/port
     */
    public void knownHost(Endpoint e) { }

    /**
     *  Add a query reply to a query screen
     */
    public void handleQueryReply( QueryReply qr ) { }

    /**
     *  Add a query string to the monitor screen
     */
    public void handleQueryString( String query ) { }

    /** Ask user if I should overwrite download file */
    public boolean overwriteFile(String file) { return false; }

    /** Add a file to the download window */
    public void addDownload(HTTPDownloader d) { }

    public void removeDownload(HTTPDownloader d) { }

    public void addUpload(HTTPUploader u) { }

    public void removeUpload(HTTPUploader u) { }

    /** Temp function until we rewrite HTTPManager for counting connections */
    public int getNumUploads() { return 0; }

    /** sets the port in the configuration window */
    public void setPort(int port) { }


    /**
     * Equivalent to error(errorCode, t).
     */
    public void error(int errorCode) { }

    /**
     * @requires errorCode is a number matching up with one of the
     * predefined error messages, e.g., "15" to show GUIStyles.E_15.
     *
     * @effects displays one the predefined error message
     *  corresponding to errorCode.  If t!=null, also displays the
     *  stack trace of t { } in this case, the associated error message
     *  should explain the stack trace to the user.
     */
    public void error(int errorCode, Throwable t) { 
        System.err.println("ACTIVITY CALLBACK REPORTS AN ERROR!");
        t.printStackTrace();
    }
}
