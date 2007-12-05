package com.limegroup.gnutella.privategroups;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.util.StringUtils;

/**
 * A ServerIPQuery encapsulates username and ipAddress information.  It is used when a user receives a 
 * direct connection request from remote user.  The user sends a ServerIPQuery object (containing ipAddress info)
 *  to the server to obtain the username belonging to the specific ipAddress.
 *
 */
public class ServerIPQuery extends IQ {

    private String remoteUsername;
    private String remoteIPAddress;

    
    
    public ServerIPQuery(String remoteIPAddress) {
        this.remoteIPAddress = remoteIPAddress;
    }

    /**
     * Returns the remote username, or <tt>null</tt> if the username hasn't been sent.
     *
     * @return the remote username.
     */
    public String getUsername() {
        return remoteUsername;
    }
    
    /**
     * Sets the remote username.
     *
     * @param username the remote username.
     */
    public void setUsername(String username) {
        this.remoteUsername = username;
    }

    public String getChildElementXML() {
        StringBuilder buf = new StringBuilder();
        buf.append("<serverIPQuery xmlns=\"jabber:iq:serveripquery\">");
        if (remoteUsername != null) {
            if (remoteUsername.equals("")) {
                buf.append("<username/>");
            }
            else {
                buf.append("<username>").append(remoteUsername).append("</username>");
            }
        }
        
        if (remoteIPAddress != null) {
            if (remoteIPAddress.equals("")) {
                buf.append("<ipAddress/>");
            }
            else {
                buf.append("<ipAddress>").append(StringUtils.escapeForXML(remoteIPAddress)).append("</ipAddress>");
            }
        }

        buf.append("</serverIPQuery>");
        return buf.toString();
    }
    
}