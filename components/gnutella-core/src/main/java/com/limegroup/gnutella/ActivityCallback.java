package com.limegroup.gnutella;

import java.io.File;
import com.limegroup.gnutella.chat.*;

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

	/** handle adding a chat, and messages to the chats */
	public void acceptChat(Chatter ctr);

	/** removes a chat from the gui */
	public void removeChat(Chatter ctr);

	public void recieveMessage(Chatter chr);

	/** lets the user know that a host is no longer available */
	public void chatUnavailable(Chatter chatter);

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
	 * Nofifies the GUI that the shared files should be visually removed.
	 */
	public void clearSharedFiles();           

    /**
     * Equivalent to error(errorCode, t).
     */
    public void error(int errorCode);

    /**
	 * Displays an error message to the user.
	 *
	 * @param errorCode  The int specifying the error message to diaplay
	 *
	 * @param t          The <code>Throwable</code> instance containing the
	 *                   stack trace to display
     */
    public void error(int errorCode, Throwable t);

    /**
	 * Displays an error stack trace to the user with a generic message.
	 *
	 * @param t  The <code>Throwable</code> instance containing the
	 *           stack trace to display
     */
    public void error(Throwable t);


    /** error on port */
	public static final int PORT_ERROR = 0;

    /** error on listening to socket*/
	public static final int SOCKET_ERROR = 1;

    /** internal error */
	public static final int INTERNAL_ERROR = 2;

    /** 
	 * Error that caused an assertion failure.
	 */
	public static final int ASSERT_ERROR = 3;

}
