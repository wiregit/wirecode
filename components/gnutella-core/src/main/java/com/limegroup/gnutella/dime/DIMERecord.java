package com.limegroup.gnutella.dime;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import com.limegroup.gnutella.Assert;
import com.sun.java.util.collections.HashMap;
import com.sun.java.util.collections.Map;

import com.limegroup.gnutella.util.DataUtils;
import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.ErrorService;

/**
 * Class holding a DIMERecord as part of a DIME Message.
 *
 * @author Gregorio Roper
 * @author Sam Berlin 
 */
public class DIMERecord {
    // A DIME Record looks like the following:
    ///////////////////////////////////////////////////////////////////
    // 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 
    // ----------------------------------------------------------------
    //  VERSION |M|M|C|  TYPE |  RSRV |          OPTIONS_LENGTH
    //          |B|E|F|       |       |
    // ----------------------------------------------------------------
    //         ID_LENGTH              |          TYPE_LENGTH
    // ----------------------------------------------------------------
    //                           DATA_LENGTH
    // ----------------------------------------------------------------
    //                        OPTIONS + PADDING
    // ----------------------------------------------------------------
    //                          ID + PADDING
    // ----------------------------------------------------------------
    //                         TYPE + PADDING
    // ----------------------------------------------------------------
    //                         DATA + PADDING
    // ----------------------------------------------------------------
    ///////////////////////////////////////////////////////////////////
    // Where padding brings the field equal to a multiple octects.
    // There must not be more than 3 octects of padding.
    // All integer fields (anything ending in _LENGTH) are in BIG ENDIAN
    // format.
    // The header is considered to be bytes 0-12 (up to the end of DATA_LENGTH)
    // since all DIMERecords must contain atleast those 12 bytes.
    // For the particulars of DIME, see: http://www.perfectxml.com/DIME.asp
    
    /**
     * The current (and only) version of a DIME Record.
     */
    public static final byte VERSION = 0x01 << 3;
    
    /**
     * The version mask.
     */
    private static final byte VERSION_MASK = (byte)0xF8;
    
    /**
     * The mask marking this is the first record in a dime message.
     */
    private static final byte MB_MASK = 0x01 << 2;
    
    /**
     * The mask marking this as the last record in a dime message.
     */
    private static final byte ME_MASK = 0x01 << 1;
    
    /**
     * The mark marking this as a chunked record (set in the first
     * and all subsequent records except for the very last one) in a DIME
     * message.
     */
    private static final byte CF_MASK = 0x01;
    
    /**
     * The first byte of the message, containing the version, mb, me, and cf.
     */
    private byte _byte1;
    
    /**
     * The flag representing the UNCHANGED type.
     *
     * This means to use the type of the previous record.  It is used by 
     * all chunked records (beginning with the 2nd chunk) and requires that
     * the TYPE_LENGTH be 0.
     */
    public static final byte TYPE_UNCHANGED = 0x0;
    
    /**
     * The flag representing the MEDIA_TYPE type.
     *
     * This means the type is a Media Type as defined by RFC 2616, described
     * at http://www.ietf.org/rfc/rfc2616.txt in section 3.7.
     */
    public static final byte TYPE_MEDIA_TYPE = 0x01 << 4;
    
    /**
     * The flag representing an absolute URI.
     */
    public static final byte TYPE_ABSOLUTE_URI = 0x02 << 4;
    
    /**
     * The flag representing an unknown type.
     */
    public static final byte TYPE_UNKNOWN = 0x03 << 4;
    
    /**
     * The flag representing no type.
     */
    public static final byte TYPE_NONE = 0x04 << 4;
    
    /**
     * The type mask.
     */
    private static final byte TYPE_MASK = (byte)0xF0;
    
    /**
     * The reserved value.  Must be 0 in a valid DIME record.
     */
    private static final byte RESERVED = 0x0;
    
    /**
     * The reserved mask.
     */
    private static final byte RESERVED_MASK = 0xF;
    
    /**
     * The second byte, containing the type & reserved flag.
     */
    private final byte _byte2;
    
    /**
     * The options.
     */
    private final byte[] _options;
    
    /**
     * The ID.
     */
    private final byte[] _id;
    
    /**
     * The type.
     */
    private final byte[] _type;
    
    /**
     * The data.
     */
    private final byte[] _data;
    
    /**
     * The ID as a string.
     */
    private String _idString = null;
    
    /**
     * A Map of the options.
     */
    private Map _optionsMap = null;
    
    /**
     * Constructs a new DIMERecord with the given data.
     */
    private DIMERecord(byte byte1, byte byte2, byte[] options,
                       byte[] id, byte[] type, byte[] data) {
        _byte1 = byte1;
        _byte2 = byte2;
        _options = options;
        _id = id;
        _type = type;
        _data = data;
        validate();
    }
    
