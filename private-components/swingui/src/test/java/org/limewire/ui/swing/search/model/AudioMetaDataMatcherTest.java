//package org.limewire.ui.swing.search.model;
//
//import java.util.HashMap;
//import java.util.Map;
//
//import org.limewire.core.api.search.SearchResult;
//import org.limewire.core.api.search.SearchResult.PropertyKey;
//
//import junit.framework.Assert;
//import junit.framework.TestCase;
//
//public class AudioMetaDataMatcherTest extends TestCase {
//
//    public void testSameName() {
//        AudioMetaDataMatcher audioMetaDataMatcher = new AudioMetaDataMatcher();
//        Map<PropertyKey, Object> properties1 = new HashMap<PropertyKey, Object>();
//        properties1.put(PropertyKey.NAME, "test");
//        SearchResult searchResult1 = new TestSearchResult("test.mp3", properties1);
//        Map<PropertyKey, Object> properties2 = new HashMap<PropertyKey, Object>();
//        properties2.put(PropertyKey.NAME, "test");
//        SearchResult searchResult2 = new TestSearchResult("test.mp3", properties2);
//        Assert.assertTrue(audioMetaDataMatcher.matches(searchResult1, searchResult2));
//    }
//    
//    public void testSameNameHyphenName() {
//        AudioMetaDataMatcher audioMetaDataMatcher = new AudioMetaDataMatcher();
//        Map<PropertyKey, Object> properties1 = new HashMap<PropertyKey, Object>();
//        properties1.put(PropertyKey.NAME, "test-blah");
//        SearchResult searchResult1 = new TestSearchResult("test-blah.mp3", properties1);
//        Map<PropertyKey, Object> properties2 = new HashMap<PropertyKey, Object>();
//        properties2.put(PropertyKey.NAME, "test-blah");
//        SearchResult searchResult2 = new TestSearchResult("test-blah.mp3", properties2);
//        Assert.assertTrue(audioMetaDataMatcher.matches(searchResult1, searchResult2));
//    }
//    
//    public void testSameNameHyphenNameHyphenName() {
//        AudioMetaDataMatcher audioMetaDataMatcher = new AudioMetaDataMatcher();
//        Map<PropertyKey, Object> properties1 = new HashMap<PropertyKey, Object>();
//        properties1.put(PropertyKey.NAME, "test-foo-bar");
//        SearchResult searchResult1 = new TestSearchResult("test-foo-bar.mp3", properties1);
//        Map<PropertyKey, Object> properties2 = new HashMap<PropertyKey, Object>();
//        properties2.put(PropertyKey.NAME, "test-foo-bar");
//        SearchResult searchResult2 = new TestSearchResult("test-foo-bar.mp3", properties2);
//        Assert.assertTrue(audioMetaDataMatcher.matches(searchResult1, searchResult2));
//    }
//    
//    public void testSameNameOrTrackMetaData() {
//        AudioMetaDataMatcher audioMetaDataMatcher = new AudioMetaDataMatcher();
//        Map<PropertyKey, Object> properties1 = new HashMap<PropertyKey, Object>();
//        properties1.put(PropertyKey.NAME, "test");
//        SearchResult searchResult1 = new TestSearchResult("test.mp3", properties1);
//        Map<PropertyKey, Object> properties2 = new HashMap<PropertyKey, Object>();
//        properties2.put(PropertyKey.NAME, "blah123");
//        properties2.put(PropertyKey.TRACK_NAME, "test");
//        SearchResult searchResult2 = new TestSearchResult("blah123.mp3", properties2);
//        Assert.assertTrue(audioMetaDataMatcher.matches(searchResult1, searchResult2));
//    }
//    
//    public void testSameNameOrAlbumAndTrackMetaData() {
//        AudioMetaDataMatcher audioMetaDataMatcher = new AudioMetaDataMatcher();
//        Map<PropertyKey, Object> properties1 = new HashMap<PropertyKey, Object>();
//        properties1.put(PropertyKey.NAME, "test-blah");
//        SearchResult searchResult1 = new TestSearchResult("test-blah.mp3", properties1);
//        Map<PropertyKey, Object> properties2 = new HashMap<PropertyKey, Object>();
//        properties2.put(PropertyKey.NAME, "blah123");
//        properties2.put(PropertyKey.ALBUM_TITLE, "test");
//        properties2.put(PropertyKey.TRACK_NAME, "blah");
//        SearchResult searchResult2 = new TestSearchResult("blah123.mp3", properties2);
//        Assert.assertTrue(audioMetaDataMatcher.matches(searchResult1, searchResult2));
//    }
//    
//    public void testSameNameOrArtistAndTrackMetaData() {
//        AudioMetaDataMatcher audioMetaDataMatcher = new AudioMetaDataMatcher();
//        Map<PropertyKey, Object> properties1 = new HashMap<PropertyKey, Object>();
//        properties1.put(PropertyKey.NAME, "test-blah");
//        SearchResult searchResult1 = new TestSearchResult("test-blah.mp3", properties1);
//        Map<PropertyKey, Object> properties2 = new HashMap<PropertyKey, Object>();
//        properties2.put(PropertyKey.NAME, "blah123");
//        properties2.put(PropertyKey.ARTIST_NAME, "test");
//        properties2.put(PropertyKey.TRACK_NAME, "blah");
//        SearchResult searchResult2 = new TestSearchResult("blah123.mp3", properties2);
//        Assert.assertTrue(audioMetaDataMatcher.matches(searchResult1, searchResult2));
//    }
//    
//    public void testMatchTrackMetaData() {
//        AudioMetaDataMatcher audioMetaDataMatcher = new AudioMetaDataMatcher();
//        Map<PropertyKey, Object> properties1 = new HashMap<PropertyKey, Object>();
//        properties1.put(PropertyKey.NAME, "asdasd");
//        properties1.put(PropertyKey.TRACK_NAME, "blah");
//        SearchResult searchResult1 = new TestSearchResult("asdasd.mp3", properties1);
//        Map<PropertyKey, Object> properties2 = new HashMap<PropertyKey, Object>();
//        properties2.put(PropertyKey.NAME, "blah123");
//        properties2.put(PropertyKey.ARTIST_NAME, "test");
//        properties2.put(PropertyKey.TRACK_NAME, "blah");
//        SearchResult searchResult2 = new TestSearchResult("blah123.mp3", properties2);
//        Assert.assertTrue(audioMetaDataMatcher.matches(searchResult1, searchResult2));
//    }
//}
