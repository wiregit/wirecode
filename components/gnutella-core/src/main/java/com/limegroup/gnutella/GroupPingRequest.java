package com.limegroup.gnutella;

import java.io.*;

/**
 * A Gnutella special case group ping message used for getting information about
 * a special group.
 */

public class GroupPingRequest extends PingRequest implements Serializable{

    /** All the data.  We extract the port, ip address and group name lazily */
    private byte[] payload;

	/**
	 *  Build a GroupPingRequest with the following structure:<br>
	 *  Generic Message Header        <br>
	 *  Port  (2 bytes)               <br>
	 *  IP    (4 bytes)               <br>
	 *  files (4 bytes, unsigned little endian)               <br>
	 *  kbytes(4 bytes, unsigned little endian)               <br>
	 *  Group (string length bytes)   <br>
	 *  null terminator               <br>
	 */
    public GroupPingRequest(byte ttl, int port, byte[] ip, 
	  long files, long kbytes, String group) 
	{
        super(ttl, (byte)(14+group.length()+1));
    
        payload=new byte[14+group.length()+1];

        // Stick port in first 2 bytes
        ByteOrder.short2leb((short)port, payload, 0);

        // Payload stores IP in BIG-ENDIAN  (4 bytes)
        payload[2]=ip[0];
        payload[3]=ip[1];
        payload[4]=ip[2];
        payload[5]=ip[3];

		// Ping Reply Information
        ByteOrder.int2leb((int)files, payload, 6);
        ByteOrder.int2leb((int)kbytes, payload, 10);

        //Copy bytes from group string to payload
        byte[] gbytes=group.getBytes();
        System.arraycopy(gbytes,0,payload,14,gbytes.length);

        //Null terminate it.
        payload[payload.length-1]=(byte)0;
    }

    /*
     * Build a reconstituted GroupPingRequest with data snatched from network
     *
     * @requires payload.length >= 15
     */
    public GroupPingRequest(byte[] guid, byte ttl, byte hops,
            byte[] payload) {
        super(guid, ttl, hops, (byte)payload.length);
		Assert.that(payload.length>=15);	
        this.payload=payload;
    }

	/**
	 *  Write out the "port, IP, Group" payload
	 */
    public void writePayload(OutputStream out) throws IOException{
        out.write(payload);
    }

    public int getPort() {
        return  ByteOrder.ubytes2int(ByteOrder.leb2short(payload,0));
    }

    /**
     * Returns the ip field in standard dotted decimal format, e.g.,
     * "127.0.0.1".  The most significant byte is written first.
     */
    public String getIP() {
        byte[] ip=getAddress();
        String ret=ip2string(ip); //takes care of signs
        return ret;
    }

    /**
     * Returns the ip field in bytes
     */
    public byte[] getAddress() {
        byte[] ip=new byte[4];
        ip[0]=payload[2];
        ip[1]=payload[3];
        ip[2]=payload[4];
        ip[3]=payload[5];
        return ip;
    }

    public long getFiles() {
        return ByteOrder.ubytes2long(ByteOrder.leb2int(payload,6));
    }

    public long getKbytes() {
        return ByteOrder.ubytes2long(ByteOrder.leb2int(payload,10));
    }

    public String getGroup() {
		// Note: We don't check the last byte to ensure null terminator
        return new String(payload,14,payload.length-15);
    }

	public boolean equals(Object obj) {
		if ( ! (obj instanceof GroupPingRequest) )
			return false;

		GroupPingRequest gpr = (GroupPingRequest) obj;
		byte [] myip = getAddress();
		byte [] ip   = gpr.getAddress();

		return( myip[0] == ip[0] && 
				myip[1] == ip[1] && 
				myip[2] == ip[2] && 
				myip[3] == ip[3] && 
				getPort() == gpr.getPort() );
	}

    public String toString() {
        return "GroupPingRequest("+getIP()+":"+getPort()+","+getGroup()
            +", "+super.toString()+")";
    }

}
