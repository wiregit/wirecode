package org.limewire.security;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * An abstract implementation of a security token that can write itself to
 * an output stream and queries the default token smith for its validity.
 */
public abstract class AbstractSecurityToken<T extends SecurityToken.TokenData> implements SecurityToken<T>{

    /** 
     * The encrypted data.
     */
    private byte[] _securityToken;
    
    protected AbstractSecurityToken(T data) {
        _securityToken = getFromMAC(MACCalculatorRepositoryManager.getDefaultRepositoryManager().getMACBytes(data), data);
    }
    
    protected AbstractSecurityToken(byte [] network) throws InvalidSecurityTokenException {
        if (!isValidBytes(network))
            throw new InvalidSecurityTokenException("invalid data: " + Arrays.toString(network));
        
        _securityToken = network;
    }
    
    public final boolean isFor(T data) {
        for (byte[] token : MACCalculatorRepositoryManager.getDefaultRepositoryManager().getAllBytes(data)) {
            if (Arrays.equals(_securityToken, getFromMAC(token ,data)))
                return true;
        }
        
        return false;
    }

    public final void write(OutputStream os) throws IOException {
        os.write(_securityToken);
    }

    public final byte[] getBytes() {
        byte[] b = new byte[_securityToken.length];
        System.arraycopy(_securityToken, 0, b, 0, _securityToken.length);
        return b;
    }
    
    
   /**
     * @param MAC the calculated cryptographic MAC
     * @param data the <tt>TokenData</tt> this security token is created from.
     * @return the payload of this security token as it will appear on the network
     */
    protected abstract byte [] getFromMAC(byte [] MAC, T data);
    
    protected boolean isValidBytes(byte [] network) {
        return network != null && network.length > 0;
    }
       
    
    /**
     * Should not be used if it is possible to call
     * {@link #isFor(org.limewire.security.SecurityToken.TokenData)} which
     * takes all possible security token smiths into account.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SecurityToken) {
            SecurityToken t = (SecurityToken)obj;
            return Arrays.equals(_securityToken, t.getBytes());
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return Arrays.hashCode(_securityToken);
    }

}