package com.limegroup.gnutella;

import com.sun.java.util.collections.Iterator;
import com.limegroup.gnutella.gml.GMLDocument;
import com.limegroup.gnutella.gml.GMLField;
import com.limegroup.gnutella.gml.GMLParseException;
import com.limegroup.gnutella.gml.TemplateNotFoundException;
import com.limegroup.gnutella.gml.repository.SimpleTemplateRepository;
import com.limegroup.gnutella.util.StringUtils;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * This class stores meta-information found in an ID3 Tag.  Instances
 * of this class are aggregated by FileDesc.
 *
 * @author Ron Vogl
 */
final class ID3Tag
{
    private static final String MP3_TEMPLATE_URI =
        "http://content.limewire.com/gmlt/mp3.gmlt";

    private String _title;
    private String _artist;
    private String _album;
    private String _year;
    private String _comment;
    private short _track; // range [0, 255]
    private short _genre; // range [0, 255]

    // These variables are redundant; the contain the same information as
    // the other member variable.  I think it's worth caching them to avoid
    // regenerating them each time we search.
    private String _gmlString;
    private String _textSearchString;

    /**
     * Private constructor.  Use ID3Tag.read(...) to instantiate.
     */
    private ID3Tag(String title, String artist, String album,
                   String year, String comment, short track, short genre)
    {
        _title = title;
        _artist = artist;
        _album = album;
        _year = year;
        _comment = comment;
        _track = track;
        _genre = genre;

        try
        {
            // Create a GML reply document from the MP3 template
            GMLDocument gmlReply =  SimpleTemplateRepository.instance().
                getTemplate(MP3_TEMPLATE_URI).createReply();

            // Fill in the appropriate values,
            if(!title.equals(""))
                gmlReply.getField("TIT1").setValue(title);
            if(!artist.equals(""))
                gmlReply.getField("TPE1").setValue(artist);
            if(!album.equals(""))
                gmlReply.getField("TALB").setValue(album);
            if(!year.equals(""))
                gmlReply.getField("TYER").setValue(year);
            if(!comment.equals(""))
                gmlReply.getField("COMM").setValue(comment);
            if(track != 0)
                gmlReply.getField("TRCK").setValue(String.valueOf(track));
            if(genre != 255)
                gmlReply.getField("TCON").setValue(String.valueOf(genre));

            _gmlString = gmlReply.getXMLString();
            _textSearchString = gmlReply.getTextSearchString();
        }
        catch(TemplateNotFoundException e)
        {
            _gmlString = "";
            _textSearchString = "";
        }
    }

    /**
     * Attempts to read an ID3 tag from the specified file.
     * @return null if the file does not have an ID3 tag.
     */
    public static final ID3Tag read(File file)
        throws IOException
    {
        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");

        long length = randomAccessFile.length();
        // We need to read at least 128 bytes
        if(length < 128)
            return null;

        randomAccessFile.seek(length - 128);
        byte[] buffer = new byte[30];

        // Read ID3 Tag, return null if not present
        randomAccessFile.readFully(buffer, 0, 3);
        String tag = new String(buffer, 0, 3, "Cp437");

        if(!tag.equals("TAG"))
            return null;

        // We have an ID3 Tag, now get the parts
        // Title
        randomAccessFile.readFully(buffer, 0, 30);
        String title = new String(buffer, 0, getTrimmedLength(buffer, 30));

        // Artist
        randomAccessFile.readFully(buffer, 0, 30);
        String artist = new String(buffer, 0, getTrimmedLength(buffer, 30));

        // Album
        randomAccessFile.readFully(buffer, 0, 30);
        String album = new String(buffer, 0, getTrimmedLength(buffer, 30));

        // Year
        randomAccessFile.readFully(buffer, 0, 4);
        String year = new String(buffer, 0, getTrimmedLength(buffer, 4));

        // Comment and track
        short track;
        randomAccessFile.readFully(buffer, 0, 30);
        int commentLength;
        if(buffer[28] == 0)
        {
            track = (short)ByteOrder.ubyte2int(buffer[29]);
            commentLength = 28;
        }
        else
        {
            track = 0;
            commentLength = 30;
        }
        String comment = new String(buffer, 0,
            getTrimmedLength(buffer, commentLength));

        // Genre
        randomAccessFile.readFully(buffer, 0, 1);
        short genre = (short)ByteOrder.ubyte2int(buffer[0]);

        return new ID3Tag(title, artist, album, year, comment, track,
                          genre);
    }

    /**
     * Walks back through the byte array to trim off null characters and
     * spaces.  A helper for read(...) above.
     * @return the number of bytes with nulls and spaces trimmed.
     */
    private static int getTrimmedLength(byte[] bytes, int includedLength)
    {
        int i;
        for(i = includedLength - 1;
            (i >= 0) && ((bytes[i] == 0) || (bytes[i] == 32));
            i--);
        return i + 1;
    }

    /**
     * @return true iff the given GMLDocument can be matched to an ID3 tag.
     */
    public static final boolean isGMLDocumentUnderstandable(
        GMLDocument document)
    {
        return document.getTemplateURI().equals(MP3_TEMPLATE_URI);
    }

    /**
     * @return true iff the given text can be matched to an ID3 tag.
     */
    public final boolean isTextQueryMatch(String textQuery) {
        return StringUtils.contains(_textSearchString, textQuery, true);
    }

    /**
     * Behavior is undefined if document is not matchable to an ID3 Tag.
     * @see isMatchableGMLDocument(GMLDocument)
     * @return true iff the given document matches this ID3 tag.
     */
    public final boolean isGMLDocumentMatch(GMLDocument document)
    {
        Assert.that(isGMLDocumentUnderstandable(document));
        String title = document.getField("TIT1").getValue();
        if((title != null) && !StringUtils.contains(_title, title))
            return false;
        String artist = document.getField("TPE1").getValue();
        if((artist != null) && !StringUtils.contains(_artist, artist))
            return false;
        String album = document.getField("TALB").getValue();
        if((album != null) && !StringUtils.contains(_album, album))
            return false;
        String year = document.getField("TYER").getValue();
        if((year != null) && !StringUtils.contains(_year, year))
            return false;
        String trackString = document.getField("TRCK").getValue();
        if(trackString != null)
        {
            int track;
            try
            {
                track = Integer.parseInt(trackString);
            }
            catch(NumberFormatException e)
            {
                return false;
            }
            if(_track != track)
                return false;
        }
        String genreString = document.getField("TCON").getValue();
        if(genreString != null)
        {
            int genre;
            try
            {
                genre = Integer.parseInt(genreString);
            }
            catch(NumberFormatException e)
            {
                return false;
            }
            if(_genre != genre)
                return false;
        }
        return true;
    }

    /**
     * @return A GML String representation of this ID3 Tag, suitable for
     *         use as metadata in a QueryReply
     */
    public String getGMLString()
    {
        return _gmlString;
    }
}
