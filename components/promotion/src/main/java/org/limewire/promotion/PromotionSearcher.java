package org.limewire.promotion;

import java.util.List;

import org.limewire.promotion.containers.PromotionMessageContainer;

public interface PromotionSearcher {
    /**
     * Performs a search based on the given query and then invokes the
     * {@link PromotionSearchResultsCallback#process(List)} method on the
     * passed-in callback passing in the results to display to the user in the
     * order they are expected to be shown. May call the callback method more
     * than once, but will not submit duplicate results.
     */
    void search(String query, PromotionSearchResultsCallback callback);

    /**
     * The recipient of promotion search results. Implementations should be able
     * to take a list of {@link PromotionMessageContainer} entries and do
     * something with them (ie, display them to the user).
     */
    interface PromotionSearchResultsCallback {
        /**
         * Process the passed in list of {@link PromotionMessageContainer} and
         * do something with them. Should expect to be called more than once.
         * 
         * @param results never-null list of {@link PromotionMessageContainer}
         */
        void process(List<PromotionMessageContainer> results);
    }

}
