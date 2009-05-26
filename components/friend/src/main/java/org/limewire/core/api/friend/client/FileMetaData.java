package org.limewire.core.api.friend.client;

import java.util.Date;
import java.util.Set;

/**
 * The file meta-data necessary to do a file exchange
 */
public interface FileMetaData {

    /**
     * @return null if not set
     */
    public String getId();
    /**
     * @return not null
     */
    public String getName();
    public long getSize();
    /**
     * @return null if not seet
     */
    public String getDescription();
    public long getIndex();
    /**
     * @return not null, mandatory
     */
    public Set<String> getUrns();
    /**
     * @return not null, mandatory
     */
    public Date getCreateTime();
    public String toXML();
}
