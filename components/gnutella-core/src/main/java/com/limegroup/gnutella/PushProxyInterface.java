package com.limegroup.gnutella;

import java.net.InetAddress;

/** A simple interface for components that can be considered PushProxies for
 *  your client.
 */
public interface PushProxyInterface {
    /** @return a non-negative integer representing the proxy's port for UDP
     *  communication.
     */
    public int getPushProxyPort();
    
    /** @return the address, in bytes, of the PushProxyHost host 
     */
    public byte[] getPushProxyAddress();
}
