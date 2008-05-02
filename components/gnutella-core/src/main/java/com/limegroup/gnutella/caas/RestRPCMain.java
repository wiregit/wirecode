package com.limegroup.gnutella.caas;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.limewire.io.IpPort;
import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.Router;
import org.restlet.data.MediaType;
import org.restlet.data.Protocol;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.DomRepresentation;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ActivityCallback;
//import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.FileManagerEvent;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.LimeWireCore;
import com.limegroup.gnutella.LimeWireCoreModule;
//import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.Uploader;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.chat.InstantMessenger;
import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.search.HostData;
import com.limegroup.gnutella.util.URLDecoder;
import com.limegroup.gnutella.version.UpdateInformation;

public class RestRPCMain implements DataStore {
    
    /**
     * This is sort of the "top most thing" for the rest rpc system. Hopefully
     * one day I'll be able to provide a better description.
     */
    private final Component _component;
    
    /**
     * An ancient artifact of poor design.
     */
    private final Injector _injector;
    
    /**
     * The lime wire core object thing.
     */
    private final LimeWireCore _lwcore;
    
    /**
     * The lime wire application maps all of the resources we define to uri
     * patterns, like "/search/{queryid}", etc.
     */
    private final LimeWireApplication _lwappl;
    
    /**
     * Looks ugly, huh? So, the results of each search are stored in this Map,
     * keyed on the guid (in the form of a string). For each search a List of
     * search results are stored. Each search result is stored in the form of
     * a Map - strings keyed on strings - containing all of the interesting
     * values that we'd like to send to the client.
     */
    private final Map<String,List<Map<String,String>>> _searches;
    
    /**
     * Creates an instance of RestRPCMain and then starts up the lime wire core
     * and the rest rpc server thingy.
     */
    public static void main(String[] args) throws Exception {
        RestRPCMain r = new RestRPCMain();
        
        r.startCore();
        r.startRestRPC();
        
        while (true) {
            try { Thread.sleep(1000); }
            catch (Exception e) { }
        }
    }

    /**
     * Initializes everything.
     */
    public RestRPCMain() {
        _injector = Guice.createInjector(new LimeWireCoreModule(MainCallback.class));
        _lwcore = _injector.getInstance(LimeWireCore.class);
        _searches = new HashMap<String, List<Map<String,String>>>();
        _component = new Component();
        _component.getServers().add(Protocol.HTTP, 9090);
        _component.getDefaultHost().attach((_lwappl = new LimeWireApplication(_component.getContext())));
        
        _injector.getInstance(MainCallback.class).setDataStore(this);
        
        // by storing the lime wire core in the application context, we can
        // access it from within the resources, which is necessary so that we 
        // can do things like starting a search.
        //
        _lwappl.getContext().getAttributes().put("lwcore", _lwcore);
        
        _lwappl.getContext().getAttributes().put("dataStore", this);
    }
    
    /**
     * Starts the lime wire core.
     */
    public void startCore() {
        _lwcore.getLifecycleManager().start();
        System.out.println("RestRPCMain::startCore().. started");
    }
    
    /**
     * Tells the lime wire core to stop.
     */
    public void stopCore() {
        _lwcore.getLifecycleManager().shutdown();
        System.out.println("RestRPCMain::stopCore().. stopped");
    }
    
    /**
     * Starts the rest rpc system.
     */
    public void startRestRPC() throws Exception {
        _component.start();
        System.out.println("RestRPCMain::startRestRPC().. started");
    }
    
    /**
     * Tells the rest rpc system to stop.
     */
    public void stopRestRPC() throws Exception {
        _component.stop();
        System.out.println("RestRPCMain::stopRestRPC().. stopped");
    }
    
    /**
     * Part of the DataStore interface.
     */
    public void addSearchResult(RemoteFileDesc rfd, HostData data, Set<? extends IpPort> alts) {
        String guid = new GUID(data.getMessageGUID()).toString();
        Map<String,String> result = new HashMap<String,String>();
        
        result.put("remote_host_addr", rfd.getHost());
        result.put("remote_host_port", Integer.toString(rfd.getPort()));
        result.put("file_name", rfd.getFileName());
        
        synchronized (_searches) {
            List<Map<String,String>> results;
            
            if (null == (results = _searches.get(guid)))
                _searches.put(guid, (results = new ArrayList<Map<String,String>>()));
            
            results.add(result);
        }
    }
    
    /**
     * Part of the DataStore interface.
     */
    public Map<String,List<Map<String,String>>> getAllSearchResults() {
        return _searches;
    }
    
    public static Element makeSearchResultXml(List<Map<String,String>> results, String guid, Element parent, Document document) throws IOException {
        Element search = (Element)parent.appendChild(document.createElement("search"));
        
        search.setAttribute("guid", guid);
        
        for (Map<String,String> result : results) {
            Element search_result = (Element)search.appendChild(document.createElement("search_result"));
            
            for (String key : result.keySet()) {
                Element search_value = (Element)search_result.appendChild(document.createElement(key));
                CDATASection cdata = document.createCDATASection(result.get(key));
                search_value.appendChild(cdata);
            }
        }
        
        return search;
    }
    
