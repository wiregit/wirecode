package org.limewire.security;

import java.util.ArrayList;
import java.util.List;

import org.limewire.concurrent.SimpleTimer;

/**
 * This class manages the crypto aspects of the security token infrastructure.
 */
public class SecurityTokenSmith {

    private volatile static SecurityTokenSmith defaultTokenSmith = new SecurityTokenSmith();

    public static SecurityTokenSmith getDefaultTokenSmith() {
        return defaultTokenSmith;
    }
    
    public static void setSettingsProvider(SettingsProvider provider) {
        defaultTokenSmith = new SecurityTokenSmith(provider);
    }
    
    /**
     * The generator chain with the private keys.
     */
    private final SecurityTokenGeneratorChain keychain;
    
    /**
     * Creates a token smith that does not change its keys.
     */
    public SecurityTokenSmith() {
        keychain = new SimpleTokenGeneratorChain();
    }

    /**
     * Creates a token smith with the specified <tt>SettingsProvider</tt>
     */
    public SecurityTokenSmith(SettingsProvider provider) {
        keychain = new SecurityTokenGeneratorRotator(SimpleTimer.sharedTimer(),
                new TEAFactory(),
                provider);
    }
    
    /**
     * @return the cryptographical output from the provided data.
     */
    public <T extends SecurityToken.TokenData>byte [] getTokenBytes(T data) {
        return keychain.getCurrentTokenGenerator().getTokenBytes(data);
    }
    
    /**
     * @return true if the provided data matches the token data.
     */
    public <T extends SecurityToken.TokenData>Iterable<byte []> getAllBytes(T data) {
        List<byte []> l = new ArrayList<byte[]>(2);
        for (SecurityTokenGenerator validKey : keychain.getValidSecurityTokenGenerators()) 
            l.add(validKey.getTokenBytes(data));
        return l;
    }
    
    /**
     * A simple token geneerator chain that does not expire its keys.
     */
    private static class SimpleTokenGeneratorChain implements SecurityTokenGeneratorChain {
        private final SecurityTokenGenerator[] generators = 
            new SecurityTokenGenerator[]{new TEASecurityTokenGenerator()};
        public SecurityTokenGenerator[] getValidSecurityTokenGenerators() {
            return generators;
        }
        
        public SecurityTokenGenerator getCurrentTokenGenerator() {
            return generators[0];
        }
    }
    
    /**
     * A factory for TEA key generators.
     */
    static class TEAFactory implements SecurityTokenGeneratorFactory {
        public SecurityTokenGenerator createSecurityTokenGenerator() {
            return new TEASecurityTokenGenerator();
        }
    }
    
}
