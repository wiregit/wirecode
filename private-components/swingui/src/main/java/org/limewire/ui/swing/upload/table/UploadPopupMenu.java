package org.limewire.ui.swing.upload.table;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.table.TableCellEditor;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.api.upload.UploadState;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

/**
 * Popup menu for the Uploads table.
 */
public class UploadPopupMenu extends JPopupMenu {

    private final UploadTable table;
    private final List<UploadItem> uploadItems;
    private final UploadActionHandler actionHandler;
    private final LibraryManager libraryManager;
    
    // TODO arrange to clear UploadItem references when menu closed!!!
    // was WeakReference in UploadPopupHandler

    private MenuListener menuListener;

    private JMenuItem launchMenuItem;
    private JMenuItem playMenuItem;
    private JMenuItem removeMenuItem;
    private JMenuItem showInLibraryMenuItem;
    private JMenuItem locateOnDiskMenuItem;
    private JMenuItem cancelMenuItem;
    private JMenuItem propertiesMenuItem;

    @Inject
    public UploadPopupMenu(
            @Assisted UploadTable table,
            @Assisted List<UploadItem> uploadItems,
            UploadActionHandler actionHandler,
            LibraryManager libraryManager) {
        this.table = table;
        this.uploadItems = uploadItems;
        this.actionHandler = actionHandler;
        this.libraryManager = libraryManager;
        
        initializeMenuItems();
        initializeMenu();
    }

    private void initializeMenuItems() {
        menuListener = new MenuListener();

        showInLibraryMenuItem = new JMenuItem(I18n.tr("Locate in Library"));
        showInLibraryMenuItem.setActionCommand(UploadActionHandler.LIBRARY_COMMAND);
        showInLibraryMenuItem.addActionListener(menuListener);        
        
        cancelMenuItem = new JMenuItem(I18n.tr("Cancel"));
        cancelMenuItem.setActionCommand(UploadActionHandler.CANCEL_COMMAND);
        cancelMenuItem.addActionListener(menuListener);

        launchMenuItem = new JMenuItem(I18n.tr("Launch File"));
        launchMenuItem.setActionCommand(UploadActionHandler.LAUNCH_COMMAND);
        launchMenuItem.addActionListener(menuListener);
        
        playMenuItem = new JMenuItem(I18n.tr("Play File"));
        playMenuItem.setActionCommand(UploadActionHandler.PLAY_COMMAND);
        playMenuItem.addActionListener(menuListener);
        
        removeMenuItem = new JMenuItem(I18n.tr("Remove from List"));
        removeMenuItem.setActionCommand(UploadActionHandler.REMOVE_COMMAND);
        removeMenuItem.addActionListener(menuListener);
        
        locateOnDiskMenuItem = new JMenuItem(I18n.tr("Locate on Disk"));
        locateOnDiskMenuItem.setActionCommand(UploadActionHandler.LOCATE_ON_DISK_COMMAND);
        locateOnDiskMenuItem.addActionListener(menuListener);
        
        propertiesMenuItem = new JMenuItem(I18n.tr("View File Info..."));
        propertiesMenuItem.setActionCommand(UploadActionHandler.PROPERTIES_COMMAND);
        propertiesMenuItem.addActionListener(menuListener);
    }
    
    private void initializeMenu() {
        // TODO for now, use first upload item; must create menu variations
        UploadItem uploadItem = uploadItems.get(0);
        
        UploadState state = uploadItem.getState();
        
        if(state == UploadState.BROWSE_HOST || state == UploadState.BROWSE_HOST_DONE || state == UploadState.DONE || state == UploadState.UNABLE_TO_UPLOAD){
            add(removeMenuItem);
        } else {
            add(cancelMenuItem);
        }
        
        if (state != UploadState.BROWSE_HOST && state != UploadState.BROWSE_HOST_DONE) {
            addSeparator();

            if (uploadItem.getCategory() == Category.VIDEO || uploadItem.getCategory() == Category.AUDIO) {
                add(playMenuItem);
            } else if (uploadItem.getCategory() != Category.PROGRAM && uploadItem.getCategory() != Category.OTHER) {
                add(launchMenuItem);
            }
            add(locateOnDiskMenuItem);
            add(showInLibraryMenuItem).setEnabled(libraryManager.getLibraryManagedList().contains(uploadItem.getUrn()));

            addSeparator();

            add(propertiesMenuItem);
        }
    }
    
    /**
     * Action listener for menu items.
     */
    private class MenuListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
//            UploadItem uploadItem = uploadItemReference.get();
            UploadItem uploadItem = uploadItems.get(0);
            if (uploadItem != null) {
                actionHandler.performAction(e.getActionCommand(), uploadItem);
            }
            //must cancel editing
            Component comp = table.getEditorComponent();
            if(comp!=null && comp instanceof TableCellEditor){
                ((TableCellEditor)comp).cancelCellEditing();
            }
        }
    }
}
