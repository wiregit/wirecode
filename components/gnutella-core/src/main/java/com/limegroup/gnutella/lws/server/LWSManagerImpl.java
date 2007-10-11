package com.limegroup.gnutella.lws.server;


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
import org.limewire.lws.server.AbstractDispatchee;
import org.limewire.lws.server.ConnectionListener;
import org.limewire.lws.server.Dispatcher;
import org.limewire.lws.server.SendsMessagesToServer;
import org.limewire.lws.server.LWSServerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.http.HttpClientManager;
import com.limegroup.gnutella.settings.StoreSettings;
import com.limegroup.gnutella.util.EncodingUtils;
import com.limegroup.gnutella.util.LimeWireUtils;


/**
 * Encapsulates a local server and dispatchee.
 */
@Singleton
public final class LWSManagerImpl implements LWSManager, 
                                        LWSManager.Handler.CanRegister, 
                                        LWSManager.Listener.CanRegister,
                                        SendsMessagesToServer {
    
    private final static Log LOG = LogFactory.getLog(LWSManagerImpl.class);
    
    /** The page for making commands to The Lime Wire Store server. */
    private final static String COMMAND_PAGE_WITH_LEADING_AND_TRAILING_SLASHES 
        = "/store/app/pages/client/ClientCom/command/";
        
    private Dispatcher dispatcher;
    private final Map<String, Handler> commands2handlers = new HashMap<String, Handler>();
    private final Map<String, List<Listener>> commands2listenerLists = new HashMap<String, List<Listener>>();
    
    /** This is provided by {@link StoreSettings}. */
    private final String hostNameAndPort;
    
    @Inject
    public LWSManagerImpl() {
        this.dispatcher = LWSServerFactory.createDispatcher(this, new  AbstractDispatchee() {

            public String dispatch(String cmd, Map<String, String> args) {
                return LWSManagerImpl.this.dispatch(cmd, args);
            }

            @Override
            protected void connectionChanged(boolean isConnected) {
                // nothing
            }
            
        });
        
        // Construct the hostname and port to which we connect for authentication
        // from remote settings
        StringBuffer hostNameAndPortBuffer = new StringBuffer(StoreSettings.AUTHENTICATION_HOSTNAME.getValue());
        int port = StoreSettings.AUTHENTICATION_PORT.getValue();
        if (port > 0) hostNameAndPortBuffer.append(":").append(port);
        this.hostNameAndPort = hostNameAndPortBuffer.toString();
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
    // Implementation of StoreManager
    // -----------------------------------------------------------------

    public HttpRequestHandler getHandler() {
        return dispatcher;
    }
    
    
    // -----------------------------------------------------------------
    // Implementation of StoreManager.Handler.CanRegister
    // -----------------------------------------------------------------
    
    public final boolean registerHandler(String cmd, Handler lis) {
        String hash = hash(cmd);
        return commands2handlers.get(hash) != null ? false : commands2handlers.put(hash, lis) != null;
    }
    
      
    // -----------------------------------------------------------------
    // Implementation of StoreManager.Listener.CanRegister
    // -----------------------------------------------------------------
    
    public final boolean registerListener(String cmd, Listener lis) {
        String hash = hash(cmd);
        List<Listener> lst = commands2listenerLists.get(hash);
        if (lst == null) commands2listenerLists.put(hash, lst = new ArrayList<Listener>());
        return lst.contains(lis) ? false : lst.add(lis);
    }    
    
    public String sendMsgToRemoteServer(final String msg, final Map<String, String> args) {
        String url = constructURL(msg, args);
        HttpClient client = HttpClientManager.getNewClient();
        GetMethod get = new GetMethod(url);
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
        StringBuffer url = new StringBuffer("http://")
            .append(hostNameAndPort)
            .append(COMMAND_PAGE_WITH_LEADING_AND_TRAILING_SLASHES);
        url.append(msg);
        for (Map.Entry<String, String> e : args.entrySet()) {
            url.append("/");
            url.append(e.getKey()).append("/").append(EncodingUtils.encode(e.getValue()));
        }
        return url.toString();
    }    
  
    private String hash(String cmd) {
        return cmd.toLowerCase();
    }    

    private String dispatch(String cmd, Map<String, String> args) {
        String hash = hash(cmd);
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
