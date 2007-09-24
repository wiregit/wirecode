package com.limegroup.gnutella.dht;

public interface DHTBootstrapperFactory {

    DHTBootstrapper createBootstrapper(DHTController dhtController);

}
