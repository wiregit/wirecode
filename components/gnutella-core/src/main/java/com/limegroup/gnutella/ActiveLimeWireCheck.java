package com.limegroup.gnutella;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.FileLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.settings.NetworkSettings;
import org.limewire.io.ByteReader;
import org.limewire.io.IOUtils;
import org.limewire.io.NetworkUtils;
import org.limewire.util.CommonUtils;
import org.limewire.util.OSUtils;

import com.limegroup.gnutella.browser.ExternalControl;

/**
 * Allows one to check if a LimeWire is already running under this user's name.
 */
public class ActiveLimeWireCheck {

    private static final Log LOG = LogFactory.getLog(ActiveLimeWireCheck.class);

    // Old-fashioned singleton because it's used before the injector is created
    private static final ActiveLimeWireCheck instance = new ActiveLimeWireCheck();

    private volatile RandomAccessFile file = null;
    private volatile FileLock lock = null;

    public static ActiveLimeWireCheck instance() {
        return instance;
    }

    /**
     * Checks if the client is already running, and if so, tries to pass it the
     * arguments, which may be null. Arguments are not passed on OSX.
     * 
     * @return true if another instance is running.
     * @throws ActiveLimeWireException if another instance appears to be
     * running but it can't be contacted.
     */
    public boolean checkForActiveLimeWire(String[] args)
    throws ActiveLimeWireException {
        // Only pass through args on windows/linux
        if(!OSUtils.isWindows() && !OSUtils.isLinux())
            return testForLimeWire(null);
        if(args == null || args.length == 0)
            return testForLimeWire(null);
        return testForLimeWire(ExternalControl.preprocessArgs(args));
    }

    /**
     * Releases the file lock obtained at startup. If no lock was obtained,
     * this method has no effect. If a lock was obtained and this method is
     * never called, the OS will remove the lock when the JVM exits.
     */
    public void releaseLock() {
        if(lock != null) {
            try {
                lock.release();
                file.close(); // This also closes the channel
                LOG.trace("Released lock");
            } catch(IOException e) {
                LOG.debug("Failed to release lock", e);
            }
        }
    }

    /**
     * Checks if the client is already running, and if so, tries to pass it the
     * argument, which may be null.
     * 
     * @return true if another instance is running, in which case this instance
     * must quit.
     * @throws ActiveLimeWireException if another instance appears to be
     * running but it can't be contacted.
     */
    private boolean testForLimeWire(String arg) throws ActiveLimeWireException {
        // Try to lock a file in the settings dir; if the lock can't be
        // acquired, another instance of LW is running.
        try {
            File f = new File(CommonUtils.getUserSettingsDir(), "lock");
            f.createNewFile();
            file = new RandomAccessFile(f, "rw");
            lock = file.getChannel().tryLock(); // Null if we can't get the lock
        } catch(IOException e) {
            // Couldn't access the file - something's badly wrong, so quit
            LOG.error("Failed to access lock file", e);
            return true;
        }
        if(lock == null) {
            // Another instance is running, but it may not be listening on the
            // port yet; try for one minute to contact it.
            LOG.trace("Could not acquire lock");
            long start = System.currentTimeMillis();
            while(System.currentTimeMillis() - start < 60 * 1000) {
                if(tryToContactRunningLimeWire(arg)) {
                    LOG.trace("Contacted existing instance");
                    return true;
                }
                try {
                    Thread.sleep(2000);
                } catch(InterruptedException ignored) {}
            }
            LOG.trace("Could not contact existing instance");
            throw new ActiveLimeWireException();
        }
        LOG.trace("Acquired lock");
        // We acquired the lock, but an older instance that doesn't know about
        // the lock might be running - try the port just in case.
        if(tryToContactRunningLimeWire(arg)) {
            LOG.trace("Contacted existing instance");
            releaseLock(); // This isn't strictly necessary
            return true;
        } else {
            return false;
        }
    }

    /**
     * Tries to contact an existing instance of LimeWire on the configured port
     * and pass it the argument, which may be null.
     * 
     * @return true if another instance was successfully contacted on the port.
     */
    private boolean tryToContactRunningLimeWire(String arg) {
        Socket socket = null;
        int port = NetworkSettings.PORT.getValue();
        if(!NetworkUtils.isValidPort(port))
            return false;
        String type = ExternalControl.isTorrentRequest(arg) ? "TORRENT" : "MAGNET";
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress("127.0.0.1", port), 1000);
            InputStream istream = socket.getInputStream(); 
            socket.setSoTimeout(1000); 
            ByteReader byteReader = new ByteReader(istream);
            OutputStream os = socket.getOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(os);
            BufferedWriter out = new BufferedWriter(osw);
            out.write(type + " " + arg + " ");
            out.write("\r\n");
            out.flush();
            String str = byteReader.readLine();
            return str != null && str.startsWith(CommonUtils.getUserName());
        } catch (IOException e) {
            LOG.debug("Failed to contact existing instance", e);
        } finally {
            IOUtils.close(socket);
        }
        return false;
    }

    public static class ActiveLimeWireException extends Exception {}
}
