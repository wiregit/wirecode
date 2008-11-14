package org.limewire.ui.swing.library.table.menu;

import java.awt.Component;
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
import org.limewire.core.settings.LibrarySettings;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.sharing.SharingTarget;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.FileUtils;

public class MyLibraryMultipleSelectionPopupMenu extends JPopupMenu {
   
    private List<LocalFileItem> fileItems;

    final private LibraryManager libraryManager;

    final private ShareListManager shareListManager;

    final private JMenuItem gnutellaShareItem;
    final private JMenuItem gnutellaUnshareItem;
    final private JMenu friendShareSubMenu;
    final private JMenu friendUnshareSubMenu;
    

    private Component repaintComponent;

    private List<SharingTarget> friendList;

    public MyLibraryMultipleSelectionPopupMenu(Category category, LibraryManager libraryManager,
            ShareListManager shareListManager, Component repaintComponent, List<SharingTarget> friendList) {
        this.libraryManager = libraryManager;
        this.shareListManager = shareListManager;
        this.repaintComponent = repaintComponent;
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
    
    private void initialize(Category category) {
        add(removeAction);
        add(deleteAction);
        add(new JSeparator());
        if (isGnutellaSharable(category)) {
            add(gnutellaShareItem);
        }
        add(friendShareSubMenu);
        add(new JSeparator());
        if (isGnutellaSharable(category)) {
            add(gnutellaUnshareItem);
        }
        add(friendUnshareSubMenu);

    }
    
    private boolean isGnutellaSharable(Category category){
        return category != Category.DOCUMENT || LibrarySettings.ALLOW_DOCUMENT_GNUTELLA_SHARING.getValue();
    }

    private LocalFileItem[] createFileItemArray(){
        return fileItems.toArray(new LocalFileItem[fileItems.size()]);
    }    

    private Action removeAction = new AbstractAction(I18n.tr("Remove from library")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            final LocalFileItem[] fileItemArray = createFileItemArray();
            for (LocalFileItem fileItem : fileItemArray) {
                if (!fileItem.isIncomplete()) {
                    libraryManager.getLibraryManagedList().removeFile(fileItem.getFile());
                }
            }
        }
    };

    //TODO combine with MyLibraryManager deleteAction
    private Action deleteAction = new AbstractAction(I18n.tr("Delete file")) {

        @Override
        public void actionPerformed(ActionEvent e) {
            final LocalFileItem[] fileItemArray = createFileItemArray();

            BackgroundExecutorService.execute(new Runnable() {                
                @Override
                public void run() {
                    for (LocalFileItem fileItem : fileItemArray) {
                        if (!fileItem.isIncomplete()) {
                            FileUtils.unlockFile(fileItem.getFile());
                            libraryManager.getLibraryManagedList().removeFile(fileItem.getFile());
                            FileUtils.delete(fileItem.getFile(), true);
                        }
                    }
                }
                
            });
        }
    };
    

   private Action gnutellaUnshareAction = new AbstractAction(I18n.tr("Unshare with LimeWire Network")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            final LocalFileItem[] fileItemArray = createFileItemArray();
            for (LocalFileItem fileItem : fileItemArray) {
                if (!fileItem.isIncomplete()) {
                    shareListManager.getGnutellaShareList().removeFile(fileItem.getFile());
                }
            }
            repaintComponent.repaint();
        }
    };

    private Action gnutellaShareAction = new AbstractAction(I18n.tr("Share with LimeWire Network")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            final LocalFileItem[] fileItemArray = createFileItemArray();
            for (LocalFileItem fileItem : fileItemArray) {
                if (!fileItem.isIncomplete()) {
                    shareListManager.getGnutellaShareList().addFile(fileItem.getFile());
                }
            }
            repaintComponent.repaint();      
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
            for (LocalFileItem fileItem : fileItemArray) {
                if (!fileItem.isIncomplete()) {
                    shareListManager.getFriendShareList(friend).removeFile(fileItem.getFile());
                }
            }
            repaintComponent.repaint();
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
            for (LocalFileItem fileItem : fileItemArray) {
                if (!fileItem.isIncomplete()) {
                    shareListManager.getFriendShareList(friend).addFile(fileItem.getFile());
                }
            }
            repaintComponent.repaint();      
        }
    };

}
