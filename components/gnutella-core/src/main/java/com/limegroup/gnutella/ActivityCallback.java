package com.limegroup.gnutella;

import java.io.File;
import com.limegroup.gnutella.chat.*;
import com.limegroup.gnutella.security.User;
import com.sun.java.util.collections.*;

/**
 *  Callback to notify the GUI of asynchronous backend events.
 *  The methods in this fall into the following categories:
 *
 *  <ul>
 *  <li>Query replies (for displaying results) and query strings 
 *     (for the monitor)
 *  <li>Update in shared file statistics
 *  <li>Change of connection state
 *  <li>New or dead uploads or downloads
 *  <li>New chat requests and chat messages
 *  <li>Error messages
 *  </ul>
 */
public interface ActivityCallback
{
    /**
     *  Add a new unitialized connection.
     */
    public void connectionInitializing(Connection c);

    /**
     *  Mark a connection as initialized
     */
    public void connectionInitialized(Connection c);

    /**
     *  Mark a connection as closed
     */
    public void connectionClosed(Connection c);

    /**
     *  Add the given host/port to the host catcher
     */
    public void knownHost(Endpoint e);

    /**
     * Add a query reply to the search result screen. 
     *
     * @param qr a query reply with the same GUID as a query you initiated.
     *  The reply is not guaranteed to match the query; use 
     *  RouterService.matchesType for that.  Also, it's possible that the
     *  user has cancelled the search.
     * @see RouterService#query(String,int,MediaType)
     * @see RouterService#matchesType(byte[],Response)
     */
    public void handleQueryReply( QueryReply qr );

    /**
     * Add a query string to the monitor screen
     */
    public void handleQueryString( String query );

    /** Add a file to the download window */
    public void addDownload(Downloader d);

    /** Remove a downloader from the download window. */
    public void removeDownload(Downloader d);

    /** Add an uploader to the upload window */
    public void addUpload(Uploader u);

    /** Remove an uploader from the upload window. */
    public void removeUpload(Uploader u);    

	/** Add a new incoming chat connection */
	public void acceptChat(Chatter ctr);

    /** A new message is available from the given chatter */
	public void receiveMessage(Chatter chr);

	/** The given chatter is no longer available */
	public void chatUnavailable(Chatter chatter);

	/** display an error message in the chat gui */
	public void chatErrorMessage(Chatter chatter, String str);

    /** display an error message since the browse host failed. 
     *  @param guid The GUID of the browse host.
     */    
    public void browseHostFailed(GUID guid);

    /**
     * A new version of LimeWire is available, notify user.
     */
    public void notifyUserAboutUpdate(String version);


    /**
     * Notifies the GUI that the given directory has been shared.  This method
     * is called exactly once per directory per change to the shared directory
     * and extension settings.  Note that the files in directory are not
     * necessarily yet indexed at the time of this call.
     *
     * @param directory MUST be a directory
     * @param parent MUST be the parent of directory, or null if no parent
     *  exists. 
     */
    public void addSharedDirectory(final File directory, final File parent);

    /**
     * Notifies the GUI that the given file has been shared.  This method is
     * called exactly once per file per change to the shared directory and
     * extension settings.
     *
     * @param file MUST be a file descriptor for the file.
     * @param parent MUST be the parent of directory, or null if no parent
     *  exists. 
     */
    public void addSharedFile(final FileDesc file, final File parent);

    /**
     * Notifies the GUI that the given shared file has new information.
     *
     * @param file The File that needs updating
     */    
    public void handleSharedFileUpdate(File file);

	/**
	 * The list of shared files has been emptied.
	 */
	public void clearSharedFiles();           


	/**
	 * Sets the enabled/disabled state of file annotation.
	 */
	public void setAnnotateEnabled(boolean enabled);

     /** 
      * Notifies the GUI that all active downloads have been completed.
      */   
    public void downloadsComplete();
    
    /** 
     * Notifies the GUI that all active uploads have been completed.
     */  
    public void uploadsComplete();

    /**
     * An error has occured.
     * 
     * @param errorCode  the error message to diaplay.  MUST be one
     *  of PORT_ERROR, SOCKET_ERROR, ASSERT_ERROR, or INTERNAL_ERROR.
     */
    public void error(int errorCode);

    /**
	 * An internal error has occured.
	 *
     * @param errorCode  the error message to diaplay.  MUST be one
     *  of PORT_ERROR, SOCKET_ERROR, ASSERT_ERROR, or INTERNAL_ERROR.
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

    //authentication callbacks
    /**
     * Asks user to authenticate, and returns the information received from
     * user
     * @param host The host who is requesting authentication
     * @return The authentication information input by user
     */
    public User getUserAuthenticationInfo(String host);
    
    /**
     * Shows the user a message informing her that a file being downloaded 
     * is corrupt.
     * <p>
     * This method MUST call dloader.discardCorruptDownload(boolean b) 
     * otherwise there will be threads piling up waiting for a notification
     */
    public void promptAboutCorruptDownload(Downloader dloader);

    public String getHostValue(String key);

    /** Could not establish a listening port. */
	public static final int PORT_ERROR = 10;

    /** Security error accepting incoming connection.  (Should never happen.) */
	public static final int SOCKET_ERROR = 11;

    /** Internal error. */
	public static final int INTERNAL_ERROR = 12;

    /** 
	 * Internal error caused by an assertion failure.
	 */
	public static final int ASSERT_ERROR = 13;

}
