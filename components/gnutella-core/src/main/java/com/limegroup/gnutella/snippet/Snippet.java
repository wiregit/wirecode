
// Created for the Learning branch

package com.limegroup.gnutella.snippet;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Properties;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;

import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.search.QueryHandler;
import com.limegroup.gnutella.search.ResultCounter;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.statistics.ReceivedMessageStatHandler;
import com.limegroup.gnutella.udpconnect.DataWindow;
import com.limegroup.gnutella.udpconnect.FinMessage;
import com.limegroup.gnutella.udpconnect.SequenceNumberExtender;
import com.limegroup.gnutella.udpconnect.SynMessage;
import com.limegroup.gnutella.udpconnect.UDPMultiplexor;
import com.limegroup.gnutella.udpconnect.UDPScheduler;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.handshaking.HeaderNames;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.GGEP;
import com.limegroup.gnutella.messages.GGEPTest;
import com.limegroup.gnutella.messages.BadGGEPBlockException;
import com.limegroup.gnutella.messages.BadGGEPPropertyException;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVM;
import com.limegroup.gnutella.messages.vendor.HeaderUpdateVendorMessage;
import com.limegroup.gnutella.messages.vendor.LimeACKVendorMessage;
import com.limegroup.gnutella.messages.vendor.QueryStatusResponse;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessage;
import com.limegroup.gnutella.messages.vendor.UDPConnectBackVendorMessage;
import com.limegroup.gnutella.messages.vendor.VendorMessage;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.ManagedConnection;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.messages.vendor.VendorMessageTest;

public class Snippet {

    public static void snippet() {

    	String s;
    	s = CommonUtils.getHttpServer();

    	/*
    	boolean result = RouterService.canReceiveUnsolicited();

    	int i;
    	if (result) {
    		
     		
    		
    	} else {
    		
    		
    	}

    	/*
//  	SynMessage m = new SynMessage((byte)5);
        
    	UDPConnectBackVendorMessage m = new UDPConnectBackVendorMessage(RouterService.getPort(), new GUID(GUID.makeGuid()));
    	
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        
        try {
            
            m.write(b);

        } catch (IOException e) {}
        
        String s = packetView(b.toByteArray());
        
        
        /*
         * A UDPConnectBack vendor message.
         * 
         * 16 d8 9f 49 97 ca a1 7c  ---I---|
         * e1 e6 ff fe 81 f8 42 00  ------B-
         * 31 01 00 1a 00 00 00 47  1------G
         * 54 4b 47 07 00 01 00 e7  TKG-----
         * 18 da 6d a5 e2 99 2c cd  --m---,-
         * 67 f1 9a 16 47 ac e0 d0  g---G---
         * 00                       -
         * 
         * 
         * 
         * 
         * 
         */
        
        
        
        
        
        
        
        
        
        
        


    }

    

    /**
     * Use an Extender object to expand a number truncated to 2 bytes into the full original 8-byte number.
     * Show it a randomly varying and gradually increasing stream of numbers, and it will detect the rollover each time it happens and account for it.
     * 
     * This code works be creating an area around the rollover boundary these comments call the bubble.
     * When we're in the bubble, high numbers haven't wrapped around yet, while low ones have.
     * 
     * Here's what the bubble looks like.
     * width is the number of numbers you can express with 2 bytes, and the rollover boundary.
     * entry and exit are a fractional distance away from width, marking the entry and exit of the bubble.
     * 
     * |----------------------------(--------|--------)----------------------
     * ^                            ^        ^        ^        ^
     * 0                            entry    width    exit     exit * 2
     * 
     * At first, the numbers we're getting are low, near 0.
     * inside is false, and base is 0.
     * 
     * When the spray of numbers approaches entry, there will be a first number that's bigger than entry.
     * We detect this as (!inside && n > entry).
     * When this happens, we set inside to true.
     * 
     * Right after the first number bigger than entry, we might get one smaller.
     * We don't leave the bubble, because while inside is true and n is bigger than exit, n isn't smaller than exit * 2.
     * 
     * Inside the bubble, we're watching for small numbers that have rolled over.
     * If we get a (n < exit), we add base + width, not just base.
     * 
     * Now n is small again, growing towards exit.
     * When we first get a n beyond exit, we set inside to false and increment the base.
     * This lets us just return base instead of base + width.
     * Even if the next number is less than exit, we're out of the bubble, and won't enter it again until n reaches entry.
     * 
     * UDP connection packets carry 512 bytes of file data, and are numbered with 2 bytes.
     * Before rolling over, a computer will transmit 0x10000 * 512 bytes = 32 MB of data.
     * The bubble width from entry to exit is a quarter of this, spanning 8 MB of stream data.
     * UDP connection packets arrive out of order, but not by a distance of megabytes.
     */
    public class Extender {

