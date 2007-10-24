package com.limegroup.gnutella.lws.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.limewire.lws.server.LWSDispatcher;
import org.limewire.lws.server.LWSDispatcherSupport;
import org.limewire.lws.server.LocalServerDelegate;
import org.limewire.lws.server.LocalServerImpl;

import com.limegroup.gnutella.http.HttpClientManager;

/**
 * Responsible for sending commands to the client, acting like a web page.
 */
class CommandSender {
    
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
        String url = "http://localhost:" + LocalServerImpl.PORT + "/" 
        + LWSDispatcher.PREFIX
            + LocalServerDelegate.NormalStyleURLConstructor.INSTANCE.constructURL(command,args);
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

    public void detach() {
        Map<String,String> args = new HashMap<String,String>();
        args.put(LWSDispatcherSupport.Parameters.CALLBACK, "_dummy");
        sendMessage(LWSDispatcherSupport.Commands.DETATCH, args);
    }
    
}
