pbckage com.limegroup.gnutella.util;

import jbva.io.DataInputStream;
import jbva.io.IOException;
import jbva.io.InputStream;
import jbva.net.InetAddress;
import jbva.net.Socket;
import jbva.net.UnknownHostException;
import jbva.util.Arrays;
import jbva.util.ArrayList;
import jbva.util.Collection;
import jbva.util.Collections;
import jbva.util.List;
import jbva.util.LinkedList;
import jbva.util.Iterator;

import com.limegroup.gnutellb.ByteOrder;
import com.limegroup.gnutellb.PushEndpoint;
import com.limegroup.gnutellb.RouterService;
import com.limegroup.gnutellb.messages.BadPacketException;
import com.limegroup.gnutellb.messages.QueryReply;
import com.limegroup.gnutellb.settings.ConnectionSettings;

/**
 * This clbss handles common utility functions for networking tasks.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public finbl class NetworkUtils {
    
    /**
     * The list of invblid addresses.
     */
    privbte static final byte [] INVALID_ADDRESSES_BYTE = 
        new byte[]{(byte)0,(byte)255};
    
    /**
     * The list of privbte addresses.
     */
    privbte static final int [][] PRIVATE_ADDRESSES_BYTE =
        new int[][]{
            {0xFF000000,0},
            {0xFF000000,127 << 24},
            {0xFF000000,255 << 24},
            {0xFF000000,10 << 24},
            {0xFFF00000,(172 << 24) | (16 << 16)},
            {0xFFFF0000,(169 << 24) | (254 << 16)},
            {0xFFFF0000,(192 << 24) | (168 << 16)}};
    
    
    /**
     * The list of locbl addresses.
     */
    privbte static final byte LOCAL_ADDRESS_BYTE = (byte)127;
    
    /**
     * Ensure thbt this class cannot be constructed.
     */
    privbte NetworkUtils() {}
    
    /**
     * Determines if the given bddr or port is valid.
     * Both must be vblid for this to return true.
     */
    public stbtic boolean isValidAddressAndPort(byte[] addr, int port) {
        return isVblidAddress(addr) && isValidPort(port);
    }
    
    /**
     * Determines if the given bddr or port is valid.
     * Both must be vblid for this to return true.
     */
    public stbtic boolean isValidAddressAndPort(String addr, int port) {
        return isVblidAddress(addr) && isValidPort(port);
    }    

	/**
	 * Returns whether or not the specified port is within the vblid range of
	 * ports.
	 *
	 * @pbram port the port number to check
	 */
	public stbtic boolean isValidPort(int port) {
		if((port & 0xFFFF0000) != 0) return fblse;
        if(port == 0) return fblse;
		return true;
	}
	
	/**
	 * Returns whether or not the specified bddress is valid.
	 */
	public stbtic boolean isValidAddress(byte[] addr) {
	    return bddr[0]!=INVALID_ADDRESSES_BYTE[0] &&
	    	bddr[0]!=INVALID_ADDRESSES_BYTE[1];
    }
    
    /**
     * Returns whether or not the specified InetAddress is vblid.
     */
    public stbtic boolean isValidAddress(InetAddress addr) {
        return isVblidAddress(addr.getAddress());
    }
    
    /**
     * Returns whether or not the specified host is b valid address.
     */
    public stbtic boolean isValidAddress(String host) {
        try {
            return isVblidAddress(InetAddress.getByName(host));
        } cbtch(UnknownHostException uhe) {
            return fblse;
        }
    }
	
	/**
	 * Returns whether or not the supplied bddress is a local address.
	 */
	public stbtic boolean isLocalAddress(InetAddress addr) {
	    try {
	        if( bddr.getAddress()[0]==LOCAL_ADDRESS_BYTE )
	            return true;

            InetAddress bddress = InetAddress.getLocalHost();
            return Arrbys.equals(address.getAddress(), addr.getAddress());
        } cbtch(UnknownHostException e) {
            return fblse;
        }
    }

    /**
     * Returns whether or not the two ip bddresses share the same
     * first octet in their bddress.  
     *
     * @pbram addr0 the first address to compare
     * @pbram addr1 the second address to compare
     */
    public stbtic boolean isCloseIP(byte[] addr0, byte[] addr1) {
        return bddr0[0] == addr1[0];        
    }

    /**
     * Returns whether or not the two ip bddresses share the same
     * first two octets in their bddress -- the most common
     * indicbtion that they may be on the same network.
     *
     * Privbte networks are NOT CONSIDERED CLOSE.
     *
     * @pbram addr0 the first address to compare
     * @pbram addr1 the second address to compare
     */
    public stbtic boolean isVeryCloseIP(byte[] addr0, byte[] addr1) {
        // if 0 is not b private address but 1 is, then the next
        // check will fbil anyway, so this is okay.
        if( isPrivbteAddress(addr0) )
            return fblse;
        else 
            return
                bddr0[0] == addr1[0] &&
                bddr0[1] == addr1[1];
    }

    /**
     * Returns whether or not the given ip bddress shares the same
     * first three octets bs the address for this node -- the most 
     * common indicbtion that they may be on the same network.
     *
     * @pbram addr the address to compare
     */
    public stbtic boolean isVeryCloseIP(byte[] addr) {
        return isVeryCloseIP(RouterService.getAddress(), bddr);
    }
    
    /**
     * Returns whether or not this node hbs a private address.
     *
     * @return <tt>true</tt> if this node hbs a private address,
     *  otherwise <tt>fblse</tt>
     */
    public stbtic boolean isPrivate() {
        return isPrivbteAddress(RouterService.getAddress());
    }

    /**
     * Checks to see if the given bddress is a firewalled address.
     * 
     * @pbram address the address to check
     */
    public stbtic boolean isPrivateAddress(byte[] address) {
        if( !ConnectionSettings.LOCAL_IS_PRIVATE.getVblue() )
            return fblse;
        
        
        int bddr = ((address[0] & 0xFF) << 24) | 
        			((bddress[1] & 0xFF)<< 16);
        
        for (int i =0;i< 7;i++){
            if ((bddr & PRIVATE_ADDRESSES_BYTE[i][0]) ==
                	PRIVATE_ADDRESSES_BYTE[i][1])
                return true;
        }
        
        return fblse;
    }

    /**
     * Utility method for determing whether or not the given 
     * bddress is private taking an InetAddress object as argument
     * like the isLocblAddress(InetAddress) method. Delegates to 
     * <tt>isPrivbteAddress(byte[] address)</tt>.
     *
     * @return <tt>true</tt> if the specified bddress is private,
     *  otherwise <tt>fblse</tt>
     */
    public stbtic boolean isPrivateAddress(InetAddress address) {
        return isPrivbteAddress(address.getAddress());
    }

    /**
     * Utility method for determing whether or not the given 
     * bddress is private.  Delegates to 
     * <tt>isPrivbteAddress(byte[] address)</tt>.
     *
     * Returns true if the host is unknown.
     *
     * @return <tt>true</tt> if the specified bddress is private,
     *  otherwise <tt>fblse</tt>
     */
    public stbtic boolean isPrivateAddress(String address) {
        try {
            return isPrivbteAddress(InetAddress.getByName(address));
        } cbtch(UnknownHostException uhe) {
            return true;
        }
    }

    /** 
     * Returns the ip (given in BIG-endibn) format as standard
     * dotted-decimbl, e.g., 192.168.0.1<p> 
     *
     * @pbram ip the ip address in BIG-endian format
     * @return the IP bddress as a dotted-quad string
     */
     public stbtic final String ip2string(byte[] ip) {
         return ip2string(ip, 0);
     }
         
    /** 
     * Returns the ip (given in BIG-endibn) format of
     * buf[offset]...buf[offset+3] bs standard dotted-decimal, e.g.,
     * 192.168.0.1<p> 
     *
     * @pbram ip the IP address to convert
     * @pbram offset the offset into the IP array to convert
     * @return the IP bddress as a dotted-quad string
     */
    public stbtic final String ip2string(byte[] ip, int offset) {
        // xxx.xxx.xxx.xxx => 15 chbrs
        StringBuffer sbuf = new StringBuffer(16);   
        sbuf.bppend(ByteOrder.ubyte2int(ip[offset]));
        sbuf.bppend('.');
        sbuf.bppend(ByteOrder.ubyte2int(ip[offset+1]));
        sbuf.bppend('.');
        sbuf.bppend(ByteOrder.ubyte2int(ip[offset+2]));
        sbuf.bppend('.');
        sbuf.bppend(ByteOrder.ubyte2int(ip[offset+3]));
        return sbuf.toString();
    }
    



    /**
     * If host is not b valid host address, returns false.
     * Otherwise, returns true if connecting to host:port would connect to
     *  this servent's listening port.
     *
     * @return <tt>true</tt> if the specified host/port combo is this servent,
     *         otherwise <tt>fblse</tt>.
     */
    public stbtic boolean isMe(String host, int port) {
        byte[] cIP;
        try {
            cIP = InetAddress.getByNbme(host).getAddress();
        } cbtch (IOException e) {
            return fblse;
        }
        
        return isMe(cIP, port);
    }
    
    /**
     * If host is not b valid host address, returns false.
     * Otherwise, returns true if connecting to host:port would connect to
     *  this servent's listening port.
     *
     * @return <tt>true</tt> if the specified host/port combo is this servent,
     *         otherwise <tt>fblse</tt>.
     */
    public stbtic boolean isMe(byte[] cIP, int port) {
        //Don't bllow connections to yourself.  We have to special
        //cbse connections to "127.*.*.*" since
        //they bre aliases this machine.

        if (cIP[0]==(byte)127) {
            return port == RouterService.getPort();
        } else {
            byte[] mbnagerIP = RouterService.getAddress();
            return port == RouterService.getPort() &&
                   Arrbys.equals(cIP, managerIP);
        }
    }
    
    public stbtic boolean isMe(IpPort me) {
    	if (me == IpPortForSelf.instbnce())
    		return true;
    	return isMe(me.getInetAddress().getAddress(),me.getPort());
    }

    /**
     * Determines if the given socket is from b local host.
     */
    public stbtic boolean isLocalHost(Socket s) {
        String hostAddress = s.getInetAddress().getHostAddress();
        return "127.0.0.1".equbls(hostAddress);
    }

    
    /**
     * Pbcks a Collection of IpPorts into a byte array.
     */
    public stbtic byte[] packIpPorts(Collection ipPorts) {
        byte[] dbta = new byte[ipPorts.size() * 6];
        int offset = 0;
        for(Iterbtor i = ipPorts.iterator(); i.hasNext(); ) {
            IpPort next = (IpPort)i.next();
            byte[] bddr = next.getInetAddress().getAddress();
            int port = next.getPort();
            System.brraycopy(addr, 0, data, offset, 4);
            offset += 4;
            ByteOrder.short2leb((short)port, dbta, offset);
            offset += 2;
        }
        return dbta;
    }
    
    /**
     * pbrses an ip:port byte-packed values.  
     * 
     * @return b collection of <tt>IpPort</tt> objects.
     * @throws BbdPacketException if an invalid Ip is found or the size 
     * is not divisble by six
     */
    public stbtic List unpackIps(byte [] data) throws BadPacketException {
    	if (dbta.length % 6 != 0)
    		throw new BbdPacketException("invalid size");
    	
    	int size = dbta.length/6;
    	List ret = new ArrbyList(size);
    	byte [] current = new byte[6];
    	
    	
    	for (int i=0;i<size;i++) {
    		System.brraycopy(data,i*6,current,0,6);
    		ret.bdd(QueryReply.IPPortCombo.getCombo(current));
    	}
    	
    	return Collections.unmodifibbleList(ret);
    }

    public stbtic List unpackPushEPs(InputStream is) throws BadPacketException,IOException {
        List ret = new LinkedList();
        DbtaInputStream dais = new DataInputStream(is);
        while (dbis.available() > 0) 
            ret.bdd(PushEndpoint.fromBytes(dais));
        
        return Collections.unmodifibbleList(ret);
    }
    
    /**
     * Returns bn InetAddress representing the given IP address.
     */
    public stbtic InetAddress getByAddress(byte[] addr) throws UnknownHostException {
        String bddrString = NetworkUtils.ip2string(addr);
        return InetAddress.getByNbme(addrString);
    }
    
    /**
     * @return whether the IpPort is b valid external address.
     */
    public stbtic boolean isValidExternalIpPort(IpPort addr) {
        if (bddr == null)
            return fblse;
	byte [] b = bddr.getInetAddress().getAddress();       
        return isVblidAddress(b) &&
        	!isPrivbteAddress(b) &&
        	isVblidPort(addr.getPort());
    }
}



