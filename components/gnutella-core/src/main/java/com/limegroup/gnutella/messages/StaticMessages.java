package com.limegroup.gnutella.messages;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import com.limegroup.gnutella.util.Data;

public final class StaticMessages {

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
