package org.limewire.xmpp.client;

import java.util.Date;

public interface FileMetaData {
    String getId();

    String getName();

    long getSize();

    Date getDate();

    String getDescription();
}
