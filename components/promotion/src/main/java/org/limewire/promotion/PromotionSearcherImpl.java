package org.limewire.promotion;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.limewire.concurrent.ManagedThread;
import org.limewire.promotion.SearcherDatabase.QueryResult;
import org.limewire.promotion.containers.PromotionMessageContainer;
import org.limewire.promotion.containers.PromotionMessageContainer.GeoRestriction;
import org.limewire.promotion.impressions.ImpressionsCollector;

import com.google.inject.Inject;
import com.limegroup.gnutella.geocode.GeocodeInformation;

public class PromotionSearcherImpl implements PromotionSearcher {
    private final KeywordUtil keywordUtil;

    private final SearcherDatabase searcherDatabase;

    private final ImpressionsCollector impressionsCollector;
    private final PromotionBinderRepository promotionBinderRepository;

    private int maxNumberOfResults;

    @Inject
    public PromotionSearcherImpl(KeywordUtil keywordUtil, SearcherDatabase searcherDatabase,
            ImpressionsCollector impressionsCollector,
            PromotionBinderRepository promotionBinderRepository) {
        this.keywordUtil = keywordUtil;
        this.searcherDatabase = searcherDatabase;
        this.impressionsCollector = impressionsCollector;
        this.promotionBinderRepository = promotionBinderRepository;
    }

    /**
     * Order of operations:
     * 
     * <ol>
     * <li> normalize the query using {@link KeywordUtil}
     * <li> expire any db results that have passed
     * <li> search the current search db and callback results
     * <li> check to see when the last time this bucket has been fetched, and
     * continue if has not been fetched or it has expired
     * <li> request the {@link PromotionBinder} for this query from the
     * {@link PromotionBinderRepository}
     * <li> insert all valid {@link PromotionMessageContainer} entries into db
     * <li> rerun the db search and callback any new results
     * </ol>
     * 
     * @param query the searched terms
     * @param callback the recipient of the results
     * @param userLocation this can be <code>null</code>
     */
    public void search(String query, PromotionSearchResultsCallback callback,
            GeocodeInformation userLocation) {
        new SearcherThread(query, callback, userLocation).start();
    }
    
    public void init(int maxNumberOfResults) {
        this.maxNumberOfResults = maxNumberOfResults;
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

        /** The latitude/longitude of the user, or null if not known. */
        private final LatitudeLongitude userLatLon;

        /** Two character territory of user ('US') or null if not known. */
        private final String userTerritory;

        SearcherThread(String query, PromotionSearchResultsCallback callback,
                GeocodeInformation userLocation) {
            this.query = query;
            this.callback = callback;
            this.normalizedQuery = keywordUtil.normalizeQuery(query);
            // Now calculate our latitude/longitude from the Geocode, or null
            if (userLocation == null) {
                this.userLatLon = null;
                this.userTerritory = null;
            } else {
                this.userLatLon = getLatitudeLongitude(userLocation);
                this.userTerritory = userLocation
                        .getProperty(GeocodeInformation.Property.CountryCode);
            }
        }

        /**
         * Pulls out the latitude and longitude properties and puts them into a
         * {@link LatitudeLongitude} instance, or returns null if there is a
         * problem parsing or missing data.
         * 
         * @param geocodeInformation this could be <code>null</code>
         */
        private LatitudeLongitude getLatitudeLongitude(GeocodeInformation geocodeInformation) {
            final String lat = geocodeInformation.getProperty(GeocodeInformation.Property.Latitude);
            final String lon = geocodeInformation
                    .getProperty(GeocodeInformation.Property.Longitude);
            if (lat == null || lon == null)
                return null;
            try {
                return new LatitudeLongitude(Double.parseDouble(lat), Double.parseDouble(lon));
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        @Override
        public void run() {
            // OK, start the meat of the query!
            final Date timeQueried = new Date();
            // Get the binder (maybe cached), ingest it, and search...
            promotionBinderRepository.getBinderForBucket(keywordUtil.getHashValue(normalizedQuery),
                    new PromotionBinderCallback() {
                        public void process(PromotionBinder binder) {
                            if (binder == null)
                                return;
                            searcherDatabase.ingest(binder);
                            searcherDatabase.expungeExpired();
                            List<QueryResult> results = searcherDatabase.query(normalizedQuery);
                            removeInvalidResults(results);

                            for (QueryResult result : results) {
                                callback.process(result.getPromotionMessageContainer());
                                // record we just showed this result.
                                impressionsCollector.recordImpression(query, timeQueried,
                                        new Date(), result.getPromotionMessageContainer(), result
                                                .getBinderUniqueId());
                            }
                        }

                    });

        }

        /**
         * Strips out results that don't seem to be valid due to territory,
         * radius, or other restriction.
         */
        private void removeInvalidResults(List<QueryResult> results) {
            for (QueryResult result : new ArrayList<QueryResult>(results)) {
                PromotionMessageContainer promo = result.getPromotionMessageContainer();
                List<GeoRestriction> restrictions = promo.getGeoRestrictions();
                // Check that the user is within any geo restrictions
                if (restrictions.size() > 0 && userLatLon != null) {
                    boolean matchedAtLeastOneRestriction = false;
                    for (GeoRestriction restriction : restrictions) {
                        if (restriction.isWithin(userLatLon)) {
                            matchedAtLeastOneRestriction = true;
                            break;
                        }
                    }
                    if (!matchedAtLeastOneRestriction) {
                        // The user is not within any of the restrictions, so
                        // remove the result
                        results.remove(result);
                        continue;
                    }
                }
                // Check that the user is within any territories
                Locale[] territories = promo.getTerritories();
                if (territories.length > 0 && userTerritory != null) {
                    boolean matchAtLeastOneTerritory = false;
                    for (Locale territory : territories) {
                        if (userTerritory.equalsIgnoreCase(territory.getCountry())) {
                            matchAtLeastOneTerritory = true;
                            break;
                        }
                    }
                    if (!matchAtLeastOneTerritory) {
                        // User isn't in any valid territories
                        results.remove(result);
                        continue;
                    }
                }
            }
        }
    }
}
