package com.limegroup.gnutella.handshaking;

import java.util.Properties;

import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.statistics.HandshakingStat;

/**
 * A very simple responder to be used by leaf-nodes during the
 * connection handshake while accepting incoming connections
 */
public final class LeafHandshakeResponder extends DefaultHandshakeResponder {
    
    private final HeadersFactory headersFactory;
    private final ConnectionManager connectionManager;
    
    /**
     * Creates a new instance of LeafHandshakeResponder
     * @param manager Instance of connection manager, managing this
     * connection
     * @param router Instance of message router, to get correct local
     * address at runtime.
     * @param host The host with whom we are handshaking
     */
    LeafHandshakeResponder(String host, HeadersFactory headersFactory, ConnectionManager connectionManager) {
        super(host);
        
        this.headersFactory = headersFactory;
        this.connectionManager = connectionManager;
    }
    
    /**
     * Responds to an outgoing connection handshake.
     *
     * @return the <tt>HandshakeResponse</tt> with the handshake 
     *  headers to send in response to the connection attempt
     */
    protected HandshakeResponse respondToOutgoing(HandshakeResponse response) {

        // only connect to ultrapeers.
        if (!response.isUltrapeer()) {
            HandshakingStat.LEAF_OUTGOING_REJECT_LEAF.incrementStat();
            return HandshakeResponse.createLeafRejectOutgoingResponse(HandshakeStatus.WE_ARE_LEAVES);
        }

        //check if this is a preferenced connection
        if (getLocalePreferencing()) {
            /* TODO: ADD STAT
              HandshakingStat.LEAF_OUTGOING_REJECT_LOCALE.incrementStat();
            */
            if (!ApplicationSettings.LANGUAGE.getValue().equals(response.getLocalePref())) {
                return HandshakeResponse.createLeafRejectLocaleOutgoingResponse();
            }
        }
        
        HandshakeStatus status = _manager.allowConnection(response);
        if(!status.isAcceptable()) {
            HandshakingStat.LEAF_OUTGOING_REJECT_OLD_UP.incrementStat();
            return HandshakeResponse.createLeafRejectOutgoingResponse(status);
        }
        
        Properties ret = new Properties();

        // might as well save a little bandwidth.
		if (response.isDeflateAccepted()) {
		    ret.put(HeaderNames.CONTENT_ENCODING, HeaderNames.DEFLATE_VALUE);
		}
        
        HandshakingStat.LEAF_OUTGOING_ACCEPT.incrementStat();
        
        return HandshakeResponse.createAcceptOutgoingResponse(ret);
    }

    
    /**
     * Responds to an incoming connection handshake.
     *
     * @return the <tt>HandshakeResponse</tt> with the handshake 
     *  headers to send in response to the connection attempt
     */
    protected HandshakeResponse respondToIncoming(HandshakeResponse hr) {
		if (hr.isCrawler()) {
		    HandshakingStat.INCOMING_CRAWLER.incrementStat();
			return HandshakeResponse.createCrawlerResponse(connectionManager);
		}
		
        //if not an ultrapeer, reject.
        if (!hr.isUltrapeer()) {
            HandshakingStat.LEAF_INCOMING_REJECT.incrementStat();
            return HandshakeResponse.createLeafRejectOutgoingResponse(HandshakeStatus.WE_ARE_LEAVES);
        }		
        
        Properties ret = headersFactory.createLeafHeaders(getRemoteIP());
        
        //If we already have enough ultrapeers, reject.
        HandshakeStatus status = _manager.allowConnection(hr);
        if (!status.isAcceptable()) {
            HandshakingStat.LEAF_INCOMING_REJECT.incrementStat();
            return HandshakeResponse.createLeafRejectIncomingResponse(hr, status);
        } 

		//deflate if we can ...
		if (hr.isDeflateAccepted()) {
		    ret.put(HeaderNames.CONTENT_ENCODING, HeaderNames.DEFLATE_VALUE);
		}         

        HandshakingStat.LEAF_INCOMING_ACCEPT.incrementStat();

        //b) We're not a leaf yet, so accept the incoming connection
        return HandshakeResponse.createAcceptIncomingResponse(hr, ret);
    }
}