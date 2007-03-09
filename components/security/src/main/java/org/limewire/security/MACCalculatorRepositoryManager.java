package org.limewire.security;

import java.util.ArrayList;
import java.util.List;

import org.limewire.concurrent.SimpleTimer;

/**
 * This class manages the crypto aspects of the security token infrastructure.
 */
public class MACCalculatorRepositoryManager {

    private volatile static MACCalculatorRepositoryManager defaultRepositoryManager = new MACCalculatorRepositoryManager();

    public static MACCalculatorRepositoryManager getDefaultRepositoryManager() {
        return defaultRepositoryManager;
    }
    
    public static void setSettingsProvider(SettingsProvider provider) {
        defaultRepositoryManager = new MACCalculatorRepositoryManager(provider);
    }
    
    /**
     * The generator chain with the private keys.
     */
    private final MACCalculatorRepository repository;
    
    /**
     * Creates a token smith that does not change its keys.
     */
    public MACCalculatorRepositoryManager() {
        repository = new SimpleMACCalculatorRepository();
    }

    /**
     * Creates a token smith with the specified <tt>SettingsProvider</tt>
     */
    public MACCalculatorRepositoryManager(SettingsProvider provider) {
        repository = new MACCalculatorRotator(SimpleTimer.sharedTimer(),
                new TEAMACCalculatorFactory(),
                provider);
    }
    
    /**
     * @return the cryptographical output from the provided data.
     */
    public <T extends SecurityToken.TokenData>byte [] getMACBytes(T data) {
        return repository.getCurrentMACCalculator().getMACBytes(data);
    }
    
    /**
     * @return true if the provided data matches the token data.
     */
    public <T extends SecurityToken.TokenData>Iterable<byte []> getAllBytes(T data) {
        List<byte []> l = new ArrayList<byte[]>(2);
        for (MACCalculator validKey : repository.getValidMACCalculators()) 
            l.add(validKey.getMACBytes(data));
        return l;
    }
    
    /**
     * A simple token geneerator chain that does not expire its keys.
     */
    private static class SimpleMACCalculatorRepository implements MACCalculatorRepository {
        private final MACCalculator[] generators = 
            new MACCalculator[]{new TEAMACCalculator()};
        public MACCalculator[] getValidMACCalculators() {
            return generators;
        }
        
        public MACCalculator getCurrentMACCalculator() {
            return generators[0];
        }
    }
    
    /**
     * A factory for TEA key generators.
     */
    static class TEAMACCalculatorFactory implements MACCalculatorFactory {
        public MACCalculator createMACCalculator() {
            return new TEAMACCalculator();
        }
    }
    
}
