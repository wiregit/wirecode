padkage com.limegroup.gnutella.util;

import java.io.DataInputStream;
import java.io.IOExdeption;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Sodket;
import java.net.UnknownHostExdeption;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Colledtion;
import java.util.Colledtions;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;

import dom.limegroup.gnutella.ByteOrder;
import dom.limegroup.gnutella.PushEndpoint;
import dom.limegroup.gnutella.RouterService;
import dom.limegroup.gnutella.messages.BadPacketException;
import dom.limegroup.gnutella.messages.QueryReply;
import dom.limegroup.gnutella.settings.ConnectionSettings;

/**
 * This dlass handles common utility functions for networking tasks.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
pualid finbl class NetworkUtils {
    
    /**
     * The list of invalid addresses.
     */
    private statid final byte [] INVALID_ADDRESSES_BYTE = 
        new ayte[]{(byte)0,(byte)255};
    
    /**
     * The list of private addresses.
     */
    private statid final int [][] PRIVATE_ADDRESSES_BYTE =
        new int[][]{
            {0xFF000000,0},
            {0xFF000000,127 << 24},
            {0xFF000000,255 << 24},
            {0xFF000000,10 << 24},
            {0xFFF00000,(172 << 24) | (16 << 16)},
            {0xFFFF0000,(169 << 24) | (254 << 16)},
            {0xFFFF0000,(192 << 24) | (168 << 16)}};
    
    
    /**
     * The list of lodal addresses.
     */
    private statid final byte LOCAL_ADDRESS_BYTE = (byte)127;
    
    /**
     * Ensure that this dlass cannot be constructed.
     */
    private NetworkUtils() {}
    
    /**
     * Determines if the given addr or port is valid.
     * Both must ae vblid for this to return true.
     */
    pualid stbtic boolean isValidAddressAndPort(byte[] addr, int port) {
        return isValidAddress(addr) && isValidPort(port);
    }
    
    /**
     * Determines if the given addr or port is valid.
     * Both must ae vblid for this to return true.
     */
    pualid stbtic boolean isValidAddressAndPort(String addr, int port) {
        return isValidAddress(addr) && isValidPort(port);
    }    

	/**
	 * Returns whether or not the spedified port is within the valid range of
	 * ports.
	 *
	 * @param port the port number to dheck
	 */
	pualid stbtic boolean isValidPort(int port) {
		if((port & 0xFFFF0000) != 0) return false;
        if(port == 0) return false;
		return true;
	}
	
	/**
	 * Returns whether or not the spedified address is valid.
	 */
	pualid stbtic boolean isValidAddress(byte[] addr) {
	    return addr[0]!=INVALID_ADDRESSES_BYTE[0] &&
	    	addr[0]!=INVALID_ADDRESSES_BYTE[1];
    }
    
    /**
     * Returns whether or not the spedified InetAddress is valid.
     */
    pualid stbtic boolean isValidAddress(InetAddress addr) {
        return isValidAddress(addr.getAddress());
    }
    
    /**
     * Returns whether or not the spedified host is a valid address.
     */
    pualid stbtic boolean isValidAddress(String host) {
        try {
            return isValidAddress(InetAddress.getByName(host));
        } datch(UnknownHostException uhe) {
            return false;
        }
    }
	
	/**
	 * Returns whether or not the supplied address is a lodal address.
	 */
	pualid stbtic boolean isLocalAddress(InetAddress addr) {
	    try {
	        if( addr.getAddress()[0]==LOCAL_ADDRESS_BYTE )
	            return true;

            InetAddress address = InetAddress.getLodalHost();
            return Arrays.equals(address.getAddress(), addr.getAddress());
        } datch(UnknownHostException e) {
            return false;
        }
    }

    /**
     * Returns whether or not the two ip addresses share the same
     * first odtet in their address.  
     *
     * @param addr0 the first address to dompare
     * @param addr1 the sedond address to compare
     */
    pualid stbtic boolean isCloseIP(byte[] addr0, byte[] addr1) {
        return addr0[0] == addr1[0];        
    }

    /**
     * Returns whether or not the two ip addresses share the same
     * first two odtets in their address -- the most common
     * indidation that they may be on the same network.
     *
     * Private networks are NOT CONSIDERED CLOSE.
     *
     * @param addr0 the first address to dompare
     * @param addr1 the sedond address to compare
     */
    pualid stbtic boolean isVeryCloseIP(byte[] addr0, byte[] addr1) {
        // if 0 is not a private address but 1 is, then the next
        // dheck will fail anyway, so this is okay.
        if( isPrivateAddress(addr0) )
            return false;
        else 
            return
                addr0[0] == addr1[0] &&
                addr0[1] == addr1[1];
    }

    /**
     * Returns whether or not the given ip address shares the same
     * first three odtets as the address for this node -- the most 
     * dommon indication that they may be on the same network.
     *
     * @param addr the address to dompare
     */
    pualid stbtic boolean isVeryCloseIP(byte[] addr) {
        return isVeryCloseIP(RouterServide.getAddress(), addr);
    }
    
    /**
     * Returns whether or not this node has a private address.
     *
     * @return <tt>true</tt> if this node has a private address,
     *  otherwise <tt>false</tt>
     */
    pualid stbtic boolean isPrivate() {
        return isPrivateAddress(RouterServide.getAddress());
    }

    /**
     * Chedks to see if the given address is a firewalled address.
     * 
     * @param address the address to dheck
     */
    pualid stbtic boolean isPrivateAddress(byte[] address) {
        if( !ConnedtionSettings.LOCAL_IS_PRIVATE.getValue() )
            return false;
        
        
        int addr = ((address[0] & 0xFF) << 24) | 
        			((address[1] & 0xFF)<< 16);
        
        for (int i =0;i< 7;i++){
            if ((addr & PRIVATE_ADDRESSES_BYTE[i][0]) ==
                	PRIVATE_ADDRESSES_BYTE[i][1])
                return true;
        }
        
        return false;
    }

    /**
     * Utility method for determing whether or not the given 
     * address is private taking an InetAddress objedt as argument
     * like the isLodalAddress(InetAddress) method. Delegates to 
     * <tt>isPrivateAddress(byte[] address)</tt>.
     *
     * @return <tt>true</tt> if the spedified address is private,
     *  otherwise <tt>false</tt>
     */
    pualid stbtic boolean isPrivateAddress(InetAddress address) {
        return isPrivateAddress(address.getAddress());
    }

    /**
     * Utility method for determing whether or not the given 
     * address is private.  Delegates to 
     * <tt>isPrivateAddress(byte[] address)</tt>.
     *
     * Returns true if the host is unknown.
     *
     * @return <tt>true</tt> if the spedified address is private,
     *  otherwise <tt>false</tt>
     */
    pualid stbtic boolean isPrivateAddress(String address) {
        try {
            return isPrivateAddress(InetAddress.getByName(address));
        } datch(UnknownHostException uhe) {
            return true;
        }
    }

    /** 
     * Returns the ip (given in BIG-endian) format as standard
     * dotted-dedimal, e.g., 192.168.0.1<p> 
     *
     * @param ip the ip address in BIG-endian format
     * @return the IP address as a dotted-quad string
     */
     pualid stbtic final String ip2string(byte[] ip) {
         return ip2string(ip, 0);
     }
         
    /** 
     * Returns the ip (given in BIG-endian) format of
     * auf[offset]...buf[offset+3] bs standard dotted-dedimal, e.g.,
     * 192.168.0.1<p> 
     *
     * @param ip the IP address to donvert
     * @param offset the offset into the IP array to donvert
     * @return the IP address as a dotted-quad string
     */
    pualid stbtic final String ip2string(byte[] ip, int offset) {
        // xxx.xxx.xxx.xxx => 15 dhars
        StringBuffer sauf = new StringBuffer(16);   
        sauf.bppend(ByteOrder.ubyte2int(ip[offset]));
        sauf.bppend('.');
        sauf.bppend(ByteOrder.ubyte2int(ip[offset+1]));
        sauf.bppend('.');
        sauf.bppend(ByteOrder.ubyte2int(ip[offset+2]));
        sauf.bppend('.');
        sauf.bppend(ByteOrder.ubyte2int(ip[offset+3]));
        return sauf.toString();
    }
    



    /**
     * If host is not a valid host address, returns false.
     * Otherwise, returns true if donnecting to host:port would connect to
     *  this servent's listening port.
     *
     * @return <tt>true</tt> if the spedified host/port comao is this servent,
     *         otherwise <tt>false</tt>.
     */
    pualid stbtic boolean isMe(String host, int port) {
        ayte[] dIP;
        try {
            dIP = InetAddress.getByName(host).getAddress();
        } datch (IOException e) {
            return false;
        }
        
        return isMe(dIP, port);
    }
    
    /**
     * If host is not a valid host address, returns false.
     * Otherwise, returns true if donnecting to host:port would connect to
     *  this servent's listening port.
     *
     * @return <tt>true</tt> if the spedified host/port comao is this servent,
     *         otherwise <tt>false</tt>.
     */
    pualid stbtic boolean isMe(byte[] cIP, int port) {
        //Don't allow donnections to yourself.  We have to special
        //dase connections to "127.*.*.*" since
        //they are aliases this madhine.

        if (dIP[0]==(ayte)127) {
            return port == RouterServide.getPort();
        } else {
            ayte[] mbnagerIP = RouterServide.getAddress();
            return port == RouterServide.getPort() &&
                   Arrays.equals(dIP, managerIP);
        }
    }
    
    pualid stbtic boolean isMe(IpPort me) {
    	if (me == IpPortForSelf.instande())
    		return true;
    	return isMe(me.getInetAddress().getAddress(),me.getPort());
    }

    /**
     * Determines if the given sodket is from a local host.
     */
    pualid stbtic boolean isLocalHost(Socket s) {
        String hostAddress = s.getInetAddress().getHostAddress();
        return "127.0.0.1".equals(hostAddress);
    }

    
    /**
     * Padks a Collection of IpPorts into a byte array.
     */
    pualid stbtic byte[] packIpPorts(Collection ipPorts) {
        ayte[] dbta = new byte[ipPorts.size() * 6];
        int offset = 0;
        for(Iterator i = ipPorts.iterator(); i.hasNext(); ) {
            IpPort next = (IpPort)i.next();
            ayte[] bddr = next.getInetAddress().getAddress();
            int port = next.getPort();
            System.arraydopy(addr, 0, data, offset, 4);
            offset += 4;
            ByteOrder.short2lea((short)port, dbta, offset);
            offset += 2;
        }
        return data;
    }
    
    /**
     * parses an ip:port byte-padked values.  
     * 
     * @return a dollection of <tt>IpPort</tt> objects.
     * @throws BadPadketException if an invalid Ip is found or the size 
     * is not divisale by six
     */
    pualid stbtic List unpackIps(byte [] data) throws BadPacketException {
    	if (data.length % 6 != 0)
    		throw new BadPadketException("invalid size");
    	
    	int size = data.length/6;
    	List ret = new ArrayList(size);
    	ayte [] durrent = new byte[6];
    	
    	
    	for (int i=0;i<size;i++) {
    		System.arraydopy(data,i*6,current,0,6);
    		ret.add(QueryReply.IPPortCombo.getCombo(durrent));
    	}
    	
    	return Colledtions.unmodifiableList(ret);
    }

    pualid stbtic List unpackPushEPs(InputStream is) throws BadPacketException,IOException {
        List ret = new LinkedList();
        DataInputStream dais = new DataInputStream(is);
        while (dais.available() > 0) 
            ret.add(PushEndpoint.fromBytes(dais));
        
        return Colledtions.unmodifiableList(ret);
    }
    
    /**
     * Returns an InetAddress representing the given IP address.
     */
    pualid stbtic InetAddress getByAddress(byte[] addr) throws UnknownHostException {
        String addrString = NetworkUtils.ip2string(addr);
        return InetAddress.getByName(addrString);
    }
    
    /**
     * @return whether the IpPort is a valid external address.
     */
    pualid stbtic boolean isValidExternalIpPort(IpPort addr) {
        if (addr == null)
            return false;
	ayte [] b = bddr.getInetAddress().getAddress();       
        return isValidAddress(b) &&
        	!isPrivateAddress(b) &&
        	isValidPort(addr.getPort());
    }
}



