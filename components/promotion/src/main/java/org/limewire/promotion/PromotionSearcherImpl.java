package org.limewire.promotion;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.limewire.concurrent.ManagedThread;
import org.limewire.promotion.SearcherDatabase.QueryResult;
import org.limewire.promotion.containers.PromotionMessageContainer;
import org.limewire.promotion.containers.PromotionMessageContainer.GeoRestriction;

import com.google.inject.Inject;
import com.limegroup.gnutella.geocode.GeocodeInformation;

public class PromotionSearcherImpl implements PromotionSearcher {
    private final KeywordUtil keywordUtil;

    private final SearcherDatabase searcherDatabase;

    @Inject
    public PromotionSearcherImpl(KeywordUtil keywordUtil, SearcherDatabase searcherDatabase) {
        this.keywordUtil = keywordUtil;
        this.searcherDatabase = searcherDatabase;
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
     * {@link PromotionBinderFactory}
     * <li> insert all valid {@link PromotionMessageContainer} entries into db
     * <li> rerun the db search and callback any new results
     * 
     */
    public void search(String query, PromotionSearchResultsCallback callback,
            GeocodeInformation userLocation) {
        new SearcherThread(query, callback, userLocation).start();
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
            // Now calculate our lat/longitude from the Geocode, or null
            this.userLatLon = getLatitudeLongitude(userLocation);
            this.userTerritory = userLocation.getProperty(GeocodeInformation.Property.CountryCode);
        }

        /**
         * Pulls out the latitude and longitude properties and puts them into a
         * {@link LatitudeLongitude} instance, or returns null if there is a
         * problem parsing or missing data.
         */
        private LatitudeLongitude getLatitudeLongitude(GeocodeInformation geocodeInformation) {
            String lat = geocodeInformation.getProperty(GeocodeInformation.Property.Latitude);
            String lon = geocodeInformation.getProperty(GeocodeInformation.Property.Longitude);
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
            searcherDatabase.expungeExpired();
            List<QueryResult> results = searcherDatabase.query(normalizedQuery);
            removeInvalidResults(results);

            for (QueryResult result : results)
                callback.process(result.getPromotionMessageContainer());

            // TODO
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
