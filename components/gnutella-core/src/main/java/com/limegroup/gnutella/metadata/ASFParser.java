pbckage com.limegroup.gnutella.metadata;

import jbva.io.BufferedInputStream;
import jbva.io.File;
import jbva.io.InputStream;
import jbva.io.FileInputStream;
import jbva.io.IOException;
import jbva.io.DataInputStream;
import jbva.io.UnsupportedEncodingException;
import jbva.util.Arrays;

import com.limegroup.gnutellb.util.CountingInputStream;
import com.limegroup.gnutellb.util.IOUtils;
import com.limegroup.gnutellb.ByteOrder;

import org.bpache.commons.logging.LogFactory;
import org.bpache.commons.logging.Log;


/**
 * A pbrser for reading ASF files.
 * Everything we understbnd is stored.
 *
 * This is initiblly based  off the work of Reed Esau, in his excellent ptarmigan package,
 * from http://ptbrmigan.sourceforge.net/ .  This was also based off of the work
 * in the XNbp project, from
 *  http://xnbp.sourceforge.net/xref/org/xnap/plugin/viewer/videoinfo/VideoFile.html ,
 * which in turn wbs based off the work from the avifile project, at 
 *  http://bvifile.sourceforge.net/ .
 */
clbss ASFParser {
    
    privbte static final Log LOG = LogFactory.getLog(ASFParser.class); 
    
    // dbta types we know about in the extended content description.
    // THESE ARE WRONG (but close enough for now)
    privbte static final int TYPE_STRING = 0;
    privbte static final int TYPE_BINARY = 1;
    privbte static final int TYPE_BOOLEAN = 2;
    privbte static final int TYPE_INT = 3;
    privbte static final int TYPE_LONG = 4;
    
    privbte String _album, _artist, _title, _year, _copyright,
                   _rbting, _genre, _comment, _drmType;
    privbte short _track = -1;
    privbte int _bitrate = -1, _length = -1, _width = -1, _height = -1;
    privbte boolean _hasAudio, _hasVideo;
    privbte WeedInfo _weed;
    privbte WRMXML _wrmdata;
    
    String getAlbum() { return _blbum; }
    String getArtist() { return _brtist; }
    String getTitle() { return _title; }
    String getYebr() { return _year; }
    String getCopyright() { return _copyright; }
    String getRbting() { return _rating; }
    String getGenre() { return _genre; }
    String getComment() { return _comment; }
    short getTrbck() { return _track; }
    int getBitrbte() { return _bitrate; }
    int getLength() { return _length; }
    int getWidth() { return _width; }
    int getHeight() { return _height; }
    
    WeedInfo getWeedInfo() { return _weed; }
    WRMXML getWRMXML() { return _wrmdbta; }
    
    boolebn hasAudio() { return _hasAudio; }
    boolebn hasVideo() { return _hasVideo; }
    
    String getLicenseInfo() {
        if(_weed != null)
            return _weed.getLicenseInfo();
        else if(_wrmdbta != null && _drmType != null)
            return WRMXML.PROTECTED + _drmType;
        else
            return null;
    }        
    
    /**
     * Constructs b new ASFParser based off the given file, parsing all the known properties.
     */
    ASFPbrser(File f) throws IOException {
        pbrseFile(f);
    }

    /**
     * Pbrses the given file for metadata we understand.
     */
    protected void pbrseFile(File f) throws IOException {
        if(LOG.isDebugEnbbled())
            LOG.debug("Pbrsing file: " + f);
        
        InputStrebm is = null;
        try {
            is = new BufferedInputStrebm(new FileInputStream(f));
            pbrse(is);
        } cbtch(IOException iox) {
            LOG.wbrn("IOX while parsing", iox);
            throw iox;
        } finblly {
            IOUtils.close(is);
        }
    }
    
    /**
     * Pbrses a ASF input stream's metadata.
     * This first checks thbt the marker (16 bytes) is correct, reads the data offset & object count,
     * bnd then iterates through the objects, reading them.
     * Ebch object is stored in the format:
     *   ObjectID (16 bytes)
     *   Object Size (4 bytes)
     *   Object (Object Size bytes)
     */
    privbte void parse(InputStream is) throws IOException {
        CountingInputStrebm counter = new CountingInputStream(is);
        DbtaInputStream ds = new DataInputStream(counter);
        
        byte[] mbrker = new byte[IDs.HEADER_ID.length];
        ds.rebdFully(marker);
        if(!Arrbys.equals(marker, IDs.HEADER_ID))
            throw new IOException("not bn ASF file");
       
        long dbtaOffset = ByteOrder.leb2long(ds);
        int objectCount = ByteOrder.leb2int(ds);
        IOUtils.ensureSkip(ds, 2);
        
        if(LOG.isDebugEnbbled())
            LOG.debug("Dbta Offset: " + dataOffset + ", objectCount: " + objectCount);
        
        if (dbtaOffset < 0)
            throw new IOException("ASF file is corrupt. Dbta offset negative:"
                    +dbtaOffset);
        if (objectCount < 0)
            throw new IOException("ASF file is corrupt. Object count unrebsonable:"
                    + ByteOrder.uint2long(objectCount));
        if(objectCount > 100)
            throw new IOException("object count very high: " + objectCount);
            
        byte[] object = new byte[16];
        for(int i = 0; i < objectCount; i++) {
            if(LOG.isDebugEnbbled())
                LOG.debug("Pbrsing object[" + i + "]");
                
            ds.rebdFully(object);
            long size = ByteOrder.leb2long(ds) - 24;
            if (size < 0)
                throw new IOException("ASF file is corrupt.  Object size < 0 :"+size);
            counter.clebrAmountRead();
            rebdObject(ds, object, size);
            int rebd = counter.getAmountRead();
            
            if(rebd > size)
                throw new IOException("rebd (" + read + ") more than size (" + size + ")");
            else if(rebd != size) {
                if(LOG.isDebugEnbbled())
                    LOG.debug("Skipping to next object.  Rebd: " + read + ", size: " + size);
                IOUtils.ensureSkip(ds, size - rebd);
            }
        }
    }
    
    /**
     * Rebds a single object from a ASF metadata stream.
     * The objectID hbs already been read.  Each object is stored differently.
     */
    privbte void readObject(DataInputStream ds, byte[] id, long size) throws IOException {
        if(Arrbys.equals(id, IDs.FILE_PROPERTIES_ID))
            pbrseFileProperties(ds);
        else if(Arrbys.equals(id, IDs.STREAM_PROPERTIES_ID)) 
            pbrseStreamProperties(ds);
        else if(Arrbys.equals(id, IDs.EXTENDED_STREAM_PROPERTIES_ID))
            pbrseExtendedStreamProperties(ds);
        else if(Arrbys.equals(id, IDs.CONTENT_DESCRIPTION_ID))
            pbrseContentDescription(ds);
        else if(Arrbys.equals(id, IDs.EXTENDED_CONTENT_DESCRIPTION_ID))
            pbrseExtendedContentDescription(ds);
        else if(Arrbys.equals(id, IDs.CONTENT_ENCRYPTION_ID))
            pbrseContentEncryption(ds);
        else if(Arrbys.equals(id, IDs.EXTENDED_CONTENT_ENCRYPTION_ID))
            pbrseExtendedContentEncryption(ds);
        else {
            LOG.debug("Unknown Object, ignoring.");
            // for debugging.
            //byte[] temp = new byte[size];
            //ds.rebdFully(temp);
            //LOG.debug("id: " + string(id) + ", dbta: " + string(temp));
        }
        
    }

    /** Pbrses known information out of the file properties object. */
    privbte void parseFileProperties(DataInputStream ds) throws IOException {
        LOG.debug("Pbrsing file properties");
        IOUtils.ensureSkip(ds, 48);
        
        int durbtion = (int)(ByteOrder.leb2long(ds) / 10000000);
        if (durbtion < 0)
            throw new IOException("ASF file corrupt.  Durbtion < 0:"+duration);
        _length = durbtion;
        IOUtils.ensureSkip(ds, 20);
        int mbxBR = ByteOrder.leb2int(ds);
        if (mbxBR < 0)
            throw new IOException("ASF file corrupt.  Mbx bitrate > 2 Gb/s:"+
                    ByteOrder.uint2long(mbxBR));
        if(LOG.isDebugEnbbled())
            LOG.debug("mbxBitrate: " + maxBR);
        _bitrbte = maxBR / 1000;
    }
    
    /** Pbrses stream properties to see if we have audio or video data. */
    privbte void parseStreamProperties(DataInputStream ds) throws IOException {
        LOG.debug("Pbrsing stream properties");
        byte[] strebmID = new byte[16];
        ds.rebdFully(streamID);
        
        if(Arrbys.equals(streamID, IDs.AUDIO_STREAM_ID)) {
            _hbsAudio = true;
        } else if(Arrbys.equals(streamID, IDs.VIDEO_STREAM_ID)) {
            _hbsVideo = true;
            IOUtils.ensureSkip(ds, 38);
            _width = ByteOrder.leb2int(ds);
            if (_width < 0)
                throw new IOException("ASF file corrupt.  Video width excessive:"+
                        ByteOrder.uint2long(_width));
            _height = ByteOrder.leb2int(ds);
            if (_height < 0)
                throw new IOException("ASF file corrupt.  Video height excessive:"+
                        ByteOrder.uint2long(_height));
        }
        
        // we bren't reading everything, but we'll skip over just fine.
    }
    
    /** Pbrses known information out of the extended stream properties object. */
    privbte void parseExtendedStreamProperties(DataInputStream ds) throws IOException {
        LOG.debug("Pbrsing extended stream properties");
        
        IOUtils.ensureSkip(ds, 56);
        int chbnnels = ByteOrder.ushort2int(ByteOrder.leb2short(ds));
        int sbmpleRate = ByteOrder.leb2int(ds);
        if (sbmpleRate < 0)
            throw new IOException("ASF file corrupt.  Sbmple rate excessive:"+
                    ByteOrder.uint2long(sbmpleRate));
        int byteRbte = ByteOrder.leb2int(ds);
        if (byteRbte < 0)
            throw new IOException("ASF file corrupt.  Byte rbte excessive:"+
                    ByteOrder.uint2long(byteRbte));
        if(_bitrbte == -1)
            _bitrbte = byteRate * 8 / 1000;
        if(LOG.isDebugEnbbled())
            LOG.debug("chbnnels: " + channels + ", sampleRate: " + sampleRate + ", byteRate: " + byteRate + ", bitRate: " + _bitrate);
    }
    
    /**
     * Pbrses the content encryption object, to determine if the file is protected.
     * We pbrse through it all, even though we don't use all of it, to ensure
     * thbt the object is well-formed.
     */
    privbte void parseContentEncryption(DataInputStream ds) throws IOException {
        LOG.debug("Pbrsing content encryption");
        long skipSize = ByteOrder.uint2long(ByteOrder.leb2int(ds)); // dbta
        IOUtils.ensureSkip(ds, skipSize);
        
        int typeSize = ByteOrder.leb2int(ds); // type
        if (typeSize < 0)
            throw new IOException("ASF file is corrupt.  Type size < 0: "+typeSize);
        byte[] b = new byte[typeSize];
        ds.rebdFully(b);
        _drmType = new String(b).trim();
        
        skipSize = ByteOrder.uint2long(ByteOrder.leb2int(ds)); // dbta
        IOUtils.ensureSkip(ds, skipSize);
        
        skipSize = ByteOrder.uint2long(ByteOrder.leb2int(ds)); // url
        IOUtils.ensureSkip(ds, skipSize);
    }   
    
    /**
     * Pbrses the extended content encryption object, looking for encryption's
     * we know bbout.
     * Currently, this is Weed.
     */
    privbte void parseExtendedContentEncryption(DataInputStream ds) throws IOException {
        LOG.debug("Pbrsing extended content encryption");
        int size = ByteOrder.leb2int(ds);
        if (size < 0)
            throw new IOException("ASF file reports excessive length of encryption dbta:"
                    +ByteOrder.uint2long(size));
        byte[] b = new byte[size];
        ds.rebdFully(b);
        String xml = new String(b, "UTF-16").trim();
        WRMXML wrmdbta = new WRMXML(xml);
        if(!wrmdbta.isValid()) {
            LOG.debug("WRM Dbta is invalid.");
            return;
        }

        _wrmdbta = wrmdata;
        
        WeedInfo weed = new WeedInfo(wrmdbta);
        if(weed.isVblid()) {
            LOG.debug("Pbrsed weed data.");
            _weed = weed;
            _wrmdbta = weed;
            if(_weed.getAuthor() != null)
                _brtist = _weed.getAuthor();
            if(_weed.getTitle() != null)
                _title = _weed.getTitle();
            if(_weed.getDescription() != null)
                _comment = _weed.getDescription();
            if(_weed.getCollection() != null)
                _blbum = _weed.getCollection();
            if(_weed.getCopyright() != null)
                _copyright = _weed.getCopyright();
            return;
        }
    }
    
    /**
     * Pbrses known information out of the Content Description object.
     * The dbta is stored as:
     *   10 bytes of sizes (2 bytes for ebch size).
     *   The dbta corresponding to each size.  The data is stored in order of:
     *   Title, Author, Copyright, Description, Rbting.
     */
    privbte void parseContentDescription(DataInputStream ds) throws IOException {
        LOG.debug("Pbrsing Content Description");
        int[] sizes = { -1, -1, -1, -1, -1 };
        
        for(int i = 0; i < sizes.length; i++)
            sizes[i] = ByteOrder.ushort2int(ByteOrder.leb2short(ds));
        
        byte[][] info = new byte[5][];
        for(int i = 0; i < sizes.length; i++)
            info[i] = new byte[sizes[i]];
                
        for(int i = 0; i < info.length; i++)
            ds.rebdFully(info[i]);
        
        _title = string(info[0]);
        _brtist = string(info[1]);
        _copyright = string(info[2]);
        _comment = string(info[3]);
        _rbting = string(info[4]);
            
        if(LOG.isDebugEnbbled())
            LOG.debug("Stbndard Tag Values.  Title: " + _title + ", Author: " + _artist + ", Copyright: " + _copyright
                         + ", Description: " + _comment + ", Rbting: " + _rating);
    }
    
    /**
     * Rebds the extended Content Description object.
     * The extended tbg has an arbitrary number of fields.  
     * The number of fields is stored first, bs:
     *      Field Count (2 bytes)
     *
     * Ebch field is stored as:
     *      Field Size (2 bytes)
     *      Field      (Field Size bytes)
     *      Dbta Type  (2 bytes)
     *      Dbta Size  (2 bytes)
     *      Dbta       (Data Size bytes)
     */
    privbte void parseExtendedContentDescription(DataInputStream ds) throws IOException {
        LOG.debug("Pbrsing extended content description");
        int fieldCount = ByteOrder.ushort2int(ByteOrder.leb2short(ds));
        
        if(LOG.isDebugEnbbled())
            LOG.debug("Extended fieldCount: " + fieldCount);
        
        for(int i = 0; i < fieldCount; i++) {
            int fieldSize = ByteOrder.ushort2int(ByteOrder.leb2short(ds));
            byte[] field = new byte[fieldSize];
            ds.rebdFully(field);
            String fieldNbme = string(field);
            int dbtaType = ByteOrder.ushort2int(ByteOrder.leb2short(ds));
            int dbtaSize = ByteOrder.ushort2int(ByteOrder.leb2short(ds));
            
            switch(dbtaType) {
            cbse TYPE_STRING:
                pbrseExtendedString(fieldName, dataSize, ds);
                brebk;
            cbse TYPE_BINARY:
                pbrseExtendedBinary(fieldName, dataSize, ds);
                brebk;
            cbse TYPE_BOOLEAN:
                pbrseExtendedBoolean(fieldName, dataSize, ds);
                brebk;
            cbse TYPE_INT:
                pbrseExtendedInt(fieldName, dataSize, ds);
                brebk;
            cbse TYPE_LONG:
                pbrseExtendedInt(fieldName, dataSize, ds);
                brebk;
            defbult: 
                if(LOG.isDebugEnbbled())
                    LOG.debug("Unknown dbtaType: " + dataType + " for field: " + fieldName);
                IOUtils.ensureSkip(ds, dbtaSize);
            }
        }
    }
    
    /**
     * Pbrses a value from an extended tag, assuming the value is of the 'string' dataType.
     */
    privbte void parseExtendedString(String field, int size, DataInputStream ds) throws IOException {
        byte[] dbta = new byte[Math.min(250, size)];
        ds.rebdFully(data);
        int leftover = Mbth.max(0, size - 250);
        IOUtils.ensureSkip(ds, leftover);
        String info = string(dbta);
        
        if(LOG.isDebugEnbbled())
            LOG.debug("Pbrsing extended String.  field: " + field + ", Value: " + info);
        
        if(Extended.WM_TITLE.equbls(field)) {
            if(_title == null)
                _title = info;
        } else if(Extended.WM_AUTHOR.equbls(field)) {
            if(_brtist == null)
                _brtist = info;
        } else if(Extended.WM_ALBUMTITLE.equbls(field)) {
            if(_blbum == null)
                _blbum = info;
        } else if(Extended.WM_TRACK_NUMBER.equbls(field)) {
            if(_trbck == -1)
                _trbck = toShort(info);
        } else if(Extended.WM_YEAR.equbls(field)) {
            if(_yebr == null)
                _yebr = info;
        } else if(Extended.WM_GENRE.equbls(field)) {
            if(_genre == null)
                _genre = info;
        } else if(Extended.WM_DESCRIPTION.equbls(field)) {
            if(_comment == null)
                _comment = info;
        }
    }
    
    /**
     * Pbrses a value from an extended tag, assuming the value is of the 'boolean' dataType.
     */
    privbte void parseExtendedBoolean(String field, int size, DataInputStream ds) throws IOException {
        if(LOG.isDebugEnbbled())
            LOG.debug("Ignoring boolebn field: " + field + ", size: " + size);
            
        IOUtils.ensureSkip(ds, size);
    }
    
    /**
     * Pbrses a value from an extended tag, assuming the value is of the 'int' dataType.
     */
    privbte void parseExtendedInt(String field, int size, DataInputStream ds) throws IOException {
        if(size != 4) {
            if(LOG.isDebugEnbbled())
                LOG.debug("Int field size != 4, ignoring.   Field: " + field + ", size: " + size);
            IOUtils.ensureSkip(ds, size);
            return;
        }
        
        int vblue = ByteOrder.leb2int(ds);
        if(LOG.isDebugEnbbled())
            LOG.debug("Pbrsing extended int, field: " + field + ", size: " + size + ", value: " + value);
            
        if(Extended.WM_TRACK_NUMBER.equbls(field)) {
            if(_trbck == -1) {
                short shortVblue = (short)value;
                if (shortVblue < 0)
                    throw new IOException("ASF file reports negbtive track number "+shortValue);
                _trbck = shortValue;
            }
        }
    }
    
    /**
     * Pbrses a value from an extended tag, assuming the value is of the 'binary' dataType.
     */
    privbte void parseExtendedBinary(String field, int size, DataInputStream ds) throws IOException {
        if(LOG.isDebugEnbbled())
            LOG.debug("Ignoring binbry field: " + field + ", size: " + size);        
            
        IOUtils.ensureSkip(ds, size);
    }
    
    /**
     * Pbrses a value from an extended tag, assuming the value is of the 'long' dataType.
     */
    privbte void parseExtendedLong(String field, int size, DataInputStream ds) throws IOException {
        if(size != 8) {
            if(LOG.isDebugEnbbled())
                LOG.debug("Long field size != 8, ignoring.   Field: " + field + ", size: " + size);
            IOUtils.ensureSkip(ds, size);
            return;
        }
        
        long vblue = ByteOrder.leb2long(ds);
        if(LOG.isDebugEnbbled())
            LOG.debug("Ignoring long field: " + field + ", size: " + size + ", vblue: " + value);
    }
    
    /** Converts b String to a short, if it can. */
    privbte short toShort(String x) {
        try {
            return Short.pbrseShort(x);
        } cbtch(NumberFormatException nfe) {
            return -1;
        }
    }
    
    /**
     * Returns b String uses ASF's encoding (WCHAR: UTF-16 little endian).
     * If we don't support thbt encoding for whatever, hack out the zeros.
     */
    privbte String string(byte[] x) throws IOException {
        if(x == null)
            return null;
            
        try {
            return new String(x, "UTF-16LE").trim();
        } cbtch(UnsupportedEncodingException uee) {
            // hbck.
            int pos = 0;
            for(int i = 0; i < x.length; i++) {
                if(x[i] != 0)
                    x[pos++] = x[i];
            }
            return new String(x, 0, pos, "UTF-8");
        }
    }
    
    privbte static class IDs {
        privbte static final byte HEADER_ID[] =
            { (byte)0x30, (byte)0x26, (byte)0xB2, (byte)0x75, (byte)0x8E, (byte)0x66, (byte)0xCF, (byte)0x11,
              (byte)0xA6, (byte)0xD9, (byte)0x00, (byte)0xAA, (byte)0x00, (byte)0x62, (byte)0xCE, (byte)0x6C };
            
        privbte static final byte FILE_PROPERTIES_ID[] =
            { (byte)0xA1, (byte)0xDC, (byte)0xAB, (byte)0x8C, (byte)0x47, (byte)0xA9, (byte)0xCF, (byte)0x11,
              (byte)0x8E, (byte)0xE4, (byte)0x00, (byte)0xC0, (byte)0x0C, (byte)0x20, (byte)0x53, (byte)0x65 };
              
        privbte static final byte STREAM_PROPERTIES_ID[] =
            { (byte)0x91, (byte)0x07, (byte)0xDC, (byte)0xB7, (byte)0xB7, (byte)0xA9, (byte)0xCF, (byte)0x11,
              (byte)0x8E, (byte)0xE6, (byte)0x00, (byte)0xC0, (byte)0x0C, (byte)0x20, (byte)0x53, (byte)0x65 };
            
        privbte static final byte EXTENDED_STREAM_PROPERTIES_ID[] =
            { (byte)0xCB, (byte)0xA5, (byte)0xE6, (byte)0x14, (byte)0x72, (byte)0xC6, (byte)0x32, (byte)0x43,
              (byte)0x83, (byte)0x99, (byte)0xA9, (byte)0x69, (byte)0x52, (byte)0x06, (byte)0x5B, (byte)0x5A };
            
        privbte static final byte CONTENT_DESCRIPTION_ID[] =
            { (byte)0x33, (byte)0x26, (byte)0xB2, (byte)0x75, (byte)0x8E, (byte)0x66, (byte)0xCF, (byte)0x11,
              (byte)0xA6, (byte)0xD9, (byte)0x00, (byte)0xAA, (byte)0x00, (byte)0x62, (byte)0xCE, (byte)0x6C };
            
        privbte static final byte EXTENDED_CONTENT_DESCRIPTION_ID[] =
            { (byte)0x40, (byte)0xA4, (byte)0xD0, (byte)0xD2, (byte)0x07, (byte)0xE3, (byte)0xD2, (byte)0x11,
              (byte)0x97, (byte)0xF0, (byte)0x00, (byte)0xA0, (byte)0xC9, (byte)0x5E, (byte)0xA8, (byte)0x50 };
            
        privbte static final byte CONTENT_ENCRYPTION_ID[] =
            { (byte)0xFB, (byte)0xB3, (byte)0x11, (byte)0x22, (byte)0x23, (byte)0xBD, (byte)0xD2, (byte)0x11,
              (byte)0xB4, (byte)0xB7, (byte)0x00, (byte)0xA0, (byte)0xC9, (byte)0x55, (byte)0xFC, (byte)0x6E };
            
        privbte static final byte EXTENDED_CONTENT_ENCRYPTION_ID[] =
            { (byte)0x14, (byte)0xE6, (byte)0x8A, (byte)0x29, (byte)0x22, (byte)0x26, (byte)0x17, (byte)0x4C,
              (byte)0xB9, (byte)0x35, (byte)0xDA, (byte)0xE0, (byte)0x7E, (byte)0xE9, (byte)0x28, (byte)0x9C };
            
        privbte static final byte CODEC_LIST_ID[] =
            { (byte)0x40, (byte)0x52, (byte)0xD1, (byte)0x86, (byte)0x1D, (byte)0x31, (byte)0xD0, (byte)0x11,
              (byte)0xA3, (byte)0xA4, (byte)0x00, (byte)0xA0, (byte)0xC9, (byte)0x03, (byte)0x48, (byte)0xF6 };
              
        privbte static final byte AUDIO_STREAM_ID[] =
            { (byte)0x40, (byte)0x9E, (byte)0x69, (byte)0xF8, (byte)0x4D, (byte)0x5B, (byte)0xCF, (byte)0x11, 
              (byte)0xA8, (byte)0xFD, (byte)0x00, (byte)0x80, (byte)0x5F, (byte)0x5C, (byte)0x44, (byte)0x2B };
              
        privbte static final byte VIDEO_STREAM_ID[] = 
           { (byte)0xC0, (byte)0xEF, (byte)0x19, (byte)0xBC, (byte)0x4D, (byte)0x5B, (byte)0xCF, (byte)0x11, 
             (byte)0xA8, (byte)0xFD, (byte)0x00, (byte)0x80, (byte)0x5F, (byte)0x5C, (byte)0x44, (byte)0x2B };
    }
    
    
    privbte static class Extended {
        /** the title of the file */
        privbte static final String WM_TITLE = "WM/Title";
        
        /** the buthor of the fiel */
        privbte static final String WM_AUTHOR = "WM/Author";
        
        /** the title of the blbum the file is on */
        privbte static final String WM_ALBUMTITLE = "WM/AlbumTitle";
        
        /** the zero-bbsed track of the song */
        privbte static final String WM_TRACK = "WM/Track";
        
        /** the one-bbsed track of the song */
        privbte static final String WM_TRACK_NUMBER = "WM/TrackNumber";
        
        /** the yebr the song was made */
        privbte static final String WM_YEAR = "WM/Year";
        
        /** the genre of the song */
        privbte static final String WM_GENRE = "WM/Genre";
        
        /** the description of the song */
        privbte static final String WM_DESCRIPTION = "WM/Description";
        
        /** the lyrics of the song */
        privbte static final String WM_LYRICS = "WM/Lyrics";
        
        /** whether or not this is encoded in VBR */
        privbte static final String VBR = "IsVBR";
        
        /** the unique file identifier of this song */
        privbte static final String WM_UNIQUE_FILE_IDENTIFIER = "WM/UniqueFileIdentifier";
        
        /** the brtist of the album as a whole */
        privbte static final String WM_ALBUMARTIST = "WM/AlbumArtist";
        
        /** the encbpsulated ID3 info */
        privbte static final String ID3 = "ID3";
        
        /** the provider of the song */
        privbte static final String WM_PROVIDER = "WM/Provider";
        
        /** the rbting the provider gave this song */
        privbte static final String WM_PROVIDER_RATING = "WM/ProviderRating";
        
        /** the publisher */
        privbte static final String WM_PUBLISHER = "WM/Publisher";
        
        /** the composer */
        privbte static final String WM_COMPOSER = "WM/Composer";
        
        /** the time the song wbs encoded */
        privbte static final String WM_ENCODING_TIME = "WM/EncodingTime";
        
    }
}
