package org.limewire.ui.swing.friends.settings;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.Icon;

import org.jdesktop.swingx.icon.EmptyIcon;
import org.limewire.core.api.friend.client.RosterEvent;
import org.limewire.io.UnresolvedIpPort;
import org.limewire.listener.EventListener;
import org.limewire.util.StringUtils;

/**
 * Stores all the information required to log into an XMPP server and
 * describe the server in the UI.
 */
class FriendAccountConfigurationImpl implements FriendAccountConfiguration {

    private final String resource;
    private final boolean isDebugEnabled;
    private final boolean modifyUser;
    private final boolean requiresDomain;
    private final Icon icon;
    
    private volatile String serviceName;
    private volatile String label;
    private volatile String username;
    private volatile String password;
    private final List<UnresolvedIpPort> defaultServers;
    private final Type type;
    /**
     * Synchronized, since accessed by the core in different threads. 
     */
    private final Map<String, Object> attributes = Collections.synchronizedMap(new HashMap<String, Object>()); 

    /** Constructs an XMPPAccountConfiguration with the following parameters. */
    public FriendAccountConfigurationImpl(boolean requireDomain, String serviceName, String label, Icon icon, String resource, List<UnresolvedIpPort> defaultServers, Type type) {
        this(requireDomain, serviceName, label, icon, resource, defaultServers, true, type);
    }
    
    /** Constructs a basic XMPPAccountConfiguration that cannot modify the serviceName. */
    public FriendAccountConfigurationImpl(String serviceName, String label, String resource, Type type) {
        this(false, serviceName, label, null, resource, UnresolvedIpPort.EMPTY_LIST, false, type);
    }
    
    private FriendAccountConfigurationImpl(boolean requireDomain, String serviceName, String label, Icon icon, String resource, List<UnresolvedIpPort> defaultServers, boolean modifyUser, Type type) {
        this.resource = resource;
        this.modifyUser = modifyUser;
        this.type = type;
        this.isDebugEnabled = false;
        this.requiresDomain = requireDomain;
        this.serviceName = serviceName;
        this.label = label;
        if(icon != null) {
            this.icon = icon;
        } else {
            this.icon = new EmptyIcon(16, 16);
        }
        this.username = "";
        this.password = "";
        this.defaultServers = defaultServers;
    }

    @Override
    public boolean isDebugEnabled() {
        return isDebugEnabled;
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }

    @Override
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public Icon getIcon() {
        return icon;
    }

    @Override
    public String getUserInputLocalID() {
        return username;
    }

    @Override
    public void setUsername(String username) {
        if(modifyUser) {
            // Some servers expect the domain to be included, others don't
            int at = username.indexOf('@');
            if(requiresDomain && at == -1)
                username += "@" + getServiceName(); // Guess the domain
            else if(!requiresDomain && at > -1)
                username = username.substring(0, at); // Strip the domain
        }
        this.username = username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String getResource() {
        return resource;
    }

    @Override
    public String getCanonicalizedLocalID() {
        // friend and friendpresence ids are
        // canonicalized to be lowercase
        return username.toLowerCase(Locale.US);
    }

    @Override
    public String getNetworkName() {
        return serviceName;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public EventListener<RosterEvent> getRosterListener() {
        return null;
    }

    @Override
    public String toString() {
        return StringUtils.toStringBlacklist(this, password);
    }

    @Override
    public List<UnresolvedIpPort> getDefaultServers() {
        return defaultServers;
    }

    @Override
    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    @Override
    public void setAttribute(String key, Object property) {
        attributes.put(key, property);
    }
}
