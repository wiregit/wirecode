package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.LifecycleManager;
import com.limegroup.gnutella.lws.server.LWSManager;

/**
 * A class that initializes listeners to the passed in instance of {@link LWSManager}.
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
}
