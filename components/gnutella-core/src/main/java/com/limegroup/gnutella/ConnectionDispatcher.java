package com.limegroup.gnutella;

import java.net.Socket;
import java.net.SocketException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.browser.ExternalControl;
import com.limegroup.gnutella.chat.ChatManager;
import com.limegroup.gnutella.http.HTTPRequestMethod;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.statistics.HTTPStat;
import com.limegroup.gnutella.util.IOUtils;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.util.ThreadFactory;

public class ConnectionDispatcher {
    
    private static final Log LOG = LogFactory.getLog(ConnectionDispatcher.class);
    
    private static final int UNKNOWN = -1;
    private static final int GNUTELLA = 0;
    private static final int LIMEWIRE = 1;
    private static final int GET = 2;
    private static final int HEAD = 3;
    private static final int GIV = 4;
    private static final int CHAT = 5;
    private static final int MAGNET = 6;
    private static final int CONNECT = 7;
    
    /**
     * Determines which protocol this word is
     * 
     * @param word
     * @return
     */
    private int parseWord(String word) {
        if(word == null)
            return UNKNOWN;
        else if(word.equals(ConnectionSettings.CONNECT_STRING_FIRST_WORD))
            return GNUTELLA;
        else if(ConnectionSettings.CONNECT_STRING.isDefault() && word.equals("LIMEWIRE"))
            return LIMEWIRE;
        else if(word.equals("GET"))
            return GET;
        else if(word.equals("HEAD"))
            return HEAD;
        else if(word.equals("GIV"))
            return GIV;
        else if(word.equals("CHAT"))
            return CHAT;
        else if(word.equals("MAGNET"))
            return MAGNET;
        else if(word.equals("CONNECT") || word.equals("\n\n"))
            return CONNECT;
        else
            return UNKNOWN;
    }
    
    /**
     * Retrieves the maximum size a word can have.
     */
    public int getMaximumWordSize() {
        return 8; // GNUTELLA == 8
    }

    
    /**
     * Dispatches this incoming connection to the appropriate manager, depending
     * on the word that was read.
     * 
     * @param word
     * @param client
     * @param newThread
     */
    public void dispatch(final String word, final Socket client, boolean newThread) {
        try {
            client.setSoTimeout(0);
        } catch(SocketException se) {
            IOUtils.close(client);
            return;
        }
        
        final int protocol = parseWord(word);

        boolean localHost = NetworkUtils.isLocalHost(client);
        // Only selectively allow localhost connections
        if (protocol != MAGNET) {
            if (ConnectionSettings.LOCAL_IS_PRIVATE.getValue() && localHost) {
                LOG.trace("Killing localhost connection with non-magnet.");
                IOUtils.close(client);
                return;
            }
        } else if (!localHost) { // && word.equals(MAGNET)
            LOG.trace("Killing non-local ExternalControl request.");
            IOUtils.close(client);
            return;
        }

        // Only CONNECT & UNKNOWN can be processed without a new thread.
        switch(protocol) {
        case CONNECT:
            if (ConnectionSettings.UNSET_FIREWALLED_FROM_CONNECTBACK.getValue())
                RouterService.getAcceptor().checkFirewall(client.getInetAddress());
            IOUtils.close(client);
            return;
        case UNKNOWN:
            HTTPStat.UNKNOWN_REQUESTS.incrementStat();
            if (LOG.isErrorEnabled())
                LOG.error("Unknown protocol: " + word);
            IOUtils.close(client);
            return;
        }
        
        Runnable runner = new Runnable() {
            public void run() {
                switch(protocol) {
                case GNUTELLA:
                    HTTPStat.GNUTELLA_REQUESTS.incrementStat();
                    RouterService.getConnectionManager().acceptConnection(client);
                    break;
                case LIMEWIRE:
                    HTTPStat.GNUTELLA_LIMEWIRE_REQUESTS.incrementStat();
                    RouterService.getConnectionManager().acceptConnection(client);
                    break;
                case GET:
                    HTTPStat.GET_REQUESTS.incrementStat();
                    RouterService.getUploadManager().acceptUpload(HTTPRequestMethod.GET, client, false);
                    break;
                case HEAD:
                    HTTPStat.HEAD_REQUESTS.incrementStat();
                    RouterService.getUploadManager().acceptUpload(HTTPRequestMethod.HEAD, client, false);
                    break;
                case GIV:
                    HTTPStat.GIV_REQUESTS.incrementStat();
                    RouterService.getDownloadManager().acceptDownload(client);
                    break;
                case CHAT:
                    HTTPStat.CHAT_REQUESTS.incrementStat();
                    ChatManager.instance().accept(client);
                    break;
                case MAGNET:
                    HTTPStat.MAGNET_REQUESTS.incrementStat();  
                    ExternalControl.fireMagnet(client);
                    break;
                default:
                    LOG.error("Parsed to unsupported protocol: " + protocol + ", word: " + word);
                }
                
                IOUtils.close(client);
            }
        };
        
        if(newThread)
            ThreadFactory.startThread(runner, "IncomingConnection");
        else
            runner.run();
    }

}
