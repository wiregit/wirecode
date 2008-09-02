package org.limewire.ui.swing.sharing.menu;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;

import org.limewire.ui.swing.sharing.friends.BuddyItem;
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
    
    private JTable table;
    private final BuddySharingActionHandler actionHandler;
    
    protected final MenuListener menuListener;

    public BuddySharingPopupHandler(JTable table, BuddySharingActionHandler handler) {
        this.table = table;
        this.actionHandler = handler;
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
        
        EventTableModel<BuddyItem> model = (EventTableModel<BuddyItem>) table.getModel();
        BuddyItem item = model.getElementAt(popupRow);
        if(item.size() > 0) {
            unshareAllItem.setEnabled(true);
        } else {
            unshareAllItem.setEnabled(false);
        }
        
        popupMenu.show(component, x, y);
    }
    
    private class MenuListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
//            actionHandler.performAction(e.getActionCommand(), fileList, fileItem);
        }
    }
}
