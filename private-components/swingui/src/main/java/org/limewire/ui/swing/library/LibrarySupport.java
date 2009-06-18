package org.limewire.ui.swing.library;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.core.settings.SharingSettings;
import org.limewire.ui.swing.components.YesNoCheckBoxDialog;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

/**
 * This class acts as a go between for adding files from the ui to a fileList.
 * It has the correct logic for deciding whether or not a user needs to be
 * warned before a folder drop occurs.
 */
public class LibrarySupport {
    public void addFiles(final LocalFileList fileList, final List<File> files) {
        int directoryCount = 0;
        for (File file : files) {
            if (file.isDirectory()) {
                directoryCount++;
                if(directoryCount > 1) {
                    //short circuit just need to know if there is more than 1
                    break;
                }
            }
        }

        SharedFileList sharedFileList = null;
        if (SharedFileList.class.isInstance(fileList)) {
            sharedFileList = (SharedFileList) fileList;
        }

        if (directoryCount > 0
                && sharedFileList != null
                && sharedFileList.getFriendIds().size() > 0
                && SharingSettings.WARN_SHARING_FOLDER.getValue()) {

            String message = null;
            String optionsMessage = null;
            if (sharedFileList.isPublic()) {
                if(directoryCount == 1) {
                    message = I18n.tr("Share files in this folder and all its subfolders with the world?");
                } else {
                    message = I18n.tr("Share files in these folders and their subfolders with the world?");
                }
                optionsMessage = I18n
                        .tr("Don't ask me again when adding folders to a shared list");
            } else {
                if(directoryCount == 1) {
                    message = I18n.tr("Share files in this folder and all its subfolders with selected friends?");
                } else {
                    message = I18n.tr("Share files in these folders and their subfolders with selected friends?");
                }
                optionsMessage = I18n.tr("Don't ask me again when adding folders to a shared list");
            }
            
            final YesNoCheckBoxDialog yesNoCheckBoxDialog = new YesNoCheckBoxDialog(message,
                    optionsMessage, false, I18n.tr("Share Files"), I18n.tr("Canel"));
            yesNoCheckBoxDialog.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    boolean yes = YesNoCheckBoxDialog.YES_COMMAND.equals(e.getActionCommand());

                    if (yes) {
                        SharingSettings.WARN_SHARING_FOLDER.setValue(!yesNoCheckBoxDialog.isCheckBoxSelected());
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
        BackgroundExecutorService.execute(new Runnable() {
            @Override
            public void run() {
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
        });
    }
}
