package com.limegroup.gnutella.dime;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.util.BandwidthThrottle;
import com.sun.java.util.collections.ArrayList;
import com.sun.java.util.collections.HashSet;
import com.sun.java.util.collections.Iterator;
import com.sun.java.util.collections.Map;
import com.sun.java.util.collections.Set;

/**
 * @author Gregorio Roper
 * 
 * Class containing a DIME message.
 */
public abstract class AbstractDIMEMessage {
    //////public constants
    /**
     * constant for unknown payload type
     */
    public static final int TYPE_UNKNOWN = 0;

    /**
     * constant for URI payload identifier
     */
    public static final int TYPE_URI = 1;

    /**
     * constant for RFC 2616 payload identifier
     */
    public static final int TYPE_MEDIA_TYPE = 2;

    /**
     * constant for no payload identifier at all
     */
    public static final int TYPE_NONE = 3;

    /**
     * constant for undefined payload identifier
     */
    static final int TYPE_UNIDENTIFIED = -1;

    //////private static constants
    // DIME version number sent in the first 5 bytes of each record
    private static final byte VERSION = 0x01 << 3;
    // Message Begin flag
    private static final byte MESSAGE_BEGIN = 0x01 << 2;
    // Message End flag
    private static final byte MESSAGE_END = 0x01 << 1;
    // The last bit of the first byte is used by the type field. Since we
    // don't support any type that could be using this byte we completely
    // ignore it (in fact there is no type defined by the DIME spec that
    // would be using this bit). Instead we read only the first three bits
    // of the second byte.
    // use type of previous record
    private static final byte TYPE_UNCHANGED_MASK = 0x00;
    // use media-type as defined in RFC 2616
    private static final byte TYPE_MEDIA_TYPE_MASK = 0x01 << 4;
    // use absolute URI as defined in RFC 2396
    private static final byte TYPE_ABSOLUTE_URI_MASK = 0x02 << 4;
    // unknown type
    private static final byte TYPE_UNKNOWN_MASK = 0x03 << 4;
    // no type payload at all
    private static final byte TYPE_NONE_MASK = (byte) (0x04 << 4);

    /////// private constants
    // be careful never to remove objects from this ArrayList without clearing
    // it completely and don't do anything that could change the order of the
    // List's elements.
    private ArrayList _records = new ArrayList();
    // store the type of this payload
    private final byte[] TYPE;
    // store the format of the type identifier, whether URI, media type or
    // unknown
    private final int TYPE_ID;

    static transient final Log LOG =
        LogFactory.getLog(AbstractDIMEMessage.class);

    /**
     * Constructs a new DIMEMessage
     * 
     * @param typeIdentifier
     *            an Integer identifying the type as either TYPE_URI,
     *            TYPE_MEDIA_TYPE or TYPE_UNKNOWN
     * @param type
     *            a <tt>String</tt> identifying the payload
     */
    public AbstractDIMEMessage(int typeIdentifier, String type) {
        try {
            TYPE = type.getBytes("UTF-8");
        } catch (UnsupportedEncodingException uee) {
            throw new IllegalArgumentException("UTF-8 encoding not supported");
        }
        TYPE_ID = typeIdentifier;
    }

    /**
     * method for writing a DIMEMessage to an OutputStream
     * 
     * @param os
     *            the <tt>OutputStream</tt> to write to.
     * @throws IOException
     *             if there was a problem writing to os.
     */
    public void write(OutputStream os) throws IOException {
        for (Iterator iter = _records.iterator(); iter.hasNext();)
             ((AbstractDIMERecord) iter.next()).write(os);
    }

    /**
     * method for writing a AbstractDIMEMessage to an OutputStream
     * 
     * @param os
     *            the <tt>OutputStream</tt> to write to.
     * @param throttle
     *            the <tt>BandwidthThrottle</tt> throttling our output
     * @throws IOException
     *             if there was a problem writing to os.
     */
    public void write(OutputStream os, BandwidthThrottle throttle)
        throws IOException {
        for (Iterator iter = _records.iterator(); iter.hasNext();)
             ((AbstractDIMERecord) iter.next()).write(os, throttle);
    }

