package com.limegroup.gnutella.bootstrap;

import java.io.*;
import java.net.*;
import java.util.Random;
import java.text.ParseException;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.*;

/**
 * A list of GWebCache servers.  Provides methods to fetch address addresses
 * from these servers, find the addresses of more such servers, and update the
 * addresses of these and other servers.<p>
 * 
 * Information on the GWebCache protocol can be found at 
 * http://zero-g.net/gwebcache/specs.html
 */
public class BootstrapServerManager {
    /** The minimum number of endpoints/urls to fetch at a time. */
    private static final int ENDPOINTS_TO_ADD=10;
    /** The maximum number of bootstrap servers to retain in memory. */
    private static final int MAX_BOOTSTRAP_SERVERS=200;
    /** The maximum number of hosts to try per request.  Prevents us from
     *  consuming all hosts if disconnected.  Non-final for testing. */
    public static int MAX_HOSTS_PER_REQUEST=20;
    /** The amount of time in milliseconds between update requests. 
     *  Public and non-final for testing purposes. */
    public static int UPDATE_DELAY_MSEC=60*60*1000;

    /** 
     * The bounded-size list of GWebCache servers, each as a BootstrapServer.
     * Order doesn't matter; hosts are chosen randomly from this.  Eventually
     * this may be prioritized by some metric.
     *  LOCKING: this 
     *  INVARIANT: _servers.size()<MAX_BOOTSTRAP_SERVERS
     */        
    private List /* of BootstrapServer */ _servers=new ArrayList();
    /** The last bootstrap server we successfully connected to, or null if none.
     *  Used for sending updates.  _lastConnectable will generally be in
     *  _servers, though this is not strictly required because of _servers'
     *  random replacement strategy.  _lastConnectable should be nulled if we
     *  later unsuccessfully try to reconnect to it. */
    private BootstrapServer _lastConnectable;
    /** Where to deposit fetched endpoints. */
    private HostCatcher _catcher; 
    /** Source of randomness for picking servers. 
     *  TODO: this is thread-safe, right? */
    private Random _rand=new Random();


    /////////////////////////// Public Interface //////////////////////

    /** @param catcher where to deposit fetched endpoints. */
    public BootstrapServerManager(HostCatcher catcher) {
        this._catcher=catcher;
    }

    /**
     * Adds server to this.
     */
    public synchronized void addBootstrapServer(BootstrapServer server) {
        if (! _servers.contains(server))
            _servers.add(server);
        if (_servers.size()>MAX_BOOTSTRAP_SERVERS) 
            _servers.remove(randomServer());
    }

    /**
     * Returns an iterator of the bootstrap servers in this, each as a
     * BootstrapServer, in any order.  To prevent ConcurrentModification
     * problems, the caller should hold this' lock while using the
     * iterator.
     * @return an Iterator of BootstrapServer.
     */
    public synchronized Iterator /*of BootstrapServer*/ getBootstrapServers() {
        return _servers.iterator();
    }

    /** 
     * Asynchronously fetches other bootstrap URLs and stores them in this.
     * Stops after getting "enough" endpoints or exhausting all caches.  Uses
     * the "urlfile=1" message.
     */
    public void fetchBootstrapServersAsync() {
        addDefaultsIfNeeded();
        requestAsync(new UrlfileRequest(), "GWebCache urlfile");
    }

    /** 
     * Asynchronously fetches host addresses from bootstrap servers and stores
     * them in the HostCatcher.  Stops after getting "enough" endpoints or
     * exhausting all caches.  Uses the "hostfile=1" message.
     */
    public void fetchEndpointsAsync() {
        addDefaultsIfNeeded();
        requestAsync(new HostfileRequest(), "GWebCache hostfile");
    }

    /** 
     * Asynchronously sends an update message to a cache.  May do nothing if
     * nothing to update.  Uses the "url" and "ip" messages.
     *
     * @param myIP my listening address and port, or null if I cannot accept 
     *  incoming connections or am not a supernode.
     */
    public void sendUpdatesAsync(Endpoint myIP) {
        addDefaultsIfNeeded();
        //For now we only send updates if the "ip=" parameter is null,
        //regardless of whether we have a url.
        if (myIP!=null)
            requestAsync(new UpdateRequest(myIP), "GWebCache update");
    }

    /**
     * Adds default bootstrap servers to this if this needs more entries.
     */
    private void addDefaultsIfNeeded() {
        if (_servers.size()>0)
            return;
        DefaultBootstrapServers.addDefaults(this);        
    }


    /////////////////////////// Request Types ////////////////////////////////

    abstract class GWebCacheRequest {
        /** Appends parameters to the given url. */
        public abstract String requestURL(String url);
        /** Called when if were unable to connect to the URL, got a non-standard
         *  HTTP response code, or got an ERROR method.  Default value: remove
         *  it from the list. */
        public void handleError(BootstrapServer server) {
            //For now, we just remove the host.  
            //Eventually we put it on probation.
            synchronized (BootstrapServerManager.this) {
                _servers.remove(server);
                if (_lastConnectable==server)
                    _lastConnectable=null;
            }
        }
        /** Called when we got a line of data.  Implementation may wish
         *  to call handleError if the data is in a bad format. */
        public abstract void handleResponseData(BootstrapServer server, String line);
        /** Should we go on to another host? */
        public abstract boolean needsMoreData();
    }
    
