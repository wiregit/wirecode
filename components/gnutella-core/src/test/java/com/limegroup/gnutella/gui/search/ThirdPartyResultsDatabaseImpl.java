package com.limegroup.gnutella.gui.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A default implementation of {@link ThirdPartyResultsDatabase} for testing.
 */
final class ThirdPartyResultsDatabaseImpl extends SimpleSpecialResultsDatabaseImpl {
       
    private final static Map<String,List<Map<String,String>>> keywords2limeXMLs 
    = new HashMap<String,List<Map<String,String>>>();
    static {
        final int I = 10;
        final int J = 5;
        for (int i=0; i<I; i++) {
            final List<Map<String,String>> lst = new ArrayList<Map<String,String>>();
            for (int j=0; j<J; j++) {
                lst.add(createXML(i,j));
            }
            keywords2limeXMLs.put("test" + i,lst);
        }
    }
    
    private static Map<String,String> createXML(int i, int j) {
        final String artist = "artist" + i;
        final String album = "album" + i + "-" + j;
        final String url = "http://limewire.com/store";
        final String genre = "genre" + i + "-" + j;
        final String license = "license" + i + "-" + j;
        final Map<String,String> res = new HashMap<String,String>();
        res.put(Attr.ARTIST,artist);
        res.put(Attr.ALBUM,album);
        res.put(Attr.URL,url);
        res.put(Attr.GENRE,genre);
        res.put(Attr.LICENSE,license);
        res.put(Attr.SIZE, "1000");
        res.put(Attr.CREATION_TIME, String.valueOf(System.currentTimeMillis()));
        res.put(Attr.VENDOR, "*special*");
        return res;     
    }
    
    @Override
    protected List<Map<String, String>> getSearchResults(String query) {
        final List<Map<String, String>> res = new ArrayList<Map<String, String>>();
        for (String keyword : keywords2limeXMLs.keySet()) {
            if (query.equalsIgnoreCase(keyword)) {
                final List<Map<String,String>> xmls = keywords2limeXMLs.get(keyword);
                res.addAll(xmls);
            }
        }
        return res;
    }  

}
