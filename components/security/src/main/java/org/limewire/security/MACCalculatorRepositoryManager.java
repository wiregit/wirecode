package org.limewire.security;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import org.limewire.concurrent.SimpleTimer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/**
 * This class manages the crypto aspects of the security token infrastructure.
 */
@Singleton
public class MACCalculatorRepositoryManager {
    
    /**
     * The generator chain with the private keys.
     */
    private final MACCalculatorRepository repository;
    
    @Inject
    public MACCalculatorRepositoryManager(@Named("backgroundExecutor") ScheduledExecutorService defaultExecutor, 
            SettingsProvider provider) {
        repository = new MACCalculatorRotator(defaultExecutor != null ? defaultExecutor : new SimpleTimer(true),
                                              new TEAMACCalculatorFactory(),
                                              provider);
    }
    
    public MACCalculatorRepositoryManager() {
        repository = new SimpleMACCalculatorRepository(new TEAMACCalculatorFactory());
    }
    
    /**
     * @return the cryptographical output from the provided data.
     */
    public byte[] getMACBytes(SecurityToken.TokenData data) {
        return repository.getCurrentMACCalculator().getMACBytes(data);
    }
    
    /**
     * @return true if the provided data matches the token data.
     */
    public Iterable<byte[]> getAllBytes(SecurityToken.TokenData data) {
        List<byte[]> l = new ArrayList<byte[]>(2);
        for (MACCalculator validKey : repository.getValidMACCalculators()) 
            l.add(validKey.getMACBytes(data));
        return l;
    }
    
    public static MACCalculatorFactory createDefaultCalculatorFactory() {
        return new TEAMACCalculatorFactory();
    }
    
}
