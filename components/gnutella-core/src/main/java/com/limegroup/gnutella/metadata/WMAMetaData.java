package com.limegroup.gnutella.metadata;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.DataInputStream;
import java.util.Arrays;

import com.limegroup.gnutella.util.CountingInputStream;
import com.limegroup.gnutella.util.IOUtils;
import com.limegroup.gnutella.ByteOrder;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * Encapsulation of WMA metadata.  WMV metadata is probably very much like this.
 * This is based off the work of Reed Esau, in his excellent ptarmigan package,
 * from http://ptarmigan.sourceforge.net/ .
 * The parsing has been tweaked/fixed and has been retrofitted to fit our MetaData package.
 */
public class WMAMetaData extends AudioMetaData {
    
    private static final Log LOG = LogFactory.getLog(WMAMetaData.class);

    private static final byte WMA_HEADER_ID[] =
        { 48,38,(byte)178,117,(byte)142,102,(byte)207,17,(byte)166,(byte)217,0,(byte)170,0,98,(byte)206,108};
        
    private static final byte WMA_FILE_PROPERTIES_ID[] =
        { (byte)161,(byte)220,(byte)171,(byte)140,71,(byte)169,(byte)207,17,(byte)142,(byte)228,0,(byte)192,12,32,83,101};
        
    private static final byte WMA_STREAM_PROPERTIES_ID[] =
        { (byte)145,7,(byte)220,(byte)183,(byte)183,(byte)169,(byte)207,17,(byte)142,(byte)230,0,(byte)192,12,32,83,101};
        
    private static final byte WMA_CONTENT_DESCRIPTION_ID[] =
        { 51,38,(byte)178,117,(byte)142,102,(byte)207,17,(byte)166,(byte)217,0,(byte)170,0,98,(byte)206,108};
        
    private static final byte WMA_EXTENDED_CONTENT_DESCRIPTION_ID[] =
        { 64,(byte)164,(byte)208,(byte)210,7,(byte)227,(byte)210,17,(byte)151,(byte)240,0,(byte)160,(byte)201,94,(byte)168,80};
    
    // field names we know about in the extended content description.
    // we don't use all of these.
    
    /** the title of the file */
    private static final String WM_TITLE = "WM/Title";
    
    /** the author of the fiel */
    private static final String WM_AUTHOR = "WM/Author";
    
    /** the title of the album the file is on */
    private static final String WM_ALBUMTITLE = "WM/AlbumTitle";
    
    /** the zero-based track of the song */
    private static final String WM_TRACK = "WM/Track";
    
    /** the one-based track of the song */
    private static final String WM_TRACK_NUMBER = "WM/TrackNumber";
    
    /** the year the song was made */
    private static final String WM_YEAR = "WM/Year";
    
    /** the genre of the song */
    private static final String WM_GENRE = "WM/Genre";
    
    /** the description of the song */
    private static final String WM_DESCRIPTION = "WM/Description";
    
    /** the lyrics of the song */
    private static final String WM_LYRICS = "WM/Lyrics";
    
    /** whether or not this is encoded in VBR */
    private static final String VBR = "isVBR";
    
    /** the unique file identifier of this song */
    private static final String WM_UNIQUE_FILE_IDENTIFIER = "WM/UniqueFileIdentifier";
    
    /** the artist of the album as a whole */
    private static final String WM_ALBUMARTIST = "WM/AlbumArtist";
    
    /** the encapsulated ID3 info */
    private static final String ID3 = "ID3";
    
    /** the provider of the song */
    private static final String WM_PROVIDER = "WM/Provider";
    
    /** the rating the provider gave this song */
    private static final String WM_PROVIDER_RATING = "WM/ProviderRating";
    
    /** the publisher */
    private static final String WM_PUBLISHER = "WM/Publisher";
    
    /** the composer */
    private static final String WM_COMPOSER = "WM/Composer";
    
    /** the time the song was encoded */
    private static final String WM_ENCODING_TIME = "WM/EncodingTime";
    
