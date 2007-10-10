package com.limegroup.gnutella.store.server;


import java.io.IOException;
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
public final class LWStoreManagerImpl implements LWStoreManager, 
                                        LWStoreManager.Handler.CanRegister, 
                                        LWStoreManager.Listener.CanRegister,
                                        SendsMessagesToServer {
    
    private final static Log LOG = LogFactory.getLog(LWStoreManagerImpl.class);
    
    private final boolean local = false;
    
    private final Dispatcher dispatcher;
    private final Map<String, Handler> commands2handlers = new HashMap<String, Handler>();
    private final Map<String, List<Listener>> commands2listenerLists = new HashMap<String, List<Listener>>();
  
    public LWStoreManagerImpl() {
        this.dispatcher = StoreServerFactory.createDispatcher(this, new  AbstractDispatchee() {

            public String dispatch(String cmd, Map<String, String> args) {
                return LWStoreManagerImpl.this.dispatch(cmd, args);
            }

            @Override
            protected void connectionChanged(boolean isConnected) {
                // do nothing
            }
            
        });
    }    
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
    // Implementation of LWStoreManager
    // -----------------------------------------------------------------

    public HttpRequestHandler getHandler() {
        return dispatcher;
    }
    
    // -----------------------------------------------------------------
    // Implementation of StoreManager.Handler.CanRegister
    // -----------------------------------------------------------------
    
    public final boolean registerHandler(String inCmd, Handler lis) {
        final String cmd = canonicalize(inCmd);
        return commands2handlers.get(cmd) != null ? false : commands2handlers.put(cmd, lis) != null;
    }
      
    // -----------------------------------------------------------------
    // Implementation of StoreManager.Listener.CanRegister
    // -----------------------------------------------------------------
    
    public final boolean registerListener(String inCmd, Listener lis) {
        final String cmd = canonicalize(inCmd);
        List<Listener> lst = commands2listenerLists.get(cmd);
        if (lst == null) commands2listenerLists.put(cmd, lst = new ArrayList<Listener>());
        return lst.contains(lis) ? false : lst.add(lis);
    }    
    
    public String sendMsgToRemoteServer(final String msg, final Map<String, String> args) {
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
        final String res = local ? constructLocalURL(msg, args) : constructRemoteURL(msg, args);
        System.out.println(res);
        return res;
    }
    
    private String constructRemoteURL(final String msg, final Map<String, String> args) {
        final StringBuffer url = new StringBuffer("http://localhost:8080/store/app/pages/client/ClientCom/command/");
        url.append(msg);
        for (Map.Entry<String, String> e : args.entrySet()) {
            url.append("/");
            url.append(e.getKey()).append("/").append(EncodingUtils.encode(e.getValue()));
        }
        return url.toString();
    }    
    
    private String constructLocalURL(final String msg, final Map<String, String> args) {
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
 
    private String canonicalize(String cmd) {
        return cmd.toLowerCase();
    }    

    private String dispatch(String inCmd, Map<String, String> args) {
        final String cmd = canonicalize(inCmd);
        Handler h = commands2handlers.get(cmd);
        String res = null;
        boolean handled = false;
        if (h != null) {
            handled = true;
            res = h.handle(args);
        }
        List<Listener> ls = commands2listenerLists.get(cmd);
        if (ls != null && !ls.isEmpty()) {
            handled = true;
            for (Listener l : ls) l.handle(args);
        }
        if (!handled) {
            //
            // we couldn't find a handler
            //
            return null;
        } else {
            if (res == null) {
                //
                // we had a handler but perhaps no result, that's fine
                //
                return "OK";
            } else {
                //
                // handled and have a result
                //
                return res;
            }
        }
    }   
}
