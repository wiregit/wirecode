package org.limewire.promotion;

import java.util.Set;

import org.limewire.promotion.impressions.ImpressionsCollector;
import org.limewire.promotion.impressions.UserQueryEvent;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class PromotionBinderRepositoryImpl implements PromotionBinderRepository {

    private final PromotionBinderRequestor requestor;

    private final ImpressionsCollector impressionsCollector;

    private final SearcherDatabase searcherDatabase;
    
    private final PromotionServices promotionServices;

    /** The search URL. */
    private Provider<String> searchUrl;

    /** The modulus to take with the bucket ID. */
    private Provider<Integer> modulus;

    @Inject
    public PromotionBinderRepositoryImpl(final PromotionBinderRequestor requestor,
            final ImpressionsCollector impressionsCollector, final SearcherDatabase searcherDatabase, PromotionServices promotionServices) {
        this.requestor = requestor;
        this.impressionsCollector = impressionsCollector;
        this.searcherDatabase = searcherDatabase;
        this.promotionServices = promotionServices;
    }

    public PromotionBinder getBinderForBucket(final long bucketNumber) {
        // See if we have a cached binder...
        final int bucket = (int) (bucketNumber % modulus.get());
        try {
            searcherDatabase.expungeExpired();
        } catch (DatabaseExecutionException e) {
            promotionServices.stop();
            return null;
        }
        final PromotionBinder binder = searcherDatabase.getBinder(bucket);
        if (binder != null)
            return binder;
        else
            return getBinderForBucketOnNetwork(bucket);
    }

    public void init(Provider<String> url, Provider<Integer> modulus) {
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
        final Set<UserQueryEvent> queries = impressionsCollector.getCollectedImpressions();
        String url = searchUrl.get();
        url += "?now=" + System.currentTimeMillis() / 1000;
        PromotionBinder result = requestor.request(url, bucketNumber, queries);
        
        //TODO: if there was an exception contacting the server, the impressions
        //      aren't really recorded, but this still erases them.
        
        // Now remove the query events
        impressionsCollector.removeImpressions(queries);
        
        return result;
    }

}
