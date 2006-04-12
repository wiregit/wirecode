package de.kapsi.net.kademlia.settings;


public class KademliaSettings extends LimeDHTProps {
    
    private KademliaSettings() {}
    
    /**
     * The replication parameter is also known as K
     */
    public static final IntSetting REPLICATION_PARAMETER
        = FACTORY.createIntSetting("REPLICATION_PARAMETER", 20);
    /**
     * The number of parallel lookups
     */
    public static final IntSetting LOOKUP_PARAMETER
        = FACTORY.createIntSetting("LOOKUP_PARAMETER", 5);
    /**
     * The FIND_NODE lookup timeout
     */
    public static final LongSetting NODE_LOOKUP_TIMEOUT
        = FACTORY.createLongSetting("NODE_LOOKUP_TIMEOUT",15L*1000L);
    /**
     * The FIND_VALUE lookup timeout
     */
    public static final LongSetting VALUE_LOOKUP_TIMEOUT
    = FACTORY.createLongSetting("NODE_LOOKUP_TIMEOUT",30L*1000L);

}
