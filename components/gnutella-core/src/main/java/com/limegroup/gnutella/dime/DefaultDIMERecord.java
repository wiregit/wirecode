package com.limegroup.gnutella.dime;

/**
 * @author Gregorio roper
 */
public class DefaultDIMERecord extends AbstractDIMERecord {

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
    public DefaultDIMERecord(
        byte[] header,
        byte[] options,
        byte[] id,
        byte[] type,
        byte[] data) {
        super(header, options, id, type, data);
    }
}
