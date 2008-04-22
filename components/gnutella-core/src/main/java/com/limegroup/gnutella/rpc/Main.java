package com.limegroup.gnutella.rpc;

import java.io.File;
import java.util.Set;

import org.limewire.io.IpPort;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.FileManagerEvent;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.LimeWireCore;
import com.limegroup.gnutella.LimeWireCoreModule;
// import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.Uploader;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.chat.InstantMessenger;
import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.search.HostData;
import com.limegroup.gnutella.version.UpdateInformation;

public class Main {

    // start the xml-rpc server
    //   listen for incoming connections
    //   handle requests / send responses
    //   register event callbacks
    //     gnutella connection count changed
    //     query response
    //     download progress
    
    public static void main (String[] args) {
        Injector injector = Guice.createInjector(new LimeWireCoreModule(MainCallback.class));
        LimeWireCore core = injector.getInstance(LimeWireCore.class);
        
        core.getLifecycleManager().start();        
        
        
        /*
                    // connect
                    try {
                        int port=6346;
                        if (commands.length>=3)
                            port=Integer.parseInt(commands[2]);
                        System.out.println("Connecting...");
                        core.getConnectionServices().connectToHostAsynchronously(commands[1], port, ConnectType.PLAIN);
                    } catch (NumberFormatException e) {
                        System.out.println("Please specify a valid port.");
                    }
                    
                    // query
                    //Get query string from command (possibly multiple words)
                    int i=command.indexOf(' ');
                    assert(i!=-1 && i<command.length());
                    String query=command.substring(i+1);
                    SearchServices searchServices = core.getSearchServices();
                    searchServices.query(searchServices.newQueryGUID(), query);
                    
                    // listen
                    try {
                        int port=Integer.parseInt(commands[1]);
                        networkManager.setListeningPort(port);
                    } catch (NumberFormatException e) {
                        System.out.println("Please specify a valid port.");
                    } catch (IOException e) {
                        System.out.println("Couldn't change port.  Try another value.");
                    }
        */
        
        core.getLifecycleManager().shutdown(); //write gnutella.net        
    }
    
    @Singleton
    private static class MainCallback implements ActivityCallback {
        
        public void connectionInitializing(RoutedConnection c) {
        }
    
        public void connectionInitialized(RoutedConnection c) {
        }
    
        public void connectionClosed(RoutedConnection c) {
        }
    
        public void knownHost(Endpoint e) {
        }
    
        public void handleQueryResult(RemoteFileDesc rfd ,HostData data, Set<? extends IpPort> loc) {
            synchronized(System.out) {
                System.out.println("Query hit from "+rfd.getHost()+":"+rfd.getPort()+":");
                System.out.println("   "+rfd.getFileName());
            }
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
    
        public void addDownload(Downloader mgr) {}
    
        public void removeDownload(Downloader mgr) {}
    
        public void addUpload(Uploader mgr) {}
    
        public void removeUpload(Uploader mgr) {}
    
        public boolean warnAboutSharingSensitiveDirectory(final File dir) { return false; }
        
        public void handleFileEvent(FileManagerEvent evt) {}
        
        public void handleSharedFileUpdate(File file) {}
    
        public void fileManagerLoading() {}
    
        public void acceptChat(InstantMessenger chat) {}
    
        public void receiveMessage(InstantMessenger chat, String message) {}
        
        public void chatUnavailable(InstantMessenger chatter) {}
    
        public void chatErrorMessage(InstantMessenger chatter, String st) {}
            
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
        
        public void updateAvailable(UpdateInformation update) {
            if (update.getUpdateCommand() != null)
                System.out.println("there's a new version out "+update.getUpdateVersion()+
                        ", to get it shutdown limewire and run "+update.getUpdateCommand());
            else
                System.out.println("You're running an older version.  Get " +
                             update.getUpdateVersion() + ", from " + update.getUpdateURL());
        }  
    
        public boolean isQueryAlive(GUID guid) {
            return false;
        }
        
        public void componentLoading(String component) {
            System.out.println("Loading component: " + component);
        }
        
        public boolean handleMagnets(final MagnetOptions[] magnets) {
            return false;
        }
    
        public void handleTorrent(File torrentFile){}
        
        public void acceptedIncomingChanged(boolean status) { }
    
        public void handleAddressStateChanged() {
        }
        
        public void handleConnectionLifecycleEvent(ConnectionLifecycleEvent evt) {
        }
        public void installationCorrupted() {
            
        }
    }
    
}
