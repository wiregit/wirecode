package org.limewire.lws.server;

import org.apache.http.nio.protocol.NHttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandler;

/**
 * This is the main part of this component and allows us to attach our
 * authentication scheme and export and interface so they we can attach
 * instances of {@link HttpRequestHandler} to HTTP acceptors.
 */
public interface LWSDispatcher extends NHttpRequestHandler {
    
    /**
     * The prefix to all requests. This will be stripped off when sending to our
     * handlers.
     */
    public static String PREFIX = "lws:";
    
    /**
     * Base32 encoded public key issued by store web-server
     * used for authenticating download requests.
     */
    public static String LWS_PUBLIC_KEY = "GCBADNZQQIASYBQHFKDERTRYAQATBAQBD4BIDAIA7V7VHAI5OUJCSUW7JKOC53HE473BDN2SHTXUIAGDDY7YBNSREZUUKXKAEJI7WWJ5RVMPVP6F6W5DB5WLTNKWZV4BHOAB2NDP6JTGBN3LTFIKLJE7T7UAI6YQELBE7O5J277LPRQ37A5VPZ6GVCTBKDYE7OB7NU6FD3BQENKUCNNBNEJS6Z27HLRLMHLSV37SEIBRTHORJAA4OAQVACLWAUEPCURQXTFSSK4YFIXLQQF7AWA46UBIDAIA67Q2BBOWTM655S54VNODNOCXXF4ZJL537I5OVAXZK5GAWPIHQJTVCWKXR25NIWKP4ZYQOEEBQC2ESFTREPUEYKAWCO346CJSRTEKNYJ4CZ5IWVD4RUUOBI5ODYV3HJTVSFXKG7YL7IQTKYXR7NRHUAJEHPGKJ4N6VBIZBCNIQPP6CWXFT4DJFC3GL2AHWVJFMQAUYO76Z5ESUA4BQQAAFAMALQOLFQHEP6MTTYBXPIXR4NDJQSXFRDO4RWJBS4OCG4C3B2RP2ICYADOS5S3M5LHS2BBRUNEEBZRDTPJBYCCKAJLDNWLMO7IYPL3BQIMHTHH5I5MDIT2YKJLC3OUZI25YHMVNS735UV4T7XVUJA5B4XSWK223JWCL63PFIAT33QYFQGRXEJ47T4DZT4M3KYGGFXO6DZMLMLIPK";

    
    
    /**
     * Returns <code>true</code> if <code>lis</code> was added as a listener,
     * <code>false</code> otherwise.
     * 
     * @param lis new listener
     * @return <code>true</code> if <code>lis</code> was added as a listener,
     *         <code>false</code> otherwise.
     */
    boolean addConnectionListener(LWSConnectionListener lis);

    /**
     * Returns <code>true</code> if <code>lis</code> was removed as a listener,
     * <code>false</code> otherwise.
     * 
     * @param lis old listener
     * @return <code>true</code> if <code>lis</code> was removed as a listener,
     *         <code>false</code> otherwise.
     */
    boolean removeConnectionListener(LWSConnectionListener lis);
    
    /**
     * Notifies all connection listeners of the new state of connectivity.
     */
    void notifyConnectionListeners(boolean isConnected);
    
}