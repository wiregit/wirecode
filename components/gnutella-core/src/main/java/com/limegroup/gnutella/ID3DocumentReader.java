package com.limegroup.gnutella;

import com.limegroup.gnutella.gml.EmbeddedDocumentReader;
import com.limegroup.gnutella.gml.GMLChoice;
import com.limegroup.gnutella.gml.GMLChoiceField;
import com.limegroup.gnutella.gml.GMLDocument;
import com.limegroup.gnutella.gml.GMLField;
import com.limegroup.gnutella.gml.GMLIntegerField;
import com.limegroup.gnutella.gml.GMLStringField;
import com.limegroup.gnutella.gml.GMLTemplateRepository;
import com.limegroup.gnutella.gml.TemplateNotFoundException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * This class provides a utility method to read ID3 Tag information from MP3
 * files and creates GMLDocuments from them.  The class is a singleton.
 *
 * @author Ron Vogl
 */
final class ID3DocumentReader
    implements EmbeddedDocumentReader
{
    private static final String MP3_TEMPLATE_URI =
        "http://content.limewire.com/gmlt/mp3.gmlt";

    private static final ID3DocumentReader _instance =
        new ID3DocumentReader();

    /**
     * Private constructor.  The class is a singleton.
     */
    private ID3DocumentReader() {}

    public static final ID3DocumentReader getInstance()
    {
        return _instance;
    }

    /**
     * Attempts to read an ID3 tag from the specified file.
     * @return an null if the document has no ID3 tag
     */
    public final GMLDocument readDocument(
        File file, GMLTemplateRepository templateRepository)
        throws IOException, TemplateNotFoundException
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

        // Create a GML reply document from the MP3 template
        GMLDocument gmlReply =  templateRepository.
            getTemplate(MP3_TEMPLATE_URI).createReply();

        // Fill the appropriate values into the GML Document
        if(!title.equals(""))
            ((GMLStringField)gmlReply.getField("TIT1")).setValue(title);
        if(!artist.equals(""))
            ((GMLStringField)gmlReply.getField("TPE1")).setValue(artist);
        if(!album.equals(""))
            ((GMLStringField)gmlReply.getField("TALB")).setValue(album);
        if(!year.equals(""))
            ((GMLStringField)gmlReply.getField("TYER")).setValue(year);
        if(!comment.equals(""))
            ((GMLStringField)gmlReply.getField("COMM")).setValue(comment);
        if(track != 0)
            ((GMLIntegerField)gmlReply.getField("TRCK")).setValue(track);
        if(genre != 255)
        {
            GMLChoiceField choiceField =
                (GMLChoiceField)gmlReply.getField("TCON");
            GMLChoice selectedChoice = choiceField.getChoice(
                String.valueOf(genre));
            // Only set the genre if it's a valid choice.  Otherwise,
            // we'll fail silently on genre.
            if(selectedChoice != null)
                choiceField.setSelectedChoice(selectedChoice);
        }

        return gmlReply;
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
}
