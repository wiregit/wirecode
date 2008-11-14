package org.limewire.ui.swing.sharing.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBoxMenuItem;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.library.FileList;
import org.limewire.core.api.library.FriendFileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.lists.CategoryFilter;
import org.limewire.ui.swing.sharing.friends.FriendItem;
import org.limewire.ui.swing.sharing.friends.FriendNameTable;
import org.limewire.ui.swing.util.BackgroundExecutorService;

import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.swing.EventTableModel;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class AddNewAudioAction implements ActionListener {

    private ShareListManager sharingManager;
    private FriendNameTable friendNameTable;
    private LibraryManager libraryManager;
    
    @Inject
    public AddNewAudioAction(ShareListManager sharingManager, FriendNameTable friendNameTable, LibraryManager libraryManager) {
        this.sharingManager = sharingManager;
        this.friendNameTable = friendNameTable;
        this.libraryManager = libraryManager;
    }

    @Override
    public void actionPerformed(ActionEvent e) {       
        if(e == null)
            return;
        JCheckBoxMenuItem checkBox = (JCheckBoxMenuItem)e.getSource();
            
        int index = friendNameTable.getSelectedRow();
        if(index == -1)
            return;
        EventTableModel<FriendItem> model = friendNameTable.getEventTableModel();
        FriendItem item = model.getElementAt(index);
        final FriendFileList fileList = sharingManager.getFriendShareList(item.getFriend());
        final FileList<LocalFileItem> libraryList = libraryManager.getLibraryManagedList();
        if(checkBox.isSelected()) {
            fileList.setAddNewAudioAlways(true);
            BackgroundExecutorService.execute(new Runnable(){
                public void run() {
                    FilterList<LocalFileItem> filter = GlazedListsFactory.filterList(libraryList.getModel(), new CategoryFilter(Category.AUDIO));
                        try {
                            filter.getReadWriteLock().readLock().lock();
                            for(LocalFileItem item : filter) {
                                fileList.addFile(item.getFile());
                            }
                        } finally {
                            filter.getReadWriteLock().readLock().unlock();
                        }
                        filter.dispose();
                      }
                  });
        }
        else {
            fileList.setAddNewAudioAlways(false);
        }
    }
    
    public boolean isSelected() {
        int index = friendNameTable.getSelectedRow();
        if(index == -1)
            return false;
        EventTableModel<FriendItem> model = friendNameTable.getEventTableModel();
        FriendItem item = model.getElementAt(index);
        final FriendFileList fileList = sharingManager.getFriendShareList(item.getFriend());
        
        return fileList.isAddNewAudioAlways();
    }
}
