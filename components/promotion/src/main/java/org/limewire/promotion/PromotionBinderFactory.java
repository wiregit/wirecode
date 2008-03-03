package org.limewire.promotion;

/**
 * Provides a mechanism to retrieve {@link PromotionBinder} instances, which may
 * be retrieved from the network, cached on disk, or distributed in some other
 * manner.
 */
public interface PromotionBinderFactory {
    
    PromotionBinder getBinder(String name);
}
