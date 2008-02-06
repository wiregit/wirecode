package com.limegroup.gnutella.lws.server;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.limewire.http.LimeHttpClient;
import org.limewire.http.SimpleLimeHttpClient;
import org.limewire.lws.server.LWSDispatcher;
import org.limewire.lws.server.LWSDispatcherSupport;


/**
 * Responsible for sending commands to the client, acting like a web page.
 */
class CommandSender {
        
    private static final Log LOG = LogFactory.getLog(CommandSender.class);
    
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
        String url = "http://localhost:" + (LocalServerImpl.PORT )  + "/"    
                + LWSDispatcher.PREFIX
                + LocalServerDelegate.NormalStyleURLConstructor.INSTANCE.constructURL(command, args);
        HttpResponse response = null;
        LimeHttpClient client = new SimpleLimeHttpClient();
        try {
            HttpGet get = new HttpGet(url);
            //
            // This is going to block, but that should be OK. This code will never
            // run in the client and really should be running outside, because it
            // represents making a call in Javascript from a web page
            //
            //get.addHeader("Connection", "close");
            response = client.execute(get);
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            return result;
        } catch (HttpException e) {
            LOG.warn("exception", e);
        } catch (IOException e) {
            LOG.warn("exception", e);
        } catch (URISyntaxException e) {
            LOG.warn("exception", e);
        } catch (InterruptedException e) {
            LOG.warn("exception", e);
        } finally {
            client.releaseConnection(response);
        }
        return null;
    }

    public void detach() {
        Map<String,String> args = new HashMap<String,String>();
        args.put(LWSDispatcherSupport.Parameters.CALLBACK, "_dummy");
        sendMessage(LWSDispatcherSupport.Commands.DETATCH, args);
    }
    
}
