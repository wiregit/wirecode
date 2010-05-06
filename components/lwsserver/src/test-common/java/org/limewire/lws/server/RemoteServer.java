package org.limewire.lws.server;

import java.security.PrivateKey;

/**
 * Represents the functionality needed for a remote server. This does <b>NOT</b>
 * represent something that will be on the client, it represents the Lime Wire
 * Store, for example.
 */
public interface RemoteServer {
    
    /**
     * Returns the {@link Thread} that started this server, after starting it.
     * 
     * @return the {@link Thread} that started this server, after starting it
     */
    Thread start();
    
    /**
     * returns download url with signed hash and ip which Javascript 
     * will then pass to client for verification.
     * 
     * /store/downloads?hash=&signedHash=&browserIP=&signedBrowserIP=
     * 
     * @param hash
     * @param ip
     * @return
     */
    String generateDownloadURL(String hash, String ip);
    
    /**
     * returns the generated private key.
     * @return
     */
    PrivateKey getPrivateKey();

}