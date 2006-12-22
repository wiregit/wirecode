package com.limegroup.gnutella.udpconnect;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.ManagedThread;
import org.limewire.service.ErrorCallback;
import org.limewire.service.ErrorService;

import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.Connection;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManagerEvent;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.Uploader;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.chat.Chatter;
import com.limegroup.gnutella.search.HostData;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.version.UpdateInformation;

/**
 * A standalone program for testing UDPConnections across machines.
 */
public class UStandalone extends ActivityCallbackStub 
    implements ActivityCallback, ErrorCallback {
	
    private static final Log LOG =
        LogFactory.getLog(UStandalone.class);


    public static void main(String args[]) {
		ActivityCallback callback = new UStandalone();
		ConnectionSettings.PORT.setValue(6346);
		ConnectionSettings.FORCED_PORT.setValue(6346);
		ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
		RouterService service = new RouterService(callback);
		service.start();    

		LOG.debug("Starting up ...");
		waitOnUDP();
		LOG.debug("UDPServices up ...");
        UDPService.instance().setReceiveSolicited(true);
        int port = UDPService.instance().getStableUDPPort();
		LOG.debug("UDPServices Stable port "+port);
        port = UDPService.instance().lastReportedPort();
		LOG.debug("UDPServices last port "+port);

        try { Thread.sleep(1000); } catch (InterruptedException ie){}

		for ( ; ;) {
			LOG.trace("Go ...");
			try {
				InetAddress remoteIP = InetAddress.getByName(args[0]);
				LOG.debug("InetAddress: "+remoteIP+" port:"+ 
                  Integer.parseInt(args[1]) );
				UDPConnection usock = 
				  new UDPConnection(remoteIP, Integer.parseInt(args[1]));
				LOG.debug("Created UDPSocket");

				if ( args.length == 2 ) {
                    LOG.debug("Starting SimpleTest:");
					simpleTest(usock);
                } else if (args[2].equals("-ec")) {
                    LOG.debug("Starting EchoClient:");
                    echoClient(usock, TARGET_BYTES);
                } else if (args[2].equals("-es")) {
                    LOG.debug("Starting EchoServer:");
                    echoServer(usock, TARGET_BYTES);
                } else if (args[2].equals("-ecb")) {
                    LOG.debug("Starting EchoClientBlock:");
                    echoClientBlock(usock, TARGET_BLOCKS);
                } else if (args[2].equals("-esb")) {
                    LOG.debug("Starting EchoServerBlock:");
                    echoServerBlock(usock, TARGET_BLOCKS);
				} else if (args[2].equals("-uc")) {
                    LOG.debug("Starting UnidirectionalClient:");
					unidirectionalClient(usock, TARGET_BYTES);
				} else if (args[2].equals("-us")) {
                    LOG.debug("Starting UnidirectionalServer:");
					unidirectionalServer(usock, TARGET_BYTES);
                }

				usock.close();
				break;
			} catch (IOException e) {
				e.printStackTrace();
				LOG.trace("Exiting  ...");
				System.exit(1);
			}
		}
		LOG.trace("Shutdown ...");
		RouterService.shutdown(); 
    }

	/** The amount of data to transfer */
    private static int TARGET_BYTES  = 2000000;
    private static int TARGET_BLOCKS = 4096;

	/** A boolean that tracks whether the read thread was successful */
    private static boolean readSuccess = false;

	public static boolean echoClient(UDPConnection usock, int numBytes) 
	  throws IOException {
		OutputStream ostream = usock.getOutputStream();
		InputStream  istream = usock.getInputStream();

		readSuccess = false;
		ClientReader reader = new ClientReader(istream, numBytes);
		reader.start();

		for (int i = 0; i < numBytes; i++) {
			ostream.write(i % 256);
			if ( (i % 1000) == 0 ) 
				LOG.debug("Write status: "+i);
		}
		LOG.trace("Done write");
		
		try { reader.join(); } catch (InterruptedException ie){}
        LOG.debug("Done echoClient test");

		return readSuccess;
	}

	static class ClientReader extends ManagedThread {
		InputStream istream;
		int         numBytes;

		public ClientReader(InputStream istream, int numBytes) {
		    super ("ClientReader");
			this.istream = istream;
			this.numBytes = numBytes;
		}

		public void run() {
			int rval;
			LOG.debug("Begin read");

			try {
				for (int i = 0; i < numBytes; i++) {
					rval = istream.read();
					if ( rval != (i % 256) ) {
						LOG.debug("Error on read expected: "+i
						  +" received: "+rval);
						break;
					} else
						LOG.trace("Properly recieved: "+i);
					if ( (i % 1000) == 0 ) 
						LOG.debug("Read status: "+i);
				}
				readSuccess = true;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			LOG.debug("Done read");
		}
	}

	public static boolean echoServer(UDPConnection usock, int numBytes) 
	  throws IOException {
		OutputStream ostream = usock.getOutputStream();
		InputStream  istream = usock.getInputStream();

		boolean success = false;

		int rval;
		for (int i = 0; i < numBytes; i++) {
			rval = istream.read();
			if ( rval != (i % 256) ) {
				LOG.debug("Error on read expected: "+i
				  +" received: "+rval);
				return false;
			} 
			if ( (i % 1000) == 0 ) 
				LOG.debug("Echo status: "+i);
			ostream.write(rval);
		}
		success = true;
		LOG.trace("Done echo");
		try { Thread.sleep(1*1000); } catch (InterruptedException ie){}
        LOG.debug("Done echoServer test");
		return success;
	}

	public static boolean echoClientBlock(UDPConnection usock, int numBlocks) 
	  throws IOException {
		OutputStream ostream = usock.getOutputStream();
		InputStream  istream = usock.getInputStream();

		readSuccess = false;
		ClientBlockReader reader = new ClientBlockReader(istream, numBlocks);
		reader.start();
	
		// setup transfer data
		byte bdata[] = new byte[512];
		for (int i = 0; i < 512; i++)
			bdata[i] = (byte) (i % 256);

		for (int i = 0; i < numBlocks; i++) {
			ostream.write(bdata, 0, 512);
			if ( (i % 8) == 0 ) 
				LOG.debug("Write status: "+i*512+
                  " time:"+System.currentTimeMillis());
		}
		LOG.trace("Done write");
		
		try { reader.join(); } catch (InterruptedException ie){}
        LOG.debug("Done echoClientBlock test");
		return readSuccess;
	}

	static class ClientBlockReader extends ManagedThread {
		InputStream istream;
		int         numBlocks;

		public ClientBlockReader(InputStream istream, int numBlocks) {
			this.istream   = istream;
			this.numBlocks = numBlocks;
		}

		public void run() {
			LOG.debug("Begin read");

			byte bdata[] = new byte[512];

            int btest;
			int len;
            int printTarget = 0;
			try {
				for (int i = 0; i < 512 * numBlocks; i += len) {
					len = istream.read(bdata);

                    if ( len != 512 ) 
                        LOG.debug("Abnormal data size: "+len+" loc: "+i);

					for (int j = 0; j < len; j++) {
                        btest = bdata[j] & 0xff;
						if ( btest != ((i+j) % 256) ) {
							LOG.debug("Error on read expected: "+(i+j)
							  +" received: "+bdata[j]);
							return;
						} 
						if ( (i+j) > printTarget ) { 
							LOG.debug("Read status: "+i+
                              " time:"+System.currentTimeMillis());
                            printTarget = i+j+1024;
                        }
					}
				}
				readSuccess = true;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			LOG.debug("Done read");
		}
	}

	public static boolean echoServerBlock(UDPConnection usock, int numBlocks) 
	  throws IOException {
		OutputStream ostream = usock.getOutputStream();
		InputStream  istream = usock.getInputStream();

		byte bdata[] = new byte[512];

		boolean success = false;

        int btest;
		int len = 0;
		for (int i = 0; i < 512 * numBlocks; i += len) {
			len = istream.read(bdata);

            if ( len != 512 ) 
                LOG.debug("Abnormal data size: "+len+" loc: "+i);

			for (int j = 0; j < len; j++) {
                btest = bdata[j] & 0xff;
				if ( btest != ((i+j) % 256) ) {
					LOG.debug("Error on echo expected: "+(i+j)
					  +" received: "+bdata[j]);
					return false;
				} 
				if ( ((i+j) % 1024) == 0 ) 
					LOG.debug("Echo status: "+i+
                      " time:"+System.currentTimeMillis());
			}
            ostream.write(bdata, 0, len);
		}
		success = true;
		LOG.trace("Done echoBlock");
		try { Thread.sleep(1*1000); } catch (InterruptedException ie){}
        LOG.debug("Done echoServerBlock test");
		return success;
	}

    public static boolean unidirectionalClient(UDPConnection usock, 
	  int numBytes) throws IOException {
        OutputStream ostream = usock.getOutputStream();

		boolean success = false;

        int i = 0;
        for (i = 0; i < numBytes; i++) {
            ostream.write(i % 256);
            if ( (i % 1000) == 0 ) 
                LOG.debug("Write status: "+i);
        }
		success = true;
        LOG.debug("Write reached: "+i);
        
        try { Thread.sleep(1*1000); } catch (InterruptedException ie){}
        LOG.debug("Done unidirectionalClient test");
		return success;
    }

    public static boolean unidirectionalServer(UDPConnection usock, 
	  int numBytes) throws IOException {
        InputStream  istream = usock.getInputStream();

		boolean success = false;
        int rval;
        int i = 0;
        for (i = 0; i < numBytes; i++) {
            rval = istream.read();
            if ( rval != (i % 256) ) {
                LOG.debug("Error on read expected: "+i
                  +" received: "+rval);
                break;
            } else {
                if ( (i % 1000) == 0 ) 
                    LOG.debug("Read Properly received: "+i);
            }
        }
		success = true;
        LOG.debug("Read reached: "+i);
        
        try { Thread.sleep(1*1000); } catch (InterruptedException ie){}
        LOG.debug("Done unidirectionalServer test");
		return success;
    }

	private static void simpleTest(UDPConnection usock) throws IOException {
		OutputStream ostream = usock.getOutputStream();
		LOG.debug("Created OutputStream");

		ostream.write(new byte[50]);
		ostream.write(new byte[50]);
		try { Thread.sleep(1*1000); } catch (InterruptedException ie){}
		ostream.write(new byte[500]);
		ostream.write(new byte[500]);

		try { Thread.sleep(1*1000); } catch (InterruptedException ie){}
		ostream.write(new byte[500]);
		ostream.write(new byte[500]);

		try { Thread.sleep(2*1000); } catch (InterruptedException ie){}
		LOG.debug("Done sleep");
        LOG.debug("Done simple test");
	}

	private static void waitOnUDP() {
        int waits = 0;
        while(!UDPService.instance().isListening() && waits < 10) {
			try {
				Thread.sleep(600);
			} catch (InterruptedException e) {
				// Should never happen.
				ErrorService.error(e);
			}
            waits++;
        }
		if ( waits >= 10 ) {
			LOG.debug("UDP didn't make it up ...");
			LOG.debug("Bubye ...");
			throw new RuntimeException("no UDP");
		}
	}

    /////////////////////////// ActivityCallback methods //////////////////////

    public void connectionInitializing(Connection c) {
    }

    public void connectionInitialized(Connection c) {
    }

    public void connectionClosed(Connection c) {
    }

    public void knownHost(Endpoint e) {
    }

	public void handleQueryResult(RemoteFileDesc rfd ,HostData data, Set loc) {
	}

    public void handleQueryString( String query ) {
    }

	public void error(int errorCode) {
		error(errorCode, null);
    }
    
    public void error(Throwable problem, String msg) {
        problem.printStackTrace();
        System.out.println(msg);
    }

    public void error(Throwable problem) {
		problem.printStackTrace();
	}

    public void error(int message, Throwable t) {
		System.out.println("Error: "+message);
		t.printStackTrace();
    }

    public void addressStateChanged() {}

    ///////////////////////////////////////////////////////////////////////////


    public boolean overwriteFile(String file) {return false;};

    public void addDownload(Downloader mgr) {}

    public void removeDownload(Downloader mgr) {}

    public void addUpload(Uploader mgr) {}

    public void removeUpload(Uploader mgr) {}

    public void setPort(int port){}

    public int getNumUploads(){ return 0; }

	public void addSharedDirectory(File file, File parent) {}

	public void addSharedFile(FileDesc file, File parent) {}
	
	public boolean warnAboutSharingSensitiveDirectory(final File dir) { return false; }
		
	public void handleSharedFileUpdate(File file) {}

	public void clearSharedFiles() {}

	public void acceptChat(Chatter chat) {}

	public void receiveMessage(Chatter chat) {}
	
	public void chatUnavailable(Chatter chatter) {}

	public void chatErrorMessage(Chatter chatter, String st) {}
        
    public void downloadsComplete() {}    
    
    public void fileManagerLoaded() {}    
    
    public void uploadsComplete() {}

    public void promptAboutCorruptDownload(Downloader dloader) {
        dloader.discardCorruptDownload(false);
    }

	public void restoreApplication() {}

	public void showDownloads() {}

    public String getHostValue(String key){
        return null;
    }
    public void browseHostFailed(GUID guid) {}

	public void setAnnotateEnabled(boolean enabled) {}

    public void updateAvailable(UpdateInformation uc) {}

    public boolean isQueryAlive(GUID guid) {
        return false;
    }

    public void componentLoading(String component) {}
    
    public void handleFileEvent(FileManagerEvent fme) {}
    
    public void fileManagerLoading() {}

	public boolean handleMagnets(final MagnetOptions[] magnets) {
		return false;
	}

	public void acceptedIncomingChanged(boolean status) { }
}
