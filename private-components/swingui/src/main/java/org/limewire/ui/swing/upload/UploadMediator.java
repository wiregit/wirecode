package org.limewire.ui.swing.upload;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPopupMenu;

import org.limewire.inject.LazySingleton;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.nav.NavMediator;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

@LazySingleton
public class UploadMediator implements NavMediator<UploadPanel> {
    
    public static final String NAME = "UploadPanel";
    
    private final Provider<UploadPanel> uploadPanel;
    private UploadPanel upload;
    
    private Action clearFinishedAction;
    
    private List<JButton> headerButtons;
    private JPopupMenu headerPopupMenu;
    
    @Inject
    public UploadMediator(Provider<UploadPanel> uploadPanel) {
        this.uploadPanel = uploadPanel;
    }
        
    @Override
    public UploadPanel getComponent() {
        if(upload == null)
            upload = uploadPanel.get();
        return upload;
    }
    
    /**
     * Returns a list of header buttons.
     */
    public List<JButton> getHeaderButtons() {
        if (headerButtons == null) {
            clearFinishedAction = new ClearFinishedAction();
            headerButtons = new ArrayList<JButton>();
            headerButtons.add(new HyperlinkButton(clearFinishedAction));
        }
        return headerButtons;
    }
    
    /**
     * Returns the header popup menu associated with the uploads table.
     */
    public JPopupMenu getHeaderPopupMenu() {
        if (headerPopupMenu == null) {
            headerPopupMenu = new UploadHeaderPopupMenu(this);
        }
        return headerPopupMenu;
    }
    
    /**
     * Clears all finished uploads.
     */
    private void clearFinished() {
        // TODO implement
    }
    
    /**
     * Action to clear all finished uploads.
     */
    private class ClearFinishedAction extends AbstractAction {

        public ClearFinishedAction() {
            super(I18n.tr("Clear Finished"));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            clearFinished();
        }
    }
}
