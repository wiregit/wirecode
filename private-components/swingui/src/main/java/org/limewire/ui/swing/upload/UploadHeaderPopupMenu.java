package org.limewire.ui.swing.upload;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.jdesktop.application.Application;
import org.jdesktop.application.Resource;
import org.limewire.core.settings.SharingSettings;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.options.OptionsDialog;
import org.limewire.ui.swing.upload.UploadMediator.SortOrder;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

/**
 * Options menu for performing actions on the upload table.
 */
class UploadHeaderPopupMenu extends JPopupMenu {

    @Resource(key="DownloadHeaderPopupMenu.upArrow") private Icon upArrow;
    @Resource(key="DownloadHeaderPopupMenu.downArrow") private Icon downArrow;
    
    private final UploadMediator uploadMediator;
    
    /**
     * Constructs an UploadHeaderPopupMenu.
     */
    public UploadHeaderPopupMenu(UploadMediator uploadMediator) {
        this.uploadMediator = uploadMediator;
        
        GuiUtils.assignResources(this);
        
        addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                removeAll();
            }
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                removeAll();
            }
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                populate();
            }
        });
    }
    
    /**
     * Adds menu items to the popup menu.
     */
    private void populate() {
        removeAll();
        
        add(new AbstractAction(I18n.tr("Pause All")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                uploadMediator.pauseAll();
            }
        }).setEnabled(uploadMediator.hasPausable());
        
        add(new AbstractAction(I18n.tr("Resume All")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                uploadMediator.resumeAll();
            }
        }).setEnabled(uploadMediator.hasResumable());
        
        add(createCancelSubMenu());
        addSeparator();
        add(createSortSubMenu());
        addSeparator();
        add(createClearFinishedMenuItem());
        addSeparator();
        add(new TransferOptionsAction());
    }
    
    /**
     * Creates the Cancel sub-menu.
     */
    private JMenu createCancelSubMenu() {
        JMenu cancelSubMenu = new JMenu(I18n.tr("Cancel"));
        
        cancelSubMenu.add(new AbstractAction(I18n.tr("All Error")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (confirmCancel(I18n.tr("Cancel all error uploads?"))) {
                    uploadMediator.cancelAllError();
                }
            }
        }).setEnabled(uploadMediator.hasErrors());
        
        cancelSubMenu.add(new AbstractAction(I18n.tr("All Torrents")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (confirmCancel(I18n.tr("Cancel uploading all torrents?"))) {
                    uploadMediator.cancelAllTorrents();
                }
            }
        }).setEnabled(uploadMediator.hasTorrents());
        
        cancelSubMenu.add(new AbstractAction(I18n.tr("All Uploads")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (confirmCancel(I18n.tr("Cancel all uploads?"))) {
                    uploadMediator.cancelAll();
                }
            }
        }).setEnabled(uploadMediator.getUploadList().size() > 0);
        
        return cancelSubMenu;
    }
    
    /**
     * Creates the Sort sub-menu.
     */
    private JMenu createSortSubMenu() {
        JMenu sortSubMenu = new JMenu(I18n.tr("Sort by"));
        
        // Create sort key menu items.
        JCheckBoxMenuItem orderStarted = new JCheckBoxMenuItem(
                new SortAction(I18n.tr("Order Started"), SortOrder.ORDER_STARTED));
        JCheckBoxMenuItem name = new JCheckBoxMenuItem(
                new SortAction(I18n.tr("Name"), SortOrder.NAME));
        JCheckBoxMenuItem progress = new JCheckBoxMenuItem(
                new SortAction(I18n.tr("Progress"), SortOrder.PROGRESS));
        JCheckBoxMenuItem timeRemaining = new JCheckBoxMenuItem(
                new SortAction(I18n.tr("Time Left"), SortOrder.TIME_REMAINING));
        JCheckBoxMenuItem speed = new JCheckBoxMenuItem(
                new SortAction(I18n.tr("Speed"), SortOrder.SPEED));
        JCheckBoxMenuItem status = new JCheckBoxMenuItem(
                new SortAction(I18n.tr("Status"), SortOrder.STATUS));
        JCheckBoxMenuItem fileType = new JCheckBoxMenuItem(
                new SortAction(I18n.tr("File Type"), SortOrder.FILE_TYPE));
        JCheckBoxMenuItem extension = new JCheckBoxMenuItem(
                new SortAction(I18n.tr("File Extension"), SortOrder.FILE_EXTENSION));        

        // Create button group.
        ButtonGroup sortButtonGroup = new ButtonGroup();
        sortButtonGroup.add(orderStarted);
        sortButtonGroup.add(name);
        sortButtonGroup.add(progress);
        sortButtonGroup.add(timeRemaining);
        sortButtonGroup.add(speed);
        sortButtonGroup.add(status);
        sortButtonGroup.add(fileType);
        sortButtonGroup.add(extension);
        
        // Add menu items to menu.
        sortSubMenu.add(orderStarted);
        sortSubMenu.add(name);
        sortSubMenu.add(progress);
        sortSubMenu.add(timeRemaining);
        sortSubMenu.add(speed);
        sortSubMenu.add(status);
        sortSubMenu.add(fileType);
        sortSubMenu.add(extension);
        
        sortSubMenu.addSeparator();
        
        // Add menu item to reverse sort direction.
        AbstractButton reverseSortButton = new JMenuItem(new ReverseSortAction());
        sortSubMenu.add(reverseSortButton);
        
        return sortSubMenu;
    }
    
    /**
     * Creates a new checkbox menu item for the Clear When Finished action.
     */
    private JCheckBoxMenuItem createClearFinishedMenuItem() {
        JCheckBoxMenuItem checkBoxMenuItem = new JCheckBoxMenuItem(I18n.tr("Clear When Finished"));
        checkBoxMenuItem.setSelected(SharingSettings.CLEAR_UPLOAD.getValue());
        checkBoxMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JMenuItem menuItem = (JMenuItem) e.getSource();
                SharingSettings.CLEAR_UPLOAD.setValue(menuItem.isSelected());
            }
        });
        
        return checkBoxMenuItem;
    }
    
    /**
     * Prompts the user to confirm an action with the specified message.
     */
    private boolean confirmCancel(String message) {
        return (FocusJOptionPane.showConfirmDialog(GuiUtils.getMainFrame(),
                message, I18n.tr("Cancel"), JOptionPane.YES_NO_OPTION, 
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION);
    }
    
    /**
     * Action to sort the uploads list.
     */
    private class SortAction extends AbstractAction {
        private final SortOrder sortOrder;
    
        public SortAction(String title, SortOrder sortOrder) {
            super(title);
            this.sortOrder = sortOrder;
            // Select this action if currently in use.
            putValue(SELECTED_KEY, sortOrder == uploadMediator.getSortOrder());
        }
      
        @Override
        public void actionPerformed(ActionEvent e) {
            uploadMediator.setSortOrder(sortOrder, uploadMediator.isSortAscending());
        }
    }; 
    
    /**
     * Action to reverse the sort order of the uploads list.
     */
    private class ReverseSortAction extends AbstractAction {
    
        public ReverseSortAction() {
            super(I18n.tr("Reverse Order"));
            
            putValue(Action.SELECTED_KEY, uploadMediator.isSortAscending());
            putValue(Action.SMALL_ICON, uploadMediator.isSortAscending() ? downArrow : upArrow);
        }
      
        @Override
        public void actionPerformed(ActionEvent e) {
            boolean direction = !uploadMediator.isSortAscending();
            putValue(Action.SMALL_ICON, direction ? downArrow : upArrow);
            uploadMediator.setSortOrder(uploadMediator.getSortOrder(), direction);
        }
    }; 
    
    /**
     * Action to display the Transfer Options dialog tab.
     */
    private static class TransferOptionsAction extends AbstractAction {

        public TransferOptionsAction() {
            super(I18n.tr("More Transfer Options..."));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            ActionMap map = Application.getInstance().getContext().getActionManager().getActionMap();
            map.get("showOptionsDialog").actionPerformed(new ActionEvent(
                    this, ActionEvent.ACTION_PERFORMED, OptionsDialog.TRANSFERS));
        }
    }
}
