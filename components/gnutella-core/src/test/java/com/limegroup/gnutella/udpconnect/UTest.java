package com.limegroup.gnutella.udpconnect;

import java.io.*;
import java.net.*;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.chat.*;
import com.limegroup.gnutella.security.User;
import com.limegroup.gnutella.search.*;
import com.limegroup.gnutella.*;

/**
 * A standalone program for testing UDPConnections across machines.
 */
public class UTest implements ActivityCallback, ErrorCallback {
    public static void main(String args[]) {
		ActivityCallback callback = new UTest();
		RouterService service = new RouterService(callback);
		service.start();    

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
                    echoClient(usock);
                } else if (args[2].equals("-es")) {
                    tlogstart("Starting EchoServer:");
                    echoServer(usock);
				} else if (args[2].equals("-uc")) {
                    tlogstart("Starting UnidirectionalClient:");
					unidirectionalClient(usock);
				} else if (args[2].equals("-us")) {
                    tlogstart("Starting UnidirectionalServer:");
					unidirectionalServer(usock);
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
        System.out.println(str);
    }

    private static long startTime;
    private static long endTime;
    private static void tlogstart(String str) {
        startTime = System.currentTimeMillis();
        System.out.println(str +" "+ startTime);
    }

    private static void tlogend(String str) {
        endTime = System.currentTimeMillis();
        System.out.println(str +" "+ endTime);
        System.out.println("Total : "+(endTime -startTime)/1000 +" seconds");
    }

    private static int TARGET_BYTES = 2000000;
	private static void echoClient(UDPConnection usock) throws IOException {
		OutputStream ostream = usock.getOutputStream();
		InputStream  istream = usock.getInputStream();

		ClientReader reader = new ClientReader(istream);
		reader.start();

		for (int i = 0; i < TARGET_BYTES; i++) {
			ostream.write(i % 128);
			if ( (i % 1000) == 0 ) 
				log2("Write status: "+i);
		}
		log("Done write");
		
		try { Thread.sleep(2*1000); } catch (InterruptedException ie){}
        tlogend("Done echoClient test");
	}

	static class ClientReader extends Thread {
		InputStream istream;

		public ClientReader(InputStream istream) {
			this.istream = istream;
		}

		public void run() {
			int rval;
			log2("Begin read");

			try {
				for (int i = 0; i < TARGET_BYTES; i++) {
					rval = istream.read();
					if ( rval != (i % 128) ) {
						log2("Error on read expected: "+i
						  +" received: "+rval);
						break;
					} else
						log("Properly recieved: "+i);
					if ( (i % 1000) == 0 ) 
						log2("Read status: "+i);
					}
			} catch (IOException e) {
				e.printStackTrace();
			}
			log2("Done read");
		}
	}

	private static void echoServer(UDPConnection usock) throws IOException {
		OutputStream ostream = usock.getOutputStream();
		InputStream  istream = usock.getInputStream();

		int rval;
		try {
			for (int i = 0; i < TARGET_BYTES; i++) {
				rval = istream.read();
				if ( rval != (i % 128) ) {
					log2("Error on read expected: "+i
					  +" received: "+rval);
					break;
				} 
				if ( (i % 1000) == 0 ) 
					log2("Echo status: "+i);
				ostream.write(rval);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		log("Done echo");
		try { Thread.sleep(1*1000); } catch (InterruptedException ie){}
        tlogend("Done echoServer test");
	}

    private static void unidirectionalClient(UDPConnection usock) 
      throws IOException {
        OutputStream ostream = usock.getOutputStream();

        int i = 0;
        for (i = 0; i < TARGET_BYTES; i++) {
            ostream.write(i % 256);
            if ( (i % 1000) == 0 ) 
                log2("Write status: "+i);
        }
        log2("Write reached: "+i);
        
        try { Thread.sleep(2*1000); } catch (InterruptedException ie){}
        tlogend("Done unidirectionalClient test");
    }

    private static void unidirectionalServer(UDPConnection usock) 
      throws IOException {
        InputStream  istream = usock.getInputStream();

        int rval;
        int i = 0;
        try {
            for (i = 0; i < TARGET_BYTES; i++) {
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
        } catch (IOException e) {
            e.printStackTrace();
        }
        log2("Read reached: "+i);
        
        try { Thread.sleep(1*1000); } catch (InterruptedException ie){}
        tlogend("Done unidirectionalServer test");
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
			System.exit(1);
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
}
