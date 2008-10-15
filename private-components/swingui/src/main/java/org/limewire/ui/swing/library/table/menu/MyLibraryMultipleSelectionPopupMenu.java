package org.limewire.ui.swing.library.table.menu;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import org.limewire.core.api.Category;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.sharing.SharingTarget;
import org.limewire.ui.swing.library.table.LibraryTable;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SwingUtils;
import org.limewire.util.FileUtils;

public class MyLibraryMultipleSelectionPopupMenu extends JPopupMenu {
   
    private List<LocalFileItem> fileItems;

    final private LibraryManager libraryManager;

    final private ShareListManager shareListManager;

    final private JMenuItem gnutellaShareItem;
    final private JMenuItem gnutellaUnshareItem;
    final private JMenu friendShareSubMenu;
    final private JMenu friendUnshareSubMenu;
    

    private LibraryTable table;

    private List<SharingTarget> friendList;

    public MyLibraryMultipleSelectionPopupMenu(Category category, LibraryManager libraryManager,
            ShareListManager shareListManager, LibraryTable table, List<SharingTarget> friendList) {
        this.libraryManager = libraryManager;
        this.shareListManager = shareListManager;
        this.table = table;
        this.friendList = friendList;
        
        gnutellaShareItem = new JMenuItem(gnutellaShareAction);
        gnutellaUnshareItem = new JMenuItem(gnutellaUnshareAction);
        friendShareSubMenu = new JMenu(I18n.tr("Share with Friends"));
        friendUnshareSubMenu = new JMenu(I18n.tr("Unshare with Friends"));
        
        initialize(category);
    }

    public void setFileItems(List<LocalFileItem> items) {
        this.fileItems = items;
        friendShareSubMenu.setVisible(friendList.size() > 0);
        friendUnshareSubMenu.setVisible(friendList.size() > 0);
        resetFriends();
    }
    
    private void resetFriends() {
       friendShareSubMenu.removeAll();
       for (SharingTarget target : friendList){            
           friendShareSubMenu.add(new FriendShareAction(target.getFriend()));
           friendUnshareSubMenu.add(new FriendUnshareAction(target.getFriend()));
       }
       
    }
    
    private void initialize(Category category){             
        add(removeAction);
        add(deleteAction);
        add(new JSeparator());
        add(gnutellaShareItem);
        add(friendShareSubMenu);
        add(new JSeparator());
        add(gnutellaUnshareItem);
        add(friendUnshareSubMenu);
    
    }

    private LocalFileItem[] createFileItemArray(){
        return fileItems.toArray(new LocalFileItem[fileItems.size()]);
    }
    

    private Action removeAction = new AbstractAction(I18n.tr("Remove from library")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            final LocalFileItem[] fileItemArray = createFileItemArray();

            BackgroundExecutorService.schedule(new Runnable() {
                @Override
                public void run() {
                    for (LocalFileItem fileItem : fileItemArray) {
                        libraryManager.getLibraryManagedList().removeFile(fileItem.getFile());
                    }

                }
            });
        }
    };

    private Action deleteAction = new AbstractAction(I18n.tr("Delete file")) {

        @Override
        public void actionPerformed(ActionEvent e) {
            final LocalFileItem[] fileItemArray = createFileItemArray();

            BackgroundExecutorService.schedule(new Runnable() {
                
                @Override
                public void run() {
                    for (LocalFileItem fileItem : fileItemArray) {
                        // TODO send to trash, instead of deleting
                        FileUtils.forceDelete(fileItem.getFile());
                    }
                }
                
            });
        }
    };
    

   private Action gnutellaUnshareAction = new AbstractAction(I18n.tr("Unshare with LimeWire Network")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            final LocalFileItem[] fileItemArray = createFileItemArray();
            
            BackgroundExecutorService.schedule(new Runnable() {
                @Override
                public void run() {
                    for (LocalFileItem fileItem : fileItemArray) {
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

    private Action gnutellaShareAction = new AbstractAction(I18n.tr("Share with LimeWire Network")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            final LocalFileItem[] fileItemArray = createFileItemArray();
            
            BackgroundExecutorService.schedule(new Runnable() {
                @Override
                public void run() {
                    for (LocalFileItem fileItem : fileItemArray) {
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
    
    private class FriendUnshareAction extends AbstractAction {
        private Friend friend;
        public FriendUnshareAction(Friend friend){
            super(friend.getRenderName());
            this.friend = friend;
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            final LocalFileItem[] fileItemArray = createFileItemArray();
            
            BackgroundExecutorService.schedule(new Runnable() {
                @Override
                public void run() {
                    for (LocalFileItem fileItem : fileItemArray) {
                        shareListManager.getFriendShareList(friend).removeFile(fileItem.getFile());
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

    private class FriendShareAction extends AbstractAction {
        private Friend friend;
        public FriendShareAction(Friend friend){
            super(friend.getRenderName());
            this.friend = friend;
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            final LocalFileItem[] fileItemArray = createFileItemArray();
            
            BackgroundExecutorService.schedule(new Runnable() {
                @Override
                public void run() {
                    for (LocalFileItem fileItem : fileItemArray) {
                        shareListManager.getFriendShareList(friend).addFile(fileItem.getFile());
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
