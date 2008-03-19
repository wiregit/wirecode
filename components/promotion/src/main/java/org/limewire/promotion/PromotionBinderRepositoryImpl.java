package org.limewire.promotion;

import java.util.Set;

import org.limewire.promotion.impressions.ImpressionsCollector;
import org.limewire.promotion.impressions.UserQueryEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class PromotionBinderRepositoryImpl implements PromotionBinderRepository {

    private final PromotionBinderRequestor requestor;

    private final ImpressionsCollector impressionsCollector;

    /** The search URL. */
    private String searchUrl;

    /** The modulus to take with the bucket ID. */
    private int modulus;

    @Inject
    public PromotionBinderRepositoryImpl(final PromotionBinderRequestor requestor,
            final ImpressionsCollector impressionsCollector) {
        this.requestor = requestor;
        this.impressionsCollector = impressionsCollector;
    }

    public void getBinderForBucket(final long bucketNumber, final PromotionBinderCallback callback) {
        getBinderForBucketOnNetwork(bucketNumber % modulus, callback);
    }

    public void init(final String url, final int modulus) {
        this.searchUrl = url;
        this.modulus = modulus;
    }

    /**
     * Queries the network for the given
     * 
     * @param bucketNumber the bucket number (modulus'ed down from the 63-bit
     *        monster)
     * @param callback where to drop the binder after retrieval
     */
    private void getBinderForBucketOnNetwork(long bucketNumber, PromotionBinderCallback callback) {
        if (searchUrl == null) {

        }
        Set<UserQueryEvent> queries = impressionsCollector.getCollectedImpressions();
        requestor.request(searchUrl, bucketNumber, queries, callback);
    }

}
