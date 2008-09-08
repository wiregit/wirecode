package org.limewire.ui.swing.sharing.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.FileList;
import org.limewire.core.api.library.FileItem.Category;
import org.limewire.ui.swing.sharing.table.CategoryFilter;

import ca.odell.glazedlists.FilterList;

/**
 * Given a userFileList and a category, copies all the files from the library of
 * type category into the user's filelist.
 */
public class SharingAddAction extends AbstractAction {

    private FileList userList;
    private FileList libraryList;
    private Category category;
    
    public SharingAddAction(FileList libraryList, Category category) {
        this(null, libraryList, category);
    }
    
    public SharingAddAction(FileList userList, FileList libraryList, Category category) {
        this.userList = userList;
        this.libraryList = libraryList;
        this.category = category;
    }
        
    @Override
    public void actionPerformed(ActionEvent e) {
        if( userList == null || libraryList == null)
            return; 
        FilterList<FileItem> audio = new FilterList<FileItem>(libraryList.getModel(), new CategoryFilter(category)); 
        for(FileItem item : audio) {
            userList.addFile(item.getFile());
        }
    }
    
    public void setUserFileList(FileList userList) {
        this.userList = userList;
    }
}