    // data types we know about in the extended content description.
    // THESE ARE WRONG (but close enough for now)
    private static final int WMA_TYPE_STRING = 0;
    private static final int WMA_TYPE_BINARY = 1;
    private static final int WMA_TYPE_BOOLEAN = 2;
    private static final int WMA_TYPE_INT = 3;
    private static final int WMA_TYPE_LONG = 4;
    
    /**
     * Constructs a new WMAMetaData based off the given file, parsing all the known properties.
     */
    public WMAMetaData(File f) throws IOException {
        super(f);
    }

    /**
     * Parses the given file for metadata we understand.
     */
    protected void parseFile(File f) throws IOException {
        if(LOG.isDebugEnabled())
            LOG.debug("Parsing file: " + f);
        
        InputStream is = null;
        try {
            is = new BufferedInputStream(new FileInputStream(f));
            parse(is);
        } catch(IOException iox) {
            LOG.warn("IOX while parsing", iox);
            throw iox;
        } finally {
            IOUtils.close(is);
        }
    }
    
    /**
     * Parses a WMA input stream's metadata.
     * This first checks that the marker (16 bytes) is correct, reads the data offset & object count,
     * and then iterates through the objects, reading them.
     * Each object is stored in the format:
     *   ObjectID (16 bytes)
     *   Object Size (4 bytes)
     *   Object (Object Size bytes)
     */
    private void parse(InputStream is) throws IOException {
        CountingInputStream counter = new CountingInputStream(is);
        DataInputStream ds = new DataInputStream(counter);
        
        byte[] marker = new byte[WMA_HEADER_ID.length];
        ds.readFully(marker);
        if(!Arrays.equals(marker, WMA_HEADER_ID))
            throw new IOException("not a WMA file");
       
        int dataOffset = ByteOrder.leb2int(ds);
        IOUtils.ensureSkip(ds, 4);
        int objectCount = ByteOrder.leb2int(ds);
        IOUtils.ensureSkip(ds, 2);
        
        if(LOG.isDebugEnabled())
            LOG.debug("Data Offset: " + dataOffset + ", objectCount: " + objectCount);
        
        if(objectCount > 100)
            throw new IOException("object count very high: " + objectCount);
            
        byte[] object = new byte[16];
        for(int i = 0; i < objectCount; i++) {
            if(LOG.isDebugEnabled())
                LOG.debug("Parsing object[" + i + "]");
                
            ds.readFully(object);
            int size = ByteOrder.leb2int(ds);
            counter.clearAmountRead();
            readObject(ds, object, size - 20);
            int read = counter.getAmountRead() + 20; // +20 for id & size.
            
            if(read > size)
                throw new IOException("read (" + read + ") more than size (" + size + ")");
            else {
                if(LOG.isDebugEnabled())
                    LOG.debug("Skipping to next object.  Read: " + read + ", size: " + size);
                IOUtils.ensureSkip(ds, size - read);
            }
        }
    }
    
    /**
     * Reads a single object from a WMA metadata stream.
     * The objectID has already been read.  Each object is stored differently.
     */
    private void readObject(DataInputStream ds, byte[] id, int size) throws IOException {
        
        if(Arrays.equals(id, WMA_FILE_PROPERTIES_ID)) {
            LOG.debug("Parsing file properties");
            
            IOUtils.ensureSkip(ds, 80); // ???
            int maxBR = ByteOrder.leb2int(ds);
            // TODO: assign duration based off this??
            if(LOG.isDebugEnabled())
                LOG.debug("maxBitrate: " + maxBR);
        }
        
        else if(Arrays.equals(id, WMA_STREAM_PROPERTIES_ID)) {
            LOG.debug("Parsing stream properties");

            IOUtils.ensureSkip(ds, 60);
            short channels = ByteOrder.leb2short(ds);
            int sampleRate = ByteOrder.leb2int(ds);
            int byteRate = ByteOrder.leb2int(ds);
            setBitrate(byteRate * 8 / 1024);
            if(LOG.isDebugEnabled())
                LOG.debug("channels: " + channels + ", sampleRate: " + sampleRate + ", byteRate: " + byteRate + ", bitRate: " + getBitrate());
        }
        
        else if(Arrays.equals(id, WMA_CONTENT_DESCRIPTION_ID)) {
            LOG.debug("Parsing Content Description");
            
            IOUtils.ensureSkip(ds, 4);
            readStandardTag(ds);
        }
        
        else if(Arrays.equals(id, WMA_EXTENDED_CONTENT_DESCRIPTION_ID)) {
            LOG.debug("Parsing Extended Content Description");
            
            IOUtils.ensureSkip(ds, 4);
            readExtendedTag(ds);
        }
        
        else {
            LOG.debug("Unknown Object, ignoring.");
            // for debugging.
            //byte[] temp = new byte[size];
            //ds.readFully(temp);
            //LOG.debug("id: " + wmaString(id) + ", data: " + wmaString(temp));
        }
        
    }
    
