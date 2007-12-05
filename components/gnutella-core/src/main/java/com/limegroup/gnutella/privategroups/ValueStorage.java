package com.limegroup.gnutella.privategroups;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.util.StringUtils;


/**
 * A class that encapsulates username, ipAddress, port, and public key information.  A ValueStorage object
 * is used to get or set username, ipAddress, port, and public key information on the server.  The "set" 
 * operation is used when the user first logs in (so current ip and public key info can be stored).  The 
 * "get" operation is used to get information about a remote user prior to establishing a direct connection.
 */
public class ValueStorage extends IQ {

    private String username;
    private String ipAddress;
    private String port;
    private String publicKey;
    
    
    public ValueStorage() {
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
        return ipAddress;
    }   

    /**
     * Sets ipAddress.
     *
     * @param ipAddress the ipAddress.
     */
    public void setIPAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    /**
     * Returns the port or <tt>null</tt> if the port hasn't
     * been set.
     *
     * @return the port.
     */
    public String getPort() {
        return port;
    }   

    /**
     * Sets the port
     *
     * @param port the port
     */
    public void setPort(String port) {
        this.port = port;
    }
    
    /**
     * Returns the publicKey <tt>null</tt> if the publicKey hasn't
     * been set.
     *
     * @return the publicKey.
     */
    public String getPublicKey() {
        return publicKey;
    }   

    /**
     * Sets the publicKey
     *
     * @param publicKey the publicKey
     */
    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }
    
    public String getChildElementXML() {
        StringBuilder buf = new StringBuilder();
        buf.append("<valueStorage xmlns=\"jabber:iq:stor\">");
        if (username != null) {
            if (username.equals("")) {
                buf.append("<username/>");
            }
            else {
                buf.append("<username>").append(username).append("</username>");
            }
        }
  
        
        if (ipAddress != null) {
            if (ipAddress.equals("")) {
                buf.append("<ipAddress/>");
            }
            else {
                buf.append("<ipAddress>").append(StringUtils.escapeForXML(ipAddress)).append("</ipAddress>");
            }
        }
        
        if (port != null) {
            if (port.equals("")) {
                buf.append("<port/>");
            }
            else {
                buf.append("<port>").append(StringUtils.escapeForXML(port)).append("</port>");
            }
        }
        
        if (publicKey != null) {
            if (publicKey.equals("")) {
                buf.append("<publicKey/>");
            }
            else {
                buf.append("<publicKey>").append(StringUtils.escapeForXML(publicKey)).append("</publicKey>");
            }
        }
        

        buf.append("</valueStorage>");
        return buf.toString();
    }

}
