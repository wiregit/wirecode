package com.limegroup.gnutella;

import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.xml.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.security.*;
import com.limegroup.gnutella.settings.*;
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
public class Backend {

	/**
	 * The <tt>RouterService</tt> instance the constructs the backend.
	 */
	private RouterService ROUTER_SERVICE;

	private final File TEMP_DIR = new File("temp");

	private final Timer TIMER = new Timer();

	/**
	 * The number of milliseconds to wait before automatically shutting off.
	 */
	private final int TIMEOUT;

	/**
	 * The <tt>MessageRouter</tt> instance to use.
	 */
	private final MessageRouter ROUTER;
	
	/**
	 * The <tt>ActivityCallback</tt> to use.
	 */
	private final ActivityCallback CALLBACK;

	/**
	 * The files that should be copied to a temporary directory on startup and
	 * returned to their original locations on shutdown.
	 */
	private final File[] FILES_TO_SAVE = {
		new File(CommonUtils.getUserSettingsDir(), "gnutella.net"),
		new File(CommonUtils.getUserSettingsDir(), "limewire.props"),
	};


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
     * @return a BackEnd that will live until you shutdown() it.
     */
	public static Backend createLongLivedBackend(ActivityCallback callback,
                                                 MessageRouter router) {
		return new Backend(callback, router, 0);
	}

    /**
     * @return a BackEnd that will live until you shutdown() it.
     */
	public static Backend createLongLivedBackend(MessageRouter router) {
		return new Backend(new ActivityCallbackStub(), router, 0);
	}

	/**
	 * Creates a new <tt>Backend</tt> that will only accept connections
	 * from the local machine.
	 *
	 * @param timeout the number of milliseconds to wait before automatically
	 *  shutting down
	 */
	public static Backend createLocalBackend(int timeout) {
		return new LocalBackend(new ActivityCallbackStub(), null, timeout);
	}

	/**
	 * Creates a new <tt>Backend</tt> that will only accept connections
	 * from the local machine.
	 *
	 * @param callback the <tt>ActivityCallback</tt> instance to use
	 */
	public static Backend createLocalBackend(ActivityCallback callback) {
		return new LocalBackend(callback, null, 0);
	}

	/**
	 * Creates a new <tt>Backend</tt> that will only accept connections
	 * from the local machine.
	 *
	 * @param callback the <tt>ActivityCallback</tt> instance to use
	 * @param router the <tt>MessageRouter</tt> to use
	 */
	public static Backend createLocalBackend(ActivityCallback callback, 
											 MessageRouter router) {
		return new LocalBackend(callback, router, 0);
	}
											 
    

    private Backend(ActivityCallback callback, int timeout) {
        this(callback, null, timeout);
    }


	/**
	 * Constructs a new <tt>Backend</tt>.
     * @param timeout a non-positive timeout will make this BackEnd long-lived.
     * In that case, be sure to call shutdown() yourself!!  Any positive timeout
     * will cause the BackEnd to be shutoff in timeout milliseconds.
	 */
	protected Backend(ActivityCallback callback, MessageRouter router,
					  int timeout) {
		System.out.println("STARTING BACKEND"); 
		makeSharedDirectory();
		copySettingsFiles();
		setStandardSettings();
		CALLBACK = callback;
		ROUTER = router;
		TIMEOUT = timeout;
	}

	/**
	 * Starts all backend threads.
	 */
	public void start() {
        if (ROUTER == null)
            ROUTER_SERVICE = new RouterService(CALLBACK);
        else
            ROUTER_SERVICE = new RouterService(CALLBACK, ROUTER);
        ROUTER_SERVICE.start();
		ROUTER_SERVICE.connect();
		try {
			// sleep to let the file manager initialize
			Thread.sleep(2000);
		} catch(InterruptedException e) {
		}
		System.out.println("BACKEND LISTENING ON PORT: "+RouterService.getPort()); 
        if (TIMEOUT > 0) {
            TIMER.schedule(new TimerTask() {
                    public void run() {
                        shutdown("AUTOMATED");
                    }
                }, TIMEOUT);
        }		
	}

	/**
	 * Creates a temporary shared directory for testing purposes.
	 */
	protected void makeSharedDirectory() {
		TEMP_DIR.mkdirs();
		TEMP_DIR.deleteOnExit();
		File coreDir = new File("com/limegroup/gnutella");				
		File[] files = coreDir.listFiles();

		for(int i=0; i<files.length; i++) {
			if(!files[i].isFile()) continue;
			copyResourceFile(files[i]);
		}		
	}

	/**
	 * Sets the standard settings for a test backend, such as the ports, the
	 * number of connections to maintain, etc.
	 */
	protected void setStandardSettings() {
		SettingsManager settings = SettingsManager.instance();
		settings.setPort(6346);
		//settings.setKeepAlive(1);
		settings.setDirectories(new File[] {TEMP_DIR});
		settings.setExtensions("java");
		ConnectionSettings.KEEP_ALIVE.setValue(1);
		SearchSettings.GUESS_ENABLED.setValue(true);
		UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(false);
		UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(true);
		UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(true);
		ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
		ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
	}

	

	protected void copySettingsFiles() {
		for(int i=0; i<FILES_TO_SAVE.length; i++) {
			File curDirFile = new File(FILES_TO_SAVE[i].getName());
			curDirFile.delete();
			FILES_TO_SAVE[i].renameTo(curDirFile);
		}
	}


	protected void restoreSettingsFiles() {
		for(int i=0; i<FILES_TO_SAVE.length; i++) {
			File curDirFile = new File(FILES_TO_SAVE[i].getName());
			FILES_TO_SAVE[i].delete();
			curDirFile.renameTo(FILES_TO_SAVE[i]);
			curDirFile.delete();
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
		restoreSettingsFiles();
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
		Backend be = Backend.createLocalBackend(Integer.parseInt(args[0]));
		be.start();
	}
}

