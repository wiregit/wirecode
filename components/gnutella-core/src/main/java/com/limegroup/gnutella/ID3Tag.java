package com.limegroup.gnutella;

import com.sun.java.util.collections.Iterator;
import com.limegroup.gnutella.gml.GMLDocument;
import com.limegroup.gnutella.gml.GMLField;
import com.limegroup.gnutella.gml.TemplateNotFoundException;
import com.limegroup.gnutella.gml.repository.SimpleTemplateRepository;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.StringWriter;

/**
 * This class stores meta-information found in an ID3 Tag.  Instances
 * of this class are aggregated by FileDesc.
 *
 * @author Ron Vogl
 */
public final class ID3Tag
{
    private String _title;
    private String _artist;
    private String _album;
    private String _year;
    private String _comment;
    private short _track; // range [0, 255]
    private short _genre; // range [0, 255]
    private String _gmlString;

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
        _gmlString = createGMLString(title, artist, album, year, comment,
                                     track, genre);
    }

    /**
     * Attempts to read an ID3 tag from the specified file.
     * @return null if the file does not have an ID3 tag.
     */
    public static ID3Tag read(File file)
    {
        try
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
        catch(IOException e)
        {
            Assert.that(false, "IOException looking for ID3 tag");
            return null;
        }
    }

    /**
     * Walks back through the byte array to trim off null characters and
     * spaces.
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

    public String getTitle()
    {
        return _title;
    }

    public String getArtist()
    {
        return _artist;
    }

    public String getAlbum()
    {
        return _album;
    }

    public String getYear()
    {
        return _year;
    }

    public String getComment()
    {
        return _comment;
    }

    public short getTrack()
    {
        return _track;
    }

    public short getGenre()
    {
        return _genre;
    }

    public String getGMLString()
    {
        return _gmlString;
    }

    private static String createGMLString(String title, String artist,
        String album, String year, String comment, short track, short genre)
    {
        // Create a GML reply document from the MP3 template, fill in
        // appropriate values, write it out to a StringWriter, and return
        // the String
        GMLDocument reply;
        try
        {
            reply = SimpleTemplateRepository.getInstance().getTemplate(
                "http://content.limewire.com/gmlt/mp3.gmlt").createReply();
        }
        catch(TemplateNotFoundException e)
        {
            return "";
        }

        for(Iterator iterFields = reply.getFields().iterator();
            iterFields.hasNext();  )
        {
            GMLField field = (GMLField)iterFields.next();
            if(field.getName().equals("TIT1") && !title.equals(""))
                field.setValue(title);
            else if(field.getName().equals("TPE1") && !artist.equals(""))
                field.setValue(artist);
            else if(field.getName().equals("TALB") && !album.equals(""))
                field.setValue(album);
            else if(field.getName().equals("TYER") && !year.equals(""))
                field.setValue(year);
            else if(field.getName().equals("COMM") && !comment.equals(""))
                field.setValue(comment);
            else if(field.getName().equals("TRCK") && track != 0)
                field.setValue(String.valueOf(track));
            else if(field.getName().equals("TCON") && genre != 255)
                field.setValue(String.valueOf(genre));
        }


        StringWriter writer = new StringWriter();
        reply.write(new PrintWriter(writer));
        return writer.toString();
    }
}
