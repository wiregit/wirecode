/*
 * This is an ActivityCallback which connects to a NetworkServerCallbackStub 
 * and notifies it of various events.
 * 
 * HOW TO USE:
 * 
 * public class MyNetworkClientCallbackStub extends NetworkClientCallbackStub {
 * 
 *      //if you want to use "removeUpload" :
 * 
 * 	     public removeUpload(Uploader parameter) {
 * 			super.removeUpload(parameter); //<--do not forget this
 * 	        ...
 * 			//serialize your parameters here
 * 	        ...
 * 	     }
 * 
 * then do the same at the server end.
 * 
 * DO NOT add constructors with different parameters as this class uses
 * reflection in order to get passed to a different jvm.
 */
package com.limegroup.gnutella.stubs;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.chat.Chatter;
import com.limegroup.gnutella.search.HostData;
import com.limegroup.gnutella.security.User;
import com.sun.java.util.collections.Set;

import java.net.*;
import java.io.*;

public class NetworkClientCallbackStub implements ActivityCallback {
	
	
	//some constants for each method.  ugly but works
	public static final int acceptChat =0;
	public static final int addDownload =1;
	public static final int addSharedDirectory =2;
	public static final int addSharedFile = 3;
	public static final int addUpload =4;
	public static final int browseHostFailed = 5;
	public static final int chatErrorMessage = 6;
	public static final int chatUnavailable = 7;
	public static final int clearSharedFiles =8;
	public static final int connectionClosed =9;
	public static final int connectionInitialized=10;
	public static final int connectionInitializing=11;
	public static final int downloadsComplete=12;
	public static final int fileManagerLoaded=13;
	public static final int getHostValue=14;
	public static final int getUserAuthenticationInfo =15;
	public static final int handleQueryResult=16;
	public static final int handleQueryString=17;
	public static final int handleSharedFileUpdate=18;
	public static final int indicateNewVersion=19;
	public static final int isQueryAlive=20;
	public static final int notifyUserAboutUpdate=21;
	public static final int promptAboutCorruptDownload=22;
	public static final int receiveMessage=23;
	public static final int removeDownload=24;
	public static final int removeUpload=25;
	public static final int restoreApplication=26;
	public static final int setAnnotateEnabled=27;
	public static final int showDownloads=28;
	public static final int uploadsComplete=29;
	
	
	private final OutputStream _os;
	/**
	 * creates a client callback and connects it to the server callback.
	 * @param host the host the server is listening on
	 * @param port the port the server is listening on
	 * @throws IOException something went wrong.
	 */
	public NetworkClientCallbackStub(String host, int port) throws IOException {
		Socket socket = new Socket(host,port);
		OutputStream os = socket.getOutputStream();
		_os = os;
	}
	
	
	private void write(int code) {
		try {
			_os.write(code);
		}catch(IOException heh) {
			//since this is intended for testing purposes,
			//I will not hesitate to use stderr
			heh.printStackTrace();
		}
	}
	/**
	 * creates a client callback and connects it to a server callback on the local machine
	 * @param port the port on the local machine the server callback is listening on.
	 * @throws IOException something went wrong.
	 */
	public NetworkClientCallbackStub(int port) throws IOException {
		this("127.0.0.1",port);
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.ActivityCallback#acceptChat(com.limegroup.gnutella.chat.Chatter)
	 */
	public void acceptChat(Chatter ctr) {
		write(acceptChat);
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.ActivityCallback#addDownload(com.limegroup.gnutella.Downloader)
	 */
	public void addDownload(Downloader d) {
		write(addDownload);
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.ActivityCallback#addSharedDirectory(java.io.File, java.io.File)
	 */
	public void addSharedDirectory(File directory, File parent) {
		write(addSharedDirectory);
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.ActivityCallback#addSharedFile(com.limegroup.gnutella.FileDesc, java.io.File)
	 */
	public void addSharedFile(FileDesc file, File parent) {
		write(addSharedFile);
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.ActivityCallback#addUpload(com.limegroup.gnutella.Uploader)
	 */
	public void addUpload(Uploader u) {
		write(addUpload);
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.ActivityCallback#browseHostFailed(com.limegroup.gnutella.GUID)
	 */
	public void browseHostFailed(GUID guid) {
		write(browseHostFailed);
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.ActivityCallback#chatErrorMessage(com.limegroup.gnutella.chat.Chatter, java.lang.String)
	 */
	public void chatErrorMessage(Chatter chatter, String str) {
		write(chatErrorMessage);
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.ActivityCallback#chatUnavailable(com.limegroup.gnutella.chat.Chatter)
	 */
	public void chatUnavailable(Chatter chatter) {
		write(chatUnavailable);
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.ActivityCallback#clearSharedFiles()
	 */
	public void clearSharedFiles() {
		write(clearSharedFiles);
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.ActivityCallback#connectionClosed(com.limegroup.gnutella.Connection)
	 */
	public void connectionClosed(Connection c) {
		write(connectionClosed);
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.ActivityCallback#connectionInitialized(com.limegroup.gnutella.Connection)
	 */
	public void connectionInitialized(Connection c) {
		write(connectionInitialized);
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.ActivityCallback#connectionInitializing(com.limegroup.gnutella.Connection)
	 */
	public void connectionInitializing(Connection c) {
		write(connectionInitializing);
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.ActivityCallback#downloadsComplete()
	 */
	public void downloadsComplete() {
		write(downloadsComplete);
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.ActivityCallback#fileManagerLoaded()
	 */
	public void fileManagerLoaded() {
		write(fileManagerLoaded);
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.ActivityCallback#getHostValue(java.lang.String)
	 */
	public String getHostValue(String key) {
		return null;
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.ActivityCallback#getUserAuthenticationInfo(java.lang.String)
	 */
	public User getUserAuthenticationInfo(String host) {
		return null;
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.ActivityCallback#handleQueryResult(com.limegroup.gnutella.RemoteFileDesc, com.limegroup.gnutella.search.HostData, com.sun.java.util.collections.Set)
	 */
	public void handleQueryResult(RemoteFileDesc rfd, HostData data, Set locs) {
		write(handleQueryResult);
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.ActivityCallback#handleQueryString(java.lang.String)
	 */
	public void handleQueryString(String query) {
		write(handleQueryString);
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.ActivityCallback#handleSharedFileUpdate(java.io.File)
	 */
	public void handleSharedFileUpdate(File file) {
		write(handleSharedFileUpdate);
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.ActivityCallback#indicateNewVersion()
	 */
	public void indicateNewVersion() {
		write(indicateNewVersion);
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.ActivityCallback#isQueryAlive(com.limegroup.gnutella.GUID)
	 */
	public boolean isQueryAlive(GUID guid) {
		return false;
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.ActivityCallback#notifyUserAboutUpdate(java.lang.String, boolean, boolean)
	 */
	public void notifyUserAboutUpdate(String message, boolean isPro, boolean l) {
		write(notifyUserAboutUpdate);
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.ActivityCallback#promptAboutCorruptDownload(com.limegroup.gnutella.Downloader)
	 */
	public void promptAboutCorruptDownload(Downloader dloader) {
		write(promptAboutCorruptDownload);
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.ActivityCallback#receiveMessage(com.limegroup.gnutella.chat.Chatter)
	 */
	public void receiveMessage(Chatter chr) {
		write(receiveMessage);
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.ActivityCallback#removeDownload(com.limegroup.gnutella.Downloader)
	 */
	public void removeDownload(Downloader d) {
		write(removeDownload);
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.ActivityCallback#removeUpload(com.limegroup.gnutella.Uploader)
	 */
	public void removeUpload(Uploader u) {
		write(removeUpload);
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.ActivityCallback#restoreApplication()
	 */
	public void restoreApplication() {
		write(restoreApplication);
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.ActivityCallback#setAnnotateEnabled(boolean)
	 */
	public void setAnnotateEnabled(boolean enabled) {
		write(setAnnotateEnabled);
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.ActivityCallback#showDownloads()
	 */
	public void showDownloads() {
		write(showDownloads);
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.ActivityCallback#uploadsComplete()
	 */
	public void uploadsComplete() {
		write(uploadsComplete);
	}
}
