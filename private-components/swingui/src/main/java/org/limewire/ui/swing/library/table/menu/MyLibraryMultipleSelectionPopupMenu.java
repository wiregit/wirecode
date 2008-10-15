package org.limewire.ui.swing.library.table.menu;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.table.LibraryTable;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SwingUtils;
import org.limewire.util.FileUtils;

public class MyLibraryMultipleSelectionPopupMenu extends JPopupMenu {
   
    private LocalFileItem[] fileItems;

    private LibraryManager libraryManager;

    private ShareListManager shareListManager;

    private  JMenuItem gnutellaShareItem;
    private  JMenuItem gnutellaUnshareItem;
    

    private LibraryTable table;

    public MyLibraryMultipleSelectionPopupMenu(Category category, LibraryManager libraryManager,
            ShareListManager shareListManager, LibraryTable table) {
        this.libraryManager = libraryManager;
        this.shareListManager = shareListManager;
        this.table = table;
        initialize(category);
    }

    public void setFileItems(LocalFileItem[] fileItems) {
        this.fileItems = fileItems;
    }
    
    private void initialize(Category category){     
        gnutellaShareItem = new JMenuItem(gnutellaShareAction);
        gnutellaUnshareItem = new JMenuItem(gnutellaUnshareAction);
        
        add(removeAction);
        add(deleteAction);
        add(gnutellaShareItem);
        add(gnutellaUnshareItem);
        //TODO
        //add(friendShareAction);
    
    }



    private Action removeAction = new AbstractAction(I18n.tr("Remove from library")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            for (LocalFileItem fileItem : fileItems) {
                libraryManager.getLibraryManagedList().removeFile(fileItem.getFile());
            }
        }
    };

    private Action deleteAction = new AbstractAction(I18n.tr("Delete file")) {

        @Override
        public void actionPerformed(ActionEvent e) {
            //TODO send to trash, instead of deleting
            for (LocalFileItem fileItem : fileItems) {
                FileUtils.forceDelete(fileItem.getFile());
            }
        }

    };
    
   private Action gnutellaUnshareAction = new AbstractAction("Unshare with LimeWire Network") {
        @Override
        public void actionPerformed(ActionEvent e) {
            BackgroundExecutorService.schedule(new Runnable() {
                @Override
                public void run() {
                    for (LocalFileItem fileItem : fileItems) {
                        shareListManager.getGnutellaShareList().removeFile(fileItem.getFile());
                    }
                    SwingUtils.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            table.repaint();
                        }
                    });
                }
            });
        }
    };

    private Action gnutellaShareAction = new AbstractAction("Share with LimeWire Network") {
        @Override
        public void actionPerformed(ActionEvent e) {
            BackgroundExecutorService.schedule(new Runnable() {
                @Override
                public void run() {
                    for (LocalFileItem fileItem : fileItems) {
                        shareListManager.getGnutellaShareList().addFile(fileItem.getFile());
                    }
                    SwingUtils.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            table.repaint();
                        }
                    });
                }
            });        
        }
    };

}
