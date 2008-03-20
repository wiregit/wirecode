package org.limewire.promotion;

/**
 * Defines an interface to create {@link PromotionBinder}s given an input
 * stream.
 */
public interface PromotionBinderFactory {

    /**
     * Returns a new {@link PromotionBinder}s given an input stream.
     * 
     * @param byets given bytes defining the binder -- these <b>can</b> be
     *        <code>null</code>
     * @return a new {@link PromotionBinder}s given the bytes or
     *         <code>null</code> if <code>bytes</code> are <code>null</code>.
     */
    PromotionBinder newBinder(byte[] bytes);
}
