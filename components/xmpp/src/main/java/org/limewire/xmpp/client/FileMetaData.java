package org.limewire.xmpp.client;

import java.util.Date;

/**
 * The file meta-data necessary to do a jingle file exchange
 */
public interface FileMetaData {

    /**
     * @return the unique id of the file, i.e., its sha1
     */
    String getId();

    /**
     * @return  the file name
     */
    String getName();

    /**
     * the size of the file in bytes
     * @return
     */
    long getSize();

    /**
     * @return the last modified time of the file
     */
    Date getDate();

    /**
     * @return the description of the file, as input by the user
     */
    String getDescription();
}
