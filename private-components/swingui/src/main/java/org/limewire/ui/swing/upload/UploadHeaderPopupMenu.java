package org.limewire.ui.swing.upload;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.jdesktop.application.Application;
import org.limewire.core.settings.SharingSettings;
import org.limewire.ui.swing.options.OptionsDialog;
import org.limewire.ui.swing.util.I18n;

/**
 * Context menu for performing actions on the upload table.
 */
class UploadHeaderPopupMenu extends JPopupMenu {

    private final UploadMediator uploadMediator;
    
    public UploadHeaderPopupMenu(UploadMediator uploadMediator) {
        this.uploadMediator = uploadMediator;
        
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
    
    public void populate() {
        removeAll();
        
        // TODO get menu actions from UploadMediator
        add(new AbstractAction(I18n.tr("Pause All")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("pausing all uploads...");
            }
        });
        add(new AbstractAction(I18n.tr("Resume All")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("resuming all uploads...");
            }
        });
//        add(getPauseUploadAction()).setEnabled(uploadMediator.hasPausable());
//        add(getResumeUploadAction()).setEnabled(uploadMediator.hasResumable());
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
        
        // TODO add items
        
        return cancelSubMenu;
    }
    
    /**
     * Creates the Sort sub-menu.
     */
    private JMenu createSortSubMenu() {
        JMenu sortSubMenu = new JMenu(I18n.tr("Sort by"));
        
        // TODO add items
        
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
