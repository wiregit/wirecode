package com.limegroup.gnutella.dime;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import com.limegroup.gnutella.Assert;
import com.sun.java.util.collections.HashMap;
import com.sun.java.util.collections.Map;

import com.limegroup.gnutella.ByteOrder;

/**
 * @author Gregorio Roper
 * 
 * Class holding a DIMERecord as part of a <tt>DIMEMessage</tt>
 */
public abstract class AbstractDIMERecord {
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
    private static final byte TYPE_MEDIA_TYPE_MASK = 0x01 << 5;
    // use absolute URI as defined in RFC 2396
    private static final byte TYPE_ABSOLUTE_URI_MASK = 0x02 << 5;
    // unknown type
    private static final byte TYPE_UNKNOWN_MASK = 0x03 << 5;
    // no type payload at all
    private static final byte TYPE_NONE_MASK = (byte) (0x04 << 5);

    private final byte[] _header;
    private final byte[] _options;
    private final byte[] _id;
    private final byte[] _type;
    private final byte[] _data;

    private int _typeId = AbstractDIMEMessage.TYPE_UNIDENTIFIED;
    private String _idString = null;
    private Map _optionsMap = null;

    /**
     * Constructs new DIMERecord
     * 
     * @param header
     *            an array of 12 bytes for the header;
     * @param options
     *            an array of data
     * @param id
     *            an array of data 
     * @param type
     *            an array of data 
     * @param data
     *            an array of data 
     */
    protected AbstractDIMERecord(
        byte[] header,
        byte[] options,
        byte[] id,
        byte[] type,
        byte[] data) {
        _header = header;
        _options = options;
        _id = id;
        _type = type;
        _data = data;
    }

    /**
     * Accessor for _typeId
     * 
     * @return return integer identifying the format for the type field of the
     *         message
     * @throws DIMEMessageException
     *             in case of a problem parsing the message;
     */
    public int getTypeId() throws DIMEMessageException {
        if (_typeId == AbstractDIMEMessage.TYPE_UNIDENTIFIED)
            _typeId = parseTypeID(_header);
        return _typeId;
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
            // this should never happen
            Assert.that(false, "UTF-8 encoding is not supported");
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
     * @return headerField of <tt>DIMERecord</tt>
     */
    public byte[] getHeader() {
        return _header;
    }

    /**
     * @return true if this is the last entry.
     */
    public boolean isLastRecord() {
        return (MESSAGE_END == (_header[0] & MESSAGE_END));
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

    void setLastMessageFalse() {
        _header[0] ^= MESSAGE_END;
        Assert.that(!isLastRecord());
    }

    void write(OutputStream os) throws IOException {
        os.write(AbstractDIMEMessage.padData(_header));
        os.write(AbstractDIMEMessage.padData(_options));
        os.write(AbstractDIMEMessage.padData(_id));
        os.write(AbstractDIMEMessage.padData(_type));
        os.write(AbstractDIMEMessage.padData(_data));
    }

    private static int parseTypeID(byte[] header) throws DIMEMessageException {
        if ((header[1] & TYPE_MEDIA_TYPE_MASK) == TYPE_MEDIA_TYPE_MASK)
            return AbstractDIMEMessage.TYPE_MEDIA_TYPE;
        else if (
            (header[1] & TYPE_ABSOLUTE_URI_MASK) == TYPE_ABSOLUTE_URI_MASK)
            return AbstractDIMEMessage.TYPE_URI;
        else if ((header[1] & TYPE_UNKNOWN_MASK) == TYPE_UNKNOWN_MASK)
            return AbstractDIMEMessage.TYPE_UNKNOWN;
        else if ((header[1] & TYPE_UNCHANGED_MASK) == TYPE_UNCHANGED_MASK)
            return AbstractDIMEMessage.TYPE_NONE;
        else if ((header[1] & TYPE_NONE_MASK) == TYPE_NONE_MASK)
            return AbstractDIMEMessage.TYPE_NONE;
        else
            throw new DIMEMessageException("Illegal TYPE ID");
    }

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