    /**
     * Reads the standard WMA Content Description Tag.
     * Standard tag has 5 bits of info.  They are stored as:
     *      Size1 (TITLE)
     *      Size2 (AUTHOR)
     *      Size3 (UNKNOWN)
     *      Size4 (TRACK)
     *      Size5 (UNKNOWN)
     *      Data1
     *      Data2
     *      Data3
     *      Data4
     *      Data5
     * Where each size is a short (2 bytes), and the data is variable length
     * according to the related size.
     */
    private void readStandardTag(DataInputStream ds) throws IOException {
        short[] sizes = new short[5];
        
        for(int i = 0; i < sizes.length; i++)
            sizes[i] = ByteOrder.leb2short(ds);
        
        byte[] titleB  = new byte[sizes[0]];
        byte[] authorB = new byte[sizes[1]];
        byte[] unknown1 = new byte[sizes[2]];
        byte[] trackB  = new byte[sizes[3]];
        byte[] unknown2 = new byte[sizes[4]];
        
        ds.readFully(titleB);
        ds.readFully(authorB);
        ds.readFully(unknown1);
        ds.readFully(trackB);
        ds.readFully(unknown2);
        
        setTitle(wmaString(titleB));
        setArtist(wmaString(authorB));
        
        try {
            setTrack(Short.parseShort(wmaString(trackB)));
        } catch(NumberFormatException ignored) {}
            
        if(LOG.isDebugEnabled())
            LOG.debug("Standard Tag Values.  Title[" + getTitle() + ", Author: " + getArtist() + ", Track: " + getTrack()
                         + ", Unknown[2]: " + wmaString(unknown1) + ", Unknown[4]: " + wmaString(unknown2));
    }
    
    /**
     * Reads the extended WMA Content Description tag.
     * The extended tag has an arbitrary number of fields.  
     * The number of fields is stored first, as:
     *      Field Count (2 bytes)
     *
     * Each field is stored as:
     *      Field Size (2 bytes)
     *      Field      (Field Size bytes)
     *      Data Type  (2 bytes)
     *      Data Size  (2 bytes)
     *      Data       (Data Size bytes)
     */
    private void readExtendedTag(DataInputStream ds) throws IOException {
        int fieldCount = ByteOrder.leb2short(ds);
        
        if(LOG.isDebugEnabled())
            LOG.debug("Extended fieldCount: " + fieldCount);
        
        for(int i = 0; i < fieldCount; i++) {
            int fieldSize = ByteOrder.leb2short(ds);
            byte[] field = new byte[fieldSize];
            ds.readFully(field);
            String fieldName = wmaString(field);
            int dataType = ByteOrder.leb2short(ds);
            int dataSize = ByteOrder.leb2short(ds);
            
            switch(dataType) {
            case WMA_TYPE_STRING:
                parseExtendedString(fieldName, dataSize, ds);
                break;
            case WMA_TYPE_BINARY:
                parseExtendedBinary(fieldName, dataSize, ds);
                break;
            case WMA_TYPE_BOOLEAN:
                parseExtendedBoolean(fieldName, dataSize, ds);
                break;
            case WMA_TYPE_INT:
                parseExtendedInt(fieldName, dataSize, ds);
                break;
            case WMA_TYPE_LONG:
                parseExtendedInt(fieldName, dataSize, ds);
                break;
            default: 
                if(LOG.isDebugEnabled())
                    LOG.debug("Unknown dataType: " + dataType + " for field: " + fieldName);
                IOUtils.ensureSkip(ds, dataSize);
            }
        }
    }
    
