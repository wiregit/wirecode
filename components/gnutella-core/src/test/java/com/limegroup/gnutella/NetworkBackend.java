/*
 * This class creates a backend which communicates through a 
 * network ActivityCallback.
 * 
 * 
 * HOW TO USE:
 * 
 * 1. Implement a pair of NetworkClientCallbackStub and NetworkServerCallbackStub's
 * 2. Start the server by creating an instance as you normally would 
 * 3. Call the launch method, passing the class name of your client callback implementation
 *   Example:
 *   
 * 	boolean success = NetworkBackend.launch(true, 6346, 9000, "localhost",10000, 
 * 			"com.limegroup.gnutella.mypackage.MyNetworkClientCallbackStub");  
 * 
 * 
 * The return value will tell you whether the launch was successful.  You may want to wait some
 * time and check if the client has connected by calling the ".isConnected()" method of the server.
 * 
 */
package com.limegroup.gnutella;

import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.settings.*;

import java.io.IOException;
import com.sun.java.util.collections.*;


public class NetworkBackend extends Backend {
	
	/**
	 * used only in construction.  It is ok to be static because
	 * there is no way to instantiate more than one network backend 
	 * in the same jvm anyways. 
	 */
	protected static boolean _ultrapeer;
	
	private NetworkBackend(int gnutellaPort, int shutDownport, ActivityCallback callback)
		throws IOException{
		super(gnutellaPort,shutDownport, callback);
	}
	
	
	public static void Main(String []ar) throws Exception{
		//check if we get called with proper length
		if  (ar.length < 5)
			return;
		
		boolean ultrapeer = Boolean.getBoolean(ar[0]);
		int gnutellaPort = Integer.parseInt(ar[1]);
		int shutdownPort = Integer.parseInt(ar[2]);
		String host = ar[3];
		int serverPort = Integer.parseInt(ar[4]);
		
		//use reflection to get us a callback.. it better be in the classpath!
		Class callbackClass = Class.forName(ar[5]);
		Object []callbackArgs = new Object[2];
		callbackArgs[0] = host;
		callbackArgs[1] = new Integer(serverPort);
		Class []callbackArgsClasses = new Class[2];
		callbackArgsClasses[0] = String.class;
		callbackArgsClasses[1] = int.class;
		NetworkClientCallbackStub callback = 
			(NetworkClientCallbackStub) PrivilegedAccessor.invokeConstructor(
					callbackClass,callbackArgs,callbackArgsClasses);
		
		//set the node status
		_ultrapeer=ultrapeer;
		
		//and get it running
		new NetworkBackend(gnutellaPort, shutdownPort, callback);
			
	}
	
	/**
	 * checks whether we are supposed to be a leaf and adjusts
	 * settings accordingly.
	 */
	protected void setStandardSettings(int port) {
		super.setStandardSettings(port);
		if (!_ultrapeer) {
			UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(true);
			UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(false);
			UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(false);
		}
	}
	
	/**
	 * launches a backend in the separate process.  Make sure the server
	 * callback listener is running when you call this method.
	 * 
	 * @param ultraPeer whether to launch an ultrapeer or a leaf
	 * @param gnutellaPort the gnutella port for the backend
	 * @param shutDownport the shutdown port for the backend
	 * @param serverHost the host where the network callback server is
	 * @param serverPort the port where the network callback server is
	 * @param the name of the class which extends NetworkClientCallback
	 * @return true if the launch was successful.
	 */
	public synchronized boolean launch
			(boolean ultraPeer,
			int gnutellaPort, 
			int shutDownport, 
			String serverHost, 
			int serverPort,
			String callbackClassName) 
				throws IOException {
		
		if (isPortInUse(gnutellaPort)) return false;
		
		Vector args = new Vector();
		args.add("java");
		args.add("-classpath");
		args.add(System.getProperty("java.class.path","."));
		args.add(NetworkBackend.class.getName());
		args.add(Boolean.toString(ultraPeer));
		args.add(Integer.toString(gnutellaPort));
		args.add(Integer.toString(shutDownport));
		args.add(serverHost);
		args.add(Integer.toString(serverPort));
		args.add(callbackClassName);
		
		String []arg = new String[args.size()];
		args.copyInto(arg);
		
		Process proc = Runtime.getRuntime().exec(arg);
        new CopyThread(proc.getErrorStream(), System.err);
        new CopyThread(proc.getInputStream(), System.out);
        try { Thread.sleep(10000); } catch (InterruptedException ex) {}
        if (! isPortInUse(gnutellaPort)) {
            proc.destroy();
            throw new IOException("Backend process failed to open port");
        }
        return true;
    
	
		
	}
	
}
