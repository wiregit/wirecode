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
    private String remoteIPPort;
    private String remoteIPPublicKey;


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
    
    
    /**
     * Returns the remote port, or <tt>null</tt> if the port hasn't been sent.
     *
     * @return the remote port.
     */
    public String getPort() {
        return remoteIPPort;
    }
    
    /**
     * Sets the remote port.
     *
     * @param username the remote port.
     */
    public void setPort(String port) {
        this.remoteIPPort = port;
    }
    
    /**
     * Returns the remote public key, or <tt>null</tt> if the public key hasn't been sent.
     *
     * @return the remote public key.
     */
    public String getPublicKey() {
        return remoteIPPublicKey;
    }
    
    /**
     * Sets the remote public key.
     *
     * @param username the remote public key.
     */
    public void setPublicKey(String publicKey) {
        this.remoteIPPublicKey = publicKey;
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
        
        if (remoteIPPort != null) {
            if (remoteIPPort.equals("")) {
                buf.append("<port/>");
            }
            else {
                buf.append("<port>").append(StringUtils.escapeForXML(remoteIPPort)).append("</port>");
            }
        }
        
        if (remoteIPPublicKey != null) {
            if (remoteIPPublicKey.equals("")) {
                buf.append("<publicKey/>");
            }
            else {
                buf.append("<publicKey>").append(StringUtils.escapeForXML(remoteIPPublicKey)).append("</publicKey>");
            }
        }

        buf.append("</serverIPQuery>");
        return buf.toString();
    }
    
}