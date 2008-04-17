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

    private final SearcherDatabase searcherDatabase;

    /** The search URL. */
    private String searchUrl;

    /** The modulus to take with the bucket ID. */
    private int modulus;

    @Inject
    public PromotionBinderRepositoryImpl(final PromotionBinderRequestor requestor,
            final ImpressionsCollector impressionsCollector, final SearcherDatabase searcherDatabase) {
        this.requestor = requestor;
        this.impressionsCollector = impressionsCollector;
        this.searcherDatabase = searcherDatabase;
    }

    public PromotionBinder getBinderForBucket(final long bucketNumber) {
        // See if we have a cached binder...
        final int bucket = (int) (bucketNumber % modulus);
        searcherDatabase.expungeExpired();
        final PromotionBinder binder = searcherDatabase.getBinder(bucket);
        if (binder != null)
            return binder;
        else
            return getBinderForBucketOnNetwork(bucket);
    }

    public void init(final String url, final int modulus) {
        this.searchUrl = url;
        this.modulus = modulus;
    }

    /**
     * Queries the network for the given bucket.
     * 
     * @param bucketNumber the bucket number (modulus'ed down from the 63-bit
     *        monster)
     * @param callback where to drop the binder after retrieval
     */
    private PromotionBinder getBinderForBucketOnNetwork(final long bucketNumber) {
        if (searchUrl == null) {

        }
        final Set<UserQueryEvent> queries = impressionsCollector.getCollectedImpressions();
        String url = searchUrl;
        url += "?now=" + System.currentTimeMillis() / 1000;
        PromotionBinder result = requestor.request(url, bucketNumber, queries);
        
        // Now remove the query events
        impressionsCollector.removeImpressions(queries);
        
        return result;
    }

}
