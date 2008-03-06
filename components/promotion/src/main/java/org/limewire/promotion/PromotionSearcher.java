package org.limewire.promotion;

import java.util.List;

import org.limewire.promotion.containers.PromotionMessageContainer;

import com.limegroup.gnutella.geocode.GeocodeInformation;

public interface PromotionSearcher {
    /**
     * Performs a search based on the given query and then invokes the
     * {@link PromotionSearchResultsCallback#process(List)} method on the
     * passed-in callback passing in the results to display to the user in the
     * order they are expected to be shown. May call the callback method more
     * than once, but will not submit duplicate results.
     * 
     * @param userLocation The current location of the user (best guess). If
     *        null, no location restrictions will be applied. If missing
     *        lat/longitude, no radius restrictions will be applied. If missing
     *        territory, no territory restrictions will be applied.
     */
    void search(String query, PromotionSearchResultsCallback callback,
            GeocodeInformation userLocation);

    /**
     * The recipient of promotion search results. Implementations should be able
     * to take a list of {@link PromotionMessageContainer} entries and do
     * something with them (ie, display them to the user).
     */
    public interface PromotionSearchResultsCallback {
        /**
         * Process the passed in {@link PromotionMessageContainer} and do
         * something with it . Should expect to be called more than once.
         */
        void process(PromotionMessageContainer result);
    }

}
