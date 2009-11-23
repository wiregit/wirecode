package org.limewire.ui.swing.library.table;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.ShareListIcons;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Provider;

/**
 * Constructs a menu list of all LocalFileLists excluding the Library.
 * Selecting a given LocalFileList will remove the given files from that
 * list. 
 */
public class RemoveFromListMenu extends JMenu {

    private final Provider<List<File>> selectedFiles;
    private final ShareListIcons icons = new ShareListIcons();
    
    /**
     * Constructs an RemoveFromListMenu with all lists enable..
     */
    public RemoveFromListMenu(Provider<List<File>> selectedFiles) {
        super(I18n.tr("Remove from List"));     
        
        this.selectedFiles = selectedFiles;
    }
    
    public void initialize(final SharedFileListManager manager, final RemoveFromAllListAction removeFromAllAction) {
        addChangeListener(new ChangeListener(){
            @Override
            public void stateChanged(ChangeEvent e) {
                JMenu menu = RemoveFromListMenu.this;
                menu.removeAll();
                
                boolean allRemoveEnabled = false;
                // once this is selected, show all the submenus
                manager.getModel().getReadWriteLock().readLock().lock();
                try { 
                    for(SharedFileList fileList : manager.getModel()) {
                        if(selectedFiles.get().size() == 1) {
                            boolean isEnabled = fileList.contains(selectedFiles.get().get(0));
                            menu.add(new RemoveFromListAction(fileList.getCollectionName(), icons.getListIcon(fileList), fileList)).setEnabled(isEnabled);
                            allRemoveEnabled |= isEnabled;
                        } else {
                            menu.add(new RemoveFromListAction(fileList.getCollectionName(), icons.getListIcon(fileList), fileList));
                        }
                    }
                } finally {
                    manager.getModel().getReadWriteLock().readLock().unlock();
                }
                menu.addSeparator();
                removeFromAllAction.putValue(Action.NAME, I18n.tr("Remove from All Lists"));
                menu.add(removeFromAllAction).setEnabled(allRemoveEnabled);
            }
            
        });
        
        getPopupMenu().addPopupMenuListener(new PopupMenuListener(){
            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {}
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}

            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                //keeps menu from expaning too far
                if(getPopupMenu().getPreferredSize().width > 300)
                    getPopupMenu().setPreferredSize(new Dimension(300, getPopupMenu().getPreferredSize().height));
            }
            
        });
        // place holder to get the -> on the parent menu
        add(new JMenuItem(I18n.tr("Public Shared")));
    }
    
    /**
     * Removes the given file from the given FileList.
     */
    private class RemoveFromListAction extends AbstractAction {
        private final LocalFileList localFileList;
        
        public RemoveFromListAction(String text, Icon icon, LocalFileList localFileList) {
            super(text);
            putValue(SMALL_ICON, icon);
            this.localFileList = localFileList;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            BackgroundExecutorService.execute(new Runnable(){
                public void run() {
                    List<File> selected = new ArrayList<File>(selectedFiles.get());
                    for(File file : selected) {
                        localFileList.removeFile(file);
                    }                    
                }
            });
        }
    }
}
