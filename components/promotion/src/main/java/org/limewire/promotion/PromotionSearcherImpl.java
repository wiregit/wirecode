package org.limewire.promotion;

import org.limewire.concurrent.ManagedThread;
import org.limewire.promotion.containers.PromotionMessageContainer;

public class PromotionSearcherImpl implements PromotionSearcher {
    private final KeywordUtil keywordUtil;

    public PromotionSearcherImpl(KeywordUtil keywordUtil) {
        this.keywordUtil = keywordUtil;
    }

    /**
     * Order of operations:
     * <ol>
     * <li>normalize the query using {@link KeywordUtil}
     * <li>expire any db results that have passed
     * <li>search the current search db and callback results
     * <li>check to see when the last time this bucket has been fetched, and
     * continue if has not been fetched or it has expired
     * <li>request the {@link PromotionBinder} for this query from the
     * {@link PromotionBinderFactory}
     * <li>insert all valid {@link PromotionMessageContainer} entries into db
     * <li>rerun the db search and callback any new results
     */
    public void search(String query, PromotionSearchResultsCallback callback) {
        new SearcherThread(query, callback).start();
    }

    /**
     * This thread handles the real work of the {@link PromotionSearcher},
     * conducting the search and invoking the callback passed to the search
     * method. When the search has completed, this thread dies.
     */
    private class SearcherThread extends ManagedThread {
        private final String query;

        private final PromotionSearchResultsCallback callback;

        private final String normalizedQuery;

        SearcherThread(String query, PromotionSearchResultsCallback callback) {
            this.query = query;
            this.callback = callback;
            this.normalizedQuery = keywordUtil.normalizeQuery(query);
        }

        @Override
        public void run() {
            // TODO Auto-generated method stub
            super.run();
        }
    }
}
