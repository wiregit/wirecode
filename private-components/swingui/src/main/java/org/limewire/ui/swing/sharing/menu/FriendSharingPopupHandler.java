package org.limewire.ui.swing.sharing.menu;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.sharing.actions.AddNewAudioAction;
import org.limewire.ui.swing.sharing.actions.AddNewImageAction;
import org.limewire.ui.swing.sharing.actions.AddNewVideoAction;
import org.limewire.ui.swing.sharing.actions.GoToLibraryAction;
import org.limewire.ui.swing.sharing.friends.FriendItem;
import org.limewire.ui.swing.sharing.friends.FriendNameTable;
import org.limewire.ui.swing.table.TablePopupHandler;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.swing.EventTableModel;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Popup menu for the friend list names in the sharing view
 */
@Singleton
public class FriendSharingPopupHandler implements TablePopupHandler {

    private int popupRow = -1;
    
    private GoToLibraryAction libraryAction;
    private AddNewAudioAction audioAction;
    private AddNewImageAction imageAction;
    private AddNewVideoAction videoAction;
    
    private final JPopupMenu popupMenu;
    private final JMenuItem viewLibraryItem;
    private final JCheckBoxMenuItem musicShareAllItem;
    private final JCheckBoxMenuItem videoShareAllItem;
    private final JCheckBoxMenuItem imageShareAllItem;
    private final JMenuItem unshareAllItem;
    
    private final FriendNameTable table;
    private final RemoteLibraryManager remoteLibraryManager;
    
    @Inject
    public FriendSharingPopupHandler(FriendNameTable table, RemoteLibraryManager remoteLibraryManager, 
            AddNewAudioAction audioAction, AddNewImageAction imageAction, AddNewVideoAction videoAction, 
            Navigator navigator) {
        this.table = table;
        this.remoteLibraryManager = remoteLibraryManager;

        
        this.audioAction = audioAction;
        this.imageAction = imageAction;
        this.videoAction = videoAction;
        
        popupMenu = new JPopupMenu();
        
        libraryAction = new GoToLibraryAction(navigator, null);
        viewLibraryItem = new JMenuItem(I18n.tr("View Library"));
        viewLibraryItem.addActionListener(libraryAction);
        
        musicShareAllItem = new JCheckBoxMenuItem(I18n.tr("Share all music"));
        musicShareAllItem.addActionListener(audioAction);
        
        videoShareAllItem = new JCheckBoxMenuItem(I18n.tr("Share all videos"));
        videoShareAllItem.addActionListener(videoAction);
        
        imageShareAllItem = new JCheckBoxMenuItem(I18n.tr("Share all images"));
        imageShareAllItem.addActionListener(imageAction);
        
        unshareAllItem = new JMenuItem(I18n.tr("Unshare all"));
        unshareAllItem.addItemListener(new ItemListener(){
            @Override
            public void itemStateChanged(ItemEvent e) {
                //TODO: implement this
                throw new UnsupportedOperationException("TODO: implement properties get info");
            }
        });
        
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
        FriendItem currentFriend = model.getElementAt(popupRow);
//        friendFileList = shareListManager.getOrCreateFriendShareList(currentFriend.getFriend());

        //if friend signed on through LW, make library view enabled
        viewLibraryItem.setEnabled(remoteLibraryManager.hasFriendLibrary(currentFriend.getFriend()));
        libraryAction.setFriend(currentFriend.getFriend());
        //display if always share types are selected already
        musicShareAllItem.setSelected(audioAction.isSelected());
        videoShareAllItem.setSelected(videoAction.isSelected());
        imageShareAllItem.setSelected(imageAction.isSelected());       
        //if sharing something, enable clear all selection
        unshareAllItem.setEnabled(currentFriend.getShareListSize() > 0);
        
        popupMenu.show(component, x, y);
    }
    
    public void showPopup(JComponent component, int x, int y) {
        if(table.getSelectedRow() < 0)
            return;
        
        EventTableModel<FriendItem> model = table.getEventTableModel();
        FriendItem currentFriend = model.getElementAt(table.getSelectedRow());

        //if friend signed on through LW, make library view enabled
        viewLibraryItem.setEnabled(remoteLibraryManager.hasFriendLibrary(currentFriend.getFriend()));
        libraryAction.setFriend(currentFriend.getFriend());
        //display if always share types are selected already
        musicShareAllItem.setSelected(audioAction.isSelected());
        videoShareAllItem.setSelected(videoAction.isSelected());
        imageShareAllItem.setSelected(imageAction.isSelected());       
        //if sharing something, enable clear all selection
        unshareAllItem.setEnabled(currentFriend.getShareListSize() > 0);
        
        popupMenu.show(component, x, y);
    }
}
