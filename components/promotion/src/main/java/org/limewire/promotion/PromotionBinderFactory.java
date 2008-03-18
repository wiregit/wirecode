package org.limewire.promotion;

import java.io.InputStream;

/**
 * Defines an interface to create {@link PromotionBinder}s given an input
 * stream.
 */
public interface PromotionBinderFactory {

    /**
     * Returns a new {@link PromotionBinder}s given an input stream.
     * 
     * @param in given input stream
     * @return a new {@link PromotionBinder}s given an input stream.
     */
    PromotionBinder newBinder(byte[] bytes);
}
