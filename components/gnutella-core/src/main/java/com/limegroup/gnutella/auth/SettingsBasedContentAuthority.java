package com.limegroup.gnutella.auth;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.settings.ContentSettings;
import com.limegroup.gnutella.util.IpPortImpl;

/** Content Authority that sends requests, randomly, to a host in the settings. */
public class SettingsBasedContentAuthority implements ContentAuthority {

    /** The list of authorities this uses. */
    private ContentAuthority[] authorities;
    
    /** RNG. */
    private Random RNG = newRandom();

    /**
     * Initializes this with the proper IpPortContentAuthorities.
     */
    public boolean initialize() {
        String[] hosts = ContentSettings.AUTHORITIES.getValue();
        List dns = new ArrayList(hosts.length);
        for(int i = 0; i < hosts.length; i++) {
            try {
                dns.add(new IpPortContentAuthority(new IpPortImpl(hosts[i])));
            } catch(UnknownHostException uhe) {}
        }
        
        authorities = (ContentAuthority[])dns.toArray(new ContentAuthority[dns.size()]);
        return !dns.isEmpty();
    }
    
    public ContentAuthority[] getAuthorities() {
        return authorities;
    }
    
    public void setAuthorities(ContentAuthority[] authorities) {
        this.authorities = authorities;
    }

    /** Sends the message to a random one of the content authorities. */
    public void send(Message m) {
        int idx = RNG.nextInt(authorities.length);
        authorities[idx].send(m);
    }
    
    /** Hook for tests. */
    protected Random newRandom() {
        return new Random();
    }

}
