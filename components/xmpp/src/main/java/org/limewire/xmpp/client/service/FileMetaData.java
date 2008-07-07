package org.limewire.xmpp.client.service;

import java.util.Date;

/**
 * The file meta-data necessary to do a jingle file exchange
 */
public interface FileMetaData {

    /**
     * @return the unique id of the file, i.e., its sha1
     */
    public String getId();

    /**
     * @return  the file name
     */
    public String getName();

    /**
     * the size of the file in bytes
     * @return
     */
    public long getSize();

    /**
     * @return the last modified time of the file
     */
    public Date getDate();

    /**
     * @return the description of the file, as input by the user
     */
    public String getDescription();
}
