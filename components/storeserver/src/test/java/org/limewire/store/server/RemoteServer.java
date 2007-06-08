package org.limewire.store.server;

/**
 * Represents the functionality needed for a remote server.
 */
interface RemoteServer {

    boolean storeKey(String publicKey,String privateKey, String ip);

    String lookUpPrivateKey(String publicKey, String ip);

}