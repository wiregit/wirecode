package org.limewire.ui.swing.upload;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

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
        
        // TODO replace dummy actions
        add(new AbstractAction(I18n.tr("Pause All")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("pausing all uploads...");
            }
        });
        add(new AbstractAction(I18n.tr("Resume All")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("resumsing all uploads...");
            }
        });
        
//        TODO get menu actions from UploadMediator
//        add(getPauseUploadAction()).setEnabled(uploadMediator.hasPausable());
//        add(getResumeUploadAction()).setEnabled(uploadMediator.hasResumable());
//        add(createCancelSubMenu());
//        addSeparator();
//        add(createSortSubMenu());
//        addSeparator();
//        add(getMoreOptionsAction());
    }
}
