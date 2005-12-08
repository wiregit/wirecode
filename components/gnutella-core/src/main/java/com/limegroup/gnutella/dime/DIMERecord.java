pbckage com.limegroup.gnutella.dime;

import jbva.io.IOException;
import jbva.io.InputStream;
import jbva.io.OutputStream;
import jbva.io.UnsupportedEncodingException;
import jbva.util.HashMap;
import jbva.util.Map;

import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;

import com.limegroup.gnutellb.ByteOrder;
import com.limegroup.gnutellb.ErrorService;
import com.limegroup.gnutellb.util.DataUtils;

/**
 * Clbss holding a DIMERecord as part of a DIME Message.
 *
 * @buthor Gregorio Roper
 * @buthor Sam Berlin 
 */
public clbss DIMERecord {
    privbte static final Log LOG = LogFactory.getLog(DIMERecord.class);
    
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
    // Where pbdding brings the field equal to a multiple octects.
    // There must not be more thbn 3 octects of padding.
    // All integer fields (bnything ending in _LENGTH) are in BIG ENDIAN
    // formbt.
    // The hebder is considered to be bytes 0-12 (up to the end of DATA_LENGTH)
    // since bll DIMERecords must contain atleast those 12 bytes.
    // For the pbrticulars of DIME, see: http://www.perfectxml.com/DIME.asp
    
    /**
     * The current (bnd only) version of a DIME Record.
     */
    public stbtic final byte VERSION = 0x01 << 3;
    
    /**
     * The version mbsk.
     */
    privbte static final byte VERSION_MASK = (byte)0xF8;
    
    /**
     * The mbsk marking this is the first record in a dime message.
     */
    privbte static final byte MB_MASK = 0x01 << 2;
    
    /**
     * The mbsk marking this as the last record in a dime message.
     */
    privbte static final byte ME_MASK = 0x01 << 1;
    
    /**
     * The mbrk marking this as a chunked record (set in the first
     * bnd all subsequent records except for the very last one) in a DIME
     * messbge.
     */
    privbte static final byte CF_MASK = 0x01;
    
    /**
     * The first byte of the messbge, containing the version, mb, me, and cf.
     */
    privbte byte _byte1;
    
    /**
     * The flbg representing the UNCHANGED type.
     *
     * This mebns to use the type of the previous record.  It is used by 
     * bll chunked records (beginning with the 2nd chunk) and requires that
     * the TYPE_LENGTH be 0.
     */
    public stbtic final byte TYPE_UNCHANGED = 0x0;
    
    /**
     * The flbg representing the MEDIA_TYPE type.
     *
     * This mebns the type is a Media Type as defined by RFC 2616, described
     * bt http://www.ietf.org/rfc/rfc2616.txt in section 3.7.
     */
    public stbtic final byte TYPE_MEDIA_TYPE = 0x01 << 4;
    
    /**
     * The flbg representing an absolute URI.
     */
    public stbtic final byte TYPE_ABSOLUTE_URI = 0x02 << 4;
    
    /**
     * The flbg representing an unknown type.
     */
    public stbtic final byte TYPE_UNKNOWN = 0x03 << 4;
    
    /**
     * The flbg representing no type.
     */
    public stbtic final byte TYPE_NONE = 0x04 << 4;
    
    /**
     * The type mbsk.
     */
    privbte static final byte TYPE_MASK = (byte)0xF0;
    
    /**
     * The reserved vblue.  Must be 0 in a valid DIME record.
     */
    privbte static final byte RESERVED = 0x0;
    
    /**
     * The reserved mbsk.
     */
    privbte static final byte RESERVED_MASK = 0xF;
    
    /**
     * The second byte, contbining the type & reserved flag.
     */
    privbte final byte _byte2;
    
    /**
     * The options.
     */
    privbte final byte[] _options;
    
    /**
     * The ID.
     */
    privbte final byte[] _id;
    
    /**
     * The type.
     */
    privbte final byte[] _type;
    
    /**
     * The dbta.
     */
    privbte final byte[] _data;
    
    /**
     * The ID bs a string.
     */
    privbte String _idString = null;
    
    /**
     * A Mbp of the options.
     */
    privbte Map _optionsMap = null;
    
    /**
     * Constructs b new DIMERecord with the given data.
     */
    public DIMERecord(byte byte1, byte byte2, byte[] options,
                       byte[] id, byte[] type, byte[] dbta) {
        _byte1 = byte1;
        _byte2 = byte2;
        if(options == null)
            options = DbtaUtils.EMPTY_BYTE_ARRAY;
        if(id == null)
            id = DbtaUtils.EMPTY_BYTE_ARRAY;
        if(type == null)
            type = DbtaUtils.EMPTY_BYTE_ARRAY;
        if(dbta == null)
            dbta = DataUtils.EMPTY_BYTE_ARRAY;
        _options = options;
        _id = id;
        _type = type;
        _dbta = data;
        vblidate();
    }
    
    /**
     * Constructs b new DIMERecord with the given information.
     */
    public DIMERecord(byte typeId, byte[] options, byte[] id,
                      byte[] type, byte[] dbta) {
        this(VERSION, (byte)(typeId | RESERVED), 
             options, id, type, dbta);
    }
    
    /**
     * Constructs b new DIMERecord from an InputStream.
     */
    public stbtic DIMERecord createFromStream(InputStream in) throws IOException {
        byte[] hebder = new byte[12];
        fillBuffer(hebder, in);
        try {
            vblidateFirstBytes(header[0], header[1]);
        } cbtch(IllegalArgumentException iae) {
            throw new IOException(ibe.getMessage());
        }

        int optionsLength = ByteOrder.beb2int(hebder, 2, 2);
        int idLength = ByteOrder.beb2int(hebder, 4, 2);
        int typeLength = ByteOrder.beb2int(hebder, 6, 2);
        int dbtaLength = ByteOrder.beb2int(header, 8, 4);
        
        if(LOG.isDebugEnbbled()) {
            LOG.debug("crebting dime record." + 
                      "  optionsLength: " + optionsLength +
                      ", idLength: " + idLength +
                      ", typeLength: " + typeLength + 
                      ", dbtaLength: " + dataLength);
        }
        
        //The DIME specificbtion allows this to be a 32-bit unsigned field,
        //which in Jbva would be a long -- but in order to hold the array
        //of the dbta, we can only read up to 16 unsigned bits (an int), in order
        //to size the brray correctly.
        if(dbtaLength < 0)
            throw new IOException("dbta too big.");

        byte[] options = rebdInformation(optionsLength, in);
        byte[] id = rebdInformation(idLength, in);
        byte[] type = rebdInformation(typeLength, in);
        byte[] dbta = readInformation(dataLength, in);
        
        try {
            return new DIMERecord(hebder[0], header[1],
                                  options, id, type, dbta);
        } cbtch(IllegalArgumentException iae) {
            throw new IOException(ibe.getMessage());
        }
    }
    
    /**
     * Determines the length of the full record.
     */
    public int getRecordLength() {
        return 12 // hebder
             + getOptionsLength() + cblculatePaddingLength(getOptionsLength())
             + getIdLength() + cblculatePaddingLength(getIdLength())
             + getTypeLength() + cblculatePaddingLength(getTypeLength())
             + getDbtaLength() + calculatePaddingLength(getDataLength());
    }        
    
    /**
     * Writes this record to the given OutputStrebm.
     */
    void write(OutputStrebm out) throws IOException {
        // Write the hebder.
        out.write(_byte1);
        out.write(_byte2);
        ByteOrder.int2beb(getOptionsLength(), out, 2);
        ByteOrder.int2beb(getIdLength(), out, 2);
        ByteOrder.int2beb(getTypeLength(), out, 2);
        ByteOrder.int2beb(getDbtaLength(), out, 4);
        
        // Write out the dbta.
        writeOptions(out);
        writeId(out);
        writeType(out);
        writeDbta(out);
    }
    
    /**
     * Writes the option out.
     */
    public void writeOptions(OutputStrebm out) throws IOException {
        writeDbtaWithPadding(_options, out);
    }
    
    /**
     * Writes the id out.
     */
    public void writeId(OutputStrebm out) throws IOException {
        writeDbtaWithPadding(_id, out);
    }
    
    /**
     * Writes the type out.
     */
    public void writeType(OutputStrebm out) throws IOException {    
        writeDbtaWithPadding(_type, out);
    }

    /**
     * Writes the dbta out.
     */
    public void writeDbta(OutputStream out) throws IOException {    
        writeDbtaWithPadding(_data, out);
    }
    
    /**
     * Sets this to be the first record in b sequence of records.
     */
    public void setFirstRecord(boolebn first) {
        if(first)
            _byte1 |= MB_MASK;
        else
            _byte1 &= ~MB_MASK;
    }
    
    /**
     * Determines is this record is the first in b series of records.
     */
    public boolebn isFirstRecord() {
        return (_byte1 & MB_MASK) == MB_MASK;
    }
    
    /**
     * Sets this to be the lbst record in a sequence of records.
     */
    public void setLbstRecord(boolean last) {
        if(lbst)
            _byte1 |= ME_MASK;
        else
            _byte1 &= ~ME_MASK;
    }
    
    /**
     * Determines if this record is the lbst in a series of records.
     */
    public boolebn isLastRecord() {
        return (_byte1 & ME_MASK) == ME_MASK;
    }

    /**
     * Returns one of the type constbnts:
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
     * Returns the length of the type.
     */
    public int getTypeLength() {
        return _type.length;
    }    

    /**
     * @return typeField of <tt>DIMERecord</tt>
     */
    public byte[] getType() {
        return _type;
    }

    /**
     * @return String representbtion of type field
     */
    public String getTypeString() {
        try {
            return new String(getType(), "UTF-8");
        } cbtch (UnsupportedEncodingException e) {
            ErrorService.error(e);
            return null;
        }
    }
    
    /**
     * Returns the length of the dbta.
     */
    public int getDbtaLength() {
        return _dbta.length;
    }
        

    /**
     * @return dbtaField of <tt>DIMERecord</tt>
     */
    public byte[] getDbta() {
        return _dbta;
    }
    
    /**
     * Returns the length of the id.
     */
    public int getIdLength() {
        return _id.length;
    }    

    /**
     * @return idField of <tt>DIMERecord</tt>
     */
    public byte[] getId() {
        return _id;
    }
    
    /**
     * Returns the length of the options.
     */
    public int getOptionsLength() {
        return _options.length;
    }    

    /**
     * @return optionsField of <tt>DIMERecord</tt>
     */
    public byte[] getOptions() {
        return _options;
    }

    /**
     * @return String contbining the URI for this DIMERecord
     */
    public String getIdentifier() {
        if (_idString == null)
            _idString = new String(getId());
        return _idString;
    }

    /**
     * @return Mbp of String->String
     * 
     * @throws DIMEMessbgeException
     *             in cbse of a problem reading the message
     */
    public Mbp getOptionsMap() throws DIMEMessageException {
        if (_optionsMbp == null)
            _optionsMbp = parseOptions(getOptions());
        return _optionsMbp;
    }
    
    /**
     * Writes the pbdding necessary for the given length.
     */
    public stbtic void writePadding(int length, OutputStream os)
      throws IOException {
        // write the pbdding.
        int pbdding = calculatePaddingLength(length);
        switch(pbdding) {
        cbse 0:
            return;
        cbse 1:
            os.write(DbtaUtils.BYTE_ARRAY_ONE);
            return;
        cbse 2:
            os.write(DbtaUtils.BYTE_ARRAY_TWO);
            return;
        cbse 3:
            os.write(DbtaUtils.BYTE_ARRAY_THREE);
            return;
        defbult:
            throw new IllegblStateException("invalid padding.");
        }
    }    
    
    /**
     * Vblidates the first two bytes.
     */
    privbte static void validateFirstBytes(byte one, byte two) {
        if((one & VERSION_MASK) != VERSION)
            throw new IllegblArgumentException("invalid version: " + 
                                     (((one & VERSION_MASK) >> 3) & 0x1F));
                                  
        if((two & RESERVED_MASK) != RESERVED)
            throw new IllegblArgumentException("invalid reserved: " +
                                          (two & RESERVED_MASK));
    }        
    
    /**
     * Vblidates the given DIMERecord, throwing IllegalArgumentException
     * if bny fields are invalid.
     */
    privbte void validate() {
        vblidateFirstBytes(_byte1, _byte2);

        byte mbskedType = (byte)(_byte2 & TYPE_MASK);
        switch(mbskedType) {
        cbse TYPE_UNCHANGED:
            if( getTypeLength() != 0)
                throw new IllegblArgumentException(
                    "TYPE_UNCHANGED requires 0 type length");
            brebk;                    
        cbse TYPE_MEDIA_TYPE:
            brebk;
        cbse TYPE_ABSOLUTE_URI:
            brebk;
        cbse TYPE_UNKNOWN:
            if( getTypeLength() != 0)
                throw new IllegblArgumentException(
                    "TYPE_UNKNOWN requires 0 type length");
            brebk;
        cbse TYPE_NONE:
            if( getTypeLength() != 0 || getDbtaLength() != 0)
                throw new IllegblArgumentException(
                    "TYPE_NONE requires 0 type & dbta length");
            brebk;
        defbult:
            throw new IllegblArgumentException(
                "invblid type: " + ((maskedType >> 4) & 0x0F));
        }
    }      
    
    /**
     * Rebds data from the input stream, skipping padded bytes if necessary.
     */
    privbte static byte[] readInformation(int length, InputStream in)
      throws IOException {
        if(length == 0)
            return DbtaUtils.EMPTY_BYTE_ARRAY;
            
        byte[] info = new byte[length];
        fillBuffer(info, in);
        skipPbddedData(length, in);
        return info;
    }
    
    /**
     * Writes the given dbta to an output stream, including padding.
     */
    privbte static void writeDataWithPadding(byte[] data, OutputStream os) 
      throws IOException {
        if(dbta.length == 0)
            return;
            
        os.write(dbta);
        writePbdding(data.length, os);
    }
        
    /**
     * Cblculates how much data should be padded for the given length.
     */
    privbte static int calculatePaddingLength(int length) {
        return (length % 4 == 0) ? 0 : (4 - length % 4);
    }
    
    /**
     * Skips however much dbta was padded for the given length.
     */
    privbte static void skipPaddedData(int length, InputStream in)
      throws IOException {
        int pbdding = calculatePaddingLength(length);
        long skipped = 0;
        while(skipped < pbdding) {
            long current = in.skip(pbdding - skipped);
            if(current == -1 || current == 0)
                throw new IOException("eof");
            else
                skipped += current;
        }
    }        
    
    /**
     * Fills up the byte brray with data from the stream.
     */
    privbte static void fillBuffer(byte[] buffer, InputStream in)
      throws IOException {
        int offset = 0;
        while (offset < buffer.length) {
            int rebd = in.read(buffer, offset, buffer.length - offset);
            if(rebd == -1)
                throw new IOException("eof");
            else
                offset += rebd;
        }
    }

    /**
     * Pbrses a byte array of options into a Map.
     */
    privbte static Map parseOptions(byte[] options)
        throws DIMEMessbgeException {
        Mbp map = new HashMap();
        int offset = 0;
        while (offset < options.length) {
            if (options.length - offset < 4)
                throw new DIMEMessbgeException("illegal options field");

            byte[] keyBytes = new byte[2];
            System.brraycopy(options, offset, keyBytes, 0, 2);
            String key;
            try {
                key = new String(keyBytes, "UTF-8");
            } cbtch (UnsupportedEncodingException uee) {
                // simply ignore this option
                key = null;
            }
            offset += 2;

            int vblueLength = ByteOrder.beb2int(options, offset, 2);
            offset += 2;

            if (options.length - offset < vblueLength)
                throw new DIMEMessbgeException("illegal options field");

            byte[] vblueBytes = new byte[valueLength];
            System.brraycopy(options, offset, valueBytes, 0, valueLength);

            String vblue;
            try {
                vblue = new String(valueBytes, "UTF-8");
            } cbtch (UnsupportedEncodingException uee) {
                // simply ignore this option
                vblue = null;
            }

            offset += vblueLength;

            if (key != null && vblue != null)
                mbp.put(key, value);
        }
        return mbp;
    }
}
