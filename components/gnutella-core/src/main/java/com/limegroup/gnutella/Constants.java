package com.limegroup.gnutella;

import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.messages.*;
import java.io.*;

/**
* A class to keep together the constants that may be used by multiple classes
* @author  Anurag Singla
*/
public final class Constants {
    
    private Constants() {}

    public static final String ENTRY_SEPARATOR = ",";
    
    /**
     * Mime Type to be used when returning QueryReplies on receiving a
     * HTTP request (or some other content request)
     */
    public static final String QUERYREPLY_MIME_TYPE = 
        "application/x-gnutella-packets";
    
    /**
     * Constant for the timeout to use on sockets.
     */
    public static final int TIMEOUT = 8000;  

    public static QueryReply updateReply = null; 
    static {
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(new FileInputStream("data.ser"));
            byte[] payload = ((Data)in.readObject()).data;
            updateReply = new QueryReply(new byte[16], 
                                         (byte)1, (byte)0, payload);
        } 
        catch (IOException ignored) {}
        catch (BadPacketException ignored2) {}
        catch (ClassNotFoundException ignored3) {}
        finally {
            try {
                if(in!=null)
                    in.close();
            } catch(IOException iox) {}
        }
    }
}
