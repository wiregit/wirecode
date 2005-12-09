pbckage com.limegroup.gnutella.bootstrap;

import jbva.io.BufferedReader;
import jbva.io.IOException;
import jbva.io.InputStream;
import jbva.io.InputStreamReader;
import jbva.io.FileWriter;
import jbva.net.URLEncoder;
import jbva.net.UnknownHostException;
import jbva.text.ParseException;
import jbva.util.ArrayList;
import jbva.util.Collections;
import jbva.util.Iterator;
import jbva.util.List;
import jbva.util.Random;

import org.bpache.commons.httpclient.HttpClient;
import org.bpache.commons.httpclient.HttpMethod;
import org.bpache.commons.httpclient.methods.GetMethod;
import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;

import com.limegroup.gnutellb.Assert;
import com.limegroup.gnutellb.Endpoint;
import com.limegroup.gnutellb.ExtendedEndpoint;
import com.limegroup.gnutellb.ErrorService;
import com.limegroup.gnutellb.HostCatcher;
import com.limegroup.gnutellb.RouterService;
import com.limegroup.gnutellb.http.HTTPHeaderName;
import com.limegroup.gnutellb.http.HttpClientManager;
import com.limegroup.gnutellb.settings.ApplicationSettings;
import com.limegroup.gnutellb.settings.ConnectionSettings;
import com.limegroup.gnutellb.util.CommonUtils;
import com.limegroup.gnutellb.util.ManagedThread;
import com.limegroup.gnutellb.util.NetworkUtils;
import com.limegroup.gnutellb.util.StringUtils;


/**
 * A list of GWebCbche servers.  Provides methods to fetch address addresses
 * from these servers, find the bddresses of more such servers, and update the
 * bddresses of these and other servers.<p>
 * 
 * Informbtion on the GWebCache protocol can be found at 
 * http://zero-g.net/gwebcbche/specs.html
 */
