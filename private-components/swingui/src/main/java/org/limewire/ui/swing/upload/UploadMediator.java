package org.limewire.ui.swing.upload;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.api.upload.UploadListManager;
import org.limewire.core.api.upload.UploadState;
import org.limewire.core.settings.SharingSettings;
import org.limewire.inject.LazySingleton;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.nav.NavMediator;
import org.limewire.ui.swing.upload.table.UploadTable;
import org.limewire.ui.swing.upload.table.UploadTableFactory;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.matchers.Matcher;

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
    
    private EventList<UploadItem> activeList;
    
    private JPanel uploadPanel;
    
    private JButton clearFinishedButton;
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
        
        // Add setting listener to clear finished uploads.  When set, we clear
        // finished uploads and hide the "clear finished" button.
        SharingSettings.CLEAR_UPLOAD.addSettingListener(new SettingListener() {
            @Override
            public void settingChanged(SettingEvent evt) {
                boolean clearUploads = SharingSettings.CLEAR_UPLOAD.getValue();
                if (clearUploads) {
                    clearFinished();
                }
                if (clearFinishedButton != null) {
                    clearFinishedButton.setVisible(!clearUploads);
                }
            }
        });
    }
    
    @Override
    public JComponent getComponent() {
        if (uploadPanel == null) {
            uploadPanel = createUploadPanel();
        }
        return uploadPanel;
    }
    
    /**
     * Creates a display panel containing the upload table.
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
     * Returns a list of active upload items.
     */
    public EventList<UploadItem> getActiveList() {
        if (activeList == null) {
            activeList = GlazedListsFactory.filterList(getUploadList(), 
                    new UploadStateMatcher(false, UploadState.DONE, UploadState.CANCELED, 
                            UploadState.BROWSE_HOST_DONE, UploadState.UNABLE_TO_UPLOAD));
        }
        return activeList;
    }
    
    /**
     * Returns a list of header buttons.
     */
    public List<JButton> getHeaderButtons() {
        if (headerButtons == null) {
            clearFinishedButton = new HyperlinkButton(new ClearFinishedAction());
            clearFinishedButton.setVisible(!SharingSettings.CLEAR_UPLOAD.getValue());
            
            headerButtons = new ArrayList<JButton>();
            headerButtons.add(clearFinishedButton);
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
    
    /**
     * Matcher to filter for upload states.
     */
    private static class UploadStateMatcher implements Matcher<UploadItem> {
        private final boolean inclusive;
        private final Set<UploadState> uploadStates;
        
        /**
         * Constructs a matcher that either includes or excludes the specified
         * upload states.
         */
        public UploadStateMatcher(boolean inclusive, UploadState first, UploadState... rest) {
            this.inclusive = inclusive;
            this.uploadStates = EnumSet.of(first, rest);
        }
        
        @Override
        public boolean matches(UploadItem item) {
            if (item == null) return false;
            
            boolean match = uploadStates.contains(item.getState());
            return inclusive ? match : !match;
        }
    }
}
