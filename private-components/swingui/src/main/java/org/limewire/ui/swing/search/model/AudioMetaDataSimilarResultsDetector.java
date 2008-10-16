package org.limewire.ui.swing.search.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.limewire.core.api.Category;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.SearchResult.PropertyKey;
import org.limewire.util.StringUtils;

/**
 * Compares the meta data of two SearchResults to see if the results match.
 * 
 * If there is missing meta data, the file name will be parsed to try and fill
 * in the missing pieces.
 * 
 * The keys identifying this VisualSearchResult are built by the
 * getCleanIdentifyingStrings method.
 */
public class AudioMetaDataSimilarResultsDetector extends AbstractNameSimilarResultsDetector {
    private static final String REPLACE = "\\(\\d*\\)|[-_. ()\\d]";

    public AudioMetaDataSimilarResultsDetector() {
        super(Pattern.compile(REPLACE));
    }

    @Override
    public void detectSimilarResult(VisualSearchResult visualSearchResult) {
        if (visualSearchResult.getCategory() == Category.AUDIO) {
            super.detectSimilarResult(visualSearchResult);
        }
    }

    @Override
    public Set<String> getCleanIdentifyingStrings(VisualSearchResult visualSearchResult) {
        Set<String> names = new HashSet<String>();
        for (SearchResult searchResult : visualSearchResult.getCoreSearchResults()) {
            addNames(names, searchResult);
        }
        return names;
    }

    private void addNames(Set<String> names, SearchResult searchResult) {
        Map<SearchResult.PropertyKey, String> metaData = getMetadata(searchResult);

        String artist = metaData.get(PropertyKey.ARTIST_NAME);
        String album = metaData.get(PropertyKey.ALBUM_TITLE);
        String track = metaData.get(PropertyKey.TRACK_NAME);

        if (!StringUtils.isEmpty(track)) {
            if (!StringUtils.isEmpty(artist)) {
                String artistTrack = metaData.get(PropertyKey.ARTIST_NAME) + "-"
                        + metaData.get(PropertyKey.TRACK_NAME);
                names.add(artistTrack);

                if (!StringUtils.isEmpty(album)) {
                    String artistAlbumTrack = metaData.get(PropertyKey.ARTIST_NAME) + "-"
                            + metaData.get(PropertyKey.ALBUM_TITLE) + "-"
                            + metaData.get(PropertyKey.TRACK_NAME);
                    names.add(artistAlbumTrack);
                }
            }

            if (!StringUtils.isEmpty(album)) {
                String albumTrack = metaData.get(PropertyKey.ALBUM_TITLE) + "-"
                        + metaData.get(PropertyKey.TRACK_NAME);
                names.add(albumTrack);
            }
        }
    }

    private Map<SearchResult.PropertyKey, String> getMetadata(SearchResult result) {
        String name = result.getProperty(SearchResult.PropertyKey.NAME).toString();
        Map<SearchResult.PropertyKey, String> metadataCopy = new HashMap<SearchResult.PropertyKey, String>();
        copyProperty(result, metadataCopy, PropertyKey.TRACK_NAME);
        copyProperty(result, metadataCopy, PropertyKey.ALBUM_TITLE);
        copyProperty(result, metadataCopy, PropertyKey.ARTIST_NAME);

        StringTokenizer st = new StringTokenizer(name, "-");
        Stack<String> nameParts = new Stack<String>();
        while (st.hasMoreElements()) {
            String part = st.nextToken().trim();
            nameParts.push(part);
        }

        if (!nameParts.empty()) {
            String trackName = nameParts.pop();
            if (StringUtils.isEmpty(metadataCopy.get(SearchResult.PropertyKey.TRACK_NAME))) {
                metadataCopy.put(SearchResult.PropertyKey.TRACK_NAME, trackName);
            }
            if (!nameParts.empty()) {
                String albumOrArtist = nameParts.pop();
                if (StringUtils.isEmpty(metadataCopy.get(SearchResult.PropertyKey.ALBUM_TITLE))) {
                    metadataCopy.put(SearchResult.PropertyKey.ALBUM_TITLE, albumOrArtist);
                }
                if (!nameParts.empty()) {
                    String artist = nameParts.pop();
                    if (StringUtils.isEmpty(metadataCopy.get(SearchResult.PropertyKey.ARTIST_NAME))) {
                        metadataCopy.put(SearchResult.PropertyKey.ARTIST_NAME, artist);
                    }
                }
            }
        }
        cleanProperty(metadataCopy, PropertyKey.TRACK_NAME);
        cleanProperty(metadataCopy, PropertyKey.ALBUM_TITLE);
        cleanProperty(metadataCopy, PropertyKey.ARTIST_NAME);

        return metadataCopy;
    }

    private void copyProperty(SearchResult result,
            Map<SearchResult.PropertyKey, String> metadataCopy, PropertyKey propertyKey) {
        metadataCopy.put(propertyKey, result.getProperty(propertyKey) == null ? "" : result
                .getProperty(propertyKey).toString());
    }

    private void cleanProperty(Map<SearchResult.PropertyKey, String> metadataCopy,
            PropertyKey propertyKey) {
        String cleanedProperty = getNameCache().getCleanString(metadataCopy.get(propertyKey));
        metadataCopy.put(propertyKey, cleanedProperty);
    }
}
