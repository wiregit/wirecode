package com.limegroup.gnutella.udpconnect;

import java.io.*;
import java.net.*;
import java.util.*;
import com.limegroup.gnutella.chat.*;
import com.limegroup.gnutella.security.User;
import com.limegroup.gnutella.search.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.ManagedThread;

/**
 * A standalone program for testing UDPConnections across machines.
 */
public class UStandalone implements ActivityCallback, ErrorCallback {
	
	/** Control some logging of state */
	private static boolean activeLogging = false;

    public static void main(String args[]) {
		ActivityCallback callback = new UStandalone();
		RouterService service = new RouterService(callback);
		service.start();    
		activeLogging = true;

		log("Starting up ...");
		waitOnUDP();
		log("UDPServices up ...");
        UDPService.instance().setReceiveSolicited(true);
        try { Thread.sleep(1000); } catch (InterruptedException ie){}

		for ( ; ;) {
			log("Go ...");
			try {
				InetAddress remoteIP = InetAddress.getByName(args[0]);
				log2("InetAddress: "+remoteIP+" port:"+ 
                  Integer.parseInt(args[1]) );
				UDPConnection usock = 
				  new UDPConnection(remoteIP, Integer.parseInt(args[1]));
				log2("Created UDPSocket");

				if ( args.length == 2 ) {
                    tlogstart("Starting SimpleTest:");
					simpleTest(usock);
                } else if (args[2].equals("-ec")) {
                    tlogstart("Starting EchoClient:");
                    echoClient(usock, TARGET_BYTES);
                } else if (args[2].equals("-es")) {
                    tlogstart("Starting EchoServer:");
                    echoServer(usock, TARGET_BYTES);
                } else if (args[2].equals("-ecb")) {
                    tlogstart("Starting EchoClientBlock:");
                    echoClientBlock(usock, TARGET_BLOCKS);
                } else if (args[2].equals("-esb")) {
                    tlogstart("Starting EchoServerBlock:");
                    echoServerBlock(usock, TARGET_BLOCKS);
				} else if (args[2].equals("-uc")) {
                    tlogstart("Starting UnidirectionalClient:");
					unidirectionalClient(usock, TARGET_BYTES);
				} else if (args[2].equals("-us")) {
                    tlogstart("Starting UnidirectionalServer:");
					unidirectionalServer(usock, TARGET_BYTES);
                }

				usock.close();
				break;
			} catch (IOException e) {
				e.printStackTrace();
				log("Exiting  ...");
				System.exit(1);
			}
		}
		log("Shutdown ...");
		RouterService.shutdown(); 
    }

    private static void log(String str) {
    }

    private static void log2(String str) {
		if (activeLogging)
        	System.out.println(str);
    }

    private static long startTime;
    private static long endTime;
    private static void tlogstart(String str) {
		if (activeLogging) {
        	startTime = System.currentTimeMillis();
        	System.out.println(str +" "+ startTime);
		}
    }

    private static void tlogend(String str) {
		if (activeLogging) {
        	endTime = System.currentTimeMillis();
        	System.out.println(str +" "+ endTime);
        	System.out.println("Total: "+(endTime -startTime)/1000 +" seconds");
		}
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
				log2("Write status: "+i);
		}
		log("Done write");
		
		try { reader.join(); } catch (InterruptedException ie){}
        tlogend("Done echoClient test");

