package com.limegroup.gnutella.handshaking;

import java.util.Properties;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.CommonUtils;

public class LazyProperties extends Properties
{
    
    private MessageRouter router;
    
    protected LazyProperties(MessageRouter router, String remoteIP)
    {
        this.router=router;
        if (router!=null)
        {
            put(ConnectionHandshakeHeaders.LISTEN_IP, "");
            //just temporary!
        }
        if (remoteIP!=null)
        {
            put(ConnectionHandshakeHeaders.REMOTE_IP, remoteIP);
        }
    }
    
    public String getProperty(String key, String defaultValue)
    {
        if (router!=null && key.equals(ConnectionHandshakeHeaders.LISTEN_IP))
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
        if (router!=null && key.equals(
        ConnectionHandshakeHeaders.LISTEN_IP))
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
        props.put(ConnectionHandshakeHeaders.X_QUERY_ROUTING,
                          "0.1");
        props.put(ConnectionHandshakeHeaders.USER_AGENT,
                          CommonUtils.getVendor());       
        props.put(ConnectionHandshakeHeaders.GGEP, 
                          "0.5");
    }
    
}

