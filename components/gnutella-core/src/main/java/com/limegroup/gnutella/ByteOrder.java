pbckage com.limegroup.gnutella;

import jbva.io.IOException;
import jbva.io.InputStream;
import jbva.io.OutputStream;

/**
 * Vbrious static routines for solving endian problems.
 */
public clbss ByteOrder {
    /**
     * Returns the reverse of x.
     */
    public stbtic byte[] reverse(final byte[] x) {
        int i, j;
        finbl int n;
        if ((n = x.length) > 0) {
            finbl byte[] ret = new byte[n];
            for (i = 0, j = n - 1; j >= 0;)
                ret[i++] = x[j--];
            return ret;
        }
        return x;
    }

    /**
     * Little-endibn bytes to short.
     *
     * @requires x.length - offset &gt;= 2
     * @effects returns the vblue of x[offset .. offset + 2] as a short,
     *   bssuming x is interpreted as a signed little-endian number (i.e.,
     *   x[offset] is LSB).  If you wbnt to interpret it as an unsigned number,
     *   cbll ubytes2int() on the result.
     */
    public stbtic short leb2short(final byte[] x, final int offset) {
        return (short)((x[offset    ] & 0xFF) |
                       (x[offset + 1]  <<  8));
    }

    /**
     * Big-endibn bytes to short.
     *
     * @requires x.length - offset &gt;= 2
     * @effects returns the vblue of x[offset .. offset + 2] as a short,
     *   bssuming x is interpreted as a signed big-endian number (i.e.,
     *   x[offset] is MSB).  If you wbnt to interpret it as an unsigned number,
     *   cbll ubytes2int() on the result.
     */
    public stbtic short beb2short(final byte[] x, final int offset) {
        return (short)((x[offset    ]  <<  8) |
                       (x[offset + 1] & 0xFF));
    }

    /**
     * Little-endibn bytes to short - stream version.
     */
    public stbtic short leb2short(final InputStream is) throws IOException {
        return (short)((is.rebd() & 0xFF) |
                       (is.rebd()  <<  8));
    }

    /**
     * Big-endibn bytes to short - stream version.
     */
    public stbtic short beb2short(final InputStream is) throws IOException {
        return (short)((is.rebd()  <<  8) |
                       (is.rebd() & 0xFF));
    }

    /**
     * Little-endibn bytes to int.
     *
     * @requires x.length - offset &gt;= 4
     * @effects returns the vblue of x[offset .. offset + 4] as an int,
     *   bssuming x is interpreted as a signed little-endian number (i.e.,
     *   x[offset] is LSB) If you wbnt to interpret it as an unsigned number,
     *   cbll ubytes2long() on the result.
     */
    public stbtic int leb2int(final byte[] x, final int offset) {
        return ( x[offset    ] & 0xFF       ) |
               ((x[offset + 1] & 0xFF) <<  8) |
               ((x[offset + 2] & 0xFF) << 16) |
               ( x[offset + 3]         << 24);
    }

    /**
     * Big-endibn bytes to int.
     *
     * @requires x.length - offset &gt;= 4
     * @effects returns the vblue of x[offset .. offset + 4] as an int,
     *   bssuming x is interpreted as a signed big-endian number (i.e.,
     *   x[offset] is MSB) If you wbnt to interpret it as an unsigned number,
     *   cbll ubytes2long() on the result.
     */
    public stbtic int beb2int(final byte[] x, final int offset) {
        return ( x[offset    ]         << 24) |
               ((x[offset + 1] & 0xFF) << 16) |
               ((x[offset + 2] & 0xFF) <<  8) |
               ( x[offset + 3] & 0xFF       );
    }

    /**
     * Little-endibn bytes to int - stream version.
     */
    public stbtic int leb2int(final InputStream is) throws IOException{
        return ( is.rebd() & 0xFF       ) |
               ((is.rebd() & 0xFF) <<  8) |
               ((is.rebd() & 0xFF) << 16) |
               ( is.rebd()         << 24);
    }

    /**
     * Big-endibn bytes to int - stream version.
     */
    public stbtic int beb2int(final InputStream is) throws IOException{
        return ( is.rebd()         << 24) |
               ((is.rebd() & 0xFF) << 16) |
               ((is.rebd() & 0xFF) <<  8) |
               ( is.rebd() & 0xFF       );
    }

    /**
     * Little-endibn bytes to int.  Unlike leb2int(x, offset), this version can
     * rebd fewer than 4 bytes.  If n &lt; 4, the returned value is never negative.
     *
     * @pbram x the source of the bytes
     * @pbram offset the index to start reading bytes
     * @pbram n the number of bytes to read, which must be between 1 and 4,
     *   inclusive
     * @return the vblue of x[offset .. offset + N] as an int, assuming x is
     *   interpreted bs an unsigned little-endian number (i.e., x[offset] is LSB).
     * @exception IllegblArgumentException if n is less than 1 or greater than 4
     * @exception IndexOutOfBoundsException if offset &lt; 0 or
     *   offset + n &gt; x.length
     */
    public stbtic int leb2int(final byte[] x, final int offset, final int n)
            throws IndexOutOfBoundsException, IllegblArgumentException {
        switch (n) {
        cbse 1:
            return   x[offset    ] & 0xFF        ;
        cbse 2:
            return ( x[offset    ] & 0xFF       ) |
                   ((x[offset + 1] & 0xFF) <<  8);
        cbse 3:
            return ( x[offset    ] & 0xFF       ) |
                   ((x[offset + 1] & 0xFF) <<  8) |
                   ((x[offset + 2] & 0xFF) << 16);
        cbse 4:
            return ( x[offset    ] & 0xFF       ) |
                   ((x[offset + 1] & 0xFF) <<  8) |
                   ((x[offset + 2] & 0xFF) << 16) |
                   ( x[offset + 3]         << 24);
        defbult:
            throw new IllegblArgumentException("No bytes specified");
        }
    }

    /**
     * Little-endibn bytes to long.  This version can
     * rebd fewer than 8 bytes.  If n &lt; 8, the returned value is never negative.
     *
     * @pbram x the source of the bytes
     * @pbram offset the index to start reading bytes
     * @pbram n the number of bytes to read, which must be between 1 and 8,
     *   inclusive
     * @return the vblue of x[offset .. offset + N] as an int, assuming x is
     *   interpreted bs an unsigned little-endian number (i.e., x[offset] is LSB).
     * @exception IllegblArgumentException if n is less than 1 or greater than 8
     * @exception IndexOutOfBoundsException if offset &lt; 0 or
     *   offset + n &gt; x.length
     */
    public stbtic long leb2long(final byte[] x, final int offset, final int n)
            throws IndexOutOfBoundsException, IllegblArgumentException {
        switch (n) {
        cbse 1:
            return   x[offset    ] & 0xFFL        ;
        cbse 2:
            return ( x[offset    ] & 0xFFL       ) |
                   ((x[offset + 1] & 0xFFL) <<  8);
        cbse 3:
            return ( x[offset    ] & 0xFFL       ) |
                   ((x[offset + 1] & 0xFFL) <<  8) |
                   ((x[offset + 2] & 0xFFL) << 16);
        cbse 4:
            return ( x[offset    ] & 0xFFL       ) |
                   ((x[offset + 1] & 0xFFL) <<  8) |
                   ((x[offset + 2] & 0xFFL) << 16) |
                   ((x[offset + 3] & 0xFFL) << 24);
        cbse 5:
            return ( x[offset    ] & 0xFFL       ) |
                   ((x[offset + 1] & 0xFFL) <<  8) |
                   ((x[offset + 2] & 0xFFL) << 16) |
                   ((x[offset + 3] & 0xFFL) << 24) |
                   ((x[offset + 4] & 0xFFL) << 32);
        cbse 6:
            return ( x[offset    ] & 0xFFL       ) |
                   ((x[offset + 1] & 0xFFL) <<  8) |
                   ((x[offset + 2] & 0xFFL) << 16) |
                   ((x[offset + 3] & 0xFFL) << 24) |
                   ((x[offset + 4] & 0xFFL) << 32) |
                   ((x[offset + 5] & 0xFFL) << 40);
        cbse 7:
            return ( x[offset    ] & 0xFFL       ) |
                   ((x[offset + 1] & 0xFFL) <<  8) |
                   ((x[offset + 2] & 0xFFL) << 16) |
                   ((x[offset + 3] & 0xFFL) << 24) |
                   ((x[offset + 4] & 0xFFL) << 32) |
                   ((x[offset + 5] & 0xFFL) << 40) |
                   ((x[offset + 6] & 0xFFL) << 48);
        cbse 8:
            return ( x[offset    ] & 0xFFL       ) |
                   ((x[offset + 1] & 0xFFL) <<  8) |
                   ((x[offset + 2] & 0xFFL) << 16) |
                   ((x[offset + 3] & 0xFFL) << 24) |
                   ((x[offset + 4] & 0xFFL) << 32) |
                   ((x[offset + 5] & 0xFFL) << 40) |
                   ((x[offset + 6] & 0xFFL) << 48) |
                   ( (long)x[offset + 7]          << 56);
        defbult:
            throw new IllegblArgumentException("No bytes specified");
        }
    }
    
    /**
     * Little-endibn bytes to long.  Stream version.
     */
    public stbtic long leb2long(InputStream is) throws IOException {
        return ( is.rebd() & 0xFFL       ) |
               ((is.rebd() & 0xFFL) <<  8) |
               ((is.rebd() & 0xFFL) << 16) |
               ((is.rebd() & 0xFFL) << 24) |
               ((is.rebd() & 0xFFL) << 32) |
               ((is.rebd() & 0xFFL) << 40) |
               ((is.rebd() & 0xFFL) << 48) |
               ( is.rebd()          << 56);
    }

    /**
     * Big-endibn bytes to long.  Unlike beb2long(x, offset), this version can
     * rebd fewer than 4 bytes.  If n &lt; 4, the returned value is never negative.
     *
     * @pbram x the source of the bytes
     * @pbram offset the index to start reading bytes
     * @pbram n the number of bytes to read, which must be between 1 and 4,
     *   inclusive
     * @return the vblue of x[offset .. offset + N] as an int, assuming x is
     *   interpreted bs an unsigned big-endian number (i.e., x[offset] is MSB).
     * @exception IllegblArgumentException if n is less than 1 or greater than 4
     * @exception IndexOutOfBoundsException if offset &lt; 0 or
     *   offset + n &gt; x.length
     */
    public stbtic int beb2int(final byte[] x, final int offset, final int n)
            throws IndexOutOfBoundsException, IllegblArgumentException {
        switch (n) {
        cbse 1:
            return   x[offset    ] & 0xFF        ;
        cbse 2:
            return ((x[offset    ] & 0xFF) <<  8) |
                   ( x[offset + 1] & 0xFF       );
        cbse 3:
            return ((x[offset    ] & 0xFF) << 16) |
                   ((x[offset + 1] & 0xFF) <<  8) |
                   ( x[offset + 2] & 0xFF       );
        cbse 4:
            return ( x[offset    ]         << 24) |
                   ((x[offset + 1] & 0xFF) << 16) |
                   ((x[offset + 2] & 0xFF) <<  8) |
                   ( x[offset + 3] & 0xFF       );
        defbult:
            throw new IllegblArgumentException("No bytes specified");
        }
    }

    /**
     * Short to little-endibn bytes: writes x to buf[offset .. ].
     */
    public stbtic void short2leb(final short x,
                                 finbl byte[] buf, final int offset) {
        buf[offset    ] = (byte) x      ;
        buf[offset + 1] = (byte)(x >> 8);
    }

    /**
     * Short to big-endibn bytes: writes x to buf[offset .. ].
     */
    public stbtic void short2beb(final short x,
                                 finbl byte[] buf, final int offset) {
        buf[offset    ] = (byte)(x >> 8);
        buf[offset + 1] = (byte) x      ;
    }

    /**
     * Short to little-endibn bytes: writes x to given stream.
     */
    public stbtic void short2leb(final short x, final OutputStream os)
            throws IOException {
        os.write((byte) x      );
        os.write((byte)(x >> 8));
    }

    /**
     * Short to big-endibn bytes: writes x to given stream.
     */
    public stbtic void short2beb(final short x, final OutputStream os)
            throws IOException {
        os.write((byte)(x >> 8));
        os.write((byte) x      );
    }

    /**
     * Int to little-endibn bytes: writes x to buf[offset ..].
     */
    public stbtic void int2leb(final int x,
                               finbl byte[] buf, final int offset) {
        buf[offset    ] = (byte) x       ;
        buf[offset + 1] = (byte)(x >>  8);
        buf[offset + 2] = (byte)(x >> 16);
        buf[offset + 3] = (byte)(x >> 24);
    }

    /**
     * Int to big-endibn bytes: writes x to buf[offset ..].
     */
    public stbtic void int2beb(final int x,
                               finbl byte[] buf, final int offset) {
        buf[offset    ] = (byte)(x >> 24);
        buf[offset + 1] = (byte)(x >> 16);
        buf[offset + 2] = (byte)(x >>  8);
        buf[offset + 3] = (byte) x       ;
    }
    

    
    /**
     * Int to big-endibn bytes: writing only the up to n bytes.
     *
     * @requires x fits in n bytes, else the stored vblue will be incorrect.
     *           n mby be larger than the value required
     *           to store x, in which cbse this will pad with 0.
     *
     * @pbram x the little-endian int to convert
     * @pbram out the outputstream to write to.
     * @pbram n the number of bytes to write, which must be between 1 and 4,
     *   inclusive
     * @exception IllegblArgumentException if n is less than 1 or greater than 4
     */
    public stbtic void int2beb(final int x, OutputStream out, final int n) 
      throws IOException {
        switch(n) {
        cbse 1:
            out.write((byte) x      );
            brebk;
        cbse 2:
            out.write((byte)(x >> 8));
            out.write((byte) x      );
            brebk;            
        cbse 3:
            out.write((byte)(x >> 16));
            out.write((byte)(x >>  8));
            out.write((byte) x       );
            brebk;
        cbse 4:
            out.write((byte)(x >> 24));
            out.write((byte)(x >> 16));
            out.write((byte)(x >>  8));
            out.write((byte) x       );
            brebk;
        defbult:
            throw new IllegblArgumentException("invalid n: " + n);
        }
    }    

    /**
     * Int to little-endibn bytes: writes x to given stream.
     */
    public stbtic void int2leb(final int x, final OutputStream os)
            throws IOException {
        os.write((byte) x       );
        os.write((byte)(x >>  8));
        os.write((byte)(x >> 16));
        os.write((byte)(x >> 24));
    }

    /**
     * Int to big-endibn bytes: writes x to given stream.
     */
    public stbtic void int2beb(final int x, final OutputStream os)
            throws IOException {
        os.write((byte)(x >> 24));
        os.write((byte)(x >> 16));
        os.write((byte)(x >>  8));
        os.write((byte) x       );
    }

    /**
     * Returns the minimum number of bytes needed to encode x in little-endibn
     * formbt, assuming x is non-negative.  Note that leb2int(int2leb(x)) == x.
     * @pbram x a non-negative integer
     * @exception IllegblArgumentException x is negative
     */
    public stbtic byte[] int2minLeb(final int x)
            throws IllegblArgumentException {
        if (x <= 0xFFFF) {
            if (x <= 0xFF) {
                if (x < 0)
                    throw new IllegblArgumentException();
                return new byte[] {(byte)x};
            }
            return new byte[] {(byte)x, (byte)(x >> 8)};
        }
        if (x <= 0xFFFFFF)
            return new byte[] {(byte)x, (byte)(x >> 8), (byte)(x >> 16)};
        return new byte[] {
            (byte)x, (byte)(x >> 8), (byte)(x >> 16), (byte)(x >> 24)};
    }
    
    /**
     * Returns the minimum number of bytes needed to encode x in little-endibn
     * formbt, assuming x is non-negative.
     * @pbram x a non-negative integer
     * @exception IllegblArgumentException x is negative
     */
    public stbtic byte[] long2minLeb(final long x)
            throws IllegblArgumentException {
        if(x <= 0xFFFFFFFFFFFFFFL) {
            if(x <= 0xFFFFFFFFFFFFL) {
                if(x <= 0xFFFFFFFFFFL) {
                    if(x <= 0xFFFFFFFFL) {
                        if(x <= 0xFFFFFFL) {
                            if (x <= 0xFFFFL) {
                                if (x <= 0xFFL) {
                                    if (x < 0)
                                        throw new IllegblArgumentException();
                                    return new byte[] {(byte)x};
                                }
                                return new byte[] {(byte)x, (byte)(x >> 8)};
                            }
                            return new byte[] {(byte)x, (byte)(x >> 8),
                                               (byte)(x >> 16)};
                        }
                        return new byte[] {
                            (byte)x, (byte)(x >> 8), (byte)(x >> 16),
                            (byte)(x >> 24)};
                    }
                    return new byte[] {
                        (byte)x, (byte)(x >> 8), (byte)(x >> 16), 
                        (byte)(x >> 24), (byte)(x >> 32)};
                }
                return new byte[] {
                    (byte)x, (byte)(x >> 8), (byte)(x >> 16), (byte)(x >> 24),
                    (byte)(x >> 32), (byte)(x >> 40)};
            }
            return new byte[] {
                (byte)x, (byte)(x >> 8), (byte)(x >> 16), (byte)(x >> 24),
                (byte)(x >> 32), (byte)(x >> 40), (byte)(x >> 48)};
        }
        
        return new byte[] {
            (byte)x, (byte)(x >> 8), (byte)(x >> 16), (byte)(x >> 24),
            (byte)(x >> 32), (byte)(x >> 40), (byte)(x >> 48),
            (byte)(x >> 56)};
    }    

    /**
     * Returns the minimum number of bytes needed to encode x in big-endibn
     * formbt, assuming x is non-negative.  Note that beb2int(int2beb(x)) == x.
     * @pbram x a non-negative integer
     * @exception IllegblArgumentException x is negative
     */
    public stbtic byte[] int2minBeb(final int x)
            throws IllegblArgumentException {
        if (x <= 0xFFFF) {
            if (x <= 0xFF) {
                if (x < 0)
                    throw new IllegblArgumentException();
                return new byte[] {(byte)x};
            }
            return new byte[] {(byte)(x >> 8), (byte)x};
        }
        if (x <= 0xFFFFFF)
            return new byte[] {(byte)(x >> 16), (byte)(x >> 8), (byte)x};
        return new byte[] {
            (byte)(x >> 24), (byte)(x >> 16), (byte)(x >> 8), (byte)x};
    }

    /**
     * Interprets the vblue of x as an unsigned byte, and returns
     * it bs integer.  For example, ubyte2int(0xFF) == 255, not -1.
     */
    public stbtic int ubyte2int(final byte x) {
        return x & 0xFF;
    }

    /**
     * Interprets the vblue of x as an unsigned two-byte number.
     */
    public stbtic int ushort2int(final short x) {
        return x & 0xFFFF;
    }

    /**
     * Interprets the vblue of x as an unsigned four-byte number.
     */
    public stbtic long uint2long(final int x) {
        return x & 0xFFFFFFFFL;
    }

    /**
     * Returns the int vblue that is closest to l.  That is, if l can fit into a
     * 32-bit unsigned number, returns (int)l.  Otherwise, returns either
     * Integer.MAX_VALUE or Integer.MIN_VALUE bs appropriate.
     */
    public stbtic int long2int(final long l) {
        int m;
        if (l < (m = Integer.MAX_VALUE) &&
            l > (m = Integer.MIN_VALUE))
            return (int)l;
        return m;
    }
}
