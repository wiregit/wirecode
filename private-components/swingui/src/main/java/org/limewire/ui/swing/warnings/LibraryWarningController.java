package org.limewire.ui.swing.warnings;

import java.io.File;
import java.util.List;

import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.core.settings.SharingSettings;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * This class acts as a go between for adding files from the ui to a fileList.
 * It has the correct logic for deciding whether or not a user needs to be
 * warned before a folder drop occurs.
 */
public class LibraryWarningController {
    private final Provider<LibraryWarningPanel> libraryWarningPanel;

    private final Provider<SharingWarningPanel> sharingWarningPanel;

    @Inject
    public LibraryWarningController(Provider<LibraryWarningPanel> libraryCategoryWarning,
            Provider<SharingWarningPanel> sharingWarningPanel) {
        this.libraryWarningPanel = libraryCategoryWarning;
        this.sharingWarningPanel = sharingWarningPanel;
    }

    public void addFiles(final LocalFileList fileList, final List<File> files) {

        int directoryCount = 0;
        for (File file : files) {
            if (file.isDirectory()) {
                directoryCount++;
                if (directoryCount > 1) {
                    // short circuit just need to know if there is more than 1
                    break;
                }
            }
        }

        SharedFileList sharedFileList = null;
        if (fileList instanceof SharedFileList) {
            sharedFileList = (SharedFileList) fileList;
        }

        if (directoryCount > 0 && LibrarySettings.ASK_ABOUT_FOLDER_DROP_CATEGORIES.getValue()) {
            LibraryWarningPanel panel = libraryWarningPanel.get();
            panel.initialize(fileList, files);
        } else if (directoryCount > 0 && SharingSettings.WARN_SHARING_FOLDER.getValue()
                && sharedFileList != null
                && (sharedFileList.isPublic() || sharedFileList.getFriendIds().size() > 0)) {
            SharingWarningPanel panel = sharingWarningPanel.get();
            panel.initialize(sharedFileList, files);
        } else {
            addFilesInner(fileList, files);
        }
    }

    static void addFilesInner(final LocalFileList fileList, final List<File> files) {
        for (File file : files) {
            if (fileList.isFileAddable(file)) {
                if (file.isDirectory()) {
                    fileList.addFolder(file);
                } else {
                    fileList.addFile(file);
                }
            }
        }
    }
}
