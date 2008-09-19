package org.limewire.core.settings;

import java.io.File;

import org.limewire.setting.FileSetting;
import org.limewire.setting.StringSetting;
import org.limewire.util.SystemUtils;
import org.limewire.util.SystemUtils.SpecialLocations;

/**
 * Settings to override mozilla default behavior.
 */
public class MozillaSettings extends LimeProps {

    private MozillaSettings() {
    }

    /**
     * The default download directory for the mozilla downloads.
     */
    public static final FileSetting DOWNLOAD_DIR = FACTORY.createFileSetting(
            "MOZILLA_DOWNLOAD_DIR", new File(SystemUtils.getSpecialPath(SpecialLocations.DESKTOP)
                    + "/mozdownloads"));

    /**
     * The list of mime-types to automatically download. Mime-types in this list
     * will not get the mozilla save or open dialog.
     */
    public static final StringSetting DOWNLOAD_MIME_TYPES = FACTORY
            .createStringSetting(
                    "MOZILLA_DOWNLOAD_MIME_TYPES",
                    "application/octet-stream, application/x-msdownload, application/exe, application/x-exe, application/dos-exe, vms/exe, application/x-winexe, application/msdos-windows, application/x-msdos-program, application/x-msdos-program, application/x-unknown-application-octet-stream, application/vnd.ms-powerpoint, application/excel, application/vnd.ms-publisher, application/x-unknown-message-rfc822, application/vnd.ms-excel, application/msword, application/x-mspublisher, application/x-tar, application/zip, application/x-gzip,application/x-stuffit,application/vnd.ms-works, application/powerpoint, application/rtf, application/postscript, application/x-gtar, audio/mpeg");


}
