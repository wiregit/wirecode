package com.limegroup.gnutella.mp3;

import java.io.*;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.xml.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.FileUtils;
import de.vdheide.mp3.*;

/**
 * Used when a user wants to edit meta-information about a media file, and asks
 * to save it. 
 * 
 * For this class to work efficiently, the removeID3Tags method
 * is called before. rewriteID3Tags method is called. 
 *
 * @author Sumeet Thadani
 */

public abstract class MetaDataEditor {

    protected String title_;
    protected String artist_;
    protected String album_;
    protected String year_;
    protected String track_;
    protected String comment_;
    protected String genre_;

    protected LimeXMLDocument correctDocument= null;


    private final boolean debugOn = false;
    protected void debug(String out) {
        if (debugOn)
            System.out.println(out);
    }
    
    public boolean equals(Object o) {
        if( o == this ) return true;
        if( !(o instanceof MetaDataEditor) ) return false;
        
        MetaDataEditor other = (MetaDataEditor)o;

        return matches(title_, other.title_) &&
               matches(artist_, other.artist_) &&
               matches(album_, other.album_) &&
               matches(year_, other.year_) &&
               matches(track_, other.track_) &&
               matches(comment_, other.comment_) &&
               matches(genre_, other.genre_);
    }
    
    protected boolean matches(final String a, final String b) {
        if( a == null )
            return b == null;
        return a.equals(b);
    }

    /** 
     * The caller of this method has the xml string that represents a
     * LimeXMLDocument, and wants to write the document out to disk. For this
     * method to work effectively, the caller must instantiate this class and
     * call this method first, and then call to actually write the ID3
     * tags out.
     * <p>
     * This method reads the complete xml string and removes the id3 *
     * components of the xml string, and stores the values of the id3 tags in a
     * class variable which will later be used to write the id3 tags in the
     * mp3file.
     * <p>
     * @return a parseable xml string which has the same attributes as the
     * xmlStr paramter minus the id3 tags.
     */
    public String removeID3Tags(String xmlStr) {
        //will be used to reconstruct xmlStr after ripping stuff from it
        int i, j;
        Object[] rippedStuff = null;

        //title        
        try {
            rippedStuff = ripTag(xmlStr, TITLE_STRING);

            title_ = (String)rippedStuff[2];
            debug("title = "+title_);

            i = ((Integer)rippedStuff[0]).intValue();
            j = ((Integer)rippedStuff[1]).intValue();        
            xmlStr = xmlStr.substring(0,i) + xmlStr.substring(j,xmlStr.length());
        } 
        catch (IOException e) {};
        //artist
        try {
            rippedStuff = ripTag(xmlStr, ARTIST_STRING);

            artist_ = (String)rippedStuff[2];
            debug("artist = "+artist_);

            i = ((Integer)rippedStuff[0]).intValue();
            j = ((Integer)rippedStuff[1]).intValue();        
            xmlStr = xmlStr.substring(0,i) + xmlStr.substring(j,xmlStr.length());
        } 
        catch (IOException e) {};
        //album
        try {
            rippedStuff = ripTag(xmlStr, ALBUM_STRING);

            album_ = (String)rippedStuff[2];

            i = ((Integer)rippedStuff[0]).intValue();
            j = ((Integer)rippedStuff[1]).intValue();        
            xmlStr = xmlStr.substring(0,i) + xmlStr.substring(j,xmlStr.length());
        } 
        catch (IOException e) {};
        //year
        try {
            rippedStuff = ripTag(xmlStr, YEAR_STRING);

            year_ = (String)rippedStuff[2];

            i = ((Integer)rippedStuff[0]).intValue();
            j = ((Integer)rippedStuff[1]).intValue();        
            xmlStr = xmlStr.substring(0,i) + xmlStr.substring(j,xmlStr.length());
        } 
        catch (IOException e) {};
        //track
        try {
            rippedStuff = ripTag(xmlStr, TRACK_STRING);

            track_ = (String)rippedStuff[2];

            i = ((Integer)rippedStuff[0]).intValue();
            j = ((Integer)rippedStuff[1]).intValue();        
            xmlStr = xmlStr.substring(0,i) + xmlStr.substring(j,xmlStr.length());
        } 
        catch (IOException e) {};
        //comment
        try {
            rippedStuff = ripTag(xmlStr, COMMENT_STRING);

            comment_ = (String)rippedStuff[2];

            i = ((Integer)rippedStuff[0]).intValue();
            j = ((Integer)rippedStuff[1]).intValue();        
            xmlStr = xmlStr.substring(0,i) + xmlStr.substring(j,xmlStr.length());
        } 
        catch (IOException e) {};
        //genre
        try {
            rippedStuff = ripTag(xmlStr, GENRE_STRING);

            genre_ = (String)rippedStuff[2];

            i = ((Integer)rippedStuff[0]).intValue();
            j = ((Integer)rippedStuff[1]).intValue();        
            xmlStr = xmlStr.substring(0,i) + xmlStr.substring(j,xmlStr.length());
        } 
        catch (IOException e) {};
        //bitrate
        try {
            rippedStuff = ripTag(xmlStr, BITRATE_STRING);

            // we get bitrate info from the mp3 file....

            i = ((Integer)rippedStuff[0]).intValue();
            j = ((Integer)rippedStuff[1]).intValue();        
            xmlStr = xmlStr.substring(0,i) + xmlStr.substring(j,xmlStr.length());
        } 
        catch (IOException e) {};
        //seconds
        try {
            rippedStuff = ripTag(xmlStr, SECONDS_STRING);

            // we get seconds info from the mp3 file....

            i = ((Integer)rippedStuff[0]).intValue();
            j = ((Integer)rippedStuff[1]).intValue();        
            xmlStr = xmlStr.substring(0,i) + xmlStr.substring(j,xmlStr.length());
        } 
        catch (IOException e) {};



        return xmlStr;//this has been suitable modified
    }
    
