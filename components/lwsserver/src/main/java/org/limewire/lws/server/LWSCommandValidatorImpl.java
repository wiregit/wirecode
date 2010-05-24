package org.limewire.lws.server;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.PublicKey;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.api.network.NetworkManager;
import org.limewire.security.SignatureVerifier;
import org.limewire.util.Base32;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;

class LWSCommandValidatorImpl implements LWSCommandValidator{
    
    private final static Log LOG = LogFactory.getLog(LWSCommandValidatorImpl.class);
    
    /**
     * Base32 encoded public key issued by store web-server
     */
    private final String lwsPublicKey;
    
    private final NetworkManager networkManager;
    
    @Inject
    public LWSCommandValidatorImpl(NetworkManager networkManager) {
        this(LWSDispatcher.LWS_PUBLIC_KEY, networkManager);
    }
    
    public LWSCommandValidatorImpl(String lwsPublicKey, NetworkManager networkManager) {
        this.lwsPublicKey = lwsPublicKey;
        this.networkManager = networkManager;
    }
    
    /**
     * Note: It is assumed that the signatureBits is a Base32 encoded string. The {@link Base32} class
     *       used here to decode is custom implementation that does not add padding. It is important
     *       that store server also uses the same implementation.
     */
    @Override
    public boolean verifySignedParameter(String param, String signatureBits) {
        PublicKey key = SignatureVerifier.readKey(lwsPublicKey, "DSA");
        SignatureVerifier signVerifier = new SignatureVerifier(StringUtils.toUTF8Bytes(param), Base32.decode(signatureBits), key, "DSA", "SHA1");
        return signVerifier.verifySignature();
    }
     
    @Override
    public boolean verifyBrowserIPAddresswithClientIP(String browserIPString) {
        byte[] externalIPAddress = networkManager.getExternalAddress();
        try {
            InetAddress browserAddress = InetAddress.getByName(browserIPString);
            byte[] bytes = browserAddress.getAddress();
            return Arrays.equals(bytes, externalIPAddress);
        } catch(UnknownHostException ex) { 
            LOG.warn("recieved invalid browserIPString: " + browserIPString); 
        }
        return false;
    }
}