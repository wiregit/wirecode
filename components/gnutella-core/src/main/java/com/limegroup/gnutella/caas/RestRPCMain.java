package com.limegroup.gnutella.caas;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ActivityCallback;
//import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.DownloadManager;
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
import com.limegroup.gnutella.downloader.CoreDownloader;
import com.limegroup.gnutella.downloader.CoreDownloaderFactory;
import com.limegroup.gnutella.downloader.ManagedDownloader;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;
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
     * 
     */
    private final RemoteFileDescFactory _rfdf;
    
    /**
     * 
     */
    private final CoreDownloaderFactory _cdf;
    
    /**
     * 
     */
    private final DownloadManager _dm;
    
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
        _rfdf = _injector.getInstance(RemoteFileDescFactory.class);
        _cdf = _injector.getInstance(CoreDownloaderFactory.class);
        _dm = _injector.getInstance(DownloadManager.class);
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
        
        //
        //
        _lwappl.getContext().getAttributes().put("rfdf", _rfdf);
        
        //
        //
        _lwappl.getContext().getAttributes().put("dataStore", this);
        
        //
        //
        _lwappl.getContext().getAttributes().put("cdf", _cdf);
        
        //
        //
        _lwappl.getContext().getAttributes().put("dm", _dm);
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
        
        // host, port, index, filename, 
        // size, clientGUID, speed, chat, quality, browseHost, 
        // xmlDoc, urns, replyToMulticast,
        // firewalled, vender, proxies, createTime,
        // FWTVersion, pe, tlsCapable, http11
        // networkInstanceUtils
        
        result.put("host", rfd.getHost());
        result.put("port", Integer.toString(rfd.getPort()));
        result.put("index", Long.toString(rfd.getIndex()));
        result.put("filename", rfd.getFileName());
        
        result.put("size", Long.toString(rfd.getSize()));
        result.put("clientGUID", GUID.toHexString(rfd.getClientGUID()));
        result.put("speed", Integer.toString(rfd.getSpeed()));
        result.put("chat", Boolean.toString(rfd.isChatEnabled()));
        result.put("quality", Integer.toString(rfd.getQuality()));
        result.put("browseHost", Boolean.toString(rfd.isBrowseHostEnabled()));
        
//      result.put("xmlDoc", rfd.getXMLDocument().getXMLString());
//      result.put("urns", serializeUrns(rfd.getUrns()));
        result.put("replyToMulticast", Boolean.toString(rfd.isReplyToMulticast()));
        
        result.put("firewalled", Boolean.toString(rfd.isFirewalled()));
        result.put("vendor", rfd.getVendor());
//      result.put("proxies", );
        result.put("createTime", Long.toString(rfd.getCreationTime()));
        
//      result.put("FWTVersion", );
//      result.put("pe", rfd.getPushAddr().toString());
        result.put("tlsCapable", Boolean.toString(rfd.isTLSCapable()));
        result.put("http11", Boolean.toString(rfd.isHTTP11()));

//      result.put("networkInstanceUtils", );

