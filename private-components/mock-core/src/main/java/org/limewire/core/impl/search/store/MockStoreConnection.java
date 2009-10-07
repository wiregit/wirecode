package org.limewire.core.impl.search.store;

import java.net.URL;
import java.util.Properties;

import org.limewire.core.api.Category;
import org.limewire.core.api.search.store.StoreConnection;
import org.limewire.core.api.search.store.StoreStyle;
import org.limewire.core.api.search.store.StoreStyle.Type;

import com.google.inject.Inject;

/**
 * Implementation of StoreConnection for the mock core.
 */
public class MockStoreConnection implements StoreConnection {
    private static final String COLON = ":";
    private static final String COMMA = ",";
    private static final String DQUOTE = "\"";
    private static final String JSON_ARR = "[]";
    private static final String JSON_OBJ = "{}";

    private final Properties properties;
    
    private StoreStyle.Type styleType;
    
    @Inject
    public MockStoreConnection() {
        properties = new Properties();
        styleType = Type.STYLE_A;
    }
    
    @Override
    public String doQuery(String query) {
        StringBuilder buf = new StringBuilder(JSON_OBJ);
        
        try {
            // Load style properties.
            URL url = MockStoreStyle.class.getResource("MockStoreStyle.properties");
            properties.load(url.openStream());

            // Insert store style and results.
            insertValue(buf, quoted("storeStyle") + COLON + createStyleJSON());
            insertValue(buf, quoted("storeResults") + COLON + createResultsJSON());
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return buf.toString();
    }
    
    /**
     * Sets the store style type for mock results.
     */
    public void setStyleType(StoreStyle.Type styleType) {
        this.styleType = styleType;
    }
    
    /**
     * Returns the store style object formatted as a JSON string.
     */
    private String createStyleJSON() {
        StringBuilder buf = new StringBuilder(JSON_OBJ);
        
        insertNameValue(buf, "type", styleType.toString());
        insertValue(buf, getNameValuePair("background"));
        insertValue(buf, getNameValuePair("buyAlbumIcon"));
        insertValue(buf, getNameValuePair("buyTrackIcon"));
        insertValue(buf, getNameValuePair("classicBuyIcon"));
        insertValue(buf, getNameValuePair("classicPauseIcon"));
        insertValue(buf, getNameValuePair("classicPlayIcon"));
        insertValue(buf, getNameValuePair("classicPriceFont"));
        insertValue(buf, getNameValuePair("classicPriceForeground"));
        insertValue(buf, getNameValuePair("downloadAlbumIcon"));
        insertValue(buf, getNameValuePair("downloadTrackIcon"));
        insertValue(buf, getNameValuePair("headingFont"));
        insertValue(buf, getNameValuePair("headingForeground"));
        insertValue(buf, getNameValuePair("infoFont"));
        insertValue(buf, getNameValuePair("infoForeground"));
        insertValue(buf, getNameValuePair("priceBackground"));
        insertValue(buf, getNameValuePair("priceBorderColor"));
        insertValue(buf, getNameValuePair("priceFont"));
        insertValue(buf, getNameValuePair("priceForeground"));
        insertValue(buf, getNameValuePair("showTracksFont"));
        insertValue(buf, getNameValuePair("showTracksForeground"));
        insertValue(buf, getNameValuePair("streamIcon"));
        insertValue(buf, getNameValuePair("subHeadingFont"));
        insertValue(buf, getNameValuePair("subHeadingForeground"));
        insertValue(buf, getNameValuePair("trackFont"));
        insertValue(buf, getNameValuePair("trackForeground"));
        insertValue(buf, getNameValuePair("trackLengthFont"));
        insertValue(buf, getNameValuePair("trackLengthForeground"));
        
        insertValue(buf, getNameValuePair("downloadButtonVisible"));
        insertValue(buf, getNameValuePair("priceButtonVisible"));
        insertValue(buf, getNameValuePair("priceVisible"));
        insertValue(buf, getNameValuePair("showInfoOnHover"));
        insertValue(buf, getNameValuePair("showTracksOnHover"));
        insertValue(buf, getNameValuePair("streamButtonVisible"));
        
        return buf.toString();
    }
    
    /**
     * Returns an array of store results formatted as a JSON string.
     */
    private String createResultsJSON() {
        int i = 0;
        StringBuilder resultArr = new StringBuilder(JSON_ARR);
        
        // Create album.
        StringBuilder album = new StringBuilder(JSON_OBJ);
        insertNameValue(album, "artist", "GreenMonkeys");
        insertNameValue(album, "title", "The Collection That Keeps on Playing and Playing and Playing and Playing and Playing");
        insertNameValue(album, "album", "The Collection That Keeps on Playing and Playing and Playing and Playing and Playing");
        insertNameValue(album, "albumIcon", "albumCover.png");
        insertNameValue(album, "albumId", "666");
        insertNameValue(album, "bitRate", String.valueOf(128));
        insertNameValue(album, "category", Category.AUDIO.toString());
        insertNameValue(album, "fileName", "Green Monkeys The Collection.mp3");
        insertNameValue(album, "fileSize", String.valueOf(9 * 1024 * 1024));
        insertNameValue(album, "genre", "Jazz");
        insertNameValue(album, "infoPage", getClass().getResource("fileInfo.html").toString());
        insertNameValue(album, "length", String.valueOf(568));
        insertNameValue(album, "price", "4 Credits");
        insertNameValue(album, "quality", String.valueOf(3));
        insertNameValue(album, "trackCount", "3");
        insertNameValue(album, "URN", "www.store.limewire.com" + i);
        
        // Create tracks.
        StringBuilder trackArr = new StringBuilder(JSON_ARR);
        StringBuilder track = new StringBuilder(JSON_OBJ);
        insertNameValue(track, "albumId", "666");
        insertNameValue(track, "artist", "Green Monkeys");
        insertNameValue(track, "title", "Heh?");
        insertNameValue(track, "bitRate", String.valueOf(128));
        insertNameValue(track, "fileName", "Green Monkeys - Heh.mp3");        
        insertNameValue(track, "fileSize", String.valueOf(3 * 1024 * 1024));
        insertNameValue(track, "length", String.valueOf(129));
        insertNameValue(track, "price", "1 Credit");
        insertNameValue(track, "quality", String.valueOf(3));
        insertNameValue(track, "trackNumber", String.valueOf(1));
        insertNameValue(track, "URN", "www.store.limewire.com" + (i + 1));
        insertValue(trackArr, track.toString());

        track = new StringBuilder(JSON_OBJ);
        insertNameValue(track, "albumId", "666");
        insertNameValue(track, "artist", "Green Monkeys");
        insertNameValue(track, "title", "Take Me To Space (Man)");
        insertNameValue(track, "bitRate", String.valueOf(128));
        insertNameValue(track, "fileName", "Green Monkeys - Take Me To Space (Man).mp3");        
        insertNameValue(track, "fileSize", String.valueOf(3 * 1024 * 1024));
        insertNameValue(track, "length", String.valueOf(251));
        insertNameValue(track, "price", "1 Credit");
        insertNameValue(track, "quality", String.valueOf(3));
        insertNameValue(track, "trackNumber", String.valueOf(2));
        insertNameValue(track, "URN", "www.store.limewire.com" + (i + 2));
        insertValue(trackArr, track.toString());

        track = new StringBuilder(JSON_OBJ);
        insertNameValue(track, "albumId", "666");
        insertNameValue(track, "artist", "Green Monkeys");
        insertNameValue(track, "title", "Crush");
        insertNameValue(track, "bitRate", String.valueOf(128));
        insertNameValue(track, "fileName", "Green Monkeys - Crush.mp3");        
        insertNameValue(track, "fileSize", String.valueOf(3 * 1024 * 1024));
        insertNameValue(track, "length", String.valueOf(188));
        insertNameValue(track, "price", "1 Credit");
        insertNameValue(track, "quality", String.valueOf(3));
        insertNameValue(track, "trackNumber", String.valueOf(3));
        insertNameValue(track, "URN", "www.store.limewire.com" + (i + 3));
        insertValue(trackArr, track.toString());
        
        // Add tracks to album, and album to results.
        insertValue(album, quoted("tracks") + COLON + trackArr.toString());
        insertValue(resultArr, album.toString());
        
        // Create single file result.
        StringBuilder media = new StringBuilder(JSON_OBJ);
        insertNameValue(media, "artist", "GreenMonkeys");
        insertNameValue(media, "title", "Chomp");
        insertNameValue(media, "album", "Premonitions, Echoes & Science");
        insertNameValue(media, "bitRate", String.valueOf(256));
        insertNameValue(media, "category", Category.AUDIO.toString());
        insertNameValue(media, "genre", "Jazz");
        insertNameValue(media, "fileName", "Green Monkeys Chomp.mp3");
        insertNameValue(media, "fileSize", String.valueOf(3 * 1024 * 1024));
        insertNameValue(media, "infoPage", getClass().getResource("fileInfo.html").toString());
        insertNameValue(media, "length", String.valueOf(208));
        insertNameValue(media, "price", "1 Credit");
        insertNameValue(media, "quality", String.valueOf(3));
        insertNameValue(media, "URN", "www.store.limewire.com" + (i + 10));
        
        // Add file to results.
        insertValue(resultArr, media.toString());
        
        return resultArr.toString();
    }
    
    /**
     * Inserts the specified name and value into the JSON buffer.  Both the 
     * name and value strings are enclosed in double-quotes.
     */
    private StringBuilder insertNameValue(StringBuilder buf, String name, String value) {
        // Create quoted name/value pair.
        String pair = quoted(name) + COLON + quoted(value);
        insertValue(buf, pair);
        return buf;
    }
    
    /**
     * Inserts the specified value into the JSON buffer.
     */
    private StringBuilder insertValue(StringBuilder buf, String value) {
        // Insert separator if needed.
        if (buf.length() > 2) {
            buf.insert(buf.length() - 1, COMMA);
        }
        
        // Insert value before closing character.
        buf.insert(buf.length() - 1, value);
        
        return buf;
    }
    
    /**
     * Returns the name/value pair for the specified style property.  Both the
     * name and value strings are enclosed in double-quotes.
     */
    private String getNameValuePair(String propertyKey) {
        String value = properties.getProperty(styleType + "." + propertyKey);
        return quoted(propertyKey) + COLON + quoted(value);
    }
    
    /**
     * Returns the specified text enclosed in double-quotes.
     */
    private String quoted(String text) {
        return DQUOTE + text + DQUOTE;
    }
}
