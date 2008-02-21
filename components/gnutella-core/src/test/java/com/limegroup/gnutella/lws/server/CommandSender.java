package com.limegroup.gnutella.lws.server;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.lws.server.LWSDispatcher;
import org.limewire.lws.server.LWSDispatcherSupport;
import org.limewire.net.HttpClientManager;


/**
 * Responsible for sending commands to the client, acting like a web page.
 */
class CommandSender {
    
    private static final Log LOG = LogFactory.getLog(CommandSender.class);
    
    private int port = -1;
    private final String HOST = "localhost";    
    
    /**
     * The prefix {@link LWSDispatcherSupport#PREFIX} is going to be put on
     * <code>msg</code>, so callers of this method cannot put the prefix on.
     * 
     * @param command command to send without the
     *        {@link LWSDispatcherSupport#PREFIX} prefix
     * @param args arguments to this command
     * @param callback TODO
     */
    public String sendMessage(String command, Map<String, String> args) {
        String url = "http://" + HOST + ":" + getPort() + "/" 
        + LWSDispatcher.PREFIX
            + LocalServerDelegate.NormalStyleURLConstructor.INSTANCE.constructURL(command,args);System.out.println("url:"+url);
        final GetMethod get = new GetMethod(url);
        //
        // This is going to block, but that should be OK. This code will never
        // run in the client and really should be running outside, because it
        // represents making a call in Javascript from a web page
        //
        HttpClient client = HttpClientManager.getNewClient();
        String res = null;
        try {
            HttpClientManager.executeMethodRedirecting(client,get);
            res = get.getResponseBodyAsString();
        } catch (HttpException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            get.releaseConnection();
        }
        return res;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.lws.server.CommandSender#detach()
     */
    public final void detach(String privateKey, String sharedKey) {
        Map<String,String> args = new HashMap<String,String>();
        args.put(LWSDispatcherSupport.Parameters.CALLBACK, "_dummy");
        args.put(LWSDispatcherSupport.Parameters.PRIVATE, privateKey);
        args.put(LWSDispatcherSupport.Parameters.SHARED, sharedKey);
        sendMessage(LWSDispatcherSupport.Commands.DETATCH, args);
    }
    
    private int getPort() {
        if (port != -1) {
            return port;
        }
        int tmpPort = LocalServerImpl.PORT;
        for (tmpPort = LocalServerImpl.PORT; tmpPort < LocalServerImpl.PORT + 10; tmpPort++) {
            try {
                Socket s = new Socket(HOST, tmpPort);
                s.getInputStream(); 
                s.getOutputStream();
                s.close();
            } catch (IOException e) {
                // ignore
            }
            break;

        }
        port = tmpPort;
        if (LOG.isDebugEnabled()) {
            LOG.debug("Connected on port " + port);
        }
        return port;
    } 
    
}
