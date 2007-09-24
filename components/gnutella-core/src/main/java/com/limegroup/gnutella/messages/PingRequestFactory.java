package com.limegroup.gnutella.messages;


public interface PingRequestFactory {

    public PingRequest createPingRequest(byte[] guid, byte ttl, byte hops);

    public PingRequest createPingRequest(byte[] guid, byte ttl, byte hops,
            byte length);

    public PingRequest createPingRequest(byte[] guid, byte ttl, byte hops,
            byte[] payload);

    public PingRequest createPingRequest(byte ttl);

    public PingRequest createPingRequest(byte[] guid, byte ttl);

    /**
     * Creates a Query Key ping.
     */
    public PingRequest createQueryKeyRequest();

    /**
     * Creates a TTL 1 Ping for faster bootstrapping, intended
     * for sending to UDP hosts.
     */
    public PingRequest createUDPPing();

    /**
     * Creates a TTL 1 Ping to request DHT nodes, intended
     * for sending to UDP hosts. 
     */
    public PingRequest createUDPingWithDHTIPPRequest();

    /**
     * Creates a TTL 1 Ping for faster bootstrapping, intended
     * for sending to UHCs.
     */
    public PingRequest createUHCPing();

    /**
     * Creates a TTL 1 Ping for faster bootstrapping, intended
     * for sending to the multicast network.
     */
    public PingRequest createMulticastPing();

}