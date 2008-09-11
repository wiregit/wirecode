package org.limewire.ui.swing.sharing.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.limewire.core.api.library.FileItem.Category;
import org.limewire.core.api.library.FileList;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.sharing.table.CategoryFilter;

import ca.odell.glazedlists.FilterList;

/**
 * Given a userFileList and a category, copies all the files from the library of
 * type category into the user's filelist.
 */
public class SharingAddAction extends AbstractAction {

    private LocalFileList userList;
    private FileList<LocalFileItem> libraryList;
    private Category category;
    
    public SharingAddAction(FileList<LocalFileItem> libraryList, Category category) {
        this(null, libraryList, category);
    }
    
    public SharingAddAction(LocalFileList userList, FileList<LocalFileItem> libraryList, Category category) {
        this.userList = userList;
        this.libraryList = libraryList;
        this.category = category;
    }
        
    @Override
    public void actionPerformed(ActionEvent e) {
        if( userList == null || libraryList == null)
            return; 
        FilterList<LocalFileItem> audio = new FilterList<LocalFileItem>(libraryList.getModel(), new CategoryFilter(category)); 
        for(LocalFileItem item : audio) {
            userList.addFile(item.getFile());
        }
    }
    
    public void setUserFileList(LocalFileList userList) {
        this.userList = userList;
    }
}
