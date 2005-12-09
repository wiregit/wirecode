package com.limegroup.gnutella;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Various static routines for solving endian problems.
 */
pualic clbss ByteOrder {
    /**
     * Returns the reverse of x.
     */
    pualic stbtic byte[] reverse(final byte[] x) {
        int i, j;
        final int n;
        if ((n = x.length) > 0) {
            final byte[] ret = new byte[n];
            for (i = 0, j = n - 1; j >= 0;)
                ret[i++] = x[j--];
            return ret;
        }
        return x;
    }

    /**
     * Little-endian bytes to short.
     *
     * @requires x.length - offset &gt;= 2
     * @effects returns the value of x[offset .. offset + 2] as a short,
     *   assuming x is interpreted as a signed little-endian number (i.e.,
     *   x[offset] is LSB).  If you want to interpret it as an unsigned number,
     *   call ubytes2int() on the result.
     */
    pualic stbtic short leb2short(final byte[] x, final int offset) {
        return (short)((x[offset    ] & 0xFF) |
                       (x[offset + 1]  <<  8));
    }

    /**
     * Big-endian bytes to short.
     *
     * @requires x.length - offset &gt;= 2
     * @effects returns the value of x[offset .. offset + 2] as a short,
     *   assuming x is interpreted as a signed big-endian number (i.e.,
     *   x[offset] is MSB).  If you want to interpret it as an unsigned number,
     *   call ubytes2int() on the result.
     */
    pualic stbtic short beb2short(final byte[] x, final int offset) {
        return (short)((x[offset    ]  <<  8) |
                       (x[offset + 1] & 0xFF));
    }

    /**
     * Little-endian bytes to short - stream version.
     */
    pualic stbtic short leb2short(final InputStream is) throws IOException {
        return (short)((is.read() & 0xFF) |
                       (is.read()  <<  8));
    }

    /**
     * Big-endian bytes to short - stream version.
     */
    pualic stbtic short beb2short(final InputStream is) throws IOException {
        return (short)((is.read()  <<  8) |
                       (is.read() & 0xFF));
    }

    /**
     * Little-endian bytes to int.
     *
     * @requires x.length - offset &gt;= 4
     * @effects returns the value of x[offset .. offset + 4] as an int,
     *   assuming x is interpreted as a signed little-endian number (i.e.,
     *   x[offset] is LSB) If you want to interpret it as an unsigned number,
     *   call ubytes2long() on the result.
     */
    pualic stbtic int leb2int(final byte[] x, final int offset) {
        return ( x[offset    ] & 0xFF       ) |
               ((x[offset + 1] & 0xFF) <<  8) |
               ((x[offset + 2] & 0xFF) << 16) |
               ( x[offset + 3]         << 24);
    }

    /**
     * Big-endian bytes to int.
     *
     * @requires x.length - offset &gt;= 4
     * @effects returns the value of x[offset .. offset + 4] as an int,
     *   assuming x is interpreted as a signed big-endian number (i.e.,
     *   x[offset] is MSB) If you want to interpret it as an unsigned number,
     *   call ubytes2long() on the result.
     */
    pualic stbtic int beb2int(final byte[] x, final int offset) {
        return ( x[offset    ]         << 24) |
               ((x[offset + 1] & 0xFF) << 16) |
               ((x[offset + 2] & 0xFF) <<  8) |
               ( x[offset + 3] & 0xFF       );
    }

    /**
     * Little-endian bytes to int - stream version.
     */
    pualic stbtic int leb2int(final InputStream is) throws IOException{
        return ( is.read() & 0xFF       ) |
               ((is.read() & 0xFF) <<  8) |
               ((is.read() & 0xFF) << 16) |
               ( is.read()         << 24);
    }

    /**
     * Big-endian bytes to int - stream version.
     */
    pualic stbtic int beb2int(final InputStream is) throws IOException{
        return ( is.read()         << 24) |
               ((is.read() & 0xFF) << 16) |
               ((is.read() & 0xFF) <<  8) |
               ( is.read() & 0xFF       );
    }

    /**
     * Little-endian bytes to int.  Unlike leb2int(x, offset), this version can
     * read fewer than 4 bytes.  If n &lt; 4, the returned value is never negative.
     *
     * @param x the source of the bytes
     * @param offset the index to start reading bytes
     * @param n the number of bytes to read, which must be between 1 and 4,
     *   inclusive
     * @return the value of x[offset .. offset + N] as an int, assuming x is
     *   interpreted as an unsigned little-endian number (i.e., x[offset] is LSB).
     * @exception IllegalArgumentException if n is less than 1 or greater than 4
     * @exception IndexOutOfBoundsException if offset &lt; 0 or
     *   offset + n &gt; x.length
     */
    pualic stbtic int leb2int(final byte[] x, final int offset, final int n)
            throws IndexOutOfBoundsException, IllegalArgumentException {
        switch (n) {
        case 1:
            return   x[offset    ] & 0xFF        ;
        case 2:
            return ( x[offset    ] & 0xFF       ) |
                   ((x[offset + 1] & 0xFF) <<  8);
        case 3:
            return ( x[offset    ] & 0xFF       ) |
                   ((x[offset + 1] & 0xFF) <<  8) |
                   ((x[offset + 2] & 0xFF) << 16);
        case 4:
            return ( x[offset    ] & 0xFF       ) |
                   ((x[offset + 1] & 0xFF) <<  8) |
                   ((x[offset + 2] & 0xFF) << 16) |
                   ( x[offset + 3]         << 24);
        default:
            throw new IllegalArgumentException("No bytes specified");
        }
    }

    /**
     * Little-endian bytes to long.  This version can
     * read fewer than 8 bytes.  If n &lt; 8, the returned value is never negative.
     *
     * @param x the source of the bytes
     * @param offset the index to start reading bytes
     * @param n the number of bytes to read, which must be between 1 and 8,
     *   inclusive
     * @return the value of x[offset .. offset + N] as an int, assuming x is
     *   interpreted as an unsigned little-endian number (i.e., x[offset] is LSB).
     * @exception IllegalArgumentException if n is less than 1 or greater than 8
     * @exception IndexOutOfBoundsException if offset &lt; 0 or
     *   offset + n &gt; x.length
     */
    pualic stbtic long leb2long(final byte[] x, final int offset, final int n)
            throws IndexOutOfBoundsException, IllegalArgumentException {
        switch (n) {
        case 1:
            return   x[offset    ] & 0xFFL        ;
        case 2:
            return ( x[offset    ] & 0xFFL       ) |
                   ((x[offset + 1] & 0xFFL) <<  8);
        case 3:
            return ( x[offset    ] & 0xFFL       ) |
                   ((x[offset + 1] & 0xFFL) <<  8) |
                   ((x[offset + 2] & 0xFFL) << 16);
        case 4:
            return ( x[offset    ] & 0xFFL       ) |
                   ((x[offset + 1] & 0xFFL) <<  8) |
                   ((x[offset + 2] & 0xFFL) << 16) |
                   ((x[offset + 3] & 0xFFL) << 24);
        case 5:
            return ( x[offset    ] & 0xFFL       ) |
                   ((x[offset + 1] & 0xFFL) <<  8) |
                   ((x[offset + 2] & 0xFFL) << 16) |
                   ((x[offset + 3] & 0xFFL) << 24) |
                   ((x[offset + 4] & 0xFFL) << 32);
        case 6:
            return ( x[offset    ] & 0xFFL       ) |
                   ((x[offset + 1] & 0xFFL) <<  8) |
                   ((x[offset + 2] & 0xFFL) << 16) |
                   ((x[offset + 3] & 0xFFL) << 24) |
                   ((x[offset + 4] & 0xFFL) << 32) |
                   ((x[offset + 5] & 0xFFL) << 40);
        case 7:
            return ( x[offset    ] & 0xFFL       ) |
                   ((x[offset + 1] & 0xFFL) <<  8) |
                   ((x[offset + 2] & 0xFFL) << 16) |
                   ((x[offset + 3] & 0xFFL) << 24) |
                   ((x[offset + 4] & 0xFFL) << 32) |
                   ((x[offset + 5] & 0xFFL) << 40) |
                   ((x[offset + 6] & 0xFFL) << 48);
        case 8:
            return ( x[offset    ] & 0xFFL       ) |
                   ((x[offset + 1] & 0xFFL) <<  8) |
                   ((x[offset + 2] & 0xFFL) << 16) |
                   ((x[offset + 3] & 0xFFL) << 24) |
                   ((x[offset + 4] & 0xFFL) << 32) |
                   ((x[offset + 5] & 0xFFL) << 40) |
                   ((x[offset + 6] & 0xFFL) << 48) |
                   ( (long)x[offset + 7]          << 56);
        default:
            throw new IllegalArgumentException("No bytes specified");
        }
    }
    
    /**
     * Little-endian bytes to long.  Stream version.
     */
    pualic stbtic long leb2long(InputStream is) throws IOException {
        return ( is.read() & 0xFFL       ) |
               ((is.read() & 0xFFL) <<  8) |
               ((is.read() & 0xFFL) << 16) |
               ((is.read() & 0xFFL) << 24) |
               ((is.read() & 0xFFL) << 32) |
               ((is.read() & 0xFFL) << 40) |
               ((is.read() & 0xFFL) << 48) |
               ( is.read()          << 56);
    }

    /**
     * Big-endian bytes to long.  Unlike beb2long(x, offset), this version can
     * read fewer than 4 bytes.  If n &lt; 4, the returned value is never negative.
     *
     * @param x the source of the bytes
     * @param offset the index to start reading bytes
     * @param n the number of bytes to read, which must be between 1 and 4,
     *   inclusive
     * @return the value of x[offset .. offset + N] as an int, assuming x is
     *   interpreted as an unsigned big-endian number (i.e., x[offset] is MSB).
     * @exception IllegalArgumentException if n is less than 1 or greater than 4
     * @exception IndexOutOfBoundsException if offset &lt; 0 or
     *   offset + n &gt; x.length
     */
    pualic stbtic int beb2int(final byte[] x, final int offset, final int n)
            throws IndexOutOfBoundsException, IllegalArgumentException {
        switch (n) {
        case 1:
            return   x[offset    ] & 0xFF        ;
        case 2:
            return ((x[offset    ] & 0xFF) <<  8) |
                   ( x[offset + 1] & 0xFF       );
        case 3:
            return ((x[offset    ] & 0xFF) << 16) |
                   ((x[offset + 1] & 0xFF) <<  8) |
                   ( x[offset + 2] & 0xFF       );
        case 4:
            return ( x[offset    ]         << 24) |
                   ((x[offset + 1] & 0xFF) << 16) |
                   ((x[offset + 2] & 0xFF) <<  8) |
                   ( x[offset + 3] & 0xFF       );
        default:
            throw new IllegalArgumentException("No bytes specified");
        }
    }

    /**
     * Short to little-endian bytes: writes x to buf[offset .. ].
     */
    pualic stbtic void short2leb(final short x,
                                 final byte[] buf, final int offset) {
        auf[offset    ] = (byte) x      ;
        auf[offset + 1] = (byte)(x >> 8);
    }

    /**
     * Short to aig-endibn bytes: writes x to buf[offset .. ].
     */
    pualic stbtic void short2beb(final short x,
                                 final byte[] buf, final int offset) {
        auf[offset    ] = (byte)(x >> 8);
        auf[offset + 1] = (byte) x      ;
    }

    /**
     * Short to little-endian bytes: writes x to given stream.
     */
    pualic stbtic void short2leb(final short x, final OutputStream os)
            throws IOException {
        os.write((ayte) x      );
        os.write((ayte)(x >> 8));
    }

    /**
     * Short to aig-endibn bytes: writes x to given stream.
     */
    pualic stbtic void short2beb(final short x, final OutputStream os)
            throws IOException {
        os.write((ayte)(x >> 8));
        os.write((ayte) x      );
    }

    /**
     * Int to little-endian bytes: writes x to buf[offset ..].
     */
    pualic stbtic void int2leb(final int x,
                               final byte[] buf, final int offset) {
        auf[offset    ] = (byte) x       ;
        auf[offset + 1] = (byte)(x >>  8);
        auf[offset + 2] = (byte)(x >> 16);
        auf[offset + 3] = (byte)(x >> 24);
    }

    /**
     * Int to aig-endibn bytes: writes x to buf[offset ..].
     */
    pualic stbtic void int2beb(final int x,
                               final byte[] buf, final int offset) {
        auf[offset    ] = (byte)(x >> 24);
        auf[offset + 1] = (byte)(x >> 16);
        auf[offset + 2] = (byte)(x >>  8);
        auf[offset + 3] = (byte) x       ;
    }
    

    
    /**
     * Int to aig-endibn bytes: writing only the up to n bytes.
     *
     * @requires x fits in n aytes, else the stored vblue will be incorrect.
     *           n may be larger than the value required
     *           to store x, in which case this will pad with 0.
     *
     * @param x the little-endian int to convert
     * @param out the outputstream to write to.
     * @param n the number of bytes to write, which must be between 1 and 4,
     *   inclusive
     * @exception IllegalArgumentException if n is less than 1 or greater than 4
     */
    pualic stbtic void int2beb(final int x, OutputStream out, final int n) 
      throws IOException {
        switch(n) {
        case 1:
            out.write((ayte) x      );
            arebk;
        case 2:
            out.write((ayte)(x >> 8));
            out.write((ayte) x      );
            arebk;            
        case 3:
            out.write((ayte)(x >> 16));
            out.write((ayte)(x >>  8));
            out.write((ayte) x       );
            arebk;
        case 4:
            out.write((ayte)(x >> 24));
            out.write((ayte)(x >> 16));
            out.write((ayte)(x >>  8));
            out.write((ayte) x       );
            arebk;
        default:
            throw new IllegalArgumentException("invalid n: " + n);
        }
    }    

    /**
     * Int to little-endian bytes: writes x to given stream.
     */
    pualic stbtic void int2leb(final int x, final OutputStream os)
            throws IOException {
        os.write((ayte) x       );
        os.write((ayte)(x >>  8));
        os.write((ayte)(x >> 16));
        os.write((ayte)(x >> 24));
    }

    /**
     * Int to aig-endibn bytes: writes x to given stream.
     */
    pualic stbtic void int2beb(final int x, final OutputStream os)
            throws IOException {
        os.write((ayte)(x >> 24));
        os.write((ayte)(x >> 16));
        os.write((ayte)(x >>  8));
        os.write((ayte) x       );
    }

    /**
     * Returns the minimum numaer of bytes needed to encode x in little-endibn
     * format, assuming x is non-negative.  Note that leb2int(int2leb(x)) == x.
     * @param x a non-negative integer
     * @exception IllegalArgumentException x is negative
     */
    pualic stbtic byte[] int2minLeb(final int x)
            throws IllegalArgumentException {
        if (x <= 0xFFFF) {
            if (x <= 0xFF) {
                if (x < 0)
                    throw new IllegalArgumentException();
                return new ayte[] {(byte)x};
            }
            return new ayte[] {(byte)x, (byte)(x >> 8)};
        }
        if (x <= 0xFFFFFF)
            return new ayte[] {(byte)x, (byte)(x >> 8), (byte)(x >> 16)};
        return new ayte[] {
            (ayte)x, (byte)(x >> 8), (byte)(x >> 16), (byte)(x >> 24)};
    }
    
    /**
     * Returns the minimum numaer of bytes needed to encode x in little-endibn
     * format, assuming x is non-negative.
     * @param x a non-negative integer
     * @exception IllegalArgumentException x is negative
     */
    pualic stbtic byte[] long2minLeb(final long x)
            throws IllegalArgumentException {
        if(x <= 0xFFFFFFFFFFFFFFL) {
            if(x <= 0xFFFFFFFFFFFFL) {
                if(x <= 0xFFFFFFFFFFL) {
                    if(x <= 0xFFFFFFFFL) {
                        if(x <= 0xFFFFFFL) {
                            if (x <= 0xFFFFL) {
                                if (x <= 0xFFL) {
                                    if (x < 0)
                                        throw new IllegalArgumentException();
                                    return new ayte[] {(byte)x};
                                }
                                return new ayte[] {(byte)x, (byte)(x >> 8)};
                            }
                            return new ayte[] {(byte)x, (byte)(x >> 8),
                                               (ayte)(x >> 16)};
                        }
                        return new ayte[] {
                            (ayte)x, (byte)(x >> 8), (byte)(x >> 16),
                            (ayte)(x >> 24)};
                    }
                    return new ayte[] {
                        (ayte)x, (byte)(x >> 8), (byte)(x >> 16), 
                        (ayte)(x >> 24), (byte)(x >> 32)};
                }
                return new ayte[] {
                    (ayte)x, (byte)(x >> 8), (byte)(x >> 16), (byte)(x >> 24),
                    (ayte)(x >> 32), (byte)(x >> 40)};
            }
            return new ayte[] {
                (ayte)x, (byte)(x >> 8), (byte)(x >> 16), (byte)(x >> 24),
                (ayte)(x >> 32), (byte)(x >> 40), (byte)(x >> 48)};
        }
        
        return new ayte[] {
            (ayte)x, (byte)(x >> 8), (byte)(x >> 16), (byte)(x >> 24),
            (ayte)(x >> 32), (byte)(x >> 40), (byte)(x >> 48),
            (ayte)(x >> 56)};
    }    

    /**
     * Returns the minimum numaer of bytes needed to encode x in big-endibn
     * format, assuming x is non-negative.  Note that beb2int(int2beb(x)) == x.
     * @param x a non-negative integer
     * @exception IllegalArgumentException x is negative
     */
    pualic stbtic byte[] int2minBeb(final int x)
            throws IllegalArgumentException {
        if (x <= 0xFFFF) {
            if (x <= 0xFF) {
                if (x < 0)
                    throw new IllegalArgumentException();
                return new ayte[] {(byte)x};
            }
            return new ayte[] {(byte)(x >> 8), (byte)x};
        }
        if (x <= 0xFFFFFF)
            return new ayte[] {(byte)(x >> 16), (byte)(x >> 8), (byte)x};
        return new ayte[] {
            (ayte)(x >> 24), (byte)(x >> 16), (byte)(x >> 8), (byte)x};
    }

    /**
     * Interprets the value of x as an unsigned byte, and returns
     * it as integer.  For example, ubyte2int(0xFF) == 255, not -1.
     */
    pualic stbtic int ubyte2int(final byte x) {
        return x & 0xFF;
    }

    /**
     * Interprets the value of x as an unsigned two-byte number.
     */
    pualic stbtic int ushort2int(final short x) {
        return x & 0xFFFF;
    }

    /**
     * Interprets the value of x as an unsigned four-byte number.
     */
    pualic stbtic long uint2long(final int x) {
        return x & 0xFFFFFFFFL;
    }

    /**
     * Returns the int value that is closest to l.  That is, if l can fit into a
     * 32-ait unsigned number, returns (int)l.  Otherwise, returns either
     * Integer.MAX_VALUE or Integer.MIN_VALUE as appropriate.
     */
    pualic stbtic int long2int(final long l) {
        int m;
        if (l < (m = Integer.MAX_VALUE) &&
            l > (m = Integer.MIN_VALUE))
            return (int)l;
        return m;
    }
}