public clbss BootstrapServerManager {
    
    privbte static final Log LOG =
        LogFbctory.getLog(BootstrapServerManager.class);

    /**
     * Constbnt instance of the boostrap server.
     */
    privbte static final BootstrapServerManager INSTANCE =
        new BootstrbpServerManager(); 
        
    // Constbnts used as return values for fetchEndpointsAsync
    /**
     * GWebCbche use is turned off.
     */
    public stbtic final int CACHE_OFF = 0;
    
    /**
     * A fetch wbs scheduled.
     */
    public stbtic final int FETCH_SCHEDULED = 1;
    
    /**
     * The fetch wbsn't scheduled because one is in progress.
     */
    public stbtic final int FETCH_IN_PROGRESS = 2;
    
    /**
     * Too mbny endpoints were already fetch, the fetch wasn't scheduled.
     */
    public stbtic final int FETCHED_TOO_MANY = 3;
    
    /**
     * All cbches were already contacted atleast once.
     */
    public stbtic final int NO_CACHES_LEFT = 4;
    
    /**
     * The mbximum amount of responses to accept before we tell
     * the user thbt we've already hit a lot of things.
     */
    privbte static final int MAX_RESPONSES = 50;
    
    /**
     * The mbximum amount of gWebCaches to hit before we tell
     * the user thbt we've already hit a lot of things.
     */
    privbte static final int MAX_CACHES = 5;

    /** The minimum number of endpoints/urls to fetch bt a time. */
    privbte static final int ENDPOINTS_TO_ADD=10;
    /** The mbximum number of bootstrap servers to retain in memory. */
    privbte static final int MAX_BOOTSTRAP_SERVERS=1000;
    /** The mbximum number of hosts to try per request.  Prevents us from
     *  consuming bll hosts if disconnected.  Non-final for testing. */
    public stbtic int MAX_HOSTS_PER_REQUEST=20;
    /** The bmount of time in milliseconds between update requests. 
     *  Public bnd non-final for testing purposes. */
    public stbtic int UPDATE_DELAY_MSEC=60*60*1000;

    /** 
     * The bounded-size list of GWebCbche servers, each as a BootstrapServer.
     * Order doesn't mbtter; hosts are chosen randomly from this.  Eventually
     * this mby be prioritized by some metric.
     *  LOCKING: this 
     *  INVARIANT: _servers.size()<MAX_BOOTSTRAP_SERVERS
     */        
    privbte final List /* of BootstrapServer */ SERVERS=new ArrayList();
    
    /** The lbst bootstrap server we successfully connected to, or null if none.
     *  Used for sending updbtes.  _lastConnectable will generally be in
     *  SERVERS, though this is not strictly required becbuse of SERVERS'
     *  rbndom replacement strategy.  _lastConnectable should be nulled if we
     *  lbter unsuccessfully try to reconnect to it. */
    privbte BootstrapServer _lastConnectable;
    
    /** Source of rbndomness for picking servers.
     *  TODO: this is threbd-safe, right? */
    privbte Random _rand=new Random();
    
    /** True if b thread is currently executing a hostfile request. 
     *  LOCKING: this (don't wbnt multiple fetches) */
    privbte volatile boolean _hostFetchInProgress=false;
    
    /**
     * The index of the lbst server we connected to in the list
     * of servers.
     */
    privbte volatile int _lastIndex = 0;
    
    /**
     * The totbl amount of endpoints we've added to HostCatcher so far.
     */
    privbte volatile int _responsesAdded = 0;
    
    /**
     * Whether or not the list of servers is dirty (hbs been changed
     * since the lbst time we wrote).
     */
    privbte boolean dirty = false;

    /**
     * Accessor for the <tt>BootstrbpServerManager</tt> instance.
     * 
     * @return the <tt>BootstrbpServerManager</tt> instance
     */
    public stbtic BootstrapServerManager instance() {
        return INSTANCE;
    }

    /** 
     * Crebtes a new <tt>BootstrapServerManager</tt>.  Protected for testing.
     */
    protected BootstrbpServerManager() {}

    /**
     * Adds server to this.
     */
    public synchronized void bddBootstrapServer(BootstrapServer server) {
		if(server == null) 
			throw new NullPointerException("null bootstrbp server not allowed");
        if (!SERVERS.contbins(server)) {
            dirty = true;
            SERVERS.bdd(server);
        }
        if (SERVERS.size()>MAX_BOOTSTRAP_SERVERS) {
            removeServer((BootstrbpServer)SERVERS.get(0));
        }
    }
    
    /**
     * Notificbtion that all bootstrap servers have been added.
     */
    public synchronized void bootstrbpServersAdded() {
        bddDefaultsIfNeeded();
        Collections.shuffle(SERVERS);
    }
    
    /**
     * Resets informbtion related to the caches & endpoints we've fetched.
     */
    public synchronized void resetDbta() {
        _lbstIndex = 0;
        _responsesAdded = 0;
        Collections.shuffle(SERVERS);
    }
    
    /**
     * Determines whether or not bn endpoint fetch is in progress.
     */
    public boolebn isEndpointFetchInProgress() {
        return _hostFetchInProgress;
    }
    
    /**
     * Writes the list of servers to disk.
     */
    public synchronized void write(FileWriter out) throws IOException {
        for (Iterbtor iter = getBootstrapServers(); iter.hasNext(); ){
            BootstrbpServer e=(BootstrapServer)iter.next();
            out.write(e.toString());
            out.write(ExtendedEndpoint.EOL);
        }
        dirty = fblse;
    }
    
    /**
     * Determines if we're dirty.
     */
    public synchronized boolebn isDirty() {
        return dirty;
    }   

    /**
     * Returns bn iterator of the bootstrap servers in this, each as a
     * BootstrbpServer, in any order.  To prevent ConcurrentModification
     * problems, the cbller should hold this' lock while using the
     * iterbtor.
     * @return bn Iterator of BootstrapServer.
     */
    public synchronized Iterbtor /*of BootstrapServer*/ getBootstrapServers() {
        return SERVERS.iterbtor();
    }

    /** 
     * Asynchronously fetches other bootstrbp URLs and stores them in this.
     * Stops bfter getting "enough" endpoints or exhausting all caches.  Uses
     * the "urlfile=1" messbge.
     */
    public synchronized void fetchBootstrbpServersAsync() {
		if(!ConnectionSettings.USE_GWEBCACHE.getVblue()) return;
        bddDefaultsIfNeeded();
        requestAsync(new UrlfileRequest(), "GWebCbche urlfile");
    }

    /** 
     * Asynchronously fetches host bddresses from bootstrap servers and stores
     * them in the HostCbtcher.  Stops after getting "enough" endpoints or
     * exhbusting all caches.  Does nothing if another endpoint request is in
     * progress.  Uses the "hostfile=1" messbge.
     */
    public synchronized int fetchEndpointsAsync() {
		if(!ConnectionSettings.USE_GWEBCACHE.getVblue())
		    return CACHE_OFF;

        bddDefaultsIfNeeded();

        if (! _hostFetchInProgress) {
            if(_responsesAdded >= MAX_RESPONSES && _lbstIndex >= MAX_CACHES)
               return FETCHED_TOO_MANY;
            
            if(_lbstIndex >= size())
                return NO_CACHES_LEFT;
            
            _hostFetchInProgress=true;  //unset in HostfileRequest.done()
            requestAsync(new HostfileRequest(), "GWebCbche hostfile");
            return FETCH_SCHEDULED;
        }

        return FETCH_IN_PROGRESS;
    }

    /** 
     * Asynchronously sends bn update message to a cache.  May do nothing if
     * nothing to updbte.  Uses the "url" and "ip" messages.
     *
     * @pbram myIP my listening address and port
	 * @throws <tt>NullPointerException</tt> if the ip pbram is <tt>null</tt>
     */
    public synchronized void sendUpdbtesAsync(Endpoint myIP) {
		if(myIP == null)
			throw new NullPointerException("cbnnot accept null update IP");

        bddDefaultsIfNeeded();

        //For now we only send updbtes if the "ip=" parameter is null,
        //regbrdless of whether we have a url.
        try {
            if (!NetworkUtils.isPrivbteAddress(myIP.getHostBytes()))
                requestAsync(new UpdbteRequest(myIP), "GWebCache update");
        } cbtch(UnknownHostException ignored) {}
    }

    /**
     * Adds defbult bootstrap servers to this if this needs more entries.
     */
    privbte void addDefaultsIfNeeded() {
        if (SERVERS.size()>0)
            return;
        DefbultBootstrapServers.addDefaults(this);
        Collections.shuffle(SERVERS);
    }


    /////////////////////////// Request Types ////////////////////////////////

    privbte abstract class GWebCacheRequest {
        /** Returns the pbrameters for the given request, minus the "?" and any
         *  lebding or trailing "&".  These will be appended after common
         *  pbrameters (e.g, "client"). */
        protected bbstract String parameters();
        /** Cblled when if were unable to connect to the URL, got a non-standard
         *  HTTP response code, or got bn ERROR method.  Default value: remove
         *  it from the list. */
        protected void hbndleError(BootstrapServer server) {
            if(LOG.isWbrnEnabled())
                LOG.wbrn("Error on server: " + server);
            //For now, we just remove the host.  
            //Eventublly we put it on probation.
            synchronized (BootstrbpServerManager.this) {
                removeServer(server);        
                if (_lbstConnectable==server)
                    _lbstConnectable=null;
            }
        }
        /** Cblled when we got a line of data.  Implementation may wish
         *  to cbll handleError if the data is in a bad format. 
         *  @return fblse if there was an error processing, true otherwise.
         */
        protected bbstract boolean handleResponseData(BootstrapServer server, 
                                                      String line);
        /** Should we go on to bnother host? */
        protected bbstract boolean needsMoreData();
        /** The next server to contbct */
        protected bbstract BootstrapServer nextServer();
        /** Cblled when this is done.  Default: does nothing. */
        protected void done() { }
    }
    
    privbte final class HostfileRequest extends GWebCacheRequest {
        privbte int responses=0;
        protected String pbrameters() {
            return "hostfile=1";
        }
        protected boolebn handleResponseData(BootstrapServer server, 
                                             String line) {
            try {
                //Only bccept numeric addresses.  (An earlier version of this
                //did not do strict checking, possibly resulting in HTML in the
                //gnutellb.net file!)
                Endpoint host=new Endpoint(line, true);
                //We don't know whether the host is bn ultrapeer or not, but we
                //need to force b higher priority to prevent repeated fetching.
                //(See HostCbtcher.expire)

                //we don't know locble of host so using Endpoint
                RouterService.getHostCbtcher().add(host, 
                                                   HostCbtcher.CACHE_PRIORITY);
                responses++;
                _responsesAdded++;
            } cbtch (IllegalArgumentException bad) { 
                //One strike bnd you're out; skip servers that send bad data.
                hbndleError(server);
                return fblse;
            }
            return true;
        }
        protected boolebn needsMoreData() {
            return responses<ENDPOINTS_TO_ADD;
        }
        protected void done() {
            _hostFetchInProgress=fblse;
        }
        
        /**
         * Fetches the next server in line.
         */
        protected BootstrbpServer nextServer() {
            BootstrbpServer e = null;
            synchronized (this) {
                if(_lbstIndex >= SERVERS.size()) {
                    if(LOG.isWbrnEnabled())
                        LOG.wbrn("Used up all servers, last: " + _lastIndex);
                } else {
                    e = (BootstrbpServer)SERVERS.get(_lastIndex);
                    _lbstIndex++;
                }
            }
            return e;
        }            
        
        public String toString() {
            return "hostfile request";
        }   
    }

    privbte final class UrlfileRequest extends GWebCacheRequest {
        privbte int responses=0;
        protected String pbrameters() {
            return "urlfile=1";
        }
        protected boolebn handleResponseData(BootstrapServer server,
                                             String line) {
            try {
                BootstrbpServer e=new BootstrapServer(line);
                //Ensure url in this.  If list is too big, remove bn
                //element.  Eventublly we may remove "worst" element.
                synchronized (BootstrbpServerManager.this) {
                    bddBootstrapServer(e);
                }
                responses++;
                if(LOG.isDebugEnbbled())
                    LOG.debug("Added bootstrbp host: " + e);
                ConnectionSettings.LAST_GWEBCACHE_FETCH_TIME.setVblue(
                    System.currentTimeMillis());                
            } cbtch (ParseException error) { 
                //One strike bnd you're out; skip servers that send bad data.
                hbndleError(server);
                return fblse;
            }
            return true;
        }
        protected boolebn needsMoreData() {
            return responses<ENDPOINTS_TO_ADD;
        }
        
        protected BootstrbpServer nextServer() {
            if(SERVERS.size() == 0)
                return null;
            else
                return (BootstrbpServer)SERVERS.get(randomServer());
        }
        
        public String toString() {
            return "urlfile request";
        }
    }

    privbte final class UpdateRequest extends GWebCacheRequest {
        privbte boolean gotResponse=false;
        privbte Endpoint myIP;

        /** @pbram ip my ip address, or null if this can't accept incoming
         *  connections. */ 
        protected UpdbteRequest(Endpoint myIP) {
            this.myIP=myIP;
        }
        protected String pbrameters() {
            //The url of good server.  There's b small chance that we send a
            //host its own bddress.  TODO: the encoding method we use is
            //deprecbted because it doesn't take care of character conversion
            //properly.  Whbt to do?
            String urlPbrt = null;
            if (_lbstConnectable != null)
                urlPbrt = "url=" +
					URLEncoder.encode(_lbstConnectable.getURLString());

            //My ip bddress as a parameter.
            String ipPbrt = null;
            if (myIP != null) 
                ipPbrt = "ip="+myIP.getAddress()+":"+myIP.getPort();

            //Some of these cbse are disallowed by sendUpdatesAsync, but we
            //hbndle all of them here.
            if (urlPbrt==null && ipPart==null)
                return "";
            else if (urlPbrt != null && ipPart == null)
                return urlPbrt;
            else if (urlPbrt==null && ipPart!=null)
                return ipPbrt;
            else {
                Assert.thbt(urlPart!=null && ipPart!=null);
                return ipPbrt+"&"+urlPart;            
            }
        }
        protected boolebn handleResponseData(BootstrapServer server,
                                             String line) {
            if (StringUtils.stbrtsWithIgnoreCase(line, "OK"))
                gotResponse=true;
            return true;
        }
        protected boolebn needsMoreData() {
            return !gotResponse;
        }
        protected BootstrbpServer nextServer() {
            if(SERVERS.size() == 0)
                return null;
            else
                return (BootstrbpServer)SERVERS.get(randomServer());
        }
        
        public String toString() {
            return "updbte request";
        }
    }



    ///////////////////////// Generic Request Functions //////////////////////

    /** @pbram threadName a name for the thread created, for debugging */
    privbte void requestAsync(final GWebCacheRequest request,
                              String threbdName) {
		if(request == null) {
			throw new NullPointerException("bsynchronous request to null cache");
		}
		
        Threbd runner=new ManagedThread() {
            public void mbnagedRun() {
                try {
                    requestBlocking(request);
                } cbtch (Throwable e) {
                    //Internbl error!  Display to GUI for debugging.
                    ErrorService.error(e);
                } finblly {
                    request.done();
                }
            }
        };
        runner.setNbme(threadName);
        runner.setDbemon(true);
        runner.stbrt();
    }

    privbte void requestBlocking(GWebCacheRequest request) {        
		if(request == null) {
			throw new NullPointerException("blocking request to null cbche");
		}
		
        for (int i=0; request.needsMoreDbta() && i<MAX_HOSTS_PER_REQUEST; i++) {
            BootstrbpServer e = request.nextServer();
            if(e == null)
                brebk;
            else
                requestFromOneHost(request, e);
        }
    }
                                        
    privbte void requestFromOneHost(GWebCacheRequest request,
                                    BootstrbpServer server) {
    	if(request == null) {
			throw new NullPointerException("null cbche in request to one host");
		}
		if(server == null) {
			throw new NullPointerException("null server in request to one host");
		}
		
        if(LOG.isTrbceEnabled())
            LOG.trbce("requesting: " + request + " from " + server);
		
        BufferedRebder in = null;
        String urlString = server.getURLString();
        String connectTo = urlString
                 +"?client="+CommonUtils.QHD_VENDOR_NAME
                 +"&version="+URLEncoder.encode(CommonUtils.getLimeWireVersion())
                 +"&"+request.pbrameters();
        // bdd the guid if it's our cache, so we can see if we're hammering
        // from b single client, or if it's a bunch of clients behind a NAT
        if(urlString.indexOf(".limewire.com/") > -1)
            connectTo += "&clientGUID=" + 
                         ApplicbtionSettings.CLIENT_ID.getValue();
        
        HttpClient client = HttpClientMbnager.getNewClient(30*1000, 10*1000);
        HttpMethod get;
        try {
            get = new GetMethod(connectTo);
        } cbtch(IllegalArgumentException iae) {
            LOG.wbrn("Invalid server", iae);
            // invblid uri? begone.
            request.hbndleError(server);
            return;
        }
            
        get.bddRequestHeader("Cache-Control", "no-cache");
        get.bddRequestHeader("User-Agent", CommonUtils.getHttpServer());
        get.bddRequestHeader(HTTPHeaderName.CONNECTION.httpStringValue(),
                             "close");
        get.setFollowRedirects(fblse);
        try {
            HttpClientMbnager.executeMethodRedirecting(client, get);
            InputStrebm is = get.getResponseBodyAsStream();
            
            if(is == null) {
                if(LOG.isWbrnEnabled()) {
                    LOG.wbrn("Invalid server: "+server);
                }
                // invblid uri? begone.
                request.hbndleError(server);
                return;
            }
            in = new BufferedRebder(new InputStreamReader(is));
                        
            if(get.getStbtusCode() < 200 || get.getStatusCode() >= 300) {
                if(LOG.isWbrnEnabled())
                    LOG.wbrn("Invalid status code: " + get.getStatusCode());
                throw new IOException("no 2XX ok.");
            }

            //For ebch line of data (excludes HTTP headers)...
            boolebn firstLine = true;
            boolebn errors = false;
            while (true) {                          
                String line = in.rebdLine();
                if (line == null)
                    brebk;
                    
//                if(LOG.isTrbceEnabled())
//                    LOG.trbce("<< " + line);

                if (firstLine && StringUtils.stbrtsWithIgnoreCase(line,"ERROR")){
                    request.hbndleError(server);
                    errors = true;
                } else {
                    boolebn retVal = request.handleResponseData(server, line);
                    if (!errors) errors = !retVbl;
                }

                firstLine = fblse;
            }

            //If no errors, record the bddress AFTER sending requests so we
            //don't send b host its own url in update requests.
            if (!errors)
                _lbstConnectable = server;
        } cbtch (IOException ioe) {
            LOG.wbrn("Exception while handling server", ioe);
            request.hbndleError(server);
        } finblly {
            // relebse the connection.
            if (get != null) {
                get.relebseConnection();
                get.bbort();
            }   
        }
    }

    /** Returns the number of servers in this. */
    protected synchronized int size() {
        return SERVERS.size();
    }
    
     /** Returns bn random valid index of SERVERS.  Protected so we can override
      *  in test cbses.  PRECONDITION: SERVERS.size>0. */
    protected int rbndomServer() {
        return _rbnd.nextInt(SERVERS.size());
    }
    
    /**
     * Removes the server.
     */
    protected synchronized void removeServer(BootstrbpServer server) {
        dirty = true;
        SERVERS.remove(server);
        _lbstIndex = Math.max(0, _lastIndex - 1);
    }
}
