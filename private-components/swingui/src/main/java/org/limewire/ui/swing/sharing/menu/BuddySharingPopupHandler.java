package org.limewire.ui.swing.sharing.menu;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.limewire.core.api.library.FileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.ui.swing.sharing.friends.BuddyItem;
import org.limewire.ui.swing.sharing.friends.BuddyNameTable;
import org.limewire.ui.swing.table.TablePopupHandler;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.swing.EventTableModel;

/**
 * Popup menu for the buddy list names in the sharing view
 */
public class BuddySharingPopupHandler implements TablePopupHandler {

    private int popupRow = -1;
    
    protected JPopupMenu popupMenu;
    private JMenuItem viewLibraryItem;
    private JMenuItem musicShareAllItem;
    private JMenuItem videoShareAllItem;
    private JMenuItem imageShareAllItem;
    private JMenuItem unshareAllItem;
    
    private BuddyNameTable table;
    private final BuddySharingActionHandler actionHandler;
    private final LibraryManager libraryManager;
    
    protected final MenuListener menuListener;

    private BuddyItem currentBuddy;
    private FileList buddyFileList;
    
    public BuddySharingPopupHandler(BuddyNameTable table, BuddySharingActionHandler handler, LibraryManager libraryManager) {
        this.table = table;
        this.actionHandler = handler;
        this.libraryManager = libraryManager;
        this.menuListener = new MenuListener();
        
        initialize();
    }
    
    protected void initialize() {
        popupMenu = new JPopupMenu();
        
        viewLibraryItem = new JMenuItem(I18n.tr("View Library"));
        viewLibraryItem.setActionCommand(BuddySharingActionHandler.VIEW_LIBRARY);
        viewLibraryItem.addActionListener(menuListener);
        
        musicShareAllItem = new JMenuItem(I18n.tr("Share all music"));
        musicShareAllItem.setActionCommand(BuddySharingActionHandler.SHARE_ALL_AUDIO);
        musicShareAllItem.addActionListener(menuListener);
        
        videoShareAllItem = new JMenuItem(I18n.tr("Share all videos"));
        videoShareAllItem.setActionCommand(BuddySharingActionHandler.SHARE_ALL_VIDEO);
        videoShareAllItem.addActionListener(menuListener);
        
        imageShareAllItem = new JMenuItem(I18n.tr("Share all images"));
        imageShareAllItem.setActionCommand(BuddySharingActionHandler.SHARE_ALL_IMAGE);
        imageShareAllItem.addActionListener(menuListener);
        
        unshareAllItem = new JMenuItem(I18n.tr("Unshare all"));
        unshareAllItem.setActionCommand(BuddySharingActionHandler.UNSHARE_ALL);
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
        
        EventTableModel<BuddyItem> model = table.getEventTableModel();
        currentBuddy = model.getElementAt(popupRow);
        if(currentBuddy.size() > 0) {
            unshareAllItem.setEnabled(true);
        } else {
            unshareAllItem.setEnabled(false);
        }
        if(currentBuddy.hasLibrary()) {
            viewLibraryItem.setEnabled(true);
        } else {
            viewLibraryItem.setEnabled(false);
        }
        
        buddyFileList = libraryManager.getBuddy(currentBuddy.getName());
        
        popupMenu.show(component, x, y);
    }
    
    private class MenuListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            actionHandler.performAction(e.getActionCommand(), buddyFileList, currentBuddy);
        }
    }
}
