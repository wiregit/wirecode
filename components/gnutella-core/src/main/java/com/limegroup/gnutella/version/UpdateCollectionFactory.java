package com.limegroup.gnutella.version;

import org.limewire.io.InvalidDataException;

public interface UpdateCollectionFactory {

    public UpdateCollection createUpdateCollection(String xml) throws InvalidDataException;

}