/*
 * Listens for events relayed from a NetworkClientCallbackStub.
 * 
 * ===========================================================================
 * HOW TO USE:
 * 
 * public class MyNetworkServerCallbackStub extends NetworkServerCallbackStub {
 * 
 * 		//if you want to use removeUpload first override
 * 		protected parseRemoveUpload() {
 * 			..
 * 			//parse the parameter from network
 * 			Uploader parameter = mySecretParseMethod();
 * 			
 * 			//then if you want call the overriden method
 * 			removeUpload(parameter);
 * 		}
 * 
 * 		//you may want to override this too
 * 		public removeUpload(Uploader uploader) {
 * 			...
 * 			//your code here
 * 			...
 * 		}
 * 
 * ==============================================================================
 * If you are just interested in waiting for notifications, you don't need to 
 * override anything:
 * 
 * 
 * public class MyTestCase extends ... {
 *
 * 		//have an instance 
 * 		NetworkServerCallbackStub callback = new NetworkServerCallbackStub(...);
 * 
 * 		/**
 *       * some test case which needs to wait for removeUpload to get called
 *       *
 * 		public void testSomething() throws Exception {
 * 			...
 * 			synchronized(callback._removeUpload) {
 * 				callback._removeUpload.wait();  //or wait(timeout);
 * 			}
 * 			...
 * 			//this code won't get executed until the callback receives the notification 
 *			//for removeUpload.
 *		} 
 *
 * 
 * 
 */

package com.limegroup.gnutella.stubs;


import java.net.*;
import java.io.*;

public class NetworkServerCallbackStub extends ActivityCallbackStub {
	
	private final ServerSocket _ss;
	private Socket _s;
	private final Thread _listener;
	
	
	//locks for various events.
	public final Object _acceptChat, _addDownload, _addSharedDirectory, _addSharedFile,_addUpload,
		_browseHostFailed, _chatErrorMessage, _chatUnavailable, _clearSharedFiles,
		_connectionClosed, _connectionInitialized, _connectionInitializing, _downloadsComplete,
		_fileManagerLoaded, _handleQueryResult, _handleQueryString, _handleSharedFileUpdate,
		_indicateNewVersion, _notifyUserAboutUpdate, _promptAboutCorruptDownload, _receiveMessage,
		_removeUpload, _removeDownload, _restoreApplication, _setAnnotateEnabled, _showDownloads,
		_uploadsComplete;
	
	/**
	 * starts listening for the client callback on the local machine
	 * @param port the port to start listening on.
	 */
	public NetworkServerCallbackStub(int port) throws IOException{
		_ss = new ServerSocket(port);
		_listener = new Thread() {
			public void run() {
				try{
					_s = _ss.accept();
				}catch(IOException ohWell) {}
			}
		};
		
		_listener.setDaemon(true);
		_listener.start();
		
		//create the various locks
		_acceptChat = new Object();
		_addDownload = new Object();
		_addSharedDirectory = new Object();
		_addSharedFile = new Object();
		_addUpload = new Object();
		_browseHostFailed = new Object();
		_chatErrorMessage = new Object();
		_chatUnavailable = new Object();
		_clearSharedFiles = new Object();
		_connectionClosed= new Object();
		_connectionInitialized = new Object();
		_connectionInitializing = new Object();
		_downloadsComplete = new Object();
		_fileManagerLoaded = new Object();
		_handleQueryResult = new Object();
		_handleQueryString = new Object();
		_handleSharedFileUpdate = new Object();
		_indicateNewVersion = new Object();
		_notifyUserAboutUpdate = new Object();
		_promptAboutCorruptDownload = new Object();
		_receiveMessage = new Object();
		_removeDownload = new Object();
		_removeUpload = new Object();
		_restoreApplication = new Object();
		_setAnnotateEnabled = new Object();
		_showDownloads = new Object();
		_uploadsComplete = new Object();
	}
	
	/**
	 * make sure the listening and parsing/notifying threads are stopped.
	 */
	protected void finalize() throws Throwable {
		//interruptible SocketChannel's are a 1.4 feature, so... 
		_ss.close();
		_s.close();
		super.finalize();
	}
	
	/**
	 * 
	 * @return whether a client callback is connected.
	 */
	public boolean isConnected() {
		return _s!=null;
	}
	
	private class NotificationDispatcher extends Thread {
		private final Socket _socket;
		
		public NotificationDispatcher(Socket s) {
			_socket =s;
		}
		
		public void run() {
			
			try {
				InputStream in = _socket.getInputStream();
				
				while(true) {
					int code = in.read();
					dispatch(code);
				}
			}catch (IOException die) {}//goodbye
		}
		
