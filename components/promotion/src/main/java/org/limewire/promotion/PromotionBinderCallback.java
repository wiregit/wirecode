package org.limewire.promotion;

/**
 * Instances of this class process a {@link PromotionBinder}. This call will
 * usually be made asynchronously.
 */
public interface PromotionBinderCallback {

    /**
     * Perform some work on <code>binder</code>.
     * 
     * @param binder the {@link PromotionBinder} on which we do work.
     */
    void process(PromotionBinder binder);

}
