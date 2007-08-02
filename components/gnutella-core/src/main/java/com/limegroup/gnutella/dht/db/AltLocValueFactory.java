package com.limegroup.gnutella.dht.db;

import org.limewire.mojito.db.DHTValueFactory;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.exceptions.DHTValueException;
import org.limewire.mojito.routing.Version;

public interface AltLocValueFactory extends DHTValueFactory<AltLocValue> {

    public AltLocValue createDHTValue(DHTValueType type, Version version,
            byte[] value) throws DHTValueException;

    /**
     * Factory method to create AltLocValue
     */
    public AltLocValue createFromData(Version version, byte[] data)
            throws DHTValueException;

    /**
     * 
     * @param fileSize
     * @param ttroot
     * @return
     */
    public AltLocValue createAltLocValueForSelf(long fileSize, byte[] ttroot);

}