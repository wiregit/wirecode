package org.limewire.core.impl.search.store;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.List;
import java.util.EnumMap;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.search.store.ReleaseResult;
import org.limewire.core.api.search.store.StoreResults;
import org.limewire.core.api.search.store.StoreStyle;
import org.limewire.core.api.search.store.StoreSearchListener;
import org.limewire.core.api.search.store.TrackResult;
import org.limewire.core.api.search.store.StoreStyle.Type;
import org.limewire.core.impl.MockURN;

/**
 * Implementation of StoreConnection for the mock core.
 */
public class MockStoreConnection  {
    
    public StoreResults doQuery(String query) {
        
        Type styleType;
        if (query.indexOf("monkey") > -1) {
            styleType = Type.STYLE_A;
        } else if (query.indexOf("bear") > -1) {
            styleType = Type.STYLE_B;
        } else if (query.indexOf("cat") > -1) {
            styleType = Type.STYLE_C;
        } else if (query.indexOf("dog") > -1) {
            styleType = Type.STYLE_D;
        } else {
            styleType = Type.STYLE_A;
        }

        ArrayList<ReleaseResult> resultList = new ArrayList<ReleaseResult>();
        
        MockReleaseResult result1 = new MockReleaseResult(this);
        resultList.add(result1);
        Map<FilePropertyKey, Object> properties = new HashMap<FilePropertyKey, Object>();
        result1.setPropertyMap(properties);
        
        properties.put(FilePropertyKey.AUTHOR, "GreenMonkeys");
        properties.put(FilePropertyKey.TITLE, "The Collection That Keeps on Playing and Playing and Playing and Playing and Playing");
        properties.put(FilePropertyKey.ALBUM, "The Collection That Keeps on Playing and Playing and Playing and Playing and Playing");
        result1.setAlbumIconUri("albumCover.png");
        result1.setAlbumId("666");
        properties.put(FilePropertyKey.BITRATE, 128l);
        result1.setCategory(Category.AUDIO);
        result1.setFileName("Green Monkeys The Collection.mp3");
        result1.setSize(9 * 1024 * 1024);
        properties.put(FilePropertyKey.GENRE, "Jazz");
        result1.setInfoUri(getClass().getResource("fileInfo.html").toString());
        properties.put(FilePropertyKey.LENGTH, 568l);
        result1.setPrice("4 Credits");
        properties.put(FilePropertyKey.QUALITY, 3l);
        result1.setTrackCount(3);
        result1.setUrn(new MockURN("www.store.limewire.com0"));
        result1.setType(ReleaseResult.Type.ALBUM);
        
        MockReleaseResult result2 = new MockReleaseResult(this);
        resultList.add(result2);
        properties = new HashMap<FilePropertyKey, Object>();
        result2.setPropertyMap(properties);
        
        properties.put(FilePropertyKey.AUTHOR, "GreenMonkeys");
        properties.put(FilePropertyKey.TITLE, "Chomp");
        properties.put(FilePropertyKey.ALBUM, "Premonitions, Echoes & Science");
        properties.put(FilePropertyKey.BITRATE, 256l);
        result2.setCategory(Category.AUDIO);
        result2.setFileName("Green Monkeys Chomp.mp3");
        result2.setSize(3 * 1024 * 1024);
        properties.put(FilePropertyKey.GENRE, "Jazz");
        result2.setInfoUri(getClass().getResource("fileInfo.html").toString());
        properties.put(FilePropertyKey.LENGTH, 208l);
        result2.setPrice("1 Credit");
        properties.put(FilePropertyKey.QUALITY, 3l);
        result2.setTrackCount(3);
        result2.setUrn(new MockURN("www.store.limewire.com1"));
        result2.setType(ReleaseResult.Type.TRACK);
        
        return new MockStoreResults(styleType.toString(), resultList);
    }
    
    public Icon loadIcon(String iconUri) {
        return new ImageIcon(getClass().getResource(iconUri));
    }
    
    public StoreStyle loadStyle(String styleId, StoreSearchListener storeSearchListener) throws IOException {        
        URL url = MockStoreStyle.class.getResource(styleId + ".properties");
        Properties properties = new Properties();
        properties.load(url.openStream());
        return new MockStoreStyle(properties, storeSearchListener, this);                
    }
    
    public List<TrackResult> loadTracks(String albumId) {
        ArrayList<TrackResult> tracks = new ArrayList<TrackResult>();
        
        MockTrackResult trackResult = new MockTrackResult();
        EnumMap<FilePropertyKey, Object> properties = new EnumMap<FilePropertyKey, Object>(FilePropertyKey.class);
        trackResult.setPropertyMap(properties);
        tracks.add(trackResult);
        
        // Create tracks.
        trackResult.setAlbumId(albumId);
        properties.put(FilePropertyKey.AUTHOR, "Green Monkeys");
        properties.put(FilePropertyKey.TITLE, "Heh?");
        properties.put(FilePropertyKey.BITRATE, 128l);
        trackResult.setFileName("Green Monkeys - Heh.mp3");        
        trackResult.setSize(3 * 1024 * 1024);
        properties.put(FilePropertyKey.LENGTH, 129l);
        trackResult.setPrice("1 Credit");
        properties.put(FilePropertyKey.QUALITY , 3l);
        properties.put(FilePropertyKey.TRACK_NUMBER, "1");
        trackResult.setUrn(new MockURN("www.store.limewire.com1"));
        
        trackResult = new MockTrackResult();
        properties = new EnumMap<FilePropertyKey, Object>(FilePropertyKey.class);
        trackResult.setPropertyMap(properties);
        tracks.add(trackResult);

        trackResult.setAlbumId(albumId);
        properties.put(FilePropertyKey.AUTHOR, "Green Monkeys");
        properties.put(FilePropertyKey.TITLE, "Take Me To Space (Man)");
        properties.put(FilePropertyKey.BITRATE, 128l);
        trackResult.setFileName("Green Monkeys - Take Me To Space (Man).mp3");        
        trackResult.setSize(3 * 1024 * 1024);
        properties.put(FilePropertyKey.LENGTH, 251l);
        trackResult.setPrice("1 Credit");
        properties.put(FilePropertyKey.QUALITY , 3l);
        properties.put(FilePropertyKey.TRACK_NUMBER, "2");
        trackResult.setUrn(new MockURN("www.store.limewire.com2"));
        
        trackResult = new MockTrackResult();
        properties = new EnumMap<FilePropertyKey, Object>(FilePropertyKey.class);
        trackResult.setPropertyMap(properties);
        tracks.add(trackResult);

        trackResult.setAlbumId(albumId);
        properties.put(FilePropertyKey.AUTHOR, "Green Monkeys");
        properties.put(FilePropertyKey.TITLE, "Crush");
        properties.put(FilePropertyKey.BITRATE, 128l);
        trackResult.setFileName("Green Monkeys - Crush.mp3");        
        trackResult.setSize(3 * 1024 * 1024);
        properties.put(FilePropertyKey.LENGTH, 188l);
        trackResult.setPrice("1 Credit");
        properties.put(FilePropertyKey.QUALITY , 3l);
        properties.put(FilePropertyKey.TRACK_NUMBER, "3");
        trackResult.setUrn(new MockURN("www.store.limewire.com2"));
        
        return tracks;
    }
}
