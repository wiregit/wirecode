package com.limegroup.gnutella.caas.restlet;

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
import com.limegroup.gnutella.LimeWireCoreModule;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.Uploader;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.caas.DataStore;
import com.limegroup.gnutella.caas.Search;
import com.limegroup.gnutella.caas.SearchFactory;
import com.limegroup.gnutella.caas.SearchParams;
import com.limegroup.gnutella.caas.SearchResult;
import com.limegroup.gnutella.caas.SearchResultHandler;
import com.limegroup.gnutella.chat.InstantMessenger;
import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.search.HostData;
import com.limegroup.gnutella.version.UpdateInformation;

public class RestletMain {
    
    public static Injector _injector;

    public static void main(String[] args) throws Exception {
        _injector = Guice.createInjector(new LimeWireCoreModule(MainCallback.class));
        
        RestletConnector.setDefaultHost("127.0.0.1", 9090);
        SearchParams params = new SearchParams("limewire");
        SearchHandler handler = new SearchHandler();
        SearchFactory searchFactory = _injector.getInstance(SearchFactory.class);
        Search search = searchFactory.createSearch(params, handler);
        
        search.start();
        
        while (true) {
            try { Thread.sleep(5000); }
            catch (Exception e) { }
            
            search.getMoreResults();
        }
    }
    
    
    
    /**
     * 
     */
    static class SearchHandler implements SearchResultHandler {
        
        public void handleSearchResult(Search s, SearchResult sr) {
            System.out.println("SearchHandler::handleSearchResult().. got one: " + sr.getFilename() + " @ " + sr.getHost() + ":" + sr.getPort());
        }
        
    }
    
    @Singleton
    private static class MainCallback implements ActivityCallback {
        
        private DataStore _dataStore;
        
        public void setDataStore(DataStore dataStore) {
            _dataStore = dataStore;
        }
        
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
            
            _dataStore.addSearchResult(rfd, data, loc);
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
