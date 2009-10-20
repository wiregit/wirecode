package org.limewire.ui.swing.upload;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.api.upload.UploadListManager;
import org.limewire.inject.LazySingleton;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.nav.NavMediator;
import org.limewire.ui.swing.upload.table.UploadTable;
import org.limewire.ui.swing.upload.table.UploadTableFactory;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;

/**
 * Mediator to control the interaction between the uploads table and various
 * services.
 */
@LazySingleton
public class UploadMediator implements NavMediator<JComponent> {
    
    public static final String NAME = "UploadPanel";
    
    private final UploadListManager uploadListManager;
    private final UploadTableFactory uploadTableFactory;
    
    private JPanel uploadPanel;
    
    private Action clearFinishedAction;
    
    private List<JButton> headerButtons;
    private JPopupMenu headerPopupMenu;
    
    @Inject
    public UploadMediator(UploadListManager uploadListManager,
            UploadTableFactory uploadTableFactory) {
        this.uploadListManager = uploadListManager;
        this.uploadTableFactory = uploadTableFactory;
    }
    
    /**
     * Start the (polling) upload monitor.  
     * <p>
     * Note: this only makes sense if this component is created on demand.
     */
    @Inject
    public void register(ServiceRegistry serviceRegister) {
        serviceRegister.start(uploadListManager);
    }
    
    @Override
    public JComponent getComponent() {
        if (uploadPanel == null) {
            uploadPanel = createUploadPanel();
        }
        return uploadPanel;
    }
    
    /**
     * Creates a panel containing the upload table.
     */
    private JPanel createUploadPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        UploadTable table = uploadTableFactory.create(this);
        table.setTableHeader(null);
        
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Returns the number of active uploads.
     */
    public int getActiveListSize() {
        // TODO return active uploads only
        return uploadListManager.getSwingThreadSafeUploads().size();
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
     * Returns a list of uploads.
     */
    public EventList<UploadItem> getUploadList() {
        return uploadListManager.getSwingThreadSafeUploads();
    }
    
    /**
     * Clears all finished uploads.
     */
    private void clearFinished() {
        uploadListManager.clearFinished();
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
