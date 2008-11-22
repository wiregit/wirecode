/**
 * 
 */
package org.limewire.ui.swing.menu.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.limewire.core.api.library.LibraryManager;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.mainframe.MainPanel;
import org.limewire.ui.swing.util.FileChooser;
import org.limewire.ui.swing.util.I18n;

public class AddFileAction extends AbstractAction {
    private final MainPanel mainPanel;

    private final LibraryManager libraryManager;

    public AddFileAction(String name, MainPanel mainPanel, LibraryManager libraryManager) {
        super(name);
        this.mainPanel = mainPanel;
        this.libraryManager = libraryManager;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        List<File> files = FileChooser.getInput(mainPanel, I18n.tr("Add File(s)"), I18n
                .tr("Add Files"), FileChooser.getLastInputDirectory(), JFileChooser.FILES_ONLY,
                JFileChooser.APPROVE_OPTION, true, new FileFilter() {
                    @Override
                    public boolean accept(File f) {
                        return f.isDirectory()
                                || libraryManager.getLibraryData().isFileManageable(f);
                    }

                    @Override
                    public String getDescription() {
                        return I18n.tr("Valid Files");
                    }
                });

        if (files != null) {
            for (File file : files) {
                libraryManager.getLibraryManagedList().addFile(file);
            }
        }
    }
}