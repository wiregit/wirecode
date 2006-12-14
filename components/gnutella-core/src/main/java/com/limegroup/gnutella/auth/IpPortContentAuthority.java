package com.limegroup.gnutella.auth;

import com.limegroup.gnutella.FileDetails;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.messages.vendor.ContentRequest;
import com.limegroup.gnutella.messages.vendor.ContentResponse;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.IpPortImpl;

/** A ContentAuthority that sends to a single IpPort. */
// TODO fberger doc
public class IpPortContentAuthority extends AbstractContentAuthority {
    
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
    	super(2000);
        this.host = host;
        this.port = port;
        this.handleResponses = handleResponses;
    }
    
    /** Constructs the authority from the host/port if necessary */
    public void initialize() throws Exception {
        if (authority == null) {
        	authority = new IpPortImpl(host, port);
        }
    }
    
    public IpPort getIpPort() {
        return authority;
    }

	public void shutdown() {
	}

	public void sendAuthorizationRequest(FileDetails details) {
		UDPService.instance().send(new ContentRequest(details), authority);			
	}

	public void setContentResponseObserver(ContentResponseObserver observer) {
		if (handleResponses && observer != null) {
			RouterService.getMessageRouter().setUDPMessageHandler(ContentResponse.class, new ContentResponseHandler(observer));
		}
	}
}
