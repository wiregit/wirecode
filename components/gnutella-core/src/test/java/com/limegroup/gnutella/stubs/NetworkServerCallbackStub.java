/*
 * Listens for events relayed from a NetworkClientCallbackStub.
 * 
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
 * This class should actually take the ActivityCallback as an argument
 * but I'm not about to write the parsing of all those arguments, plus
 * you may not need anything other than notification anyways.
 * 
 */
package com.limegroup.gnutella.stubs;


import java.net.*;
import java.io.*;

public class NetworkServerCallbackStub extends ActivityCallbackStub {
	
	private final ServerSocket _ss;
	private Socket _s;
	
	
	
	/**
	 * starts listening for the client callback on the local machine
	 * @param port the port to start listening on.
	 */
	public NetworkServerCallbackStub(int port) throws IOException{
		_ss = new ServerSocket(port);
	}
	
	/**
	 * waits for connection, blocking.  Make sure you run it before using.
	 * the callback stub is not intended to be re-used.
	 * @throws IOException couldn't connect.
	 */
	public void initialize() throws IOException{
		_s = _ss.accept();
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
				case NetworkClientCallbackStub.acceptChat : parseAcceptChat();break;
				case NetworkClientCallbackStub.addDownload : parseAddDownload();break;
				case NetworkClientCallbackStub.addSharedDirectory : parseAddSharedDirectory();break;
				case NetworkClientCallbackStub.addSharedFile : parseAddSharedFile();break;
				case NetworkClientCallbackStub.addUpload : parseAddUpload();break;
				case NetworkClientCallbackStub.browseHostFailed : parseBrowseHostFailed();break;
				case NetworkClientCallbackStub.chatErrorMessage : parseChatErrorMessage();break;
				case NetworkClientCallbackStub.chatUnavailable : parseChatUnavailable() ;break;
				case NetworkClientCallbackStub.clearSharedFiles : parseClearSharedFiles(); break;
				case NetworkClientCallbackStub.connectionClosed : parseConnectionClosed(); break;
				case NetworkClientCallbackStub.connectionInitialized : parseConnectionInitialized(); break;
				case NetworkClientCallbackStub.connectionInitializing : parseConnectionInitializing(); break;
				case NetworkClientCallbackStub.downloadsComplete : parseDownloadsComplete(); break;
				case NetworkClientCallbackStub.fileManagerLoaded : parseFileManagerLoaded(); break;
				case NetworkClientCallbackStub.handleQueryResult : parseHandleQueryResult(); break;
				case NetworkClientCallbackStub.handleQueryString : parseHandleQueryString(); break;
				case NetworkClientCallbackStub.handleSharedFileUpdate : parseHandleSharedFileUpdate(); break;
				case NetworkClientCallbackStub.indicateNewVersion : parseIndicateNewVersion(); break;
				case NetworkClientCallbackStub.notifyUserAboutUpdate : parseNotifyUserAboutUpdate(); break;
				case NetworkClientCallbackStub.promptAboutCorruptDownload : parsePromptAboutCorruptDownload(); break;
				case NetworkClientCallbackStub.receiveMessage : parseReceiveMessage(); break;
				case NetworkClientCallbackStub.removeDownload : parseRemoveDownload(); break;
				case NetworkClientCallbackStub.removeUpload : parseRemoveUpload(); break;
				case NetworkClientCallbackStub.restoreApplication : parseRestoreApplication(); break;
				case NetworkClientCallbackStub.setAnnotateEnabled : parseSetAnnotateEnabled(); break;
				case NetworkClientCallbackStub.showDownloads : parseShowDownloads(); break;
				case NetworkClientCallbackStub.uploadsComplete : parseUploadsComplete(); break;
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
}
