package com.limegroup.gnutella.util;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import com.limegroup.gnutella.MessageService;
import com.limegroup.gnutella.ErrorService;

/**
 * Provides utility I/O methods, used by multiple classes
 * @author Anurag Singla
 */
public class IOUtils {
    
    /**
     * Attempts to handle an IOException.  If we know expect the problem,
     * we can either ignore it or display a friendly error (both returning
     * true, for handled) or expect the outer-world to handle it (and
     * return false).
     *
     * If friendly is null, a generic error related to the bug is displayed.
     *
     * @return true if we could handle the error.
     */
    public static boolean handleException(IOException ioe, String friendly) {
        if(friendly == null)
            friendly = "GENERIC";
        
        return handle(ioe, friendly);
    }
    
    /**
     * Looks through every cause of an Exception to see if we know how 
     * to handle it.
     */
    private static boolean handle(Throwable e, String friendly) {
        while(e != null) {
            String msg = e.getMessage();
            
            if(msg != null) {
                msg = msg.toLowerCase();
                // If the user's disk is full, let them know.
                if(StringUtils.contains(msg, "no space left") || 
                   StringUtils.contains(msg, "not enough space")) {
                    MessageService.showError("ERROR_DISK_FULL_" + friendly);
                    return true;
                }
                // If the file is locked, let them know.
                if(StringUtils.contains(msg, "being used by another process")) {
                    MessageService.showError("ERROR_LOCKED_BY_PROCESS_" + friendly);
                    return true;
                }
                // If we don't have permissions to write, let them know.
                if(StringUtils.contains(msg, "access is denied") || 
                   StringUtils.contains(msg, "permission denied") ) {
                    MessageService.showError("ERROR_ACCESS_DENIED_" + friendly);
                    return true;
                }
                
                if(StringUtils.contains(msg, "invalid argument")) {
                    MessageService.showError("ERROR_INVALID_NAME_" + friendly);
                    return true;
                }
            }
            
            e = e.getCause();
        }

        // dunno what to do, let the outer world handle it.
        return false;
    }       

   /**
     * Returns the first word of specified maximum size up to the first space
     * and returns it.  This does not read up to the first whitespace
     * character -- it only looks for a single space.  This is particularly
     * useful for reading HTTP requests, as the request method, the URI, and
     * the HTTP version must all be separated by a single space.
     * Note that only one extra character is read from the stream in the case of
     * success (the white space character after the word).
     *
     * @param in The input stream from where to read the word
     * @param maxSize The maximum size of the word.
     * @return the first word (i.e., no whitespace) of specified maximum size
     * @exception IOException if the word of specified maxSize couldnt be read,
     * either due to stream errors, or timeouts
     */
    public static String readWord(InputStream in, int maxSize)
      throws IOException {
        final char[] buf = new char[maxSize];
        int i = 0;
        //iterate till maxSize + 1 (for white space)
        while (true) {
            int got;
            try {
                got = in.read();
                if (got >= 0) { // not EOF
                    if ((char)got != ' ') { //didn't get word. Exclude space.
                        if (i < maxSize) { //We dont store the last letter
                            buf[i++] = (char)got;
                            continue;
                        }
                        //if word of size upto maxsize not found, throw an
                        //IOException. (Fixes bug 26 in 'core' project)
                        throw new IOException("could not read word");
                    }
                    return new String(buf, 0, i);
                }
                throw new IOException("unexpected end of file");
            } catch (ArrayIndexOutOfBoundsException aioobe) {
                // thrown in strange circumstances of in.read(), consider IOX.
                throw new IOException("unexpected aioobe");
            }
        }
    }
    
    /**
     * Reads a word, but if the connection closes, returns the largest word read
     * instead of throwing an IOX.
     */
    public static String readLargestWord(InputStream in, int maxSize)
      throws IOException {
        final char[] buf = new char[maxSize];
        int i = 0;
        //iterate till maxSize + 1 (for white space)
        while (true) {
            int got;
            try {
                got = in.read();
                if(got == -1) {
                    if(i == 0)
                        throw new IOException("could not read any word.");
                    else
                        return new String(buf, 0, i);
                } else if (got >= 0) {
                    if ((char)got != ' ') { //didn't get word. Exclude space.
                        if (i < maxSize) { //We dont store the last letter
                            buf[i++] = (char)got;
                            continue;
                        }
                        //if word of size upto maxsize not found, throw an
                        //IOException. (Fixes bug 26 in 'core' project)
                        throw new IOException("could not read word");
                    }
                    return new String(buf, 0, i);
                }
                throw new IOException("unknown got amount");
            } catch (ArrayIndexOutOfBoundsException aioobe) {
                // thrown in strange circumstances of in.read(), consider IOX.
                throw new IOException("unexpected aioobe");
            }
        }
    }
    
    public static long ensureSkip(InputStream in, long length) throws IOException {
    	long skipped = 0;
    	while(skipped < length) {
    		long current = in.skip(length - skipped);
    	    if(current == -1 || current == 0)
    	        throw new EOFException("eof");
    	    else
    	        skipped += current;
    	}
    	return skipped;
    }
    
    public static void close(InputStream in) {
        if(in != null) {
            try {
                in.close();
            } catch(IOException ignored) {}
        }
    }
    
    public static void close(OutputStream out) {
        if(out != null) {
            try {
                out.close();
            } catch(IOException ignored) {}
        }
    }
    
    public static void close(RandomAccessFile raf) {
        if(raf != null) {
            try {
                raf.close();
            } catch(IOException ignored) {}
        }
    }
    
    public static void close(Socket s) {
        if(s != null) {
            try {
                close(s.getInputStream());
            } catch(IOException ignored) {}

            try {
                close(s.getOutputStream());
            } catch(IOException ignored) {}

            try {
                s.close();
            } catch(IOException ignored) {}
        }
    }
    
    public static void close(ServerSocket s) {
        if(s != null) {
            try {
                s.close();
            } catch(IOException ignored) {}
        }
    }
    
    /**
     * Deflates (compresses) the data.
     */
    public static byte[] deflate(byte[] data) {
        OutputStream dos = null;
        try {
            ByteArrayOutputStream baos=new ByteArrayOutputStream();
            dos = new DeflaterOutputStream(baos);
            dos.write(data, 0, data.length);
            dos.close();                      //flushes bytes
            return baos.toByteArray();
        } catch(IOException impossible) {
            ErrorService.error(impossible);
            return null;
        } finally {
            IOUtils.close(dos);
        }
    }
    
    /**
     * Inflates (uncompresses) the data.
     */
    public static byte[] inflate(byte[] data) throws IOException {
        InputStream in = null;
        try {
            in = new InflaterInputStream(new ByteArrayInputStream(data));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[64];
            while(true) {
                int read = in.read(buf, 0, buf.length);
                if(read == -1)
                    break;
                out.write(buf, 0, read);
            }
            return out.toByteArray();
        } catch(OutOfMemoryError oome) {
            throw new IOException(oome.getMessage());
        } finally {
            IOUtils.close(in);
        }
    }    

}
