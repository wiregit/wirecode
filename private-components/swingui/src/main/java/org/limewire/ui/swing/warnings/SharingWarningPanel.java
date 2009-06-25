package org.limewire.ui.swing.warnings;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.library.SharedFileList;
import org.limewire.core.settings.SharingSettings;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

class SharingWarningPanel extends LimeJDialog {

    public SharingWarningPanel() {
        setTitle(I18n.tr("Share files?"));
    }

    public void initialize(final SharedFileList fileList, final List<File> files) {

        int directoryCount = 0;
        File folder = null;
        for (File file : files) {
            if (file.isDirectory()) {
                directoryCount++;
                folder = file;
                if (directoryCount > 1) {
                    // short circuit just need to know if there is more than 1
                    // null folder when more than 1 folder.
                    folder = null;
                    break;
                }
            }
        }

        setLayout(new MigLayout("nogrid"));
        add(new JLabel(getMessage(fileList, folder)), "wrap");
        final JCheckBox warnMeCheckbox = new JCheckBox(I18n
                .tr("Warn me before adding folders to a shared list"), true);
        add(warnMeCheckbox, "wrap");
        add(new JButton(new AbstractAction(I18n.tr("Share")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                SharingSettings.WARN_SHARING_FOLDER.setValue(warnMeCheckbox.isSelected());
                LibraryWarningController.addFilesInner(fileList, files);
                SharingWarningPanel.this.dispose();
            }
        }), "alignx right");
        add(new JButton(new AbstractAction(I18n.tr("Cancel")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                SharingWarningPanel.this.dispose();
            }
        }), "wrap");
        pack();
        setLocationRelativeTo(GuiUtils.getMainFrame());
        setVisible(true);
    }

    private String getMessage(SharedFileList fileList, File folder) {
        if (fileList.isPublic()) {
            if (folder != null) {
                return I18n.tr("Share files in \"{0}\" and its subfolders with the world?", folder
                        .getName());
            } else {
                return I18n.tr("Share files in these folders and their subfolders with the world?");
            }
        } else {
            if (folder != null) {
                return I18n.tr("Share files in \"{0}\" and its subfolders with selected friends? ",
                        folder.getName());
            } else {
                return I18n
                        .tr("Share files in these folders and their subfolders with selected friends?");
            }
        }
    }
}
