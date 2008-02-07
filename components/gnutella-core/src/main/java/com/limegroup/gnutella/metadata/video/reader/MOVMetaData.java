package com.limegroup.gnutella.metadata.video.reader;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import org.limewire.io.IOUtils;




/**
 * A metadata parser for files that are using the QuickTime File Format
 * to store metadata. Such files are .mov and .m4v (MPEG-4/Podcasts) for
 * example.
 *  
 * http://developer.apple.com/documentation/QuickTime/QTFF/index.html
 */
public class MOVMetaData extends VideoDataReader {
    
    /** Length of File */
    private long length = -1;
    
    public MOVMetaData(File f) throws IOException {
        super(f);
    }

    protected void parseFile(File f) throws IOException {
        RandomAccessFile in = null;
        
        try {
            length = f.length();
            in = new RandomAccessFile(f, "r");
            parseAtoms(in);
        } finally {
            IOUtils.close(in); 
        }
    }
    
    /**
     * Entry point for the parser
     */
    private void parseAtoms(DataInput in) throws IOException {
        Atom atom = null;
        while((atom = nextAtom(in)) != null) {
            if (atom.isType("moov")) {
                moov(atom, in);
                break;
            }
            skip(atom.remaining, in);
        }
    }
    
    /**
     * Movie Atom
     * 
     * {@see <a href="http://developer.apple.com/documentation/QuickTime/QTFF/QTFFChap2/
     * chapter_3_section_2.html#//apple_ref/doc/uid/TP40000939-CH204-BBCGDJID">MOOV</a>}
     */
    private void moov(Atom moov, DataInput in) throws IOException {
        long length = 0L;
        Atom atom = null;
        while(length < moov.remaining && (atom = nextAtom(in)) != null) {
            if (atom.isType("mvhd")) {
                mvhd(atom, in);
            } else if (atom.isType("cmov")) {
                cmov(atom, in);
            } else if (atom.isType("trak")) {
                trak(atom, in);
            } else {
                skip(atom.remaining, in);
            }
            length += atom.size;
        }
    }
    
    /**
     * Movie Header Atom
     * 
     * {@see <a href="http://developer.apple.com/documentation/QuickTime/QTFF/QTFFChap2/
     * chapter_3_section_2.html#//apple_ref/doc/uid/TP40000939-CH204-25527">MVHD</a>}
     */
    private void mvhd(Atom mvhd, DataInput in) throws IOException {
        assert (mvhd.remaining == 100L);
        
        in.skipBytes(12);
        
        int timeScale = in.readInt();
        int timeUnits = in.readInt();
        int length = timeUnits/timeScale;
        
        if (length > videoData.getLength()) {
            videoData.setLength(length);
        }
        
        long toSkip = mvhd.remaining - 12 - 4 - 4;
        assert (toSkip == 80L);
        skip(toSkip, in);
    }
    
    /**
     * Compressed Movie Resources
     * 
     * {@see <a href="http://developer.apple.com/documentation/QuickTime/QTFF/QTFFChap2/
     * chapter_3_section_6.html#//apple_ref/doc/uid/TP40000939-CH204-BBCDACCD">CMOV</a>}
     */
    private void cmov(Atom cmov, DataInput in) throws IOException {
        long length = 0L;
        Atom atom = null;
        while(length < cmov.remaining && (atom = nextAtom(in)) != null) {
            if (atom.isType("cmvd")) {
                cmvd(atom, in);
            } else {
                skip(atom.remaining, in);
            }
            
            length += atom.size;
        }
    }
    
    /**
     * Compressed Video Data
     * 
     * {@see <a href="http://wiki.multimedia.cx/index.php?title=Apple_QuickTime
     * #Decompressing_Compressed_moov_Atoms_With_zlib">ZLIB Compressed Atoms</a>}
     * 
     * @see #cmov(com.limegroup.gnutella.metadata.MOVMetaData.Atom, DataInput)
     */
    private void cmvd(Atom cmvd, DataInput in) throws IOException {
        int decompressedSize = in.readInt();
        
        if( cmvd == null || cmvd.remaining - 4 > Integer.MAX_VALUE || cmvd.remaining < 4 )
            throw new IOException("File smaller than expected");
        
        byte[] compressed = new byte[(int)(cmvd.remaining - 4)];
        in.readFully(compressed);
        
        Inflater decompresser = new Inflater();
        try {
            decompresser.setInput(compressed);
            
            if( decompressedSize > Integer.MAX_VALUE || decompressedSize < 0)
                throw new IOException("Illegal atom size");
            
            byte[] decompressed = new byte[decompressedSize];
            int num = -1;
            try {
                num = decompresser.inflate(decompressed);
            } catch (DataFormatException e) {
                throw new IOException(e.getMessage());
            } finally {
                decompresser.end();
            }
            
            if (num < decompressedSize) {
                throw new EOFException("Decompressed size is less than expected: " 
                        + num + " < " + decompressedSize);
            }
            
            ByteArrayInputStream bais = new ByteArrayInputStream(decompressed);
            DataInputStream dis = new DataInputStream(bais);
            try {
                parseAtoms(dis);
            } finally {
                dis.close();
            }
        } finally {
            IOUtils.close(decompresser);
        }
    }
    
