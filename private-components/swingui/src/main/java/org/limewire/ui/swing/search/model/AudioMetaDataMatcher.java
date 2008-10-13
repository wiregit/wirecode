package org.limewire.ui.swing.search.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.limewire.core.api.search.SearchResult;
import org.limewire.util.MediaType;
import org.limewire.util.StringUtils;

/**
 * Compares the meta data of two SearchResults to see if the results match.
 * 
 * If there is missing meta data, the file name will be parsed to try and fill
 * in the missing pieces.
 */
public class AudioMetaDataMatcher extends AbstractSearchResultMatcher {
    private static final String REPLACE = "\\(\\d*\\)|[-_. ()\\d]";

    private Pattern pattern = Pattern.compile(REPLACE);

    @Override
    public boolean matches(SearchResult result1, SearchResult result2) {
        MediaType mediaType1 = MediaType.getMediaTypeForExtension(result1.getFileExtension());
        MediaType mediaType2 = MediaType.getMediaTypeForExtension(result2.getFileExtension());
        
        if (MediaType.getAudioMediaType().equals(mediaType1)
                && MediaType.getAudioMediaType().equals(mediaType2)) {
            Map<SearchResult.PropertyKey, Object> metaData1 = getMetadata(result1);
            Map<SearchResult.PropertyKey, Object> metaData2 = getMetadata(result2);
            return approximateMatch(metaData1, metaData2);
        }
        return false;
    }

    private Map<SearchResult.PropertyKey, Object> getMetadata(SearchResult result) {
        String name = result.getProperty(SearchResult.PropertyKey.NAME).toString();
        StringTokenizer st = new StringTokenizer(name, "-");
        Map<SearchResult.PropertyKey, Object> metadataCopy = new HashMap<SearchResult.PropertyKey, Object>(
                result.getProperties());
        Stack<String> nameParts = new Stack<String>();
        while (st.hasMoreElements()) {
            String part = st.nextToken().trim();
            nameParts.push(part);
        }

        if (metadataCopy.get(SearchResult.PropertyKey.TRACK_NAME) == null) {
            metadataCopy.put(SearchResult.PropertyKey.TRACK_NAME, "");
        }
        if (metadataCopy.get(SearchResult.PropertyKey.ALBUM_TITLE) == null) {
            metadataCopy.put(SearchResult.PropertyKey.ALBUM_TITLE, "");
        }
        if (metadataCopy.get(SearchResult.PropertyKey.ARTIST_NAME) == null) {
            metadataCopy.put(SearchResult.PropertyKey.ARTIST_NAME, "");
        }

        if (!nameParts.empty()) {
            String trackName = nameParts.pop();
            if (StringUtils.isEmpty((String) metadataCopy.get(SearchResult.PropertyKey.TRACK_NAME))) {
                metadataCopy.put(SearchResult.PropertyKey.TRACK_NAME, trackName);
            }
            if (!nameParts.empty()) {
                String albumOrArtist = nameParts.pop();
                if (StringUtils.isEmpty((String) metadataCopy
                        .get(SearchResult.PropertyKey.ALBUM_TITLE))) {
                    metadataCopy.put(SearchResult.PropertyKey.ALBUM_TITLE, albumOrArtist);
                }
                if (!nameParts.empty()) {
                    String artist = nameParts.pop();
                    if (StringUtils.isEmpty((String) metadataCopy
                            .get(SearchResult.PropertyKey.ARTIST_NAME))) {
                        metadataCopy.put(SearchResult.PropertyKey.ARTIST_NAME, artist);
                    }
                }
            }
        }
        return metadataCopy;
    }

    private boolean approximateMatch(Map<SearchResult.PropertyKey, Object> metaData1,
            Map<SearchResult.PropertyKey, Object> metaData2) {
        String track1 = (String) metaData1.get(SearchResult.PropertyKey.TRACK_NAME);
        String track2 = (String) metaData2.get(SearchResult.PropertyKey.TRACK_NAME);
        track1 = cleanString(track1);
        track2 = cleanString(track2);

        if (!track1.equalsIgnoreCase(track2)) {
            return false;
        }

        String album1 = (String) metaData1.get(SearchResult.PropertyKey.ALBUM_TITLE);
        String album2 = (String) metaData2.get(SearchResult.PropertyKey.ALBUM_TITLE);
        album1 = cleanString(album1);
        album2 = cleanString(album2);

        if (!StringUtils.isEmpty(album1) && !album1.equalsIgnoreCase(album2)) {
            String artist1 = (String) metaData1.get(SearchResult.PropertyKey.ARTIST_NAME);
            String artist2 = (String) metaData2.get(SearchResult.PropertyKey.ARTIST_NAME);
            artist1 = cleanString(artist1);
            artist2 = cleanString(artist2);
            if (StringUtils.isEmpty(artist1)) {
                return artist2.equalsIgnoreCase(album1);
            } else if (StringUtils.isEmpty(artist2)) {
                return artist1.equalsIgnoreCase(album2);
            } else {
                return false;
            }
        }

        String artist1 = (String) metaData1.get(SearchResult.PropertyKey.ARTIST_NAME);
        String artist2 = (String) metaData2.get(SearchResult.PropertyKey.ARTIST_NAME);
        artist1 = cleanString(artist1);
        artist2 = cleanString(artist2);

        if (!StringUtils.isEmpty(artist1) && !StringUtils.isEmpty(artist2)) {
            return artist1.equalsIgnoreCase(artist2);
        } else {
            return true;
        }
    }

    /**
     * Removes all symbols and spaces in the string. Also removes any leaf
     * elements on the name. file1.txt file1(1).txt, file1(2).txt
     */
    private String cleanString(String string) {
        if (string == null) {
            return "";
        }
        Matcher matcher = pattern.matcher(string);
        string = matcher.replaceAll("");
        return string;
    }
}
