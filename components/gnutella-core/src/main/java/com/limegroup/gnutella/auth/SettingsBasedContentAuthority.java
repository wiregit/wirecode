package com.limegroup.gnutella.auth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.limegroup.gnutella.FileDetails;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.messages.vendor.ContentResponse;
import com.limegroup.gnutella.settings.ContentSettings;
import com.limegroup.gnutella.util.IpPortImpl;

/** Content Authority that sends requests, randomly, to a host in the settings. */
public class SettingsBasedContentAuthority extends AbstractContentAuthority {

    /** The list of authorities this uses. */
    private ContentAuthority[] authorities;
    
    /** RNG. */
    private Random RNG = newRandom();
    
    public SettingsBasedContentAuthority() {
    	super(10 * 1000);
    }
    
    /**
     * Initializes this with the proper IpPortContentAuthorities.
     * @throws Exception 
     */
    public void initialize() throws Exception {
        String[] hosts = ContentSettings.AUTHORITIES.getValue();
        List<ContentAuthority> dns = new ArrayList<ContentAuthority>(hosts.length);
        for (String host : hosts) {
        	try {
        		ContentAuthority auth = new IpPortContentAuthority(new IpPortImpl(host));
        		auth.initialize();
        		dns.add(auth);
        	} 
        	catch(Exception e) {
            }
        }
        
        authorities = dns.toArray(new ContentAuthority[dns.size()]);
        
        if (dns.isEmpty()) {
        	throw new Exception("None of the hosts could be resolved: " + Arrays.asList(hosts));
        }
    }
    
    public void shutdown() {
    	for (ContentAuthority auth : authorities) {
    		auth.shutdown();
    	}
    	authorities = null;
    	RouterService.getMessageRouter().setUDPMessageHandler(ContentResponse.class, null);
    }
    
    public ContentAuthority[] getAuthorities() {
        return authorities;
    }
    
    public void setAuthorities(ContentAuthority[] authorities) {
        this.authorities = authorities;
    }

    /** Hook for tests. */
    protected Random newRandom() {
        return new Random();
    }

	public void sendAuthorizationRequest(FileDetails details) {
	    int idx = RNG.nextInt(authorities.length);
		authorities[idx].sendAuthorizationRequest(details);	
	}

	public void setContentResponseObserver(ContentAuthorityResponseObserver observer) {
		if (observer != null) { 
			RouterService.getMessageRouter().setUDPMessageHandler(ContentResponse.class, new ContentResponseHandler(this, observer));
		}
	}
}
