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
import org.limewire.ui.swing.library.LibrarySupport;
import org.limewire.ui.swing.util.FileChooser;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

public class AddFileAction extends AbstractAction {
    private final LibraryManager libraryManager;

    private final LibrarySupport librarySupport;

    @Inject
    public AddFileAction(LibraryManager libraryManager, LibrarySupport librarySupport) {
        super(I18n.tr("Add F&ile to Library..."));
        this.libraryManager = libraryManager;
        this.librarySupport = librarySupport;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        List<File> files = FileChooser.getInput(GuiUtils.getMainFrame(), I18n.tr("Add to Library"),
                I18n.tr("Add Files"), FileChooser.getLastInputDirectory(),
                JFileChooser.FILES_AND_DIRECTORIES, JFileChooser.APPROVE_OPTION, true,
                new FileFilter() {
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
            librarySupport.addFiles(libraryManager.getLibraryManagedList(), files);
        }
    }
}