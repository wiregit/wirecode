package org.limewire.ui.swing.library.table.menu;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

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
   
    private LocalFileItem[] fileItems;

    private LibraryManager libraryManager;

    private ShareListManager shareListManager;

    private JMenuItem gnutellaShareItem;
    private JMenuItem gnutellaUnshareItem;
    private JMenu friendSubMenu;
    

    private LibraryTable table;

    private List<SharingTarget> friendList;

    public MyLibraryMultipleSelectionPopupMenu(Category category, LibraryManager libraryManager,
            ShareListManager shareListManager, LibraryTable table, List<SharingTarget> friendList) {
        this.libraryManager = libraryManager;
        this.shareListManager = shareListManager;
        this.table = table;
        this.friendList = friendList;
        initialize(category);
    }

    public void setFileItems(LocalFileItem[] fileItems) {
        this.fileItems = fileItems;
        friendSubMenu.setVisible(friendList.size() > 0);
        resetFriends();
    }
    
    private void resetFriends() {
       friendSubMenu.removeAll();
       for (SharingTarget target : friendList){            
           friendSubMenu.add(new FriendShareAction(target.getFriend()));
           friendSubMenu.add(new FriendUnshareAction(target.getFriend()));
       }
       
    }
    
    private void initialize(Category category){     
        gnutellaShareItem = new JMenuItem(gnutellaShareAction);
        gnutellaUnshareItem = new JMenuItem(gnutellaUnshareAction);
        
        friendSubMenu = new JMenu(I18n.tr("Share with Friends"));
        
        add(removeAction);
        add(deleteAction);
        add(gnutellaShareItem);
        add(gnutellaUnshareItem);
        add(friendSubMenu);
    
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
    
   private Action gnutellaUnshareAction = new AbstractAction(I18n.tr("Unshare with LimeWire Network")) {
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

    private Action gnutellaShareAction = new AbstractAction(I18n.tr("Share with LimeWire Network")) {
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
    
    private class FriendUnshareAction extends AbstractAction {
        private Friend friend;
        public FriendUnshareAction(Friend friend){
            super(I18n.tr("Unshare with {0}", friend.getRenderName()));
            this.friend = friend;
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            BackgroundExecutorService.schedule(new Runnable() {
                @Override
                public void run() {
                    for (LocalFileItem fileItem : fileItems) {
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
            super(I18n.tr("Share with {0}", friend.getRenderName()));
            this.friend = friend;
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            BackgroundExecutorService.schedule(new Runnable() {
                @Override
                public void run() {
                    for (LocalFileItem fileItem : fileItems) {
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