        /** 0x10000, the number of different numbers you can express with 2 bytes. */
        private final long width = 0x10000;

        /** The number that marks where we exit the bubble. */
        private final long exit = width / 8; // Defines the bubble size

        /** The number that marks where we enter the bubble. */
        private final long entry = width - exit;

        /** True when we're inside the bubble. */
        private boolean inside;

        /** The base to add to the numbers we get. */
        private long base;

        /**
         * Translate a given number that fits into 2 bytes and wraps around into a full 8-byte number. 
         * The given number can grow from 0x0000 to 0xffff and then wrap around to small numbers again, and the returned number will just keep growing.
         * This works even if the numbers aren't in order, they just can't be wildly out of order.
         * 
         * @param n A sequence number that has been truncated to 2 bytes
         * @return  The full 8-byte number
         */
        public long extend(long n) {

            // This is the first number we've gotten inside the bubble
            if (!inside && n > entry) {

                // Enter the bubble
                inside = true;

            // This is the first number we've gotten beyond the bubble
            } else if (inside && n > exit && n < exit * 2) { // If we didn't check n < exit * 2, a number just shy of entry would pop us back out of the bubble

                // Exit the bubble, incrementing the base
                inside = false;
                base += width;
            }

            // Add the base to n, and if we're in the right half of the bubble, another width
            if (inside && n < exit) return n + base + width; // n has wrapped around
            else                    return n + base;
        }
    }






    private static int calculateNewHosts(int degree, byte ttl) {

        double newHosts = 0;
        for ( ; ttl > 0; ttl--) {

            newHosts += Math.pow((degree - 1), ttl - 1);
        }

        return (int)newHosts;
    }
    
    
    
    /**
     * Write out the data in a byte array into a human-readable block of text.
     * Generates text like this:
     * 
     * 74 73 28 3d 74 3a 8b f9  ts(=t:--
     * c0 33 87 6f 76 39 e9 00  -3-ov9--
     * 
     * Puts 8 bytes of data on each line.
     * On the left, the method expresses each byte as two base 16 digits, "00" through "ff".
     * On the right, the method expresses each byte as ASCII characters.
     * If a byte is out of the range of readable characters, the method puts a hyphen in its place.
     * 
     * @param data A byte array of data
     * @return     A String with several lines of text that shows the data
     */
    public static String packetView(byte[] data) {

        // Make strings to put together the text
        String lines   = ""; // lines will hold all the finished lines, like "74 73 28 3d 74 3a 8b f9  ts(=t:--"
        String codes   = ""; // codes will hold the first part with each byte expressed in base 16, like "74 73 28 3d 74 3a 8b f9"
        String letters = ""; // letters will hold the bytes as ASCII charcters, like "ts(=t:--", with hyphens for bytes out of range

        // Loop down each byte in the given array
        for (int i = 0; i < data.length; i++) {

            // This is the start of a line
            if (i % 8 == 0) {

                // Compose a line, and add it to the finished text
                lines += codes + " " + letters + "\r\n";

                // Make codes and letters blank for the next line
                codes   = "";
                letters = "";
            }

            // Get the byte b in the data that our index is on
            byte b = data[i];

            // Express it as 2 base 16 characters, like "00" through "ff"
            String code;
            code = Integer.toHexString(ByteOrder.ubyte2int(b));
            if (code.length() == 1) code = "0" + code; // Add a leading 0 if necessary
            codes += code + " ";

            // Express it as an ASCII character, or a hyphen if not in range
            char letter = '-';
            if (b >= '!' && b <= '~') letter = (char)b;
            letters += letter;
        }

        // Add the codes and letters that didn't make it into a complete line
        lines += codes + " " + letters + "\r\n";

        // Return the composed text
        return lines;
    }
}
