package org.limewire.promotion.impressions;

import java.util.Date;

import org.limewire.promotion.containers.PromotionMessageContainer;

/**
 * Represents an impression for a single promo message. Should be contained
 * within a {@link UserQueryEvent} for full information about the event,
 * including ordering of results.
 */
public final class Impression {
    private final String binderUniqueName;
    private final Date timeShown;
    private final PromotionMessageContainer promo;

    Impression(PromotionMessageContainer promo, String binderUniqueName, Date timeShown) {
        this.binderUniqueName = binderUniqueName;
        this.promo = promo;
        this.timeShown = timeShown;
    }

    public Date getTimeShown() {
        return timeShown;
    }

    public String getBinderUniqueName() {
        return binderUniqueName;
    }

    public PromotionMessageContainer getPromo() {
        return promo;
    }
}
