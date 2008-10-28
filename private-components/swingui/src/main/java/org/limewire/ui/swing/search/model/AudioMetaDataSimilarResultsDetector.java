package org.limewire.ui.swing.search.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.search.SearchResult;
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
        Map<FilePropertyKey, String> metaData = getMetadata(searchResult);

        String artist = metaData.get(FilePropertyKey.AUTHOR);
        String album = metaData.get(FilePropertyKey.TITLE);
        String track = metaData.get(FilePropertyKey.TRACK_NAME);

        if (!StringUtils.isEmpty(track)) {
            if (!StringUtils.isEmpty(artist)) {
                String artistTrack = metaData.get(FilePropertyKey.AUTHOR) + "-"
                        + metaData.get(FilePropertyKey.TRACK_NAME);
                names.add(artistTrack);

                if (!StringUtils.isEmpty(album)) {
                    String artistAlbumTrack = metaData.get(FilePropertyKey.AUTHOR) + "-"
                            + metaData.get(FilePropertyKey.TITLE) + "-"
                            + metaData.get(FilePropertyKey.TRACK_NAME);
                    names.add(artistAlbumTrack);
                }
            }

            if (!StringUtils.isEmpty(album)) {
                String albumTrack = metaData.get(FilePropertyKey.TITLE) + "-"
                        + metaData.get(FilePropertyKey.TRACK_NAME);
                names.add(albumTrack);
            }
        }
    }

    private Map<FilePropertyKey, String> getMetadata(SearchResult result) {
        String name = result.getProperty(FilePropertyKey.NAME).toString();
        Map<FilePropertyKey, String> metadataCopy = new HashMap<FilePropertyKey, String>();
        copyProperty(result, metadataCopy, FilePropertyKey.TRACK_NAME);
        copyProperty(result, metadataCopy, FilePropertyKey.TITLE);
        copyProperty(result, metadataCopy, FilePropertyKey.AUTHOR);

        StringTokenizer st = new StringTokenizer(name, "-");
        Stack<String> nameParts = new Stack<String>();
        while (st.hasMoreElements()) {
            String part = st.nextToken().trim();
            nameParts.push(part);
        }

        if (!nameParts.empty()) {
            String trackName = nameParts.pop();
            if (StringUtils.isEmpty(metadataCopy.get(FilePropertyKey.TRACK_NAME))) {
                metadataCopy.put(FilePropertyKey.TRACK_NAME, trackName);
            }
            if (!nameParts.empty()) {
                String albumOrArtist = nameParts.pop();
                if (StringUtils.isEmpty(metadataCopy.get(FilePropertyKey.TITLE))) {
                    metadataCopy.put(FilePropertyKey.TITLE, albumOrArtist);
                }
                if (!nameParts.empty()) {
                    String artist = nameParts.pop();
                    if (StringUtils.isEmpty(metadataCopy.get(FilePropertyKey.AUTHOR))) {
                        metadataCopy.put(FilePropertyKey.AUTHOR, artist);
                    }
                }
            }
        }
        cleanProperty(metadataCopy, FilePropertyKey.TRACK_NAME);
        cleanProperty(metadataCopy, FilePropertyKey.TITLE);
        cleanProperty(metadataCopy, FilePropertyKey.AUTHOR);

        return metadataCopy;
    }

    private void copyProperty(SearchResult result,
            Map<FilePropertyKey, String> metadataCopy, FilePropertyKey propertyKey) {
        metadataCopy.put(propertyKey, result.getProperty(propertyKey) == null ? "" : result
                .getProperty(propertyKey).toString());
    }

    private void cleanProperty(Map<FilePropertyKey, String> metadataCopy,
            FilePropertyKey propertyKey) {
        String cleanedProperty = getNameCache().getCleanString(metadataCopy.get(propertyKey));
        metadataCopy.put(propertyKey, cleanedProperty);
    }
}
