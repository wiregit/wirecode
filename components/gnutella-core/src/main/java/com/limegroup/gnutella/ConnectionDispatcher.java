package com.limegroup.gnutella;

import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
    private static final Map<String, Delegator> protocols = 
    	Collections.synchronizedMap(new HashMap<String, Delegator>());
    
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

    public void addConnectionAcceptor(ConnectionAcceptor acceptor,
    		String [] words,
    		boolean localOnly,
    		boolean blocking) {
    	Delegator d = new Delegator(acceptor, localOnly, blocking);
    	synchronized(protocols) {
    		for (int i = 0; i < words.length; i++) {
    			if (words[i].length() > longestWordSize)
    				longestWordSize = words[i].length();
    			protocols.put(words[i],d);
    		}
    	}
    }
    
    public void removeConnectionAcceptor(String [] words) {
    	synchronized(protocols) {
            protocols.keySet().removeAll(Arrays.asList(words));
    		longestWordSize = 0;
            for(String word : protocols.keySet()) {
    			if (word.length() > longestWordSize)
    				longestWordSize = word.length();
    		}
    	}
    }
    
    /**
     * Dispatches this incoming connection to the appropriate manager, depending
     * on the word that was read.
     * 
     * @param word
     * @param client
     * @param newThread whether or not a new thread is necessary when dispatching
     *                  to a blocking protocol.
     */
    public void dispatch(final String word, final Socket client, boolean newThread) {
        try {
            client.setSoTimeout(0);
        } catch(SocketException se) {
            IOUtils.close(client);
            return;
        }
        
        // try to find someone who understands this protocol
        Delegator delegator = protocols.get(word);
       
        // no protocol available to handle this word 
        if (delegator == null) {
        	HTTPStat.UNKNOWN_REQUESTS.incrementStat();
        	if (LOG.isErrorEnabled())
        		LOG.error("Unknown protocol: " + word);
        	IOUtils.close(client);
        	return;
        }

        delegator.delegate(word, client, newThread);
    }
    
    /**
     * Utility wrapper that checks whether the new protocol is
     * supposed to be local, and whether the reading should happen
     * in a new thread or not.
     */
    private class Delegator {
    	private final ConnectionAcceptor acceptor;
    	private final boolean localOnly, blocking;
    	
    	Delegator(ConnectionAcceptor acceptor, 
    			boolean localOnly, 
    			boolean blocking) {
    		this.acceptor = acceptor;
    		this.localOnly = localOnly;
    		this.blocking = blocking;
    	}
    	
    	public void delegate(final String word, 
    			final Socket sock, 
    			boolean newThread) {
    		boolean localHost = NetworkUtils.isLocalHost(sock);
    		boolean drop = false;
    		if (localOnly && !localHost)
    			drop = true;
    		if (!localOnly && localHost && 
    				ConnectionSettings.LOCAL_IS_PRIVATE.getValue())
    			drop = true;
    		
    		if (drop) {
    			IOUtils.close(sock);
    			return;
    		}
    		
    		if (blocking && newThread) {
    			Runnable r = new Runnable() {
    				public void run() {
    					acceptor.acceptConnection(word, sock);
    				}
    			};
    			ThreadFactory.startThread(r, "IncomingConnection");
    		} else
    			acceptor.acceptConnection(word, sock);
    	}
    }
}

