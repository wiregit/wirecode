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
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.FileManagerEvent;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.LimeWireCore;
import com.limegroup.gnutella.LimeWireCoreModule;
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
    private final Map<String,ManagedDownloader> _downloads;
    
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
        _downloads = new HashMap<String,ManagedDownloader>();
        _component = new Component();
        _component.getServers().add(Protocol.HTTP, 9090);
        _component.getDefaultHost().attach((_lwappl = new LimeWireApplication(_component.getContext())));
        
        _injector.getInstance(MainCallback.class).setDataStore(this);
        
        // by storing the lime wire core in the application context, we can
        // access it from within the resources, which is necessary so that we 
        // can do things like starting a search.
        //
        _lwappl.getContext().getAttributes().put("lwcore", _lwcore);
        _lwappl.getContext().getAttributes().put("rfdf", _rfdf);
        _lwappl.getContext().getAttributes().put("dataStore", this);
        _lwappl.getContext().getAttributes().put("cdf", _cdf);
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
        
        System.out.println("urns = " + Common.urnsToString(rfd.getUrns()));
        
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
        result.put("urns", Common.urnsToString(rfd.getUrns()));
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
        result.put("sha1urn", rfd.getSHA1Urn().toString());
        
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
    
    /**
     * Part of the DataStore interface.
     */
    public Map<String,ManagedDownloader> getAllDownloads() {
        return _downloads;
    }
    
    /**
     * Part of the DataStore interface.
     */
    public ManagedDownloader getDownloadForId(String id) {
        return _downloads.get(id);
    }
    
    /**
     * Part of the DataStore interface.
     */
    public void addDownload(ManagedDownloader cd, String id) {
        _downloads.put(id, cd);
    }

    public static Element makeSearchResultXml(List<Map<String,String>> results, String guid, Element parent, Document document) throws IOException {
        Element search = (Element)parent.appendChild(document.createElement("search"));
        
        search.setAttribute("id", guid);
        
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
    
    public static Element makeDownloadXml(Document document, Element parent, CoreDownloader cd, String id) throws IOException {
        Element download = (Element)parent.appendChild(document.createElement("download"));
        
        download.setAttribute("id", id);
        
        RestRPCMain.addCDATAElement(document, download, "id", id);
        RestRPCMain.addCDATAElement(document, download, "amount_read", cd.getAmountRead());
        RestRPCMain.addCDATAElement(document, download, "amount_pending", cd.getAmountPending());
        RestRPCMain.addCDATAElement(document, download, "amount_verified", cd.getAmountVerified());
        RestRPCMain.addCDATAElement(document, download, "is_complete", cd.isCompleted());
        RestRPCMain.addCDATAElement(document, download, "filename", cd.getSaveFile().getName());
//      RestRPCMain.addCDATAElement(document, download, "sha1urn", cd.getSha1Urn().toString());
        RestRPCMain.addCDATAElement(document, download, "state", RestRPCMain.stateToString(cd.getState()));
        
        return download;
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
    
    public static String stateToString(Downloader.DownloadStatus ds) {
        switch (ds) {
        case INITIALIZING:
            return "Initializing";
        case QUEUED:
            return "Queued";
        case CONNECTING:
            return "Connecting";
        case DOWNLOADING:
            return "Downloading";
        case BUSY:
            return "Busy";
        case COMPLETE:
            return "Complete";
        case ABORTED:
            return "Aborted";
        case GAVE_UP:
            return "Gave up";
        case DISK_PROBLEM:
            return "Disk problem";
        case WAITING_FOR_GNET_RESULTS:
            return "Waiting for gnet resuts";
        case CORRUPT_FILE:
            return "Corrupt file";
        case REMOTE_QUEUED:
            return "Remote queued";
        case HASHING:
            return "Hashing";
        case SAVING:
            return "Saving";
        case WAITING_FOR_USER:
            return "Waiting for user";
        case WAITING_FOR_CONNECTIONS:
            return "Waiting for connections";
        case ITERATIVE_GUESSING:
            return "Iterative guessing";
        case QUERYING_DHT:
            return "Querying dht";
        case IDENTIFY_CORRUPTION:
            return "Identify corruption";
        case RECOVERY_FAILED:
            return "Recovery failed";
        case PAUSED:
            return "Paused";
        case INVALID:
            return "Invalid";
        case RESUMING:
            return "Resuming";
        case FETCHING:
            return "Fetching";
        default:
            return "Unknown";
        }
    }
    
    public static RemoteFileDesc createRemoteFileDesc(RemoteFileDescFactory rfdf, Element root) {
        RemoteFileDesc rfd;
        
        rfd = rfdf.createRemoteFileDesc (
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
                Common.stringToURNs(root.getElementsByTagName("urns").item(0).getTextContent()), 
                Boolean.parseBoolean(root.getElementsByTagName("replyToMulticast").item(0).getTextContent()), 
                Boolean.parseBoolean(root.getElementsByTagName("firewalled").item(0).getTextContent()), 
                root.getElementsByTagName("vendor").item(0).getTextContent(),
                null, // proxies
                Long.parseLong(root.getElementsByTagName("createTime").item(0).getTextContent()), 
                Boolean.parseBoolean(root.getElementsByTagName("tlsCapable").item(0).getTextContent())
        );
             
        return rfd;
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
            router.attach("/search/cancel/{queryid}", SearchCancelResource.class);  // stop a search
            
            router.attach("/download", DownloadResource.class);                     // start a download
            router.attach("/download/all", DownloadResultsResource.class);          // get status of all downloads
            router.attach("/download/{id}", DownloadResultsResource.class);         // get status of one download
            router.attach("/download/pause/{id}", DownloadPauseResource.class);     // pause a specific download
            router.attach("/download/resume/{id}", DownloadResumeResource.class);   // resume a specific download
            router.attach("/download/stop/{id}", DownloadStopResource.class);       // stops a specific download
            router.attach("/download/addsource/{id}", DownloadAddSourceResource.class);     // add a source to an existing download
            
            // TODO - search - /search/
            //
            //   if we add "streaming comet" support, then this would 
            //   immediately return results to the relevant gui instead of
            //   piling them up waiting for the gui to ask for the results.
            //
            //
            // TODO - download - /download/
            //
            //   if we add "streaming comet" support, then updates to the on-
            //   going downloads would be returned to the gui immediately. we
            //   don't want to swap the gui with an update every time a single
            //   byte of data is received - so, this would need to be throttled
            //   in some fashion.
            //
            //
            // TODO - authentication - /auth/
            //
            //   presently this interface is one big security hole. some form 
            //   of authentication must be implemented. given the non-
            //   persistent-socket nature of this REST protocol, authentication 
            //   must provide a token which is handed back to the core for all 
            //   subsequent requests. this token should be bound to a 
            //   particular ip address. the entirety of the communication 
            //   between the gui and the core *should* be encrypted.
            //
            //   an interesting alternative to having to encrypt all of the 
            //   data going between the gui and the core (so as to prevent 
            //   someone from obtaining the session token) is that of a set of 
            //   "challenges" sent by the core to the gui in its various 
            //   responses to requests - which challenges could be used by the 
            //   gui when sending future requests.
            //
            // TODO - file manager stuff - /file/
            //
            //
            //   if we assume that the gui can be run from a machaine other 
            //   than the machine on which the core is running, then we must 
            //   provide an interface that allows the gui to "browse" the file 
            //   system in its entirety - just as one can, presently, with Lime
            //   Wire in the Sharing preferences when selecting which 
            //   directories to share.
            //
            //   get list of files (including various stats - size, path, uploads, hits, locations, shared, etc.)
            //   delete a file
            //   rename a file
            //   set file/directory as shared/not shared
            //   list all shared files
            //   list all store files (?)
            //   list all saved files
            //   list all incomplete files
            //
            //   should the core send the icon?
            //   we must provide full file system access
            // 
            //   image, audio and video previews will each require that we can send files to the GUI
            //
            //   /file/{command}/?{path} => /file/delete/?/path/to/file
            //
            //     list | info | stat
            //     download | get (read offset)
            //     upload | put (write offset; or just append?)
            //     get preview (generate a preview on the core side. helpful?)
            //     stream (if this is effectively any different than download/get)
            //     move | rename (same thing on one level)
            //     share
            //     unshare
            // 
            //   Apache Commons VFS & VFSJFileChooser
            //
            //     creating a vfs plugin for our rest-based "file system" looks
            //     to be a pain. certainly not impossible though. I don't see it
            //     as making life that much easier for Turkel, however, so it
            //     probably really isn't worth the effort.
            //
            //   FileDesc
            //
            //     Semes like a good idea to implement FileDesc for 
            //     representing files via REST.
            //
            //     We'll need to also add a "refresh" of some sort, so that it
            //     can synchronize with the remote file system
            //
            //   File Event Callbacks
            //
            //     We need to be able to register file event listeners that 
            //     will fire when an event occurs on a file / directory on the 
            //     remote file system. Unless we go with the streaming variety 
            //     of a comet-style server, we will not be able to promptly 
            //     inform the gui that a file event has occured. But can 
            //     restlet support this? I don't know.
            //
            //
            // TODO - streaming comet
            //
            //   It seems necessary for the server to support the "streaming 
            //   comet" approach to supporting low-latency, non-polling responses
            //   to asynchronous events. Anything less than supporting streaming
            //   comet will amount to hackery that will provide a sub-optimal
            //   user experience.
            //
            //   If Restlet's threading model (in particular) won't freak out
            //   at being made to persistently hold open sockets, then that's
            //   the path of least resistance. Otherwise, either another xmlrpc
            //   implementation will need to be found that is suitable, or one
            //   will have to be rolled in-house.
            //
            //   It's not a significant amount of work to write an http server,
            //   and support basic get, post, multi-part, etc; although it is
            //   error prone and requires a quite a bit of testing. such a home
            //   rolled system would likely provide the best performance.
            //
            //
            // TODO - connections
            //
            //   connections tab in the existing client.
            //
            //
            // TODO - logging
            //
            //   logging tab in the existing client.
            //
            //
            // TODO - monitor
            //
            //   monitor tab in the existing client.
            //
            //
            // TODO - console
            //
            //   console tab in the existing client.
            //
            //
            // TODO - preferences
            //
            //   all of the preferences that pertain to the operation of the 
            //   core will need a restlet interface for retrieving and setting.
            //
            //   we must either force there to be only a single gui controlling
            //   a given core at a time, or we must add preference event 
            //   listeners such that a gui can be notified when a preference 
            //   changes - as the preference may have been modified by another
            //   gui.
            //
            //   with the dream of having light-weight web and smartphone guis,
            //   it seems prudent to be able to support multiple, simultaneous
            //   guis on a core. it's a little more work, possibly, but it
            //   seems worth while.
            //
            
            
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
        
        public DataStore getDataStore() {
            return (DataStore)getContext().getAttributes().get("dataStore");
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
        private Document _document;
        
        public SearchResource(Context context, Request request, Response response) {
            super(context, request, response);
            
            Application lwapp = (Application)context.getAttributes().get(Application.KEY);
            LimeWireCore lwcore = (LimeWireCore)lwapp.getContext().getAttributes().get("lwcore");
            
            try {
                if (getRequest().isEntityAvailable())
                    _document = getRequest().getEntityAsDom().getDocument();
            }
            catch (IOException ioe) {
                System.err.println("SearchResource::SearchResource().. " + ioe.getMessage());
                ioe.printStackTrace();
            }
            
            _query = (String)getRequest().getAttributes().get("query");
            _guid  = new GUID(lwcore.getSearchServices().newQueryGUID());
            
            if (_query != null) {
                try { _query = URLDecoder.decode(_query); }
                catch (IOException e) {
                    // Error service thingy
                }
            }
            
            getVariants().add(new Variant(MediaType.TEXT_XML));
            
            if (_document == null)
                lwcore.getSearchServices().query(_guid.bytes(), _query);
            else {
                Element root = _document.getDocumentElement();
                String typestr = root.getTagName();
                com.limegroup.gnutella.MediaType mediaType = null;
                
                if (typestr.equals("videos"))
                    mediaType = com.limegroup.gnutella.MediaType.getVideoMediaType();
                else if (typestr.equals("audios"))
                    mediaType = com.limegroup.gnutella.MediaType.getAudioMediaType();
                else if (typestr.equals("images"))
                    mediaType = com.limegroup.gnutella.MediaType.getImageMediaType();
                else if (typestr.equals("documents"))
                    mediaType = com.limegroup.gnutella.MediaType.getDocumentMediaType();
                else if (typestr.equals("applications"))
                    mediaType = com.limegroup.gnutella.MediaType.getProgramMediaType();
                else
                    mediaType = com.limegroup.gnutella.MediaType.getAnyTypeMediaType();
                
                lwcore.getSearchServices().query(_guid.bytes(), _query, Common.xmlToString(root), mediaType);
            }
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
                
                search.setAttribute("id", _guid.toString());
            }
            catch (IOException e) {
                System.out.println("SearchResource::represent().. " + e.getMessage());
                e.printStackTrace();
            }
            
            return representation;
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
                Element searches = (Element)document.appendChild(document.createElement("searches"));
                Element search = (Element)searches.appendChild(document.createElement("search"));
                
                search.setAttribute("id", _guid.toString());
            }
            catch (IOException e) {
                System.out.println("SearchResource::acceptRepresentation().. " + e.getMessage());
                e.printStackTrace();
            }
             
            getResponse().setEntity(representation);
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
            
            String guid = (String)getRequest().getAttributes().get("queryid");
            
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
                        
                        if (results != null)
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
    public static class SearchCancelResource extends Resource {
       
        private GUID _guid;
        
        public SearchCancelResource(Context context, Request request, Response response) {
            super(context, request, response);
            
            String guid = (String)getRequest().getAttributes().get("queryid");
            
            if (guid != null && guid.length() != 0)
                _guid = new GUID(guid);
            
            getVariants().add(new Variant(MediaType.TEXT_XML));
        }
        
        @Override
        public Representation represent(Variant variant) throws ResourceException {
            DomRepresentation representation = null;
            Application lwapp = (Application)getContext().getAttributes().get(Application.KEY);
            LimeWireCore lwcore = (LimeWireCore)lwapp.getContext().getAttributes().get("lwcore");
//          DataStore dataStore = (DataStore)lwapp.getContext().getAttributes().get("dataStore");
            
            try {
                representation = new DomRepresentation(MediaType.TEXT_XML);
                Document document = representation.getDocument();
                Element searches = (Element)document.appendChild(document.createElement("searches"));
//              Map<String,List<Map<String,String>>> searchList = dataStore.getAllSearchResults();
                
                lwcore.getSearchServices().stopQuery(_guid);
                
                Element search = (Element)searches.appendChild(document.createElement("search"));
                search.setAttribute("id", _guid.toString());
                
                /*
                synchronized (searchList) {
                    if (_guid != null) {
                        List<Map<String,String>> results = searchList.get(_guid.toString());
                        
                        if (results != null)
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
                */
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
     */
    public static class DownloadResource extends Resource {
        
        private GUID _guid;
        private RemoteFileDesc _rfd;
        private String _error;
        
        public DownloadResource(Context context, Request request, Response response) {
            super(context, request, response);
            
            Application lwapp = (Application)context.getAttributes().get(Application.KEY);
            LimeWireCore lwcore = (LimeWireCore)lwapp.getContext().getAttributes().get("lwcore");
            RemoteFileDescFactory rfdf = ((LimeWireApplication)lwapp).getRemoteFileDescFactory();
            DomRepresentation dom = getRequest().getEntityAsDom();
            CoreDownloaderFactory cdf = ((LimeWireApplication)lwapp).getCoreDownloaderFactory();
            DownloadManager dm = ((LimeWireApplication)lwapp).getDownloadManager();
            DataStore ds = ((LimeWireApplication)lwapp).getDataStore();
            
            try {
                Document doc = dom.getDocument();
                Element root = doc.getDocumentElement();
                
                _guid  = new GUID(lwcore.getSearchServices().newQueryGUID());
                
                _rfd = RestRPCMain.createRemoteFileDesc(rfdf, root);
                
                ManagedDownloader downloader = cdf.createManagedDownloader(new RemoteFileDesc[]{_rfd}, null, null, null, true);
                
                // add the download to the data store, so that we can keep
                // track of all of the downloads.
                //
                ds.addDownload(downloader, _guid.toString());
                
                downloader.initialize();
                dm.remove(downloader, true);
                downloader.startDownload();
            }
            catch (Exception e) {
                _error = e.getMessage();
                System.err.println("Error: " + e.getMessage());
                e.printStackTrace();
            }
            
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
                
               download.setAttribute("id", _guid.toString());
               download.setAttribute("error", _error);
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
//          DownloadManager dm = ((LimeWireApplication)lwapp).getDownloadManager();
            DataStore ds = ((LimeWireApplication)lwapp).getDataStore();
            
            try {
                representation = new DomRepresentation(MediaType.TEXT_XML);
                Document document = representation.getDocument();
                Element downloads = (Element)document.appendChild(document.createElement("downloads"));
                
                // if the request specified a download id, then just give them 
                // info for that download.
                //
                if (_guid != null) {
                    CoreDownloader cd = ds.getDownloadForId(_guid.toString());
                    
                    if (cd != null) {
                        RestRPCMain.makeDownloadXml(document, downloads, cd, _guid.toString());
                    }
                }
                
                // otherwise, give them the info for all of the downloads.
                //
                else {
                    Map<String,ManagedDownloader> downloadList = ds.getAllDownloads();
                    
                    for (String did : downloadList.keySet()) {
                        ManagedDownloader cd = downloadList.get(did);
                        RestRPCMain.makeDownloadXml(document, downloads, cd, did);
                    }
                    
                    /*
                    for (CoreDownloader cd : dm.getAllDownloaders()) {
                        RestRPCMain.makeDownloadXml(document, downloads, cd);
                    }
                    */
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
    public static class DownloadStopResource extends Resource {
       
        private GUID _guid;
        
        public DownloadStopResource(Context context, Request request, Response response) {
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
            DataStore ds = ((LimeWireApplication)lwapp).getDataStore();
            
            try {
                representation = new DomRepresentation(MediaType.TEXT_XML);
                Document document = representation.getDocument();
                Element downloads = (Element)document.appendChild(document.createElement("downloads"));
                
                if (_guid != null) {
                    ManagedDownloader md = ds.getDownloadForId(_guid.toString());
                    
                    if (md != null) {
                        md.stop();
                        RestRPCMain.makeDownloadXml(document, downloads, md, _guid.toString());
                    }
                }
            }
            catch (IOException e ) {
                System.out.println("DownloadStopResource::represent().. " + e.getMessage());
                e.printStackTrace();
            }
            
            return representation;
        }
    }

    /**
     * 
     *
     */
    public static class DownloadResumeResource extends Resource {
       
        private GUID _guid;
        private String _error;
        
        public DownloadResumeResource(Context context, Request request, Response response) {
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
            DataStore ds = ((LimeWireApplication)lwapp).getDataStore();
            
            try {
                representation = new DomRepresentation(MediaType.TEXT_XML);
                Document document = representation.getDocument();
                Element downloads = (Element)document.appendChild(document.createElement("downloads"));
                
                if (_guid != null) {
                    ManagedDownloader md = ds.getDownloadForId(_guid.toString());
                    
                    if (md != null) {
                        if (!md.resume())
                            _error = "Failed to resume. Sorry.";
                        
                        Element download = RestRPCMain.makeDownloadXml(document, downloads, md, _guid.toString());
                        download.setAttribute("error", _error);
                    }
                }
            }
            catch (IOException e ) {
                System.out.println("DownloadStopResource::represent().. " + e.getMessage());
                e.printStackTrace();
            }
            
            return representation;
        }
    }
    
    /**
     * 
     *
     */
    public static class DownloadPauseResource extends Resource {
       
        private GUID _guid;
        
        public DownloadPauseResource(Context context, Request request, Response response) {
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
            DataStore ds = ((LimeWireApplication)lwapp).getDataStore();
            
            try {
                representation = new DomRepresentation(MediaType.TEXT_XML);
                Document document = representation.getDocument();
                Element downloads = (Element)document.appendChild(document.createElement("downloads"));
                
                if (_guid != null) {
                    ManagedDownloader md = ds.getDownloadForId(_guid.toString());
                    
                    if (md != null) {
                        md.pause();
                        RestRPCMain.makeDownloadXml(document, downloads, md, _guid.toString());
                    }
                }
            }
            catch (IOException e ) {
                System.out.println("DownloadStopResource::represent().. " + e.getMessage());
                e.printStackTrace();
            }
            
            return representation;
        }
    }
    
    /**
     * Add a source to an existing download.
     * 
     */
    public static class DownloadAddSourceResource extends Resource {
        
        private GUID _guid;
        private RemoteFileDesc _rfd;
        private String _error;
        private ManagedDownloader _downloader;
        
        public DownloadAddSourceResource(Context context, Request request, Response response) {
            super(context, request, response);
            
            Application lwapp = (Application)context.getAttributes().get(Application.KEY);
//          LimeWireCore lwcore = (LimeWireCore)lwapp.getContext().getAttributes().get("lwcore");
            RemoteFileDescFactory rfdf = ((LimeWireApplication)lwapp).getRemoteFileDescFactory();
            DomRepresentation dom = getRequest().getEntityAsDom();
//          CoreDownloaderFactory cdf = ((LimeWireApplication)lwapp).getCoreDownloaderFactory();
//          DownloadManager dm = ((LimeWireApplication)lwapp).getDownloadManager();
            DataStore ds = ((LimeWireApplication)lwapp).getDataStore();
            
            try {
                Document doc = dom.getDocument();
                Element root = doc.getDocumentElement();
                
                String guid = (String)getRequest().getAttributes().get("id");
                
                if (guid != null && guid.length() != 0)
                    _guid = new GUID(guid);
                
                if (_guid != null) {
                    _rfd = RestRPCMain.createRemoteFileDesc(rfdf, root);
                    _downloader = ds.getDownloadForId(_guid.toString());
                    
                    if (_downloader != null) {
                        _downloader.addDownload(_rfd, true);
                    }
                }
                
                /*
                _guid  = new GUID(lwcore.getSearchServices().newQueryGUID());
                
                // get the target ManagedDownloader
                // downloader.addDownload(rfd, true);
                
                ManagedDownloader downloader = cdf.createManagedDownloader(new RemoteFileDesc[]{_rfd}, null, null, null, true);
                
                // add the download to the data store, so that we can keep
                // track of all of the downloads.
                //
                ds.addDownload(downloader, _guid.toString());
                
                downloader.initialize();
                dm.remove(downloader, true);
                downloader.startDownload();
                */
            }
            catch (Exception e) {
                _error = e.getMessage();
                System.err.println("Error: " + e.getMessage());
                e.printStackTrace();
            }
            
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
                
                download.setAttribute("id", _guid.toString());
                download.setAttribute("error", _error);
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
