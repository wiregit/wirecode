package com.limegroup.gnutella.downloader;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.http.HttpException;

import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.LifecycleManager;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SaveLocationException;
import com.limegroup.gnutella.lws.server.LWSManager;

/**
 * A class that initializes listeners to the passed in instance of
 * {@link LWSManager}.
 */
public interface LWSIntegrationServices {

    /** Constants for querying the information about the running client. */
    public enum Info {

        /** Constant key for the value of {@link LimeWireUtils#isAlphaRelease()}. */
        IsAlphaRelease("is.alpha.release"),

        /**
         * Constant key for the value of
         * {@link LimeWireUtils#getMinorVersionNumber()}.
         */
        MinorVersionNumber("minor.version.number"),

        /** Constant key for the value of {@link LimeWireUtils#isPro()}. */
        IsPro("is.pro"),

        /**
         * Constant key for the value of
         * {@link LimeWireUtils#getMajorVersionNumber()}.
         */
        MajorVersionNumber("major.version.number"),

        /**
         * Constant key for the value of
         * {@link LimeWireUtils#getServiceVersionNumber()}.
         */
        ServiceVersionNumber("service.version.number"),

        /** Constant key for the value of {@link LimeWireUtils#isBetaRelease()}. */
        IsBetaRelease("is.beta.release"),

        /** Constant key for the value of {@link LimeWireUtils#getVendor()}. */
        Vendor("vendor"),

        /**
         * constant key for the value of
         * {@link LimeWireUtils#getLimeWireVersion()}.
         */
        Version("version");

        private final String s;

        Info(String s) {
            this.s = s;
        }

        public String getValue() {
            return s;
        }

        public String toString() {
            return s;
        }
    }

    /**
     * Initializes this in the start up of the {@link LifecycleManager}
     */
    void init();

    /**
     * The prefix with which to start download URLS. For example, if we call the
     * <code>Download</code> command with a <code>url</code> argument
     * <code>/SomeURL</code>, then if we passed in <code>limewire.org</code>
     * to this method, the resulting download would come from
     * <code>http://limewire.org/SomeURL</code>.
     * 
     * @param downloadPrefix new download prefix
     */
    void setDownloadPrefix(String downloadPrefix);
    
    /*
     * The following two are mainly for testing.
     */
    
    /**
     * Returns a new {@link RemoveFileDesc} for the file name, relative path,
     * and file length given.
     * 
     * @param fileName simple file name to which we save
     * @param urlString relative path of the URL we use to perform the download
     * @param length length of the file or <code>-1</code> to look up the
     *        length remotely
     * @return a new {@link RemoveFileDesc} for the file name, relative path,
     *         and file length given.
     */
    RemoteFileDesc createRemoteFileDescriptor(String fileName, String urlString, long length)
            throws IOException, URISyntaxException, HttpException, InterruptedException;

    /**
     * Returns a new Store {@link Downloader} for the given arguments.
     * 
     * @param rfd file descriptor used for the download. This should be created
     *        from {@link #createRemoteFileDescriptor(String, String, long)}.
     * @param saveDir directory to which we save the downloaded file
     * @return a new Store {@link Downloader} for the given arguments.
     * @throws SaveLocationException
     */
    Downloader createDownloader(RemoteFileDesc rfd, File saveDir) throws SaveLocationException;
    
}