    /**
     * Constructs a new DIMERecord with the given information.
     */
    public static DIMERecord create(byte typeId, byte[] options, byte[] id,
                             byte[] type, byte[] data) {
        byte byte1 = VERSION;
        byte byte2 = (byte)(typeId | RESERVED);
        if(options == null)
            options = DataUtils.EMPTY_BYTE_ARRAY;
        if(id == null)
            id = DataUtils.EMPTY_BYTE_ARRAY;
        if(type == null)
            type = DataUtils.EMPTY_BYTE_ARRAY;
        if(data == null)
            data = DataUtils.EMPTY_BYTE_ARRAY;
        return new DIMERecord(byte1, byte2, options, id, type, data);
    }
    
    /**
     * Constructs a new DIMERecord from an InputStream.
     */
    public static DIMERecord createFromStream(InputStream in) throws IOException {
        byte[] header = new byte[12];
        fillBuffer(header, in);

        int optionsLength = ByteOrder.beb2int(header, 2, 2);
        int idLength = ByteOrder.beb2int(header, 4, 2);
        int typeLength = ByteOrder.beb2int(header, 6, 2);
        int dataLength = ByteOrder.beb2int(header, 8, 4);
        
        //The DIME specification allows this to be a 32-bit unsigned field,
        //which in Java would be a long -- but in order to hold the array
        //of the data, we can only read up to 16 unsigned bits (an int), in order
        //to size the array correctly.
        if(dataLength < 0)
            throw new IOException("data too big.");

        byte[] options = readInformation(optionsLength, in);
        byte[] id = readInformation(idLength, in);
        byte[] type = readInformation(typeLength, in);
        byte[] data = readInformation(dataLength, in);
        
        
        try {
            return new DIMERecord(header[0], header[1],
                                  options, id, type, data);
        } catch(IllegalArgumentException iae) {
            throw new IOException(iae.getMessage());
        }
    }
    
    /**
     * Writes this record to the given OutputStream.
     */
    void write(OutputStream out) throws IOException {
        // Write the header.
        out.write(_byte1);
        out.write(_byte2);
        ByteOrder.int2beb(_options.length, out, 2);
        ByteOrder.int2beb(_id.length, out, 2);
        ByteOrder.int2beb(_type.length, out, 2);
        ByteOrder.int2beb(_data.length, out, 4);
        
        // Write out the data.
        writeDataWithPadding(_options, out);
        writeDataWithPadding(_id, out);
        writeDataWithPadding(_type, out);
        writeDataWithPadding(_data, out);
    }        
    
    /**
     * Sets this to be the first record in a sequence of records.
     */
    public void setFirstRecord(boolean first) {
        if(first)
            _byte1 |= MB_MASK;
        else
            _byte1 &= ~MB_MASK;
    }
    
    /**
     * Determines is this record is the first in a series of records.
     */
    public boolean isFirstRecord() {
        return (_byte1 & MB_MASK) == MB_MASK;
    }
    
    /**
     * Sets this to be the last record in a sequence of records.
     */
    public void setLastRecord(boolean last) {
        if(last)
            _byte1 |= ME_MASK;
        else
            _byte1 &= ~ME_MASK;
    }
    
    /**
     * Determines if this record is the last in a series of records.
     */
    public boolean isLastRecord() {
        return (_byte1 & ME_MASK) == ME_MASK;
    }

    /**
     * Returns one of the type constants:
     *  TYPE_UNCHANGED
     *  TYPE_MEDIA_TYPE
     *  TYPE_ABSOLUTE_URI
     *  TYPE_UNKNOWN
     *  TYPE_NONE
     */
    public int getTypeId() {
        return _byte2 & TYPE_MASK;
    }

    /**
     * @return typeField of <tt>DIMERecord</tt>
     */
    public byte[] getType() {
        return _type;
    }

