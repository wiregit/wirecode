package com.limegroup.gnutella.messages;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.IOUtils;
import org.limewire.util.Base32;
import org.limewire.util.CommonUtils;

import com.limegroup.gnutella.util.Data;

public final class StaticMessages {
    
    private static final Log LOG = LogFactory.getLog(StaticMessages.class);

    private static QueryReply updateReply, limeReply;
    
    public static void initialize() {
        updateReply = readUpdateReply();
        limeReply = createLimeReply();
    }
    
    private static QueryReply readUpdateReply() {
        try {
            return createReply(new FileInputStream(new File(CommonUtils.getUserSettingsDir(), "data.ser")));
        } catch (FileNotFoundException bad) {
            return null;
        }
    }
    
    private static QueryReply createLimeReply() {
        byte [] reply = Base32.decode("VTWQABLTOIACAY3PNUXGY2LNMVTXE33VOAXGO3TVORSWY3DBFZ2XI2LMFZCGC5DBD4HW4LDZA65LCAQAAFNQABDEMF2GC5AAAJNUE6DQOVZAAAS3IKWPGF7YAYEFJYACAAAHQ4AAAAAUMAOKDBAD2GNL77776777"+
                "77776AAEAAAEY2LNMVLWS4TFEBIFETZAIF3GC2LMMFRGYZJAMF2CATDJNVSVO2LSMUXGG33NAAAEYSKNIUCDYONUAAAMHASCJBAMGASTIJAIGU2JI5XDALACCR5UR6XTYJEZVCPOYJWXZXF2ESOLUKXMM4BBIFF5"+
                "T7EFWL6YYKMY3SK65A6WH5DA53GIAPB7PBWWYIDWMVZHG2LPNY6SEMJOGARD6PR4MF2WI2LPOMQHQ43JHJXG6TTBNVSXG4DBMNSVGY3IMVWWCTDPMNQXI2LPNY6SE2DUORYDULZPO53XOLTMNFWWK53JOJSS4Y3P"+
                "NUXXGY3IMVWWC4ZPMF2WI2LPFZ4HGZBCHY6GC5LENFXSAYLDORUW63R5EJUHI5DQHIXS653XO4XGY2LNMV3WS4TFFZRW63JPOVYGIYLUMU7WS3TDNRUWK3TUEIQGS3TEMV4D2IRQEIXT4PBPMF2WI2LPOM7AAAAA"+
                "AAAAAAAAAAAAAAAAAAAAAAA");
        return createReply(new ByteArrayInputStream(reply));
    }
    
    private static QueryReply createReply(InputStream source) {
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(source);
            byte[] payload = ((Data) in.readObject()).data;
            return new QueryReply(new byte[16], (byte) 1, (byte) 0, payload);
        } catch (Throwable t) {
            LOG.error("Unable to read serialized data", t);
            return null;
        } finally {
            IOUtils.close(in);
        }
    }
    
    public static QueryReply getUpdateReply() {
        return updateReply;
    }
    
    public static QueryReply getLimeReply() {
        return limeReply;
    }
}
