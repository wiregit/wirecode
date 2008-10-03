package org.limewire.ui.swing.sharing.menu;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.limewire.core.api.library.FriendFileList;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.sharing.friends.FriendItem;
import org.limewire.ui.swing.sharing.friends.FriendNameTable;
import org.limewire.ui.swing.table.TablePopupHandler;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.swing.EventTableModel;

/**
 * Popup menu for the friend list names in the sharing view
 */
public class FriendSharingPopupHandler implements TablePopupHandler {

    private int popupRow = -1;
    
    private final JPopupMenu popupMenu;
    private final JMenuItem viewLibraryItem;
    private final JCheckBoxMenuItem musicShareAllItem;
    private final JCheckBoxMenuItem videoShareAllItem;
    private final JCheckBoxMenuItem imageShareAllItem;
    private final JMenuItem unshareAllItem;
    
    private final FriendNameTable table;
    private final FriendSharingActionHandler actionHandler;
    private final RemoteLibraryManager remoteLibraryManager;
    private final ShareListManager shareListManager;
    
    private final MenuListener menuListener;

    private FriendItem currentFriend;
    private FriendFileList friendFileList;
    
    public FriendSharingPopupHandler(FriendNameTable table, FriendSharingActionHandler handler,
            RemoteLibraryManager remoteLibraryManager, ShareListManager shareListManager) {
        this.table = table;
        this.actionHandler = handler;
        this.shareListManager = shareListManager;
        this.remoteLibraryManager = remoteLibraryManager;
        this.menuListener = new MenuListener();
        
        popupMenu = new JPopupMenu();
        
        viewLibraryItem = new JMenuItem(I18n.tr("View Library"));
        viewLibraryItem.setActionCommand(FriendSharingActionHandler.VIEW_LIBRARY);
        viewLibraryItem.addActionListener(menuListener);
        
        musicShareAllItem = new JCheckBoxMenuItem(I18n.tr("Share all music"));
        musicShareAllItem.setActionCommand(FriendSharingActionHandler.SHARE_ALL_AUDIO);
        musicShareAllItem.addActionListener(menuListener);
        
        videoShareAllItem = new JCheckBoxMenuItem(I18n.tr("Share all videos"));
        videoShareAllItem.setActionCommand(FriendSharingActionHandler.SHARE_ALL_VIDEO);
        videoShareAllItem.addActionListener(menuListener);
        
        imageShareAllItem = new JCheckBoxMenuItem(I18n.tr("Share all images"));
        imageShareAllItem.setActionCommand(FriendSharingActionHandler.SHARE_ALL_IMAGE);
        imageShareAllItem.addActionListener(menuListener);
        
        unshareAllItem = new JMenuItem(I18n.tr("Unshare all"));
        unshareAllItem.setActionCommand(FriendSharingActionHandler.UNSHARE_ALL);
        unshareAllItem.addActionListener(menuListener);
        
        popupMenu.add(viewLibraryItem);
        popupMenu.addSeparator();
        popupMenu.add(musicShareAllItem);
        popupMenu.add(videoShareAllItem);
        popupMenu.add(imageShareAllItem);
        popupMenu.addSeparator();
        popupMenu.add(unshareAllItem);
    }

    @Override
    public boolean isPopupShowing(int row) {
        return popupMenu.isVisible() && row == popupRow;
    }

    @Override
    public void maybeShowPopup(Component component, int x, int y) {
        popupRow = table.rowAtPoint(new Point(x, y));
        
        EventTableModel<FriendItem> model = table.getEventTableModel();
        currentFriend = model.getElementAt(popupRow);
        friendFileList = shareListManager.getOrCreateFriendShareList(currentFriend.getFriend());

        //if friend signed on through LW, make library view enabled
        viewLibraryItem.setEnabled(remoteLibraryManager.hasFriendLibrary(currentFriend.getFriend()));
        //display if always share types are selected already
        musicShareAllItem.setSelected(friendFileList.isAddNewAudioAlways());
        videoShareAllItem.setSelected(friendFileList.isAddNewVideoAlways());
        imageShareAllItem.setSelected(friendFileList.isAddNewImageAlways());
        //if sharing something, enable clear all selection
        unshareAllItem.setEnabled(currentFriend.getShareListSize() > 0);
        
        popupMenu.show(component, x, y);
    }
    
    private class MenuListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            actionHandler.performAction(e.getActionCommand(), friendFileList, currentFriend);
        }
    }
}