    /**
     * Reads a new DIMERecord from a stream
     * 
     * @param is
     *            the <tt>InputStream</tt> to read from.
     * @return new instance of <tt>DIMERecord</tt>
     * @throws IOException
     *             if there was a problem reading from the network.
     */
    protected static AbstractDIMERecord readRecord(InputStream is)
        throws IOException {
        byte[] header = new byte[12];
        readFromStream(header, is);

        if ((header[0] & VERSION) != VERSION)
            throw new DIMEMessageException("unknown DIMEVersion" + header[0]);
        // bytes 2 & 3 contain the optionsLength in BIG ENDIAN format
        int optionsLength = getIntBigEndian(header, 2, 2);

        // bytes 4 & 5 contain the idLength in BIG ENDIAN format
        int idLength = getIntBigEndian(header, 4, 2);
        if (idLength < 0)
            throw new IOException("illegal data field in DIME message");

        // bytes 6 & 7 contain the typeLength in BIG ENDIAN format
        int typeLength = getIntBigEndian(header, 6, 2);
        if (typeLength < 0)
            throw new IOException("illegal data field in DIME message");

        // bytes 8 - 11 contain the dataLength in BIG ENDIAN format
        // possibly dangerous cast, but I don't expect messages that are longer
        // than Integer.MAX_VALUE
        int dataLength = (int) getLongBigEndian(header, 8, 4);

        if (dataLength < 0)
            throw new IOException("illegal data field in DIME message");

        byte[] options = new byte[optionsLength];
        readFromStream(options, is);
        skipPaddedData(is, optionsLength);

        byte[] id = new byte[idLength];
        readFromStream(id, is);
        skipPaddedData(is, idLength);

        byte[] type = new byte[typeLength];
        readFromStream(type, is);
        skipPaddedData(is, typeLength);

        byte[] data = new byte[dataLength];
        readFromStream(data, is);
        skipPaddedData(is, dataLength);
        return new DefaultDIMERecord(header, options, id, type, data);
    }

    /**
     * Adds a DIMERecord to the current message
     * 
     * @param options
     *            a <tt>Map</tt> of String->String
     * @param id
     *            a <tt>String</tt> holding the URI for this DIMERecord
     * @param data
     *            <tt>byte[]</tt> containing arbitrary data
     */
    protected void addRecord(Map options, String id, byte[] data) {
        AbstractDIMERecord dr = createRecord(options, id, data);
        _records.add(dr);
    }

    /**
     * Adds a DIMERecord to the current DIMEMessage
     * 
     * @param record
     *            the <tt>DIMERecord</tt> to add.
     */
    protected void addRecord(AbstractDIMERecord record) {
        _records.add(record);
    }

    protected AbstractDIMERecord createRecord(
        Map options,
        String id,
        byte[] data) {

        byte[] optionsField = createOptions(options);
        // add type field in the first record and if TYPE_ID!=TYPE_NONE

        byte[] typeField;
        if (_records.size() == 0 && TYPE_ID != TYPE_NONE)
            typeField = TYPE;
        else
            typeField = new byte[0];

        byte[] idField = id.getBytes();

        byte[] dataField = data;

        byte[] headerField =
            createHeader(
                optionsField.length,
                idField.length,
                typeField.length,
                dataField.length);

        AbstractDIMERecord ret =
            new DefaultDIMERecord(
                headerField,
                optionsField,
                idField,
                typeField,
                dataField);
        return ret;
    }

    protected void reset() {
        _records.clear();
    }

    private byte[] createOptions(Map options) {
        if (options == null)
            return new byte[0];
        Set optionsSet = new HashSet();
        int optionsLength = 0;
        for (Iterator iter = options.keySet().iterator(); iter.hasNext();) {
            byte[] key;
            try {
                key = ((String) iter.next()).getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new IllegalArgumentException("UTF-8 encoding not supported");
            }
            if (key.length != 2)
                throw new IllegalArgumentException("Illegal option id for DIMERecord");

            byte[] value;
            try {
                value = ((String) options.get(key)).getBytes("UTF-8");
            } catch (UnsupportedEncodingException e1) {
                throw new IllegalArgumentException("UTF-8 encoding not supported");
            }
            if (value == null)
                throw new NullPointerException("can't accept null options for DIMERecord");

            int elementDataLength = calculatePaddedDataLength(value.length);

            // create byte array containing 2 bytes element id, 2 bytes element
            // data lengh and padded elementData
            byte[] element = new byte[4 + elementDataLength];
            System.arraycopy(key, 0, element, 0, 2);
            System.arraycopy(
                getBytesBigEndian(elementDataLength, 2),
                0,
                element,
                2,
                2);
            System.arraycopy(padData(value), 0, element, 4, elementDataLength);

            // save the element in the optionsSet for now.
            optionsSet.add(element);
            // remember how many bytes we have added
            optionsLength += element.length;
        }

        // now copy all elements into a single byteArray
        byte[] ret = new byte[optionsLength];
        int index = 0;
        for (Iterator iter = optionsSet.iterator(); iter.hasNext();) {
            byte[] element = (byte[]) iter.next();
            System.arraycopy(element, 0, ret, index, element.length);
            index += element.length;
        }

        // done!
        return ret;
    }

