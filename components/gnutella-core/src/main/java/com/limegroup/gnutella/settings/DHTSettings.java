package com.limegroup.gnutella.settings;

public class DHTSettings extends LimeProps {

    private DHTSettings() {}
    
    public static final StringSetting DHT_BOOTSTRAP_SEED = 
        FACTORY.createStringSetting("DHT_BOOTSTRAP_SEED","dht.aelitis.com");
    
    public static final IntSetting DHT_BOOTSTRAP_PORT = 
        FACTORY.createIntSetting("DHT_BOOTSTRAP_PORT",6881);
    
    public static final IntSetting DHT_SEND_DELAY = 
        FACTORY.createIntSetting("DHT_SEND_DELAY",50);
    
    public static final IntSetting DHT_RECEIVE_DELAY = 
        FACTORY.createIntSetting("DHT_RECEIVE_DELAY",25);
    
    public static final BooleanSetting DHT_BOOTSTRAPNODE = 
        FACTORY.createBooleanSetting("DHT_BOOTSTRAPNODE",false);
    
    public static final BooleanSetting DHT_REACHABLE =
        FACTORY.createBooleanSetting("DHT_REACHABLE",true);
    
    public static final IntSetting DHT_UDP_WARN_PORT = 
        FACTORY.createIntSetting("DHT_UDP_WARN_PORT",0);
    
    public static final BooleanSetting IP_FILTER_PERSISTENT =
        FACTORY.createBooleanSetting("IP_FILTER_PERSISTENT",true);
    
    public static final BooleanSetting IP_FILTER_ENABLED =
        FACTORY.createBooleanSetting("IP_FILTER_ENABLED",true);
    
    public static final BooleanSetting IP_FILTER_ALLOW_RANGE =
        FACTORY.createBooleanSetting("IP_FILTER_ALLOW_RANGE",false);
    
    public static final IntSetting IP_FILTER_BAN_BLOCK_LIMIT =
        FACTORY.createIntSetting("IP_FILTER_BAN_BLOCK_LIMIT",4);
    
    public static final StringSetting DHT_BIND_IP = 
        FACTORY.createStringSetting("DHT_BIND_IP","");
}
