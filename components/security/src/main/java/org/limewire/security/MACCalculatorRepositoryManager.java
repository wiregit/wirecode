package org.limewire.security;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import org.limewire.concurrent.SimpleTimer;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * This class manages the crypto aspects of the security token infrastructure.
 */
public class MACCalculatorRepositoryManager {
    
    @Inject @Named("backgroundExecutor") private volatile static ScheduledExecutorService defaultExecutor;

    private volatile static MACCalculatorRepositoryManager defaultRepositoryManager =
        new MACCalculatorRepositoryManager();

    public static MACCalculatorRepositoryManager getDefaultRepositoryManager() {
        return defaultRepositoryManager;
    }
    
    /** Replaces the default repository manager with one backed by the given SettingsProvider. */
    @Inject public static void setDefaultSettingsProvider(SettingsProvider provider) {
        defaultRepositoryManager = new MACCalculatorRepositoryManager(provider);
    }
    
    /** Replaces the default repository manager with the given manager. */
    public static void setDefaultRepositoryManager(MACCalculatorRepositoryManager manager) {
        defaultRepositoryManager = manager;
    }
    
    /**
     * The generator chain with the private keys.
     */
    private final MACCalculatorRepository repository;
    
    /**
     * Creates a token smith that does not change its keys.
     */
    public MACCalculatorRepositoryManager() {
        repository = new SimpleMACCalculatorRepository(new TEAMACCalculatorFactory());
    }

    /**
     * Creates a token smith with the specified <tt>SettingsProvider</tt>
     */
    public MACCalculatorRepositoryManager(SettingsProvider provider) {
        repository = new MACCalculatorRotator(defaultExecutor != null ? defaultExecutor : SimpleTimer.sharedTimer(),
                                              new TEAMACCalculatorFactory(),
                                              provider);
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