    /**
     * @return true if I have better data than other, false otherwise. Better is
     * defined as having better values for every field. If there is even one
     * field where other has better values than me, I am not better. We do this
     * so we have a chance to pick the better fields later
     */
    public boolean betterThan(MetaDataEditor other) {
        return ( firstBetter(title_, other.title_) &&
                 firstBetter(artist_, other.artist_) &&
                 firstBetter(album_, other.album_) &&
                 firstBetter(track_, other.track_) &&
                 firstBetter(comment_, other.comment_) &&
                 firstBetter(genre_, other.genre_) );                 
    }
    
    /**
     * @return true if first field is better than the second field. Better is
     * defined as being equal to the second, or having a value 
     */
    private boolean firstBetter(String first, String second) {
        if(first == null && second == null)
            return true;
        if((first != null) && first.equals(second))
            return true;
        if(first != null && !"".equals(first))
            return true;
        //first has no value, and second does
        return false;
    }

    /**
     * Sets the fields of this if the corresponding fields of other are better
     * than their values. In this case other's values get presidence. 
     */
    public void pickBetterFields(MetaDataEditor other) {
        if(firstBetter(other.title_, title_))
            title_ = other.title_;
        if(firstBetter(other.artist_, artist_))
           artist_ = other.artist_;
        if(firstBetter(other.album_, album_))
           album_ = other.album_;
        if(firstBetter(other.track_, track_))
           track_ = other.track_;
        if(firstBetter(other.comment_, comment_))
           comment_ = other.comment_;
        if(firstBetter(other.genre_, genre_))
           genre_ = other.genre_;
    }


    public abstract int commitMetaData(String filename);
    
    public void setCorrectDocument(LimeXMLDocument document) {
        this.correctDocument = document;
    }

    public LimeXMLDocument getCorrectDocument() {
        return correctDocument;
    }
}
