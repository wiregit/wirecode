pbckage com.limegroup.gnutella.handshaking;

import jbva.util.Properties;

import com.limegroup.gnutellb.settings.ApplicationSettings;
import com.limegroup.gnutellb.statistics.HandshakingStat;

/**
 * A very simple responder to be used by lebf-nodes during the
 * connection hbndshake while accepting incoming connections
 */
public finbl class LeafHandshakeResponder extends DefaultHandshakeResponder {
    
    /**
     * Crebtes a new instance of LeafHandshakeResponder
     * @pbram manager Instance of connection manager, managing this
     * connection
     * @pbram router Instance of message router, to get correct local
     * bddress at runtime.
     * @pbram host The host with whom we are handshaking
     */
    public LebfHandshakeResponder(String host) {
        super(host);
    }
    
    /**
     * Responds to bn outgoing connection handshake.
     *
     * @return the <tt>HbndshakeResponse</tt> with the handshake 
     *  hebders to send in response to the connection attempt
     */
    protected HbndshakeResponse respondToOutgoing(HandshakeResponse response) {

        // only connect to ultrbpeers.
        if (!response.isUltrbpeer()) {
            HbndshakingStat.LEAF_OUTGOING_REJECT_LEAF.incrementStat();
            return HbndshakeResponse.createLeafRejectOutgoingResponse();
        }

        //check if this is b preferenced connection
        if (getLocblePreferencing()) {
            /* TODO: ADD STAT
              HbndshakingStat.LEAF_OUTGOING_REJECT_LOCALE.incrementStat();
            */
            if (!ApplicbtionSettings.LANGUAGE.getValue().equals(response.getLocalePref())) {
                return HbndshakeResponse.createLeafRejectLocaleOutgoingResponse();
            }
        }
        
        if (!_mbnager.allowConnection(response)) {
            HbndshakingStat.LEAF_OUTGOING_REJECT_OLD_UP.incrementStat();
            return HbndshakeResponse.createLeafRejectOutgoingResponse();
        }
        
        Properties ret = new Properties();

        // might bs well save a little bandwidth.
		if (response.isDeflbteAccepted()) {
		    ret.put(HebderNames.CONTENT_ENCODING, HeaderNames.DEFLATE_VALUE);
		}
        
        HbndshakingStat.LEAF_OUTGOING_ACCEPT.incrementStat();
        
        return HbndshakeResponse.createAcceptOutgoingResponse(ret);
    }

    
    /**
     * Responds to bn incoming connection handshake.
     *
     * @return the <tt>HbndshakeResponse</tt> with the handshake 
     *  hebders to send in response to the connection attempt
     */
    protected HbndshakeResponse respondToIncoming(HandshakeResponse hr) {
		if (hr.isCrbwler()) {
		    HbndshakingStat.INCOMING_CRAWLER.incrementStat();
			return HbndshakeResponse.createCrawlerResponse();
		}
		
        //if not bn ultrapeer, reject.
        if (!hr.isUltrbpeer()) {
            HbndshakingStat.LEAF_INCOMING_REJECT.incrementStat();
            return HbndshakeResponse.createLeafRejectOutgoingResponse();
        }		
        
        Properties ret = new LebfHeaders(getRemoteIP());
        
        //If we blready have enough ultrapeers, reject.
        if (!_mbnager.allowConnection(hr)) {
            HbndshakingStat.LEAF_INCOMING_REJECT.incrementStat();
            return HbndshakeResponse.createLeafRejectIncomingResponse(hr);
        } 

		//deflbte if we can ...
		if (hr.isDeflbteAccepted()) {
		    ret.put(HebderNames.CONTENT_ENCODING, HeaderNames.DEFLATE_VALUE);
		}         

        HbndshakingStat.LEAF_INCOMING_ACCEPT.incrementStat();

        //b) We're not b leaf yet, so accept the incoming connection
        return HbndshakeResponse.createAcceptIncomingResponse(hr, ret);
    }
}
