package org.limewire.security;

import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.SimpleTimer;
import org.limewire.security.SecurityToken.TokenData;

/**
 * This class manages the crypto aspects of the query key 
 * infrastructure.
 */
public class QueryKeySmith {

    private static final Log LOG = LogFactory.getLog(QueryKeySmith.class);

    private volatile static QueryKeySmith defaultKeySmith = new QueryKeySmith();

    public static QueryKeySmith getDefaultKeySmith() {
        return defaultKeySmith;
    }
    
    public static void setSettingsProvider(SettingsProvider provider) {
        defaultKeySmith = new QueryKeySmith(provider);
    }
    
    /**
     * The keychain with the private keys.
     */
    private final QKGeneratorKeychain keychain;
    
    /**
     * Creates a keysmith that does not change its keys.
     */
    public QueryKeySmith() {
        keychain = new SimpleKeychain();
    }

    /**
     * Creates a keysmith with the specified <tt>SettingsProvider</tt>
     */
    public QueryKeySmith(SettingsProvider provider) {
        keychain = new QKGeneratorRotator(SimpleTimer.sharedTimer(),
                new TEAFactory(),
                provider);
    }
    
    /**
     * @return the cryptographical output from the provided data.
     */
    public byte [] getKeyBytes(TokenData data) {
        return keychain.getSecretKey().getKeyBytes(data);
    }
    
    /**
     * @return true if the provided data matches the token data.
     */
    public boolean isFor(byte [] toCheck, TokenData data) {
        for (QueryKeyGenerator validKey : keychain.getValidQueryKeyGenerators()) {
            if (Arrays.equals(validKey.getKeyBytes(data), toCheck)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("invalid data: "+data);
                }
                return true;
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Data corrupt or expired:"+data);
        }
        return false;
    }
    
    /**
     * A simple keychain that does not expire its keys.
     */
    private static class SimpleKeychain implements QKGeneratorKeychain {
        private final QueryKeyGenerator []keys = 
            new QueryKeyGenerator[]{new TEAQueryKeyGenerator()};
        public QueryKeyGenerator[] getValidQueryKeyGenerators() {
            return keys;
        }
        
        public QueryKeyGenerator getSecretKey() {
            return keys[0];
        }
    }
    
    /**
     * A factory for TEA key generators.
     */
    private static class TEAFactory implements QKGeneratorFactory {
        public QueryKeyGenerator createQueryKeyGenerator() {
            return new TEAQueryKeyGenerator();
        }
    }
    
}
