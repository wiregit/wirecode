package org.limewire.ui.swing.library.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.navigator.LibraryNavItem;
import org.limewire.ui.swing.library.navigator.LibraryNavigatorPanel;
import org.limewire.ui.swing.util.FileChooser;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class AddFileAction extends AbstractAction {

    private final Provider<LibraryNavigatorPanel> libraryNavigatorPanel;
    private final Provider<SharedFileListManager> sharedFileListManager;
    private final Provider<LibraryManager> libraryManager;
    
    @Inject
    public AddFileAction(Provider<LibraryNavigatorPanel> libraryNavigatorPanel,
            Provider<SharedFileListManager> sharedFileListManager,
            Provider<LibraryManager> libraryManager) {
        super(I18n.tr("Add Files"));
        
        this.libraryNavigatorPanel = libraryNavigatorPanel;
        this.sharedFileListManager = sharedFileListManager;
        this.libraryManager = libraryManager;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        List<File> files = FileChooser.getInput(GuiUtils.getMainFrame(), I18n.tr("Add Files(s)"), 
                I18n.tr("Add Files(s)"), FileChooser.getLastInputDirectory(),
                JFileChooser.FILES_AND_DIRECTORIES, JFileChooser.APPROVE_OPTION, true,
                new FileFilter() {
                    @Override
                    public boolean accept(File f) {
                        return f.exists();
                    }

                    @Override
                    public String getDescription() {
                        return I18n.tr("All Files and Folders");
                    }
                });

        if (files != null) {
            LibraryNavItem item = libraryNavigatorPanel.get().getSelectedNavItem();
            SharedFileList sharedFileList = sharedFileListManager.get().getSharedFileList(item.getTabID());
            
            for(File file : files) {
                if(file.isDirectory()) {
                    if(sharedFileList != null) 
                        sharedFileList.addFolder(file);
                    else
                        libraryManager.get().getLibraryManagedList().addFolder(file);
                } else {
                    if(sharedFileList != null) 
                        sharedFileList.addFile(file);
                    else
                        libraryManager.get().getLibraryManagedList().addFile(file);
                }
            }
        }
    } 
}