		public void dispatch(int code){
			switch(code) {
				case NetworkClientCallbackStub.acceptChat : 
					notiffy(_acceptChat);
					parseAcceptChat();break;
				case NetworkClientCallbackStub.addDownload :
					notiffy(_addDownload);
					parseAddDownload();break;
				case NetworkClientCallbackStub.addSharedDirectory : 
					notiffy(_addSharedDirectory);
					parseAddSharedDirectory();break;
				case NetworkClientCallbackStub.addSharedFile : 
					notiffy(_addSharedFile);
					parseAddSharedFile();break;
				case NetworkClientCallbackStub.addUpload : 
					notiffy(_addUpload);
					parseAddUpload();break;
				case NetworkClientCallbackStub.browseHostFailed : 
					notiffy(_browseHostFailed);
					parseBrowseHostFailed();break;
				case NetworkClientCallbackStub.chatErrorMessage : 
					notiffy(_chatErrorMessage);
					parseChatErrorMessage();break;
				case NetworkClientCallbackStub.chatUnavailable : 
					notiffy(_chatUnavailable);
					parseChatUnavailable() ;break;
				case NetworkClientCallbackStub.clearSharedFiles : 
					notiffy(_clearSharedFiles);
					parseClearSharedFiles(); break;
				case NetworkClientCallbackStub.connectionClosed : 
					notiffy(_connectionClosed);
					parseConnectionClosed(); break;
				case NetworkClientCallbackStub.connectionInitialized : 
					notiffy(_connectionInitialized);
					parseConnectionInitialized(); break;
				case NetworkClientCallbackStub.connectionInitializing : 
					notiffy(_connectionInitializing);
					parseConnectionInitializing(); break;
				case NetworkClientCallbackStub.downloadsComplete : 
					notiffy(_downloadsComplete);
					parseDownloadsComplete(); break;
				case NetworkClientCallbackStub.fileManagerLoaded : 
					notiffy(_fileManagerLoaded);
					parseFileManagerLoaded(); break;
				case NetworkClientCallbackStub.handleQueryResult : 
					notiffy(_handleQueryResult);
					parseHandleQueryResult(); break;
				case NetworkClientCallbackStub.handleQueryString : 
					notiffy(_handleQueryString);
					parseHandleQueryString(); break;
				case NetworkClientCallbackStub.handleSharedFileUpdate : 
					notiffy(_handleSharedFileUpdate);
					parseHandleSharedFileUpdate(); break;
				case NetworkClientCallbackStub.indicateNewVersion : 
					notiffy(_indicateNewVersion);
					parseIndicateNewVersion(); break;
				case NetworkClientCallbackStub.notifyUserAboutUpdate : 
					notiffy(_notifyUserAboutUpdate);
					parseNotifyUserAboutUpdate(); break;
				case NetworkClientCallbackStub.promptAboutCorruptDownload : 
					notiffy(_promptAboutCorruptDownload);
					parsePromptAboutCorruptDownload(); break;
				case NetworkClientCallbackStub.receiveMessage : 
					notiffy(_receiveMessage);
					parseReceiveMessage(); break;
				case NetworkClientCallbackStub.removeDownload : 
					notiffy(_removeDownload);
					parseRemoveDownload(); break;
				case NetworkClientCallbackStub.removeUpload : 
					notiffy(_removeUpload);
					parseRemoveUpload(); break;
				case NetworkClientCallbackStub.restoreApplication : 
					notiffy(_restoreApplication);
					parseRestoreApplication(); break;
				case NetworkClientCallbackStub.setAnnotateEnabled : 
					notiffy(_setAnnotateEnabled);
					parseSetAnnotateEnabled(); break;
				case NetworkClientCallbackStub.showDownloads : 
					notiffy(_showDownloads);
					parseShowDownloads(); break;
				case NetworkClientCallbackStub.uploadsComplete : 
					notiffy(_uploadsComplete);
					parseUploadsComplete(); break;
			}
		}
		
		
		protected void parseAcceptChat() {}
		protected void parseAddDownload() {}
		protected void parseAddSharedDirectory() {}
		protected void parseAddSharedFile() {}
		protected void parseAddUpload(){}
		protected void parseBrowseHostFailed(){}
		protected void parseChatErrorMessage() {}
		protected void parseChatUnavailable(){}
		
		void parseClearSharedFiles(){
			clearSharedFiles();
		}
		
		protected void parseConnectionClosed(){}
		protected void parseConnectionInitialized(){}
		protected void parseConnectionInitializing(){}
		
		private void parseDownloadsComplete(){
			downloadsComplete();
		}
		
		private void parseFileManagerLoaded(){
			fileManagerLoaded();
		}
		protected void parseHandleQueryResult(){}
		protected void parseHandleQueryString(){}
		protected void parseHandleSharedFileUpdate(){}
		
		private void parseIndicateNewVersion(){
			indicateNewVersion();
		}
		
		protected void parseNotifyUserAboutUpdate(){}
		protected void parsePromptAboutCorruptDownload(){}
		protected void parseReceiveMessage(){}
		protected void parseRemoveDownload(){}
		protected void parseRemoveUpload(){}
		
		private void parseRestoreApplication(){
			restoreApplication();
		}
		
		protected void parseSetAnnotateEnabled(){}
		
		private void parseShowDownloads(){
			showDownloads();
		}
		private void parseUploadsComplete(){
			uploadsComplete();
		}
	}
	
	/**
	 * notifies all listeners on a specific object.
	 * misspelled on purpose. 
	 * @param o the object to be notified.
	 */
	private final void notiffy(Object o) {
		synchronized(o) {
			o.notifyAll();
		}
	}
}