    private byte[] createHeader(
        int optionsLength,
        int idLength,
        int typeLength,
        long dataLength) {
        byte[] header = new byte[12];
        header[0] = VERSION;
        // set message end flag for this record, this will be modified by
        // any further records that will be added.
        header[0] |= MESSAGE_END;
        // set message begin flag if necessary or reset the message end
        // flag of the last record that was added.
        if (_records.size() == 0)
            header[0] |= MESSAGE_BEGIN;
        else
            ((AbstractDIMERecord) _records.get(_records.size() - 1))
                .setLastMessageFalse();

        // only set TYPE in the first record
        if (_records.size() == 0) {
            switch (TYPE_ID) {
                case TYPE_UNKNOWN :
                    header[1] |= TYPE_UNKNOWN_MASK;
                    break;
                case TYPE_URI :
                    header[1] |= TYPE_ABSOLUTE_URI_MASK;
                    break;
                case TYPE_MEDIA_TYPE :
                    header[1] |= TYPE_MEDIA_TYPE_MASK;
                    break;
                case TYPE_NONE :
                    header[1] |= TYPE_NONE_MASK;
                    break;
                default :
                    throw new IllegalArgumentException("Illegal content type identifier in DIME Message");
            }
        } else
            header[1] |= TYPE_UNCHANGED_MASK;

        // add options length BIG ENDIAN in bytes 2 & 3 of header
        System.arraycopy(getBytesBigEndian(optionsLength, 2), 0, header, 2, 2);

        // add id length BIG ENDIAN in bytes 4 & 5 of header
        System.arraycopy(getBytesBigEndian(idLength, 2), 0, header, 4, 2);

        // add type length BIG ENDIAN in bytes 6 & 7 of header
        System.arraycopy(getBytesBigEndian(typeLength, 2), 0, header, 6, 2);

        // add data length BIG ENDIAN in bytes 8-11 of header
        System.arraycopy(getBytesBigEndian(dataLength, 4), 0, header, 8, 4);

        return header;
    }

    ///////////////////////////////////
    ////// static helper methods //////
    ///////////////////////////////////

    // also used by DIMERecord
    static byte[] padData(byte[] data) {
        if (data.length % 4 == 0)
            return data;
        byte[] ret = new byte[calculatePaddedDataLength(data.length)];
        System.arraycopy(data, 0, ret, 0, data.length);
        for (int i = data.length; i < ret.length; i++)
            ret[i] = 0x00;
        return ret;
    }

    static int calculatePaddedDataLength(int length) {
        int ret = length;
        ret += (length % 4 == 0) ? 0 : (4 - length % 4);
        return ret;
    }

    static byte[] getBytesBigEndian(long number, int byteCount) {
        byte[] ret = new byte[byteCount];
        for (int i = byteCount - 1; i >= 0; i--) {
            ret[i] = (byte) (0xFF & number);
            number >>>= 8;
        }
        return ret;
    }

    static int getIntBigEndian(byte[] bytes, int index, int length) {
        int ret = 0;
        for (int i = index; i < index + length; i++) {
            ret <<= 8;
            ret |= getUnsignedByte(bytes[i]);
        }
        return ret;
    }

    static int getIntLittleEndian(byte[] bytes, int index, int length) {
        int ret = 0;
        for (int i = index + length - 1; i >= length; i--) {
            ret <<= 8;
            ret |= getUnsignedByte(bytes[i]);
        }
        return ret;
    }

    static long getLongBigEndian(byte[] bytes, int index, int length) {
        long ret = 0;
        for (int i = index; i < index + length; i++) {
            ret <<= 8;
            ret |= getUnsignedByte(bytes[i]);
        }
        return ret;
    }

    static int getUnsignedByte(byte b) {
        if (b < 0)
            return (256 + b);
        else
            return b;
    }

    static void skipPaddedData(InputStream is, int dataLength)
        throws IOException {
        int paddedLength = calculatePaddedDataLength(dataLength);
        is.skip(paddedLength - dataLength);
    }

    private static int readFromStream(byte[] buf, InputStream is)
        throws IOException {
        int offset = 0;
        while (offset < buf.length) {
            buf[offset] = (byte)is.read();
            offset ++;
        }
        return offset;
    }

}