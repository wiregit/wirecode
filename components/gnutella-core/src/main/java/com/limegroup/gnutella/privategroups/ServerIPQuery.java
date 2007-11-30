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

    private String username = null;
    private String remoteIPAddress = null;

    
    
    public ServerIPQuery(String remoteIPAddress) {
        this.remoteIPAddress = remoteIPAddress;
    }
    
    
    public void setType(String type) {
        if(type.equals("GET"))
            setType(IQ.Type.GET);
        else if (type.equals("SET"))
            setType(IQ.Type.SET);
        else{
            //neither get or set
        }
    }

    /**
     * Returns the username, or <tt>null</tt> if the username hasn't been sent.
     *
     * @return the username.
     */
    public String getUsername() {
        return username;
    }
    
    /**
     * Sets the username.
     *
     * @param username the username.
     */
    public void setUsername(String username) {
        this.username = username;
    }
    
    
    /**
     * Returns the ipAddress or <tt>null</tt> if the ipAddress hasn't
     * been set.
     *
     * @return the ipAddress.
     */
    public String getIPAddress() {
        return remoteIPAddress;
    }   

//    /**
//     * Sets ipAddress.
//     *
//     * @param ipAddress the ipAddress.
//     */
//    public void setIPAddress(String ipAddress) {
//        this.remoteIPAddress = ipAddress;
//    }
    
    
    public String getChildElementXML() {
        StringBuilder buf = new StringBuilder();
        buf.append("<serverIPQuery xmlns=\"jabber:iq:serveripquery\">");
        if (username != null) {
            if (username.equals("")) {
                buf.append("<username/>");
            }
            else {
                buf.append("<username>").append(username).append("</username>");
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