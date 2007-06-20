package org.limewire.net;

import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.ThreadExecutor;
import org.limewire.io.IOUtils;
import org.limewire.io.NetworkUtils;

public class ConnectionDispatcher {
    
    private static final Log LOG = LogFactory.getLog(ConnectionDispatcher.class);
    
    /**
     * Mapping of first protocol word -> SocketAcceptor
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

    /**
     * Associates the given SocketAcceptor with the given words.
     * If localOnly is true, non-local-host sockets will be closed
     * when using the word.  Otherwise, localhost sockets will be
     * forbidden from using the word.
     * If blocking is true, a new thread will be spawned when calling
     * acceptor.acceptConnection.
     * 
     * @param acceptor The SocketAcceptor to call acceptConnection on
     * @param localOnly True if localhost connections are required, false if none allowed
     * @param blocking True if the acceptor may block on I/O after acceptConnection is called
     * @param words The list of words to associate with this SocketAcceptor
     */
    public void addConnectionAcceptor(ConnectionAcceptor acceptor,
    		boolean localOnly,
    		boolean blocking,
    		String... words) {
    	Delegator d = new Delegator(acceptor, localOnly, blocking);
    	synchronized(protocols) {
    		for (int i = 0; i < words.length; i++) {
    			if (words[i].length() > longestWordSize)
    				longestWordSize = words[i].length();
    			protocols.put(words[i],d);
    		}
    	}
    }
    
    /** Removes any ConnectionAcceptors from being associated with the given words. */
    public void removeConnectionAcceptor(String... words) {
    	synchronized(protocols) {
            protocols.keySet().removeAll(Arrays.asList(words));
    		longestWordSize = 0;
            for(String word : protocols.keySet()) {
    			if (word.length() > longestWordSize)
    				longestWordSize = word.length();
    		}
    	}
    }
    
    /** Determines if the word is valid for the understood protocols. */
    public boolean isValidProtocolWord(String word) {
        return protocols.containsKey(word);
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
            LOG.warn("Unable to set soTimeout, closing client", se);
            IOUtils.close(client);
            return;
        }
        
        // try to find someone who understands this protocol
        Delegator delegator = protocols.get(word);
       
        // no protocol available to handle this word 
        if (delegator == null) {
            // FIXME notify some listener
//            HTTPStat.UNKNOWN_REQUESTS.incrementStat();
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
    private static class Delegator {
    	private final ConnectionAcceptor acceptor;
    	private final boolean localOnly, blocking;
    	
    	Delegator(ConnectionAcceptor acceptor, 
    			boolean localOnly, 
    			boolean blocking) {
    		this.acceptor = acceptor;
    		this.localOnly = localOnly;
    		this.blocking = blocking;
    	}
    	
    	public void delegate(final String word,  final Socket sock, boolean newThread) {
    		boolean localHost = NetworkUtils.isLocalHost(sock);
    		boolean drop = false;
    		if (localOnly && !localHost) {
                LOG.debug("Dropping because we want a local connection, and this isn't localhost");
    			drop = true;
            }
            
    		if (!localOnly && localHost && NetworkUtils.isPrivateAddress(sock.getLocalAddress())) {
                LOG.debug("Dropping because we want an external connection, and this is localhost");
    			drop = true;
            }
    		
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
                if(LOG.isDebugEnabled())
                    LOG.debug("Spawning new thread to dispatch: " + word);
    			ThreadExecutor.startThread(r, "IncomingConnection");
    		} else {
                if(LOG.isDebugEnabled())
                    LOG.debug("Handling dispatched word: " + word + " in same thread");
    			acceptor.acceptConnection(word, sock);
            }
    	}
    }
}

