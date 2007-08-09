package com.limegroup.gnutella.dht;

public interface DHTNodeFetcherFactory {

    DHTNodeFetcher createNodeFetcher(DHTBootstrapper dhtBootstrapper);

}
