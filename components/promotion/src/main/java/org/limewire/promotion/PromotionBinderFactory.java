package org.limewire.promotion;

/**
 * Provides a mechanism to retrieve {@link PromotionBinder} instances, which may
 * be retrieved from the network, cached on disk, or distributed in some other
 * manner.
 */
public interface PromotionBinderFactory {

    /**
     * This is the preferred way to retrieve a bucket-based binder. This method
     * may hit the network, or may pull the content from disk cache.
     * 
     * @param bucketNumber a 63-bit number, that this factory will take a
     *        modulus of to determine the real bucket to retrieve.
     * @return A promo binder that corresponds to the given bucket number, or
     *         null if there is no matching bucket (which should be rare).
     */
    PromotionBinder getBinderForBucket(long bucketNumber);

}
