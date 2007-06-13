package com.limegroup.gnutella.store.server;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.protocol.HttpRequestHandler;
import org.limewire.store.server.AbstractDispatchee;
import org.limewire.store.server.ConnectionListener;
import org.limewire.store.server.Dispatcher;
import org.limewire.store.server.SendsMessagesToServer;
import org.limewire.store.server.StoreServerFactory;

import com.limegroup.gnutella.http.HttpClientManager;
import com.limegroup.gnutella.util.EncodingUtils;
import com.limegroup.gnutella.util.LimeWireUtils;


/**
 * Encapsulates a local server and dispatchee.
 */
final class StoreManagerImpl implements StoreManager, 
                                        StoreManager.Handler.CanRegister, 
                                        StoreManager.Listener.CanRegister,
                                        SendsMessagesToServer {
    
    private final static Log LOG = LogFactory.getLog(StoreManagerImpl.class);
    
    // -----------------------------------------------------------------
    // Factory
    // -----------------------------------------------------------------
  
    static StoreManagerImpl newDemoInstance() {
        final StoreManagerImpl s = new StoreManagerImpl();
        s.dispatcher = StoreServerFactory.createDispatcher(s, new  AbstractDispatchee() {

            public String dispatch(String cmd, Map<String, String> args) {
                return s.dispatch(cmd, args);
            }

            @Override
            protected void connectionChanged(boolean isConnected) {
            }
            
        });
        return s;
    }
    
    private Dispatcher dispatcher;
    private final Map<String, Handler> commands2handlers = new HashMap<String, Handler>();
    private final Map<String, List<Listener>> commands2listenerLists = new HashMap<String, List<Listener>>();
    
    private StoreManagerImpl() {}
    
    // -----------------------------------------------------------------
    // Implementation of ConnectionListener.HasSome
    // -----------------------------------------------------------------

    public final boolean addConnectionListener(ConnectionListener lis) {
        return dispatcher.addConnectionListener(lis);
    }

    public final boolean removeConnectionListener(ConnectionListener lis) {
        return dispatcher.removeConnectionListener(lis);
    }
    
    // -----------------------------------------------------------------
    // Implementation of StoreManager
    // -----------------------------------------------------------------

    public HttpRequestHandler getHandler() {
        return dispatcher;
    }
    
    // -----------------------------------------------------------------
    // Implementation of StoreManager.Handler.CanRegister
    // -----------------------------------------------------------------
    
    public final boolean registerHandler(String cmd, Handler lis) {
        final String hash = hash(cmd);
        return commands2handlers.get(hash) != null ? false : commands2handlers.put(hash, lis) != null;
    }
      
    // -----------------------------------------------------------------
    // Implementation of StoreManager.Listener.CanRegister
    // -----------------------------------------------------------------
    
    public final boolean registerListener(String cmd, Listener lis) {
        final String hash = hash(cmd);
        List<Listener> lst = commands2listenerLists.get(hash);
        if (lst == null) commands2listenerLists.put(hash, lst = new ArrayList<Listener>());
        return lst.contains(lis) ? false : lst.add(lis);
    }    
    
    public String sendMsgToRemoteServer(String msg, Map<String, String> args) {
        final String url = constructURL(msg, args);
        HttpClient client = HttpClientManager.getNewClient();
        final GetMethod get = new GetMethod(url);
        get.addRequestHeader("User-Agent", LimeWireUtils.getHttpServer());
        try {
            HttpClientManager.executeMethodRedirecting(client, get);
            final String res = get.getResponseBodyAsString();
            return res;
        } catch(IOException ioe) {
            LOG.warn("Can't contact store server: " + url, ioe);
            return null;
        } finally {
            get.releaseConnection();
        }
    }
    
    private String constructURL(final String msg, final Map<String, String> args) {
        final StringBuffer url = new StringBuffer("http://localhost:8091/");
        url.append(msg);
        boolean firstTime = true;
        for (Map.Entry<String, String> e : args.entrySet()) {
            url.append(firstTime ? "?" : "&");
            firstTime = false;
            url.append(e.getKey()).append("=").append(EncodingUtils.encode(e.getValue()));
        }
        return url.toString();
    }
 
    private String hash(String cmd) {
        return cmd.toLowerCase();
    }    

    private String dispatch(String cmd, Map<String, String> args) {
        final String hash = hash(cmd);
        Handler h = commands2handlers.get(hash);
        String res = null;
        boolean handled = false;
        if (h != null) {
            handled = true;
            res = h.handle(args);
        }
        List<Listener> ls = commands2listenerLists.get(hash);
        if (ls != null && !ls.isEmpty()) {
            handled = true;
            for (Listener l : ls) l.handle(args);
        }
        if (!handled) {
            return null;
        } else {
            if (res == null) {
                return "OK";
            } else {
                return res;
            }
        }
    }   
}
