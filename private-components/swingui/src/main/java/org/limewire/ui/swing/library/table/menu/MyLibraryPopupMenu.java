package org.limewire.ui.swing.library.table.menu;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.List;

import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import org.limewire.core.api.Category;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.MagnetLinkFactory;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.sharing.SharingTarget;
import org.limewire.ui.swing.player.PlayerUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.OSUtils;

public class MyLibraryPopupMenu extends JPopupMenu {
   
    private LocalFileItem fileItem;

    private LibraryManager libraryManager;

    private ShareListManager shareListManager;
    

    private Component repaintComponent;
    
    private JCheckBoxMenuItem gnutellaShareItem;
    
    private JMenu friendSubMenu;
    
  //only accessed on EDT
    private List<SharingTarget> friendList;

    private MagnetLinkFactory magnetFactory;

    public MyLibraryPopupMenu(Category category, LibraryManager libraryManager,
            ShareListManager shareListManager, MagnetLinkFactory magnetFactory, Component repaintComponent, List<SharingTarget> friendList) {
        this.libraryManager = libraryManager;
        this.shareListManager = shareListManager;
        this.magnetFactory = magnetFactory;
        this.repaintComponent = repaintComponent;
        this.friendList = friendList;
        initializeCategory(category);
    }

    public void setFileItem(LocalFileItem fileItem) {
        this.fileItem = fileItem;
        gnutellaShareItem.setSelected(fileItem.isSharedWithGnutella());
        friendSubMenu.setVisible(friendList.size() > 0);
        resetFriends();
    }
    
    private void resetFriends() {
       friendSubMenu.removeAll();
       for (SharingTarget target : friendList){
           JCheckBoxMenuItem item = new JCheckBoxMenuItem(I18n.tr("Share with {0}", target.getFriend().getRenderName()));
           item.setSelected(shareListManager.getFriendShareList(target.getFriend()).getSwingModel().contains(fileItem));
           item.addItemListener(new FriendShareListener(target.getFriend()));
           friendSubMenu.add(item);
       }
       
    }

    private void initializeCategory(Category category){
        gnutellaShareItem = new JCheckBoxMenuItem(I18n.tr("Share with LimeWire Network"));
        gnutellaShareItem.addItemListener(gnutellaShareListener);
        
        friendSubMenu = new JMenu(I18n.tr("Share with Friends"));
        
        boolean isLocateActionShown = false;
        switch(category){
        case AUDIO:
        case VIDEO:
            add(playAction);
            break;
        case IMAGE:
        case DOCUMENT:
            add(viewAction);
            break;
        case PROGRAM:
        case OTHER:
            isLocateActionShown = true;
            add(locateAction);           
        }
        add(new JSeparator());
        add(removeAction);
        add(deleteAction);
        add(new JSeparator());
        if (isGnutellaSharable(category)) {
            add(gnutellaShareItem);
        }
        add(friendSubMenu);
        add(new JSeparator());
        add(copyLinkAction);
        if(!isLocateActionShown){
            add(locateAction);
        }
        add(propertiesAction);
        
    }
    
    //TODO: refactor to common parent with MyLibraryMultipleSelectionPopupMenu
    private boolean isGnutellaSharable(Category category){
        return category != Category.DOCUMENT || LibrarySettings.DOCUMENT_SHARING_ENABLED.getValue();
    }

    
    private File getFile() {
        return fileItem.getFile();
    }

    private Action playAction = new AbstractAction(I18n.tr("Play")) {

        @Override
        public void actionPerformed(ActionEvent e) {
            PlayerUtils.playOrLaunch(getFile());
        }
    };

    private Action locateAction = new AbstractAction(I18n.tr("Locate file")) {

        @Override
        public void actionPerformed(ActionEvent e) {
            NativeLaunchUtils.launchExplorer(getFile());
        }
    };

    private Action viewAction = new AbstractAction(I18n.tr("View")) {

        @Override
        public void actionPerformed(ActionEvent e) {
            NativeLaunchUtils.launchFile(getFile());
        }

    };

    private Action copyLinkAction = new AbstractAction(I18n.tr("Copy Link to clipboard")) {

        @Override
        public void actionPerformed(ActionEvent e) {
            new MagnetLinkCopier(fileItem, magnetFactory).copyLinkToClipBoard();
        }

    };

    private Action propertiesAction = new AbstractAction(I18n.tr("Properties")) {

        @Override
        public void actionPerformed(ActionEvent e) {
            throw new RuntimeException("implement me");
        }

    };

    private Action removeAction = new AbstractAction(I18n.tr("Remove from library")) {

        @Override
        public void actionPerformed(ActionEvent e) {
            libraryManager.getLibraryManagedList().removeFile(fileItem.getFile());
        }

    };

    private Action deleteAction = new AbstractAction(I18n.tr("Delete file")) {

        @Override
        public void actionPerformed(ActionEvent e) {
            FileUtils.unlockFile(getFile());
            libraryManager.getLibraryManagedList().removeFile(fileItem.getFile());
            //TODO offload delete from the event queue?
            FileUtils.delete(getFile(), OSUtils.supportsTrash());
        }

    };
    
    private ItemListener gnutellaShareListener = new ItemListener() {

        @Override
        public void itemStateChanged(ItemEvent e) {
            if (e.getStateChange() == ItemEvent.SELECTED){
                shareListManager.getGnutellaShareList().addFile(getFile());
            } else {
                shareListManager.getGnutellaShareList().removeFile(getFile());
            }
            repaintComponent.repaint();
        }        
   };
   
   private class FriendShareListener implements ItemListener {
        private Friend friend;

        public FriendShareListener(Friend friend) {
            this.friend = friend;
        }

        @Override
        public void itemStateChanged(ItemEvent e) {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                shareListManager.getFriendShareList(friend).addFile(getFile());
            } else {
                shareListManager.getFriendShareList(friend).removeFile(getFile());
            }
            repaintComponent.repaint();
        }
    };

}
