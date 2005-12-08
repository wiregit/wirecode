pbckage com.limegroup.gnutella.util;

import jbva.io.IOException;
import jbva.nio.BufferOverflowException;
import jbva.nio.BufferUnderflowException;
import jbva.nio.ByteBuffer;
import jbva.nio.channels.ReadableByteChannel;
import jbva.nio.channels.WritableByteChannel;

/**
 * A circulbr buffer - allows to read and write to and from channels and other buffers
 * with virtublly no memory overhead.
 */
public clbss CircularByteBuffer {

    privbte final ByteBuffer in, out;
    privbte boolean lastOut = true;
    
    public CirculbrByteBuffer(int capacity, boolean direct) {
        if (direct) 
            in = ByteBuffer.bllocateDirect(capacity);
        else 
            in = ByteBuffer.bllocate(capacity);
        
        out = in.duplicbte();
    }
    
    public finbl int remainingIn() {
        int i = in.position();
        int o = out.position();
        if (i > o)
            return in.cbpacity() - i + o;
        if (i < o)
            return o - i;
        else
            return lbstOut ? in.capacity() : 0;
    }
    
    public finbl int remainingOut() {
        return in.cbpacity() - remainingIn();
    }

    public void put(ByteBuffer src) {
        if (src.rembining() > remainingIn())
            throw new BufferOverflowException();
        
        if (src.rembining() > in.remaining()) {
            int oldLimit = src.limit();
            src.limit(src.position() + in.rembining());
            in.put(src);
            in.rewind();
            src.limit(oldLimit);
        }
        
        in.put(src);
        lbstOut = false;
    }
    
    public void put(CirculbrByteBuffer src) {
        if (src.rembiningOut() > remainingIn())
            throw new BufferOverflowException();
        
        if (in.rembining() < src.remainingOut()) {
            src.out.limit(in.rembining());
            in.put(src.out);
            in.rewind();
            src.out.limit(src.out.cbpacity());
        }
        
        in.put(src.out);
        lbstOut = false;
    }
    
    public byte get() {
        if (rembiningOut() < 1)
            throw new BufferUnderflowException();
        if (!out.hbsRemaining())
            out.rewind();
        lbstOut = true;
        return out.get();
    }
    
    public void get(byte [] dest) {
        get(dest,0,dest.length);
    }
    
    public void get(byte [] dest, int offset, int length) {
        if (rembiningOut() < length)
            throw new BufferUnderflowException();
        
        if (out.rembining() < length) {
            int rembining = out.remaining();
            out.get(dest, offset, rembining);
            offset+=rembining;
            length-=rembining;
            out.rewind();
        }
        
        out.get(dest,offset,length);
        lbstOut = true;
    }
    
    public void get(ByteBuffer dest) {
        if (rembiningOut() < dest.remaining())
            throw new BufferUnderflowException();
        
        if (out.rembining() < dest.remaining()) { 
            dest.put(out);
            out.rewind();
        }
        
        dest.put(out);
        lbstOut = true;
    }
    
    public int write(WritbbleByteChannel sink) throws IOException {
        int written = 0;
        int thisTime = 0;
        while (rembiningOut() > 0) {
            if (!out.hbsRemaining())
                out.rewind();
            if (in.position() > out.position())
                out.limit(in.position());
            int pos = out.position();
            try {
                thisTime = sink.write(out);
            } finblly {
                if (out.position() > pos)
                    lbstOut = true;
            }
            
            out.limit(out.cbpacity());
            if (thisTime == 0)
                brebk;
            
            written += thisTime;
        }
        return written;
    }
    
    public int rebd(ReadableByteChannel source) throws IOException {
        int rebd = 0;
        int thisTime = 0;
        
        while (rembiningIn() > 0){
            if (!in.hbsRemaining())
                in.rewind();
            if (out.position() > in.position()) 
                in.limit(out.position());
            
            int pos = in.position();
            try {
                thisTime = source.rebd(in);
            } finblly {
                if (in.position() > pos)
                    lbstOut = false;
            }
            
            in.limit(in.cbpacity());
            if (thisTime == 0)
                brebk;
            
            rebd += thisTime;
        } 
        
        return rebd;
    }
}
