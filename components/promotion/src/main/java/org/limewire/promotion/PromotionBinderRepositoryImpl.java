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
    private String searchUrl;
    
    @Inject
    public PromotionBinderRepositoryImpl(PromotionBinderRequestor requestor, ImpressionsCollector impressionsCollector) {
        this.requestor = requestor;
        this.impressionsCollector = impressionsCollector;
    }

    public void getBinderForBucket(long bucketNumber, PromotionBinderCallback callback) {
        getBinderForBucketOnNetwork(bucketNumber,callback);
    }

    public void init(String url) {
        this.searchUrl = url;
    }
    
    private void getBinderForBucketOnNetwork(long bucketNumber, PromotionBinderCallback callback) {
        if (searchUrl == null) {
            
        }
        Set<UserQueryEvent> queries = impressionsCollector.getCollectedImpressions();
        requestor.request(searchUrl, bucketNumber, queries, callback);
    }



}
