package com.limegroup.gnutella;

import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.xml.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.security.*;
import java.io.*;
import java.util.*;

/**
 * Utility class that constructs a LimeWire backend for testing
 * purposes.  This creates a backend with a true <tt>FileManager</tt>,
 * creating a temporary shared directory that files are copied into.
 * The only component of this backend that is a stub is 
 * <tt>ActivityCallbackStub</tt>.  Otherwise, all classes are 
 * constructed as they normally would be in the client.
 */
public final class Backend {

	private final RouterService ROUTER_SERVICE;

	private final File TEMP_DIR = new File("temp");

	private final Timer TIMER = new Timer();


	public static Backend createBackend(ActivityCallback callback, int timeout){
		return new Backend(callback, timeout);
	}


	public static Backend createBackend(int timeout) {
		return new Backend(new ActivityCallbackStub(), timeout);
	}

    /**
     * @return a BackEnd that will live until you shutdown() it.
     */
	public static Backend createLongLivedBackend() {
		return new Backend(new ActivityCallbackStub(), 0);
	}
    
    
    /**
     * @return a BackEnd that will live until you shutdown() it.
     */
	public static Backend createLongLivedBackend(ActivityCallback callback) {
		return new Backend(callback, 0);
	}
    


	/**
	 * Constructs a new <tt>Backend</tt>.
     * @param timeout a non-positive timeout will make this BackEnd long-lived.
     * In that case, be sure to call shutdown() yourself!!  Any positive timeout
     * will cause the BackEnd to be shutoff in timeout milliseconds.
	 */
	private Backend(ActivityCallback callback, int timeout) {
		System.out.println("STARTING BACKEND"); 
		File gnutellaDotNet = new File("gnutella.net");
		File propsFile = new File("limewire.props");
		
		// remove hosts file to get rid of variable conditions across
		// test runs
		gnutellaDotNet.delete();

		// remove props file to get rid of variable settings
		propsFile.delete();

		SettingsManager settings = SettingsManager.instance();
		settings.setPort(6346);
		settings.setKeepAlive(1);
		TEMP_DIR.mkdirs();
		TEMP_DIR.deleteOnExit();
		File coreDir = new File("com/limegroup/gnutella");				
		File[] files = coreDir.listFiles();

		for(int i=0; i<files.length; i++) {
			if(!files[i].isFile()) continue;
			copyResourceFile(files[i]);
		}


		settings.setDirectories(new File[] {TEMP_DIR});
		settings.setExtensions("java");
		settings.setGuessEnabled(true);		

		ROUTER_SERVICE = new RouterService(callback);
        ROUTER_SERVICE.start();
		try {
			// sleep to let the file manager initialize
			Thread.sleep(2000);
		} catch(InterruptedException e) {
		}
        if (timeout > 0) {
            TIMER.schedule(new TimerTask() {
                    public void run() {
                        shutdown("AUTOMATED");
                    }
                }, timeout);
        }
	}

	/**
	 * Accessor for the <tt>RouterService</tt> instance for this 
	 * <tt>Backend</tt>.
	 *
	 * @return the <tt>RouterService</tt> instance for this <tt>Backend</tt>
	 */
	public RouterService getRouterService() {
		return ROUTER_SERVICE;
	}

	/**
	 * Notifies <tt>RouterService</tt> that the backend should be shut down.
	 */
	public void shutdown(String msg) {
		System.out.println("BACKEND SHUTDOWN: "+msg); 
		ROUTER_SERVICE.shutdown();
		File propsFile = new File("limewire.props");
		File gnutellaDotNet = new File("gnutella.net");		
		propsFile.delete();
		gnutellaDotNet.delete();	
		System.exit(0);
	}

	/**
	 * Copies the specified resource file into the current directory from
	 * the jar file. If the file already exists, no copy is performed.
	 *
	 * @param fileName the name of the file to copy
	 */
	private final void copyResourceFile(final File fileToCopy) {

		File file = new File(TEMP_DIR, fileToCopy.getName());
		// return quickly if the dll is already there, no copy necessary
		if(file.exists() ) return;

		BufferedInputStream bis = null;
		BufferedOutputStream bos = null;            
		try {	
			InputStream is = new FileInputStream(fileToCopy);
			//buffer the streams to improve I/O performance
			final int bufferSize = 2048;
			bis = new BufferedInputStream(is, bufferSize);
			file.deleteOnExit();
			bos = new BufferedOutputStream(new FileOutputStream(file), bufferSize);
			byte[] buffer = new byte[bufferSize];
			int c = 0;
			
			do { //read and write in chunks of buffer size until EOF reached
				c = bis.read(buffer, 0, bufferSize);
				bos.write(buffer, 0, c);
			}
			while (c == bufferSize); //(# of bytes read)c will = bufferSize until EOF
			
		} catch(Exception e) {	
			//if there is any error, delete any portion of file that did write
			file.delete();
		} finally {
			try {
				if(bis != null) bis.close();
				if(bos != null) bos.close();
			} catch(IOException ioe) {}	// all we can do is try to close the streams
		} 
	}

	/**
	 * Main method is necessary to run a stand-alone server that tests can be 
	 * run off of.
	 */
	public static void main(String[] args) {
		Backend.createBackend(200*1000);
	}
}

