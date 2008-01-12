package com.limegroup.gnutella.lws.server;

import java.util.HashMap;
import java.util.Map;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.limewire.http.HttpClientManager;
import org.limewire.io.IOUtils;
import org.limewire.lws.server.LWSDispatcher;
import org.limewire.lws.server.LWSDispatcherSupport;


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
        String url = "http://localhost:" + (LocalServerImpl.PORT )  + "/"    
                + LWSDispatcher.PREFIX
                + LocalServerDelegate.NormalStyleURLConstructor.INSTANCE.constructURL(command, args);
        HttpResponse response = null;
        try {
            final HttpGet get = new HttpGet(url);
            //
            // This is going to block, but that should be OK. This code will never
            // run in the client and really should be running outside, because it
            // represents making a call in Javascript from a web page
            //
            HttpClient client = HttpClientManager.getNewClient();
            //get.addHeader("Connection", "close");
            response = client.execute(get);
            if (response.getEntity() != null) {
                return new String(IOUtils.readFully(response.getEntity().getContent()));
            }
        } catch (HttpException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            HttpClientManager.releaseConnection(response);
        }
        return null;
    }

    public void detach() {
        Map<String,String> args = new HashMap<String,String>();
        args.put(LWSDispatcherSupport.Parameters.CALLBACK, "_dummy");
        sendMessage(LWSDispatcherSupport.Commands.DETATCH, args);
    }
    
}
