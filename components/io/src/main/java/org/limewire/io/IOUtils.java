package org.limewire.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.limewire.i18n.I18nMarker;
import org.limewire.service.ErrorService;
import org.limewire.service.MessageService;
import org.limewire.util.FileUtils;
import org.limewire.util.StringUtils;


/**
 * Provides utility input and output related methods. <code>IOUtils</code> 
 * includes methods to read and skip over data, and handle exceptions. Furthermore,
 * this class lets you compress and uncompress data and to close {@link Closeable}
 * objects, {@link Socket Sockets} and {@link ServerSocket ServerSockets}.
 */
public class IOUtils {
    
    // mark message strings for translation
    static {
        I18nMarker.marktr("LimeWire was unable to write a necessary file because your hard drive is full. To continue using LimeWire you must free up space on your hard drive.");
		I18nMarker.marktr("LimeWire was unable to open a necessary file because another program has locked the file. LimeWire may act unexpectedly until this file is released.");
		I18nMarker.marktr("LimeWire was unable to write a necessary file because you do not have the necessary permissions. Your preferences may not be maintained the next time you start LimeWire, or LimeWire may behave in unexpected ways.");
		I18nMarker.marktr("LimeWire cannot open a necessary file because the filename contains characters which are not supported by your operating system. LimeWire may behave in unexpected ways.");

        I18nMarker.marktr("LimeWire cannot download the selected file because your hard drive is full. To download more files, you must free up space on your hard drive.");
		I18nMarker.marktr("LimeWire was unable to download the selected file because another program is using the file. Please close the other program and retry the download.");
		I18nMarker.marktr("LimeWire was unable to create or continue writing an incomplete file for the selected download because you do not have permission to write files to the incomplete folder. To continue using LimeWire, please choose a different Save Folder.");
		I18nMarker.marktr("LimeWire was unable to open the incomplete file for the selected download because the filename contains characters which are not supported by your operating system.");
    }
    
    /**
     * Attempts to handle an IOException. If we know expect the problem,
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
                if(StringUtils.contains(msg, "being used by another process") ||
                   StringUtils.contains(msg, "with a user-mapped section open")) {
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
     * and returns it. This does not read up to the first whitespace
     * character -- it only looks for a single space. This is particularly
     * useful for reading HTTP requests, as the request method, the URI, and
     * the HTTP version must all be separated by a single space.
     * Note that only one extra character is read from the stream in the case of
     * success (the white space character after the word).
     *
     * @param in The input stream from where to read the word
     * @param maxSize The maximum size of the word.
     * @return the first word (i.e., no whitespace) of specified maximum size
     * @exception IOException if the word of specified maxSize couldn't be read,
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
                        if (i < maxSize) { //We don't store the last letter
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
                        if (i < maxSize) { //We don't store the last letter
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

    /**
     * A utility method to close Closeable objects (Readers, Writers, 
     * Input- and OutputStreams and RandomAccessFiles).
     */
    public static void close(Closeable closeable) {
        FileUtils.close(closeable);
    }
    
    /**
     * A utility method to flush Flushable objects (Readers, Writers, 
     * Input- and OutputStreams and RandomAccessFiles).
     */
    public static void flush(Flushable flushable) {
        FileUtils.flush(flushable);
    }
    
    /**
     * A utility method to close Sockets
     */
    public static void close(Socket s) {
        if(s != null) {
            try {
                s.close();
            } catch(IOException ignored) {}
            
            try {
                close(s.getInputStream());
            } catch(IOException ignored) {}

            try {
                close(s.getOutputStream());
            } catch(IOException ignored) {}
        }
    }
    
    /**
     * A utility method to close ServerSockets
     */
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
        Deflater def = null;
        try {
            def = Pools.getDeflaterPool().borrowObject();
            ByteArrayOutputStream baos=new ByteArrayOutputStream();
            dos = new DeflaterOutputStream(baos, def);
            dos.write(data, 0, data.length);
            dos.close();                      //flushes bytes
            return baos.toByteArray();
        } catch(IOException impossible) {
            ErrorService.error(impossible);
            return null;
        } finally {
            close(dos);
            Pools.getDeflaterPool().returnObject(def);
        }
    }
    
    /**
     * Inflates (uncompresses) the data.
     */
    public static byte[] inflate(byte[] data) throws IOException {
        InputStream in = null;
        Inflater inf = null;
        try {
            inf = Pools.getInflaterPool().borrowObject();
            in = new InflaterInputStream(new ByteArrayInputStream(data), inf);
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
            close(in);
            Pools.getInflaterPool().returnObject(inf);
        }
    }    

}
