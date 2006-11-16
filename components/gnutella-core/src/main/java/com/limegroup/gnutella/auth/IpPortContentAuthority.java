package com.limegroup.gnutella.auth;

import java.net.UnknownHostException;

import com.limegroup.gnutella.FileDetails;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.messages.vendor.ContentRequest;
import com.limegroup.gnutella.messages.vendor.ContentResponse;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.IpPortImpl;

/** A ContentAuthority that sends to a single IpPort. */
// TODO fberger doc
public class IpPortContentAuthority implements ContentAuthority {
    
    private IpPort authority;
    
    /** host/port to store which'll be used when initializing, if necessary */
    private String host;
    private int port;
    
    private final boolean handleResponses;
    
    public IpPortContentAuthority(IpPort host) {
        this(host, false);
    }
    
    /** Constructs the authority with the given IpPort. */
    public IpPortContentAuthority(IpPort host, boolean handleResponses) {
    	this(host.getAddress(), host.getPort(), handleResponses);
    	this.authority = host;
    }

    /**
     * Constructs the authority with the given host/port.
     * You must call initialize prior to sending a message.
     */
    public IpPortContentAuthority(String host, int port, boolean handleResponses) {
        this.host = host;
        this.port = port;
        this.handleResponses = handleResponses;
    }
    
    /** Constructs the authority from the host/port if necessary */
    public boolean initialize() {
        if (authority == null) {
            try {
                authority = new IpPortImpl(host, port);
            } catch (UnknownHostException uhe) {
                return false;
                // ignored.
            }
        }
            
        return true;
    }
    
    public IpPort getIpPort() {
        return authority;
    }

	public void shutdown() {
	}

	public void sendAuthorizationRequest(FileDetails details, long timeout) {
		UDPService.instance().send(new ContentRequest(details), authority);			
	}

	public void setContentResponseObserver(ContentResponseObserver observer) {
		if (handleResponses && observer != null) {
			RouterService.getMessageRouter().setUDPMessageHandler(ContentResponse.class, new ContentResponseHandler(observer));
		}
	}
}