    /**
     * 
     *
     */
    private static class LimeWireApplication extends Application {
        
        public LimeWireApplication(Context parentContext) {
            super(parentContext);
            
            getContext().getAttributes().put(Application.KEY, this);
        }
        
        @Override
        public synchronized Restlet createRoot() {
            Router router = new Router(getContext());
            
            router.attachDefault(HelloWorldResource.class);
            router.attach("/search/?{query}", SearchResource.class);
            router.attach("/search/{queryid}", SearchResultsResource.class);
//          router.attach("/search/{queryid}/cancel", ...);
            
            return router;
        }
        
        public LimeWireCore getLimeWireCore() {
            return (LimeWireCore)getContext().getAttributes().get("lwcore");
        }
    }
    
    /**
     * 
     *
     */
    public static class HelloWorldResource extends Resource {
        
        public HelloWorldResource() {
            super();
        }
        
        public HelloWorldResource(Context context, Request request, Response response) {
            super(context, request, response);
            
            getVariants().add(new Variant(MediaType.TEXT_PLAIN));
        }
        
        @Override
        public Representation represent(Variant variant) throws ResourceException {
            Representation representation = new StringRepresentation("Hello, world!", MediaType.TEXT_PLAIN);
            
            return representation;
        }
    }
    
    /**
     * 
     *
     */
    public static class SearchResource extends Resource {
        
        private String _query;
        private GUID _guid;
        
        public SearchResource(Context context, Request request, Response response) {
            super(context, request, response);
            
            Application lwapp = (Application)context.getAttributes().get(Application.KEY);
            LimeWireCore lwcore = (LimeWireCore)lwapp.getContext().getAttributes().get("lwcore");
            
            _query = (String)getRequest().getAttributes().get("query");
            _guid  = new GUID(lwcore.getSearchServices().newQueryGUID());
            
            if (_query != null) {
                try { _query = URLDecoder.decode(_query); }
                catch (IOException e) {
                    // Error service thingy
                }
            }
            
            getVariants().add(new Variant(MediaType.TEXT_XML));
            
            lwcore.getSearchServices().query(_guid.bytes(), _query);
        }
        
        @Override
        public Representation represent(Variant variant) throws ResourceException {
            DomRepresentation representation = null;
            
            try {
                representation = new DomRepresentation(MediaType.TEXT_XML);
                Document document = representation.getDocument();
                Element searches = (Element)document.appendChild(document.createElement("searches"));
                Element search = document.createElement("search");
                
//              document.appendChild(searches);
                searches.appendChild(search);
                
                search.setAttribute("guid", _guid.toString());
            }
            catch (IOException e) {
                System.out.println("SearchResource::represent().. " + e.getMessage());
                e.printStackTrace();
            }
            
            return representation;
        }
    }
    
    /**
     * 
     *
     */
    public static class SearchResultsResource extends Resource {
       
        private GUID _guid;
        
        public SearchResultsResource(Context context, Request request, Response response) {
            super(context, request, response);
            
            String guid = (String)getRequest().getAttributes().get("querid");
            
            if (guid != null && guid.length() != 0)
                _guid = new GUID(guid);
            
            getVariants().add(new Variant(MediaType.TEXT_XML));
        }
        
        @Override
        public Representation represent(Variant variant) throws ResourceException {
            DomRepresentation representation = null;
            Application lwapp = (Application)getContext().getAttributes().get(Application.KEY);
            DataStore dataStore = (DataStore)lwapp.getContext().getAttributes().get("dataStore");
            
            try {
                representation = new DomRepresentation(MediaType.TEXT_XML);
                Document document = representation.getDocument();
                Element searches = (Element)document.appendChild(document.createElement("searches"));
                Map<String,List<Map<String,String>>> searchList = dataStore.getAllSearchResults();
                
                synchronized (searchList) {
                    if (_guid != null) {
                        List<Map<String,String>> results = searchList.get(_guid.toString());
                        
                        RestRPCMain.makeSearchResultXml(results, _guid.toString(), searches, document);
                        searchList.remove(_guid.toString());
                    }
                    else {
                        for (String guid : searchList.keySet()) {
                            List<Map<String,String>> results = searchList.get(guid);
                            RestRPCMain.makeSearchResultXml(results, guid, searches, document);
                        }
                        
                        searchList.clear();
                    }
                }
            }
            catch (IOException e ) {
                System.out.println("SearchResultsResource::represent().. " + e.getMessage());
                e.printStackTrace();
            }
            
            return representation;
        }
    }
    
    /**
     * 
     *
     */
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
