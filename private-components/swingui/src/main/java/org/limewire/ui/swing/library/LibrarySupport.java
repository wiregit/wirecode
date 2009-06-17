package org.limewire.ui.swing.library;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.core.settings.SharingSettings;
import org.limewire.ui.swing.components.YesNoCheckBoxDialog;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

/**
 * This class acts as a go between for adding files from the ui to a fileList.
 * It has the correct logic for deciding whether or not a user needs to be
 * warned before a folder drop occurs.
 */
public class LibrarySupport {
    public void addFiles(final LocalFileList fileList, final List<File> files) {
        boolean hasDirectory = false;
        for (File file : files) {
            if (file.isDirectory()) {
                hasDirectory = true;
                break;
            }
        }

        SharedFileList sharedFileList = null;
        if (SharedFileList.class.isInstance(fileList)) {
            sharedFileList = (SharedFileList) fileList;
        }

        final SharedFileList sharedFileListCopy = sharedFileList;

        if (hasDirectory
                && sharedFileList != null
                && sharedFileList.getFriendIds().size() > 0
                && ((sharedFileList.isPublic() && SharingSettings.WARN_SHARING_FOLDER_WORLD
                        .getValue()) || (!sharedFileList.isPublic() && SharingSettings.WARN_SHARING_FOLDER_FRIENDS
                        .getValue()))) {

            String message = null;
            String optionsMessage = null;
            if (sharedFileList.isPublic()) {
                //TODO change the wording for Share Files etc., depending on whehter 1 folder in the list or more.s
                message = I18n.tr("Share files in folder and all it's subfolders with the world?");
                //TODO change the wording for don't ask me etc.
                optionsMessage = I18n
                        .tr("Don't ask me again when adding a folder share with the world");
            } else {
                //TODO change the wording for Share Files etc., depending on whehter 1 folder in the list or more.s
                message = I18n.tr("Share files in folder and all it's subfolders with {0} friends",
                        sharedFileList.getFriendIds().size());
                //TODO change the wording for don't ask me etc.
                optionsMessage = I18n.tr("Don't ask me again when adding a folder to a list.");
            }

            final YesNoCheckBoxDialog yesNoCheckBoxDialog = new YesNoCheckBoxDialog(message,
                    optionsMessage, false);
            yesNoCheckBoxDialog.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    boolean yes = YesNoCheckBoxDialog.YES_COMMAND.equals(e.getActionCommand());

                    if (yes) {
                        if (sharedFileListCopy.isPublic()) {
                            SharingSettings.WARN_SHARING_FOLDER_WORLD.setValue(!yesNoCheckBoxDialog
                                    .isCheckBoxSelected());
                        } else {
                            SharingSettings.WARN_SHARING_FOLDER_FRIENDS
                                    .setValue(!yesNoCheckBoxDialog.isCheckBoxSelected());
                        }
                        addFilesInner(fileList, files);
                    }
                }
            });
            yesNoCheckBoxDialog.pack();
            yesNoCheckBoxDialog.setLocationRelativeTo(GuiUtils.getMainFrame());
            yesNoCheckBoxDialog.setVisible(true);
        } else {
            addFilesInner(fileList, files);
        }
    }

    private void addFilesInner(final LocalFileList fileList, final List<File> files) {
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
