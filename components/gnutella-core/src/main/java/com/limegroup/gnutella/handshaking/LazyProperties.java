package com.limegroup.gnutella.handshaking;

import java.util.Properties;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.updates.*;
import com.limegroup.gnutella.util.CommonUtils;

public class LazyProperties extends Properties
{
    
    protected LazyProperties(String remoteIP)
    {
		put(ConnectionHandshakeHeaders.LISTEN_IP, "");
		//just temporary!

        if (remoteIP!=null)
        {
            put(ConnectionHandshakeHeaders.REMOTE_IP, remoteIP);
        }
    }
    
    /** 
     * Use this if you want to create a LazyProperty with the TCP
     * ConnectBack request.
     * @param tcpConnectBack true if you want to get connected back via TCP.
     */
    protected LazyProperties(boolean tcpConnectBack) {
        this(null, tcpConnectBack, false, null);
    }
    
    /** 
     * Use this if you want to create a LazyProperty with the UDP
     * ConnectBack request.
     * @param udpConnectBack true if you want to get connected back via UDP.
     * @param GUID The GUID you want to be connected back with via UDP.
     */
    protected LazyProperties(boolean udpConnectBack, byte[] GUID) {
        this(null, false, udpConnectBack, GUID);
    }
    
    /** 
     * Use this if you want to create a LazyProperty with both the TCP
     * ConnectBack request and the UDP ConnectBack request.
     * @param tcpConnectBack true if you want to get connected back via TCP.
     * @param udpConnectBack true if you want to get connected back via UDP.
     * @param udpGUID The GUID you want to be connected back with via UDP.
     */
    protected LazyProperties(String remoteIP, boolean tcpConnectBack, 
                             boolean udpConnectBack, byte[] udpGUID) {
        this(remoteIP);
        if (tcpConnectBack)
            put(ConnectionHandshakeHeaders.X_TCP_CONNECTBACK, "");
        if (udpConnectBack && (udpGUID != null))
            put(ConnectionHandshakeHeaders.X_UDP_CONNECTBACK, 
                new String(udpGUID));
    }
    
    public String getProperty(String key, String defaultValue)
    {
        if (key.equals(ConnectionHandshakeHeaders.LISTEN_IP))
        {
            Endpoint e=new Endpoint(RouterService.getAddress(), 
									RouterService.getPort());
            return e.getHostname()+":"+e.getPort();
        } else
        {
            return super.getProperty(key, defaultValue);
        }
    }
    
    public String getProperty(String key)
    {
        if (key.equals(ConnectionHandshakeHeaders.LISTEN_IP))
        {
            Endpoint e=new Endpoint(RouterService.getAddress(), 
									RouterService.getPort());
            return e.getHostname()+":"+e.getPort();
        } else
        {
            return super.getProperty(key);
        }
    }
    
    /** Sets the common properties in props, like Query-Routing and
     *  User-Agent.*/
    protected void addCommonProperties(Properties props) {
        props.put(ConnectionHandshakeHeaders.X_QUERY_ROUTING, "0.1");
        props.put(ConnectionHandshakeHeaders.USER_AGENT,
				  CommonUtils.getHttpServer());       
        props.put(ConnectionHandshakeHeaders.GGEP, "0.5");
		props.put(ConnectionHandshakeHeaders.X_GUESS, "0.1");
        UpdateManager u = UpdateManager.instance();
        String latestVersion = u.getVersion();
        if(!latestVersion.equals("@version@"))//don't send header for @version@
           props.put(ConnectionHandshakeHeaders.X_VERSION, latestVersion);
    }
    
}

