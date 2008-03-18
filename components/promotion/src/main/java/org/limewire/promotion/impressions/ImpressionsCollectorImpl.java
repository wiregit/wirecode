package org.limewire.promotion.impressions;

import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.limewire.promotion.containers.PromotionMessageContainer;

import com.google.inject.Singleton;

@Singleton
public final class ImpressionsCollectorImpl implements ImpressionsCollector {
    private Map<String, UserQueryEvent> queries = new Hashtable<String, UserQueryEvent>();

    public Set<UserQueryEvent> getCollectedImpressions() {
        return new HashSet<UserQueryEvent>(queries.values());
    }

    /*
     * We store the impression into a query event, and store that event in our
     * queries map. If the event doesn't yet exist, create it.
     */
    public void recordImpression(String originalQuery, Date timeQueried, Date timeShown,
            PromotionMessageContainer promo, long binderUniqueID) {
        UserQueryEvent event = queries.get(getMapKey(originalQuery, timeQueried));
        if (event == null) {
            event = new UserQueryEvent(originalQuery, timeQueried);
            queries.put(getMapKey(originalQuery, timeQueried), event);
        }
        event.addImpression(new Impression(promo, binderUniqueID, timeShown));
    }

    /** The key we use to store/retrieve */
    private String getMapKey(String originalQuery, Date timeQueried) {
        return originalQuery + ":" + timeQueried.getTime();
    }

    public void removeImpressions(Set<UserQueryEvent> events) {
        for (UserQueryEvent event : events)
            queries.remove(getMapKey(event.getOriginalQuery(), event.getOriginalQueryTime()));
    }

}