		return readSuccess;
	}

	static class ClientReader extends ManagedThread {
		InputStream istream;
		int         numBytes;

		public ClientReader(InputStream istream, int numBytes) {
			this.istream = istream;
			this.numBytes = numBytes;
		}

		public void managedRun() {
			int rval;
			log2("Begin read");

			try {
				for (int i = 0; i < numBytes; i++) {
					rval = istream.read();
					if ( rval != (i % 256) ) {
						log2("Error on read expected: "+i
						  +" received: "+rval);
						break;
					} else
						log("Properly recieved: "+i);
					if ( (i % 1000) == 0 ) 
						log2("Read status: "+i);
				}
				readSuccess = true;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			log2("Done read");
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
				log2("Error on read expected: "+i
				  +" received: "+rval);
				return false;
			} 
			if ( (i % 1000) == 0 ) 
				log2("Echo status: "+i);
			ostream.write(rval);
		}
		success = true;
		log("Done echo");
		try { Thread.sleep(1*1000); } catch (InterruptedException ie){}
        tlogend("Done echoServer test");
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
				log2("Write status: "+i*512+
                  " time:"+System.currentTimeMillis());
		}
		log("Done write");
		
		try { reader.join(); } catch (InterruptedException ie){}
        tlogend("Done echoClientBlock test");
		return readSuccess;
	}

	static class ClientBlockReader extends ManagedThread {
		InputStream istream;
		int         numBlocks;

		public ClientBlockReader(InputStream istream, int numBlocks) {
			this.istream   = istream;
			this.numBlocks = numBlocks;
		}

		public void managedRun() {
			int rval;
			log2("Begin read");

			byte bdata[] = new byte[512];

            int btest;
			int len;
            int printTarget = 0;
			try {
				for (int i = 0; i < 512 * numBlocks; i += len) {
					len = istream.read(bdata);

                    if ( len != 512 ) 
                        log2("Abnormal data size: "+len+" loc: "+i);

					for (int j = 0; j < len; j++) {
                        btest = bdata[j] & 0xff;
						if ( btest != ((i+j) % 256) ) {
							log2("Error on read expected: "+(i+j)
							  +" received: "+bdata[j]);
							return;
						} 
						if ( (i+j) > printTarget ) { 
							log2("Read status: "+i+
                              " time:"+System.currentTimeMillis());
                            printTarget = i+j+1024;
                        }
					}
				}
				readSuccess = true;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			log2("Done read");
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
                log2("Abnormal data size: "+len+" loc: "+i);

			for (int j = 0; j < len; j++) {
                btest = bdata[j] & 0xff;
				if ( btest != ((i+j) % 256) ) {
					log2("Error on echo expected: "+(i+j)
					  +" received: "+bdata[j]);
					return false;
				} 
				if ( ((i+j) % 1024) == 0 ) 
					log2("Echo status: "+i+
                      " time:"+System.currentTimeMillis());
			}
            ostream.write(bdata, 0, len);
		}
		success = true;
		log("Done echoBlock");
		try { Thread.sleep(1*1000); } catch (InterruptedException ie){}
        tlogend("Done echoServerBlock test");
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
                log2("Write status: "+i);
        }
		success = true;
        log2("Write reached: "+i);
        
        try { Thread.sleep(1*1000); } catch (InterruptedException ie){}
        tlogend("Done unidirectionalClient test");
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
                log2("Error on read expected: "+i
                  +" received: "+rval);
                break;
            } else {
                if ( (i % 1000) == 0 ) 
                    log2("Read Properly received: "+i);
            }
        }
		success = true;
        log2("Read reached: "+i);
        
        try { Thread.sleep(1*1000); } catch (InterruptedException ie){}
        tlogend("Done unidirectionalServer test");
		return success;
    }

	private static void simpleTest(UDPConnection usock) throws IOException {
		OutputStream ostream = usock.getOutputStream();
		log2("Created OutputStream");

		ostream.write(new byte[50]);
		ostream.write(new byte[50]);
		try { Thread.sleep(1*1000); } catch (InterruptedException ie){}
		ostream.write(new byte[500]);
		ostream.write(new byte[500]);

		try { Thread.sleep(1*1000); } catch (InterruptedException ie){}
		ostream.write(new byte[500]);
		ostream.write(new byte[500]);

		try { Thread.sleep(2*1000); } catch (InterruptedException ie){}
		log2("Done sleep");
        tlogend("Done simple test");
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
			log2("UDP didn't make it up ...");
			log2("Bubye ...");
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
	
	public void handleSharedFileUpdate(File file) {}

	public void clearSharedFiles() {}

	public void acceptChat(Chatter chat) {}

	public void receiveMessage(Chatter chat) {}
	
	public void chatUnavailable(Chatter chatter) {}

	public void chatErrorMessage(Chatter chatter, String st) {}
        
    public void downloadsComplete() {}    
    
    public void fileManagerLoaded() {}    
    
    public User getUserAuthenticationInfo(String host){
        return null;
    }

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

    public void notifyUserAboutUpdate(String message,boolean isPro,boolean loc){
    }

    public void indicateNewVersion() {}

    public boolean isQueryAlive(GUID guid) {
        return false;
    }

    public void componentLoading(String component) {}
    
    public void handleFileManagerEvent(FileManagerEvent fme) {}
}