    class HostfileRequest extends GWebCacheRequest {
        private int responses=0;
        public String requestURL(String url) {
            return url+"?hostfile=1";
        }
        public void handleResponseData(BootstrapServer server, String line) {
            try {
                //Endpoint's constructor won't choke on "ERROR", so we check.            
                Endpoint host=new Endpoint(line);                    
                _catcher.add(host, true);
                responses++;
            } catch (IllegalArgumentException bad) { 
                //One strike and you're out; skip servers that send bad data.
                handleError(server);
            }            
        }
        public boolean needsMoreData() {
            return responses<ENDPOINTS_TO_ADD;
        }
    }

    class UrlfileRequest extends GWebCacheRequest {
        private int responses=0;
        public String requestURL(String url) {
            return url+"?urlfile=1";
        }
        public void handleResponseData(BootstrapServer server, String line) {
            try {
                BootstrapServer e=new BootstrapServer(line);
                //Ensure url in this.  If list is too big, remove random
                //element.  Eventually we may remove "worst" element.
                synchronized (BootstrapServerManager.this) {
                    addBootstrapServer(e);
                }
                responses++;
            } catch (ParseException error) { 
                //One strike and you're out; skip servers that send bad data.
                handleError(server);
            }            
        }
        public boolean needsMoreData() {
            return responses<ENDPOINTS_TO_ADD;
        }
    }

    class UpdateRequest extends GWebCacheRequest {
        private boolean gotResponse=false;
        private Endpoint myIP;

        /** @param ip my ip address, or null if this can't accept incoming
         *  connections. */ 
        public UpdateRequest(Endpoint myIP) {
            this.myIP=myIP;
        }
        public String requestURL(String url) {
            //The url of good server.  There's a small chance that we send a
            //host its own address.
            String urlPart=null;
            if (_lastConnectable!=null)       //TODO: URL encode!
                urlPart="url="+_lastConnectable.getURL().toString(); 
            //My ip address as a parameter.
            String ipPart=null;
            if (myIP!=null) 
                ipPart="ip="+myIP.getHostname()+":"+myIP.getPort();

            //Some of these case are disallowed by sendUpdatesAsync, but we
            //handle all of them here.
            if (urlPart==null && ipPart==null)
                return url;
            else if (urlPart!=null && ipPart==null)
                return url+"?"+urlPart;
            else if (urlPart==null && ipPart!=null)
                return url+"?"+ipPart;
            else {
                Assert.that(urlPart!=null && ipPart!=null);
                return url+"?"+ipPart+"&"+urlPart;            
            }
        }
        public void handleResponseData(BootstrapServer server, String line) {
            if (line.startsWith("OK"))      //TODO: ignore case
                gotResponse=true;
        }
        public boolean needsMoreData() {
            return !gotResponse;
        }
    }



    /////////////////////////// Generic Request Functions ///////////////////////

    /** @param threadName a name for the thread created, for debugging */
    private void requestAsync(final GWebCacheRequest request,
                              String threadName) {
        Thread runner=new Thread() {
            public void run() {
                try {
                    requestBlocking(request);
                } catch (Exception e) {
                    e.printStackTrace();  //TODO: this will never be seen
                    Assert.that(false, "Uncaught exception: "+e);         
                }
            }
        };
        runner.setName(threadName);
        runner.setDaemon(true);
        runner.start();
    }

    private void requestBlocking(GWebCacheRequest request) {        
        for (int i=0; request.needsMoreData() && i<MAX_HOSTS_PER_REQUEST; i++) {
            //Pick a random server.  We may visit the same server twice, but
            //that's improbable.  Alternative: shuffle list and remove first.
            BootstrapServer e;
            synchronized (this) {
                if (_servers.size()==0)
                    break;
                e=(BootstrapServer)_servers.get(randomServer());
            }
            requestFromOneHost(request, e);
        }
    }
                                        
    private void requestFromOneHost(GWebCacheRequest request,
                                    BootstrapServer server) {
        try {
            URL url=new URL(request.requestURL(server.getURL().toString()));
            URLConnection connection=url.openConnection();
            BufferedReader in=new BufferedReader(
                                  new InputStreamReader(
                                      connection.getInputStream()));
            //For each line of data (excludes HTTP headers)...
            boolean firstLine=true;
            boolean errors=false;
            while (true) {                
                String line=in.readLine();
                if (line==null)
                    break;

                if (firstLine && line.startsWith("ERROR")) { //TODO: ignore case
                    request.handleError(server);
                    errors=true;
                } else {
                    request.handleResponseData(server, line);
                }

                firstLine=false;
            }

            //If no errors, record the address AFTER sending requests so we
            //don't send a host its own url in update requests.
            if (!errors)
                _lastConnectable=server;
            //Close the connection.  TODO: is this really the preferred way?
            in.close();   
        } catch (IOException ioe) {
            request.handleError(server);
        }
    }

    /** Returns an random valid index of _servers.  Protected so we can override
     *  in test cases.  */
    protected int randomServer() {
        return _rand.nextInt(_servers.size());
    }

    /** Returns the number of servers in this. */
    protected synchronized int size() {
        return _servers.size();
    }
}
