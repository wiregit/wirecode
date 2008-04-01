package org.limewire.promotion;

import java.util.Set;

import org.limewire.promotion.impressions.UserQueryEvent;

/**
 * Instances of this class make POST requests to the URL given for containers,
 * and also add the data given in the {@link UserQueryEvent}.
 */
public interface PromotionBinderRequestor {

    /**
     * The main entry point. This will create a <code>POST</code> request to
     * <code>url</code> and include the proper information we want to store.
     */
    void request(String url, long id, Set<? extends UserQueryEvent> queries, PromotionBinderCallback callback);

}