    /**
     * @return String representation of type field
     */
    public String getTypeString() {
        try {
            return new String(_type, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            ErrorService.error(e);
            return null;
        }
    }

    /**
     * @return dataField of <tt>DIMERecord</tt>
     */
    public byte[] getData() {
        return _data;
    }

    /**
     * @return idField of <tt>DIMERecord</tt>
     */
    public byte[] getId() {
        return _id;
    }

    /**
     * @return optionsField of <tt>DIMERecord</tt>
     */
    public byte[] getOptions() {
        return _options;
    }

    /**
     * @return String containing the URI for this DIMERecord
     */
    public String getIdentifier() {
        if (_idString == null)
            _idString = new String(_id);
        return _idString;
    }

    /**
     * @return Map of String->String
     * 
     * @throws DIMEMessageException
     *             in case of a problem reading the message
     */
    public Map getOptionsMap() throws DIMEMessageException {
        if (_optionsMap == null)
            _optionsMap = parseOptions(_options);
        return _optionsMap;
    }
    
    /**
     * Validates the given DIMERecord, throwing IllegalArgumentException
     * if any fields are invalid.
     */
    private void validate() {
        if((_byte1 & VERSION_MASK) != VERSION)
            throw new IllegalArgumentException("invalid version: " + 
                                     (((_byte1 & VERSION_MASK) >> 3) & 0x1F));
                                  
        if((_byte2 & RESERVED_MASK) != RESERVED)
            throw new IllegalArgumentException("invalid reserved: " +
                                          (_byte2 & RESERVED_MASK));

        byte maskedType = (byte)(_byte2 & TYPE_MASK);
        switch(maskedType) {
        case TYPE_UNCHANGED:
            if(_type.length != 0)
                throw new IllegalArgumentException(
                    "TYPE_UNCHANGED requires 0 type length");
            break;                    
        case TYPE_MEDIA_TYPE:
            break;
        case TYPE_ABSOLUTE_URI:
            break;
        case TYPE_UNKNOWN:
            if(_type.length != 0)
                throw new IllegalArgumentException(
                    "TYPE_UNKNOWN requires 0 type length");
            break;
        case TYPE_NONE:
            if(_type.length != 0 || _data.length != 0)
                throw new IllegalArgumentException(
                    "TYPE_NONE requires 0 type & data length");
            break;
        default:
            throw new IllegalArgumentException(
                "invalid type: " + ((maskedType >> 4) & 0x0F));
        }
    }      
    
    /**
     * Reads data from the input stream, skipping padded bytes if necessary.
     */
    private static byte[] readInformation(int length, InputStream in)
      throws IOException {
        if(length == 0)
            return DataUtils.EMPTY_BYTE_ARRAY;
            
        byte[] info = new byte[length];
        fillBuffer(info, in);
        skipPaddedData(length, in);
        return info;
    }
    
    /**
     * Writes the given data an output stream, including padding.
     */
    private static void writeDataWithPadding(byte[] data, OutputStream os) 
      throws IOException {
        if(data.length == 0)
            return;
            
        os.write(data);
        
        // write the padding.
        int padding = calculatePaddingLength(data.length);
        switch(padding) {
        case 0:
            return;
        case 1:
            os.write(DataUtils.BYTE_ARRAY_ONE);
            return;
        case 2:
            os.write(DataUtils.BYTE_ARRAY_TWO);
            return;
        case 3:
            os.write(DataUtils.BYTE_ARRAY_THREE);
            return;
        default:
            throw new IllegalStateException("invalid padding.");
        }
    }   
        
    /**
     * Calculates how much data should be padded for the given length.
     */
    private static int calculatePaddingLength(int length) {
        return (length % 4 == 0) ? 0 : (4 - length % 4);
    }
    
    /**
     * Skips however much data was padded for the given length.
     */
    private static void skipPaddedData(int length, InputStream in)
      throws IOException {
        int padding = calculatePaddingLength(length);
        in.skip(padding);
    }        
    
    /**
     * Fills up the byte array with data from the stream.
     */
    private static void fillBuffer(byte[] buffer, InputStream in)
      throws IOException {
        int offset = 0;
        while (offset < buffer.length) {
            int read = in.read(buffer, offset, buffer.length - offset);
            if(read == -1)
                throw new IOException("eof");
            else
                offset += read;
        }
    }

    /**
     * Parses a byte array of options into a Map.
     */
    private static Map parseOptions(byte[] options)
        throws DIMEMessageException {
        Map map = new HashMap();
        int offset = 0;
        while (offset < options.length) {
            if (options.length - offset < 4)
                throw new DIMEMessageException("illegal options field");

            byte[] keyBytes = new byte[2];
            System.arraycopy(options, offset, keyBytes, 0, 2);
            String key;
            try {
                key = new String(keyBytes, "UTF-8");
            } catch (UnsupportedEncodingException uee) {
                // simply ignore this option
                key = null;
            }
            offset += 2;

            int valueLength = ByteOrder.beb2int(options, offset, 2);
            offset += 2;

            if (options.length - offset < valueLength)
                throw new DIMEMessageException("illegal options field");

            byte[] valueBytes = new byte[valueLength];
            System.arraycopy(options, offset, valueBytes, 0, valueLength);

            String value;
            try {
                value = new String(valueBytes, "UTF-8");
            } catch (UnsupportedEncodingException uee) {
                // simply ignore this option
                value = null;
            }

            offset += valueLength;

            if (key != null && value != null)
                map.put(key, value);
        }
        return map;
    }
}
