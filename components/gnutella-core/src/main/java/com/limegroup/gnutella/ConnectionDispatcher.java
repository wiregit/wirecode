package com.limegroup.gnutella;

import java.net.Socket;
import java.net.SocketException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.statistics.HTTPStat;
import com.limegroup.gnutella.util.IOUtils;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.util.ThreadFactory;

public class ConnectionDispatcher {
    
    private static final Log LOG = LogFactory.getLog(ConnectionDispatcher.class);
    
    /**
     * Mapping of first protocol word -> ConnectionAcceptor
     */
    private static final Map protocols = 
    	Collections.synchronizedMap(new HashMap());
    
    /** 
     * The longest protocol word we understand.
     * LOCKING: protocols.
     */
    private int longestWordSize = 0;
    
    /**
     * Retrieves the maximum size a word can have.
     */
    public int getMaximumWordSize() {
    	synchronized(protocols) {
    		return longestWordSize; // currently GNUTELLA == 8
    	}
    }

    public void addConnectionAcceptor(ConnectionAcceptor acceptor) {
    	synchronized(protocols) {
    		for (Iterator iter = acceptor.getFirstWords().iterator(); iter.hasNext();) {
    			String word = (String) iter.next();
    			if (word.length() > longestWordSize)
    				longestWordSize = word.length();
    			protocols.put(word,acceptor);
    		}
    	}
    }
    
    public void removeConnectionAcceptor(ConnectionAcceptor acceptor) {
    	synchronized(protocols) {
    		for (Iterator iter = acceptor.getFirstWords().iterator(); iter.hasNext();)
    			protocols.remove(iter.next());
    		int newLongestSize = 0;
    		for (Iterator iter = protocols.keySet().iterator();iter.hasNext();){
    			String word = (String) iter.next();
    			if (word.length() > newLongestSize)
    				newLongestSize = word.length();
    		}
    		longestWordSize = newLongestSize;
    	}
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
        
        // try to find someone who understands this protocol
        final ConnectionAcceptor acceptor = (ConnectionAcceptor) protocols.get(word);
       
        // no protocol available to handle this word 
        if (acceptor == null) {
        	HTTPStat.UNKNOWN_REQUESTS.incrementStat();
        	if (LOG.isErrorEnabled())
        		LOG.error("Unknown protocol: " + word);
        	IOUtils.close(client);
        }
        
        // Only selectively allow localhost connections
        boolean localHost = NetworkUtils.isLocalHost(client);
        if (!acceptor.localOnly()) {
            if (ConnectionSettings.LOCAL_IS_PRIVATE.getValue() && localHost) {
                LOG.trace("Killing localhost connection with non-local protocol.");
                IOUtils.close(client);
                return;
            }
        } else if (!localHost) { // && word.equals(MAGNET)
            LOG.trace("Killing non-local request for local protcol.");
            IOUtils.close(client);
            return;
        }

        // GNUTELLA & LIMEWIRE can do this because if we dispatched with 'newThread' true,
        // that means it was dispatched from NIODispatcher, meaning the connection can
        // support asynchronous handshaking & looping, so acceptConnection will return immediately.
        // Conversely, if 'newThread' is false, the connection has already been given its own
        // thread, so the fact that acceptConnection will block is okay.
        //
        // The others perform no blocking operations, so either way, it is okay to process
        // them immediately.
        if (!acceptor.isBlocking()) {
        	acceptor.acceptConnection(word, client);
        	return;
        }
        
        // All the below protocols are handled in a blocking manner by their managers.
        // Thus, if this was handled by NIODispatcher (and 'newThread' is true), we need
        // to spawn a new thread to process them.  Conversely, if 'newThread' is false,
        // they have already been given their own thread and the calls can block
        // with no problems.
        Runnable runner = new Runnable() {
            public void run() {
            		acceptor.acceptConnection(word,client);
                // We must not close the connection at this point, because some things may
                // have only done a temporary blocking operation and then handed the socket
                // off to a callback (such as DownloadManager parsing the GIV).
            }
        };
        
        // (see comment above)
        if(newThread)
            ThreadFactory.startThread(runner, "IncomingConnection");
        else
            runner.run();
    }
}

