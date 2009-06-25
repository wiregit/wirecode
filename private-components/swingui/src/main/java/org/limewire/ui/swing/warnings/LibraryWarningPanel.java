package org.limewire.ui.swing.warnings;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.LibraryFileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.HorizonalCheckBoxListPanel;
import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

class LibraryWarningPanel extends LimeJDialog {

    private final LibraryManager libraryManager;

    @Inject
    public LibraryWarningPanel(LibraryManager libraryManager) {
        this.libraryManager = libraryManager;
    }

    public void initialize(final LocalFileList fileList, final List<File> files) {

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
        final HorizonalCheckBoxListPanel<Category> categories = new HorizonalCheckBoxListPanel<Category>(
                Category.getCategoriesInOrder());
        categories.setSelected(libraryManager.getLibraryData().getManagedCategories());
        add(new JLabel(getMessage(fileList, folder)), "wrap");
        add(categories, "wrap");
        final JCheckBox alwaysAsk = new JCheckBox(
                I18n
                        .tr("Always choose these kind of files when I add a folder to the Library or a List"),
                false);
        add(alwaysAsk, "wrap");
        add(new JButton(new AbstractAction(getButtonName(fileList)) {
            @Override
            public void actionPerformed(ActionEvent e) {
                LibrarySettings.ASK_ABOUT_FOLDER_DROP_CATEGORIES.set(!alwaysAsk.isSelected());
                libraryManager.getLibraryData().setCategoriesToIncludeWhenAddingFolders(
                        categories.getSelected());
                LibraryWarningController.addFilesInner(fileList, files);
                LibraryWarningPanel.this.dispose();
            }
        }), "alignx right");
        add(new JButton(new AbstractAction(I18n.tr("Cancel")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                LibraryWarningPanel.this.dispose();
            }
        }), "wrap");
        pack();
        setLocationRelativeTo(GuiUtils.getMainFrame());
        setVisible(true);
    }

    private String getButtonName(LocalFileList fileList) {
        SharedFileList sharedFileList = null;
        if (fileList instanceof SharedFileList) {
            sharedFileList = (SharedFileList) fileList;
        }

        LibraryFileList libraryFileList = null;
        if (fileList instanceof LibraryFileList) {
            libraryFileList = (LibraryFileList) fileList;
        }

        if (libraryFileList != null || sharedFileList != null && !sharedFileList.isPublic()
                && sharedFileList.getFriendIds().size() == 0) {
            return I18n.tr("Add");
        } else {
            return I18n.tr("Share");
        }
    }

    private String getMessage(LocalFileList fileList, File folder) {
        SharedFileList sharedFileList = null;
        if (fileList instanceof SharedFileList) {
            sharedFileList = (SharedFileList) fileList;
        }

        LibraryFileList libraryFileList = null;
        if (fileList instanceof LibraryFileList) {
            libraryFileList = (LibraryFileList) fileList;
        }

        if (libraryFileList != null || sharedFileList != null && !sharedFileList.isPublic()
                && sharedFileList.getFriendIds().size() == 0) {
            if (folder != null) {
                return I18n.tr(
                        "What kind of files do you want to add from \"{0}\" and its subfolders?",
                        folder.getName());
            } else {
                return I18n
                        .tr("What kind of files do you want to add from these folders and their subfolders?");
            }
        } else if (sharedFileList != null && sharedFileList.isPublic()) {
            if (folder != null) {
                return I18n
                        .tr(
                                "What kind of files do you want to share with the world from \"{0}\" and its subfolders?",
                                folder.getName());
            } else {
                return I18n
                        .tr("What kind of files do you want to share with the world from these folders and their subfolders?");
            }
        } else {
            if (folder != null) {
                return I18n
                        .tr(
                                "What kind of files do you want to share with selected friends from \"{0}\" and its subfolders?",
                                folder.getName());
            } else {
                return I18n
                        .tr("What kind of files do you want to share with selected friends from these fodlers and their subfolders?");
            }
        }
    }
}
