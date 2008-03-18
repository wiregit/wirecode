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
    
    /** The modulous to take with teh bucket ID. */
    private int mod;
    
    @Inject
    public PromotionBinderRepositoryImpl(PromotionBinderRequestor requestor, ImpressionsCollector impressionsCollector) {
        this.requestor = requestor;
        this.impressionsCollector = impressionsCollector;
    }

    public void getBinderForBucket(long bucketNumber, PromotionBinderCallback callback) {
        getBinderForBucketOnNetwork(bucketNumber % mod,callback);
    }

    public void init(String url, int mod) {
        this.searchUrl = url;
        this.mod = mod;
    }
    
    /**
     * 
     * @param bucketNumber the modded bucket number
     * @param callback
     */
    private void getBinderForBucketOnNetwork(long bucketNumber, PromotionBinderCallback callback) {
        if (searchUrl == null) {
            
        }
        Set<UserQueryEvent> queries = impressionsCollector.getCollectedImpressions();
        requestor.request(searchUrl, bucketNumber, queries, callback);
    }



}
