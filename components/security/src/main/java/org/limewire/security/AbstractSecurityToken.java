package org.limewire.security;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * An abstract implementation of a security token that can write itself to
 * an output stream and queries the default token smith for its validity.
 */
public abstract class AbstractSecurityToken implements SecurityToken {

    /** 
     * The encrypted data.
     */
    private byte[] _securityToken;
    
    protected AbstractSecurityToken(TokenData data) {
        _securityToken = getFromMAC(MACCalculatorRepositoryManager.getDefaultRepositoryManager().getMACBytes(data), data);
    }
    
    protected AbstractSecurityToken(byte [] network) throws InvalidSecurityTokenException {
        if (!isValidBytes(network))
            throw new InvalidSecurityTokenException("invalid data: " + Arrays.toString(network));
        
        _securityToken = network;
    }
    
    public final boolean isFor(TokenData data) {
        if(!isValidTokenData(data))
            return false;
        
        for (byte[] token : MACCalculatorRepositoryManager.getDefaultRepositoryManager().getAllBytes(data)) {
            if (Arrays.equals(_securityToken, getFromMAC(token ,data)))
                return true;
        }
        
        return false;
    }

    /**
     * Determines if the given TokenData is valid for this SecurityToken.
     * By default, all TokenDatas are valid.
     */
    protected boolean isValidTokenData(TokenData data) {
        return true;
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
    protected abstract byte [] getFromMAC(byte [] MAC, TokenData data);
    
    /**
     * Determines if the given data bytes are valid.
     * By default, all non-null and non-empty bytes are valid.
     * 
     * @param network
     * @return
     */
    protected boolean isValidBytes(byte[] network) {
        return network != null && network.length > 0;
    }
       
    
    /**
     * Should not be used if it is possible to call
     * {@link #isFor(org.limewire.security.SecurityToken.TokenData)} which
     * takes all possible MACCalculatorRepositories into account.
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