//      result.put("remote_host_addr", rfd.getHost());
//      result.put("remote_host_port", Integer.toString(rfd.getPort()));
//      result.put("file_name", rfd.getFileName());
        
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
    
    public static Element addCDATAElement(Document document, Element parent, String name, String value) {
        Element child = (Element)parent.appendChild(document.createElement(name));
        CDATASection cdata = document.createCDATASection(value);
        child.appendChild(cdata);
        
        return child;
    }
    
    public static Element addCDATAElement(Document document, Element parent, String name, int value) {
        return addCDATAElement(document, parent, name, Integer.toString(value));
    }
    
    public static Element addCDATAElement(Document document, Element parent, String name, long value) {
        return addCDATAElement(document, parent, name, Long.toString(value));
    }
    
    public static Element addCDATAElement(Document document, Element parent, String name, boolean value) {
        return addCDATAElement(document, parent, name, Boolean.toString(value));
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
            router.attach("/search/?{query}", SearchResource.class);                // start a new search
            router.attach("/search/{queryid}", SearchResultsResource.class);        // get search results
//          router.attach("/search/{queryid}/cancel", ...);                         // stop a search
            
            router.attach("/download", DownloadResource.class);                     // start a download
            router.attach("/download/all", DownloadResultsResource.class);          // get status of all downloads
//          router.attach("/download/{id}", DownloadResultsResource.class);         // get status of one download
//          router.attach("/download/pause/{id}", DownloadPauseResource.class);     // pause a specific download
            
            return router;
        }
        
        public LimeWireCore getLimeWireCore() {
            return (LimeWireCore)getContext().getAttributes().get("lwcore");
        }
        
        public RemoteFileDescFactory getRemoteFileDescFactory() {
            return (RemoteFileDescFactory)getContext().getAttributes().get("rfdf");
        }
        
        public CoreDownloaderFactory getCoreDownloaderFactory() {
            return (CoreDownloaderFactory)getContext().getAttributes().get("cdf");
        }
        
        public DownloadManager getDownloadManager() {
            return (DownloadManager)getContext().getAttributes().get("dm");
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
    public static class DownloadResource extends Resource {
        
        private GUID _guid;
        private RemoteFileDesc _rfd;
        
        public DownloadResource(Context context, Request request, Response response) {
            super(context, request, response);
            
            Application lwapp = (Application)context.getAttributes().get(Application.KEY);
            LimeWireCore lwcore = (LimeWireCore)lwapp.getContext().getAttributes().get("lwcore");
            RemoteFileDescFactory rfdf = ((LimeWireApplication)lwapp).getRemoteFileDescFactory();
//          DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DomRepresentation dom = getRequest().getEntityAsDom();
            CoreDownloaderFactory cdf = ((LimeWireApplication)lwapp).getCoreDownloaderFactory();
            DownloadManager dm = ((LimeWireApplication)lwapp).getDownloadManager();
            
            try {
//              System.out.println("DownloadResource::DownloadResource().. text = '" + getRequest().getEntity().getText() + "'");
                
                Document doc = dom.getDocument();
                Element root = doc.getDocumentElement();
//              NodeList values = root.getChildNodes();
                
                
                // read values from "values"
                // create rfd
                // start download
                
                
                /*
                byte[] bytes = getRequest().getEntity().getText().getBytes();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document doc = db.parse(new ByteArrayInputStream(bytes));
                Node root = doc.getFirstChild();
                */
                
                _rfd = rfdf.createRemoteFileDesc (
                        root.getElementsByTagName("host").item(0).getTextContent(), 
                        Integer.parseInt(root.getElementsByTagName("port").item(0).getTextContent()), 
                        Long.parseLong(root.getElementsByTagName("index").item(0).getTextContent()), 
                        root.getElementsByTagName("filename").item(0).getTextContent(),
                        Long.parseLong(root.getElementsByTagName("size").item(0).getTextContent()),
                        GUID.fromHexString(root.getElementsByTagName("clientGUID").item(0).getTextContent()),
                        Integer.parseInt(root.getElementsByTagName("speed").item(0).getTextContent()), 
                        Boolean.parseBoolean(root.getElementsByTagName("chat").item(0).getTextContent()), 
                        Integer.parseInt(root.getElementsByTagName("quality").item(0).getTextContent()), 
                        Boolean.parseBoolean(root.getElementsByTagName("browseHost").item(0).getTextContent()), 
                        null, // xmlDoc 
                        null, // urns 
                        Boolean.parseBoolean(root.getElementsByTagName("replyToMulticast").item(0).getTextContent()), 
                        Boolean.parseBoolean(root.getElementsByTagName("firewalled").item(0).getTextContent()), 
                        root.getElementsByTagName("vendor").item(0).getTextContent(),
                        null, // proxies
                        Long.parseLong(root.getElementsByTagName("createTime").item(0).getTextContent()), 
                        Boolean.parseBoolean(root.getElementsByTagName("tlsCapable").item(0).getTextContent())
                );
                
                ManagedDownloader downloader = cdf.createManagedDownloader(new RemoteFileDesc[]{_rfd}, null, null, null, true);
                
                downloader.initialize();
                dm.remove(downloader, true);
                downloader.startDownload();
            }
            catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                e.printStackTrace();
            }
            
            _guid  = new GUID(lwcore.getSearchServices().newQueryGUID());
            
           /*
            _query = (String)getRequest().getAttributes().get("query");
            _guid  = new GUID(lwcore.getSearchServices().newQueryGUID());
            
            if (_query != null) {
                try { _query = URLDecoder.decode(_query); }
                catch (IOException e) {
                    // Error service thingy
                }
            }
            */
            
            getVariants().add(new Variant(MediaType.TEXT_XML));
       }
        
        public boolean allowPost () {
            return true;
        }
        
        @Override
        public void acceptRepresentation(Representation entity) throws ResourceException {
            DomRepresentation representation = null;
            
            try {
                representation = new DomRepresentation(MediaType.TEXT_XML);
                Document document = representation.getDocument();
                Element downloads = (Element)document.appendChild(document.createElement("downloads"));
                Element download = document.createElement("download");
                
                downloads.appendChild(download);
                
                download.setAttribute("guid", _guid.toString());
            }
            catch (IOException e) {
                System.out.println("DownloadResource::represent().. " + e.getMessage());
                e.printStackTrace();
            }
            
            getResponse().setEntity(representation);
        }
    }
    
    /**
     * 
     *
     */
    public static class DownloadResultsResource extends Resource {
       
        private GUID _guid;
        
        public DownloadResultsResource(Context context, Request request, Response response) {
            super(context, request, response);
            
            String guid = (String)getRequest().getAttributes().get("id");
            
            if (guid != null && guid.length() != 0)
                _guid = new GUID(guid);
            
            getVariants().add(new Variant(MediaType.TEXT_XML));
        }
        
        @Override
        public Representation represent(Variant variant) throws ResourceException {
            DomRepresentation representation = null;
            Application lwapp = (Application)getContext().getAttributes().get(Application.KEY);
            DownloadManager dm = ((LimeWireApplication)lwapp).getDownloadManager();
            
            try {
                representation = new DomRepresentation(MediaType.TEXT_XML);
                Document document = representation.getDocument();
                Element downloads = (Element)document.appendChild(document.createElement("downloads"));
                
                for (CoreDownloader cd : dm.getAllDownloaders()) {
                    Element download = (Element)downloads.appendChild(document.createElement("download"));
                    
                    RestRPCMain.addCDATAElement(document, download, "amount_read", cd.getAmountRead());
                    RestRPCMain.addCDATAElement(document, download, "amount_pending", cd.getAmountPending());
                    RestRPCMain.addCDATAElement(document, download, "amount_verified", cd.getAmountVerified());
                    RestRPCMain.addCDATAElement(document, download, "is_complete", cd.isCompleted());
                    RestRPCMain.addCDATAElement(document, download, "filename", cd.getSaveFile().getName());
                }
            }
            catch (IOException e ) {
                System.out.println("DownloadResultsResource::represent().. " + e.getMessage());
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
