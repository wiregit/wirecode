package org.limewire.ui.swing.shell;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.limewire.ui.swing.util.I18n;
import org.limewire.util.OSUtils;
import org.limewire.util.SystemUtils;

public class LimeAssociations {

    private enum AssociationType {
        TORRENT, MAGNET
    }

    private static final String PROGRAM;

    private static final String UNSUPPORTED_PLATFORM = "";

    static {
        if (OSUtils.isWindows())
            PROGRAM = "LimeWire";
        else if (OSUtils.isUnix())
            PROGRAM = System.getProperty("unix.executable", UNSUPPORTED_PLATFORM);
        else
            PROGRAM = UNSUPPORTED_PLATFORM;
    }

    private static Map<AssociationType, LimeAssociationOption> fileAssociations = null;

    public synchronized static boolean anyAssociationsSupported() {
        return isTorrentAssociationSupported() || isMagnetAssociationSupported();
    }

    public synchronized static boolean isTorrentAssociationSupported() {
        return getTorrentAssociation() != null;
    }

    public synchronized static boolean isMagnetAssociationSupported() {
        return getMagnetAssociation() != null;
    }

    public static LimeAssociationOption getTorrentAssociation() {
        return getSupportedAssociations().get(AssociationType.TORRENT);
    }

    public static LimeAssociationOption getMagnetAssociation() {
        return getSupportedAssociations().get(AssociationType.MAGNET);
    }

    private static Map<AssociationType, LimeAssociationOption> getSupportedAssociations() {
        // if (!ResourceManager.instance().isJdicLibraryLoaded()) TODO can we
        // assume it is loaded?
        // return Collections.emptyList();

        if (fileAssociations == null) {
            fileAssociations = new HashMap<AssociationType, LimeAssociationOption>();
            // strings that the shell will understand
            String fileOpener = null;
            String fileIcon = null;
            String protocolOpener = null;

            if (OSUtils.isWindows()) {
                String runningPath = SystemUtils.getRunningPath();
                if (runningPath != null && runningPath.endsWith(PROGRAM + ".exe")) {
                    protocolOpener = runningPath;
                    fileOpener = "\"" + runningPath + "\" \"%1\"";
                    fileIcon = runningPath + ",1";
                }
            }

            // if we have a string that opens a file, register torrents
            if (fileOpener != null) {
                ShellAssociation file = new FileTypeAssociation("torrent",
                        "application/x-bittorrent", fileOpener, "open", "LimeWire Torrent",
                        fileIcon);
                LimeAssociationOption torrent = new LimeAssociationOption(file, ".torrent", I18n
                        .tr("\".torrent\" files"));
                fileAssociations.put(AssociationType.TORRENT, torrent);
            }

            // if we have a string that opens a protocol, register magnets
            if (protocolOpener != null) {
                // Note: MagnetAssociation will only work on windows
                MagnetAssociation mag = new MagnetAssociation(PROGRAM, protocolOpener);
                LimeAssociationOption magOption = new LimeAssociationOption(mag, "magnet:", I18n
                        .tr("\"magnet:\" links"));
                fileAssociations.put(AssociationType.MAGNET, magOption);
            }
        }
        return Collections.unmodifiableMap(fileAssociations);
    }
}