    /**
     * Parses a value from an extended tag, assuming the value is of the 'string' dataType.
     */
    private void parseExtendedString(String field, int size, DataInputStream ds) throws IOException {
        byte[] data = new byte[Math.min(250, size)];
        ds.readFully(data);
        int leftover = Math.max(0, size - 250);
        IOUtils.ensureSkip(ds, leftover);
        String info = wmaString(data);
        
        if(LOG.isDebugEnabled())
            LOG.debug("Parsing extended String.  field: " + field + ", Value: " + info);
        
        if(WM_TITLE.equals(field)) {
            setTitle(info);
        } else if(WM_AUTHOR.equals(field)) {
            setArtist(info);
        } else if(WM_ALBUMTITLE.equals(field)) {
            setAlbum(info);
        } else if(WM_TRACK_NUMBER.equals(field)) {
            try {
                setTrack(Short.parseShort(info));
            } catch(NumberFormatException ignored) {}
        } else if(WM_YEAR.equals(field)) {
            setYear(info);
        } else if(WM_GENRE.equals(field)) {
            setGenre(info);
        } else if(WM_DESCRIPTION.equals(field)) {
            setComment(info);
        }
    }
    
    /**
     * Parses a value from an extended tag, assuming the value is of the 'boolean' dataType.
     */
    private void parseExtendedBoolean(String field, int size, DataInputStream ds) throws IOException {
        if(LOG.isDebugEnabled())
            LOG.debug("Ignoring boolean field: " + field + ", size: " + size);
            
        IOUtils.ensureSkip(ds, size);
    }
    
    /**
     * Parses a value from an extended tag, assuming the value is of the 'int' dataType.
     */
    private void parseExtendedInt(String field, int size, DataInputStream ds) throws IOException {
        if(size != 4) {
            if(LOG.isDebugEnabled())
                LOG.debug("Int field size != 4, ignoring.   Field: " + field + ", size: " + size);
            IOUtils.ensureSkip(ds, size);
            return;
        }
        
        int value = ByteOrder.leb2int(ds);
        if(LOG.isDebugEnabled())
            LOG.debug("Parsing extended int, field: " + field + ", size: " + size + ", value: " + value);
            
        if(WM_TRACK_NUMBER.equals(field)) {
            setTrack((short)value);
        }
    }
    
    /**
     * Parses a value from an extended tag, assuming the value is of the 'binary' dataType.
     */
    private void parseExtendedBinary(String field, int size, DataInputStream ds) throws IOException {
        if(LOG.isDebugEnabled())
            LOG.debug("Ignoring binary field: " + field + ", size: " + size);        
            
        IOUtils.ensureSkip(ds, size);
    }
    
    /**
     * Parses a value from an extended tag, assuming the value is of the 'long' dataType.
     */
    private void parseExtendedLong(String field, int size, DataInputStream ds) throws IOException {
        if(size != 8) {
            if(LOG.isDebugEnabled())
                LOG.debug("Long field size != 8, ignoring.   Field: " + field + ", size: " + size);
            IOUtils.ensureSkip(ds, size);
            return;
        }
        
        long value = ByteOrder.leb2long(ds);
        if(LOG.isDebugEnabled())
            LOG.debug("Ignoring long field: " + field + ", size: " + size + ", value: " + value);
    }
    
    /**
     * Returns a String uses WMA's encoding.
     * WMA strings have 0s interspersed between the characters.
     */
    private String wmaString(byte[] x) throws IOException {
        int pos = 0;
        for(int i = 0; i < x.length; i++) {
            if(x[i] != 0)
                x[pos++] = x[i];
        }
        return new String(x, 0, pos, "UTF-8");
    }
}
