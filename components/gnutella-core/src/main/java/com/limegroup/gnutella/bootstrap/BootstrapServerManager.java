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
 * addresses of these and other servers.
 */
public class BootstrapServerManager {
    /** The minimum number of endpoints to fetch at a time. */
    private static final int ENDPOINTS_TO_ADD=10;
    /** The maximum number of bootstrap servers. */
    private static final int MAX_BOOTSTRAP_SERVERS=200;

    /** 
     * The bounded-size list of GWebCache servers, each as a BootstrapServer.
     * Order doesn't matter; hosts are chosen randomly from this.  Eventually
     * this may be prioritized by some metric.
     *  LOCKING: this 
     *  INVARIANT: _servers.size()<MAX_BOOTSTRAP_SERVERS
     */        
    private List /* of BootstrapServer */ _servers=new ArrayList();
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
        _servers.add(server);
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
     * Asynchronously fetches other bootstrap hosts and stores them in this.
     * Stops after getting "enough" endpoints or exhausting all caches.
     * Uses the "urlfile=1" message.
     */
    public void fetchBootstrapServersAsync() {
        requestAsync(new UrlfileRequest());
    }

    /** 
     * Asynchronously fetches endpoints from bootstrap servers and stores them
     * in catcher.  Stops after getting "enough" endpoints or exhausting all
     * caches.  Uses the "hostfile=1" message.
     */
    public void fetchEndpointsAsync() {
        requestAsync(new HostfileRequest());
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
            //TODO: Eventually we put it on probation.
            synchronized (BootstrapServerManager.this) {
                _servers.remove(server);
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
                //Ensure url in this.  If list is too big, remove random element.
                //TODO: eventually we may remove "worst" element.
                synchronized (BootstrapServerManager.this) {
                    if (! _servers.contains(e))
                        _servers.add(e);
                    if (_servers.size()>MAX_BOOTSTRAP_SERVERS) 
                        _servers.remove(randomServer());
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


    /////////////////////////// Generic Request Functions ///////////////////////

    private void requestAsync(final GWebCacheRequest request) {
        Thread runner=new Thread() {
            public void run() {
                requestBlocking(request);
            }
        };
        runner.setDaemon(true);
        runner.start();
    }

    private void requestBlocking(GWebCacheRequest request) {
        //TODO: prevent infinite loops.  What if nobody has data?
        while (request.needsMoreData()) {
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
            while (true) {                
                String line=in.readLine();
                if (line==null)
                    break;
                if (line.startsWith("ERROR"))
                    request.handleError(server);
                else
                    request.handleResponseData(server, line);
            }
            //Close.  TODO: is this really the preferred way?
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
