package org.limewire.promotion;


/**
 * Provides a mechanism to retrieve {@link PromotionBinder} instances, which may
 * be retrieved from the network, cached on disk, or distributed in some other
 * manner.
 */
public interface PromotionBinderRepository {

    /**
     * This is the preferred way to retrieve a bucket-based binder. This method
     * may hit the network, or may pull the content from disk cache.
     * 
     * @param bucketNumber a 63-bit number, that this factory will take a
     *        modulus of to determine the real bucket to retrieve.
     * @param callback this receives the result
     * @return A promo binder that corresponds to the given bucket number, or
     *         null if there is no matching bucket (which should be rare).
     */
    void getBinderForBucket(long bucketNumber, PromotionBinderCallback callback);
    
    /**
     * Sets a remote URL to use for search.
     * 
     * @param url the new URL
     * @param mod the modulous with which to send the bucket ID
     */
    void init(String url, int mod);

}
