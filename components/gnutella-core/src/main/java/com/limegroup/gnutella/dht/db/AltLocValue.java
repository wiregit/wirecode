package com.limegroup.gnutella.dht.db;

import org.limewire.io.GUID;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.storage.ValueType;

import com.limegroup.gnutella.altlocs.AlternateLocation;

public interface AltLocValue extends SerializableValue {

    /**
     * {@link ValueType} for {@link AlternateLocation}s.
     */
    public static final ValueType ALT_LOC 
        = ValueType.valueOf("Gnutella Alternate Location", "ALOC");

    public static final Version VERSION_ONE = Version.valueOf(1);

    /**
     * Version of {@link AltLocValue}.
     */
    public static final Version VERSION = VERSION_ONE;

    /**
     * Returns the value's {@link Version}
     */
    public Version getVersion();

    /**
     * The {@link GUID} of the AltLoc.
     */
    public byte[] getGUID();

    /**
     * The (Gnutella) Port of the AltLoc.
     */
    public int getPort();

    /**
     * The length of the file.
     */
    public long getFileSize();

    /**
     * The TigerTree root hash.
     */
    public byte[] getRootHash();

    /**
     * Returns true if the AltLoc is firewalled.
     */
    public boolean isFirewalled();

    /**
     * @return true if the alternative location supports TLS
     */
    public boolean supportsTLS();

}