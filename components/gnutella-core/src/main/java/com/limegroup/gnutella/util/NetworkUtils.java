package com.limegroup.gnutella.util;

import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella .*;
import java.io.*;
import java.net.*;

import com.sun.java.util.collections.Arrays;

/**
 * This class handles common utility functions for networking tasks.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class NetworkUtils {

    /**
     * Ensure that this class cannot be constructed.
     */
    private NetworkUtils() {}

	/**
	 * Returns whether or not the specified port is within the valid range of
	 * ports.
	 *
	 * @param port the port number to check
	 */
	public static boolean isValidPort(int port) {
		if((port & 0xFFFF0000) != 0) return false;
        if(port == 0) return false;
		return true;
	}
	
	/**
	 * Returns whether or not the specified address is valid.
	 */
	public static boolean isValidAddress(byte[] addr) {
	    if( addr[0] == 0 && addr[1] == 0 && addr[2] == 0 && addr[3] == 0)
            return false;
	    else
	        return true;
    }
    
    /**
     * Returns whether or not the specified InetAddress is valid.
     */
    public static boolean isValidAddress(InetAddress addr) {
        return isValidAddress(addr.getAddress());
    }       
	
	/**
	 * Returns whether or not the supplied address is a local address.
	 */
	public static boolean isLocalAddress(InetAddress addr) {
	    try {
            InetAddress address = InetAddress.getLocalHost();
            byte[] byteAddress = addr.getAddress();
            return (address.equals(addr) ||
                    byteAddress[0] == 127);
        } catch(UnknownHostException e) {
            return false;
        }
    }

    /**
     * Returns whether or not the two ip addresses share the same
     * first octet in their address.  
     *
     * @param addr0 the first address to compare
     * @param addr1 the second address to compare
     */
    public static boolean isCloseIP(byte[] addr0, byte[] addr1) {
        return addr0[0] == addr1[0];        
    }

    /**
     * Returns whether or not the two ip addresses share the same
     * first two octets in their address -- the most common
     * indication that they may be on the same network.
     *
     * Private networks are NOT CONSIDERED CLOSE.
     *
     * @param addr0 the first address to compare
     * @param addr1 the second address to compare
     */
    public static boolean isVeryCloseIP(byte[] addr0, byte[] addr1) {
        // if 0 is not a private address but 1 is, then the next
        // check will fail anyway, so this is okay.
        if( isPrivateAddress(addr0) )
            return false;
        else 
            return
                addr0[0] == addr1[0] &&
                addr0[1] == addr1[1];
    }

    /**
     * Returns whether or not the given ip address shares the same
     * first three octets as the address for this node -- the most 
     * common indication that they may be on the same network.
     *
     * @param addr the address to compare
     */
    public static boolean isVeryCloseIP(byte[] addr) {
        return isVeryCloseIP(RouterService.getAddress(), addr);
    }
    
    /**
     * Returns whether or not this node has a private address.
     *
     * @return <tt>true</tt> if this node has a private address,
     *  otherwise <tt>false</tt>
     */
    public static boolean isPrivate() {
        return isPrivateAddress(RouterService.getAddress());
    }

    /**
     * Checks to see if the given address is a firewalled address.
     * 
     * @param address the address to check
     */
    public static boolean isPrivateAddress(byte[] address) {
        if( !ConnectionSettings.LOCAL_IS_PRIVATE.getValue() )
            return false;
            
        if (address[0]==(byte)10) {
            return true;  //10.0.0.0 - 10.255.255.255
        } else if (address[0]==(byte)127) {
            return true;  //127.x.x.x
        } else if (address[0]==(byte)172 &&
                   address[1]>=(byte)16 &&
                   address[1]<=(byte)31) {
            return true;  //172.16.0.0 - 172.31.255.255
        } else if (address[0]==(byte)192 &&
                   address[1]==(byte)168) {
            return true; //192.168.0.0 - 192.168.255.255
        } else if (address[0]==(byte)169 &&
                   address[1]==(byte)254) {
            return true; //169.254.0.0 - 169.254.255.255 (local link)
        } else if (address[0]==(byte)0) {
            return true; //0.0.0.0 -- reserved 
            //} else if (address[0]>=(byte)240) {
            //return true; //240 and above -- broadcast, multicast
        } else {
            return false; // otherwise, it's not private
        }
    }

    /**
     * Utility method for determing whether or not the given 
     * address is private.  Delegates to 
     * <tt>isPrivateAddress(byte[] address)</tt>.
     *
     * @return <tt>true</tt> if the specified address is private,
     *  otherwise <tt>false</tt>
     * @throws <tt>UnknownHostException</tt> if the address is 
     *  unknown in the <tt>InetAddress</tt> lookup
     */
    public static boolean isPrivateAddress(String address) 
        throws UnknownHostException {
        return isPrivateAddress(InetAddress.getByName(address).getAddress());
    }

    /** 
     * Returns the ip (given in BIG-endian) format as standard
     * dotted-decimal, e.g., 192.168.0.1<p> 
     *
     * @param ip the ip address in BIG-endian format
     * @return the IP address as a dotted-quad string
     */
     public static final String ip2string(byte[] ip) {
         return ip2string(ip, 0);
     }
         
    /** 
     * Returns the ip (given in BIG-endian) format of
     * buf[offset]...buf[offset+3] as standard dotted-decimal, e.g.,
     * 192.168.0.1<p> 
     *
     * @param ip the IP address to convert
     * @param offset the offset into the IP array to convert
     * @return the IP address as a dotted-quad string
     */
    public static final String ip2string(byte[] ip, int offset) {
        // xxx.xxx.xxx.xxx => 15 chars
        StringBuffer sbuf = new StringBuffer(16);   
        sbuf.append(ByteOrder.ubyte2int(ip[offset]));
        sbuf.append('.');
        sbuf.append(ByteOrder.ubyte2int(ip[offset+1]));
        sbuf.append('.');
        sbuf.append(ByteOrder.ubyte2int(ip[offset+2]));
        sbuf.append('.');
        sbuf.append(ByteOrder.ubyte2int(ip[offset+3]));
        return sbuf.toString();
    }

    /**
     * Utility method for closing sockets.  This explicitly closes
     * the input and output streams and has special handling for
     * a bug in NIO on at least Windows NT/2000 etc and Linux where
     * calling close() on a socket does not send a FIN even though
     * it should.  This only occurs when socket timeouts are used,
     * but we use socket timeouts frequently.  This was reported
     * to Sun by Chris in 2002, which we just happened upon -- 
     * JDK bug 4724030.  Nice work Chris!
     *
     * @param socket the socket to close
     */
    public static final void closeSocket(Socket socket) {
        if(socket == null) return;
        // This is necessary to work around the 1.4 bug where calling 
        // close on a socket does not send a FIN to indicate the 
        // socket is closed -- socket.shutdownOutput() ensures that
        // the FIN is sent.  This was reported by Chris Rohrs back
        // in the day -- JDK bug 4724030
        if(CommonUtils.isJava14OrLater()) {
            try {
                socket.shutdownOutput();

            } catch(Exception e) {
                // we don't care
            }
            try {
                socket.shutdownInput();
            } catch(Exception e) {
                // we don't care
            }
        } else {
            // shut them down the normal way -- there is no bug on 
            // non-1.4 systems, so we can just close the streams
            try {
                socket.getOutputStream().close();
            } catch (Exception e) {
                // we don't care
            }
            try {
                socket.getInputStream().close();
            } catch (Exception e) {
                // we don't care
            }
        }
        try {
            socket.close();
        } catch (Exception e) {
            // we don't care
        }        
    }



    /**
     * If host is not a valid host address, returns false.
     * Otherwise, returns true if connecting to host:port would connect to
     *  this servent's listening port.
     *
     * @return <tt>true</tt> if the specified host/port combo is this servent,
     *         otherwise <tt>false</tt>.
     */
    public static boolean isMe(String host, int port) {
        //Don't allow connections to yourself.  We have to special
        //case connections to "localhost" or "127.*.*.*" since
        //they are aliases this machine.
        byte[] cIP;
        try {
            cIP=InetAddress.getByName(host).getAddress();
        } catch (IOException e) {
            return false;
        }

        if (cIP[0]==(byte)127) {
            return port == RouterService.getPort();
        } else {
            byte[] managerIP = RouterService.getAddress();
            return port == RouterService.getPort() &&
                   Arrays.equals(cIP, managerIP);
        }
    }    
}