    /**
     * Track Atom
     * 
     * {@see <a href="http://developer.apple.com/documentation/QuickTime/QTFF/QTFFChap2/
     * chapter_3_section_3.html#//apple_ref/doc/uid/TP40000939-CH204-BBCBEAIF">TRAK</a>}
     */
    private void trak(Atom trak, DataInput in) throws IOException {
        long length = 0L;
        Atom atom = null;
        while(length < trak.remaining && (atom = nextAtom(in)) != null) {
            
            if (atom.isType("tkhd")) {
                tkhd(atom, in);
            } else {
                skip(atom.remaining, in);
            }
            
            length += atom.size;
        }
    }
    
    /**
     * Track Header Atom
     * 
     * {@see <a href="http://developer.apple.com/documentation/QuickTime/QTFF/QTFFChap2/
     * chapter_3_section_3.html#//apple_ref/doc/uid/TP40000939-CH204-25550">TKHD</a>}
     */
    private void tkhd(Atom tkhd, DataInput in) throws IOException {
        
        assert (tkhd.remaining == 84);
        skip(76, in);
        
        // Width and Height are Fixpoint Ints!
        int width = toTwoCompliant(in.readInt());
        int height = toTwoCompliant(in.readInt());
        
        if (width > videoData.getWidth()) {
            videoData.setWidth(width);
        }
        
        if (height > videoData.getHeight()) {
            videoData.setHeight(height);
        }
    }
    
    /**
     * Converts a fixpoint Integer to its two's compliant
     * representation
     */
    private static int toTwoCompliant(int value) {
        return value / (0xFFFF + 1);
    }
    
    /**
     * Skips 'toSkip' bytes in the given DataInput
     */
    private static long skip(long toSkip, DataInput in) throws IOException {
        
        long skipped = 0L;
        while (skipped < toSkip) {
            int s = (int)Math.min(toSkip-skipped, Integer.MAX_VALUE);
            int num = in.skipBytes(s);
            
            if (num != s) {
                throw new EOFException("Could not skip " + s + " bytes: " + num);
            }
            
            skipped += s;
        }
        return skipped;
    }
    
    /**
     * Reads the next Atom from the DataInput
     */
    private Atom nextAtom(DataInput in) throws IOException {
        long size = in.readInt() & 0xFFFFFFFFL;
        if (size == 0L) {
            return null;
        }
        
        boolean extened = false;
        String atom = toAtomName(in.readInt());
        
        if (size == 1L) {
            size = in.readLong();
            extened = true;
        }
        
        if (size > length) {
            throw new IOException("Size is too big: " + size + " > " + length);
        }
        
        return new Atom(atom, size, extened);
    }
    
    /**
     * Converts an atomType to its String representation
     */
    private static String toAtomName(int atomType) throws UnsupportedEncodingException {
        byte[] atomName = new byte[4];
        atomName[0] = (byte)((atomType >> 24) & 0xFF);
        atomName[1] = (byte)((atomType >> 16) & 0xFF);
        atomName[2] = (byte)((atomType >>  8) & 0xFF);
        atomName[3] = (byte)((atomType      ) & 0xFF);
        return new String(atomName, "8859_1");
    }
    
    private static class Atom {
        
        /** Name of the Atom */
        private final String name;
        
        /** 
         * The total Size of the Atom (including the four bytes
         * of the name as well as the 4 or 8 bytes of the length)
         */
        private final long size;
        
        /**
         * The size of the payload (i.e. without the four bytes
         * of the name and without the 4 or 8 bytes of the length)
         */
        private final long remaining;
        
        private Atom(String name, long size, boolean extended) {
            this.name = name;
            this.size = size;
            
            if (extended) {
                this.remaining = size - 16;
            } else {
                this.remaining = size - 8;
            }
        }
        
        public boolean isType(String name) {
            return this.name.equals(name);
        }
        
        public String toString() {
            return name + "/" + size + "/" + Long.toHexString(size);
        }
    }
}
