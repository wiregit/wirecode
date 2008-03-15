package org.limewire.promotion.impressions;

import java.util.Date;

import org.limewire.promotion.containers.PromotionMessageContainer;

/**
 * Represents an impression for a single promo message. Should be contained
 * within a {@link UserQueryEvent} for full information about the event,
 * including ordering of results.
 */
public class Impression {
    private final long binderUniqueID;
    private final Date timeShown;
    private final PromotionMessageContainer promo;

    Impression(PromotionMessageContainer promo, long binderUniqueID, Date timeShown) {
        this.binderUniqueID = binderUniqueID;
        this.promo = promo;
        this.timeShown = timeShown;
    }

    public Date getTimeShown() {
        return timeShown;
    }

    public long getBinderUniqueID() {
        return binderUniqueID;
    }

    public PromotionMessageContainer getPromo() {
        return promo;
    }
}
