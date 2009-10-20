package org.limewire.ui.swing.downloads;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableCellEditor;

import org.jdesktop.application.Application;
import org.limewire.core.api.URN;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.file.CategoryManager;
import org.limewire.core.settings.DownloadSettings;
import org.limewire.inject.EagerSingleton;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.downloads.table.DownloadTable;
import org.limewire.ui.swing.downloads.table.DownloadTableFactory;
import org.limewire.ui.swing.event.DownloadVisibilityEvent;
import org.limewire.ui.swing.tray.Notification;
import org.limewire.ui.swing.tray.TrayNotifier;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SwingUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.google.inject.Inject;
import com.google.inject.Provider;

//EagerSingleton to ensure that register() is called and the listeners are in place at startup
@EagerSingleton
public class MainDownloadPanel extends JPanel {  	
    
    public static final String NAME = "MainDownloadPanel";    
        
    private final DownloadMediator downloadMediator;
    private final Provider<DownloadTableFactory> downloadTableFactory;
    private final DownloadListManager downloadListManager;
    private final CategoryManager categoryManager;
    private final ArrayList<DownloadVisibilityListener> downloadVisibilityListeners = new ArrayList<DownloadVisibilityListener>();
    
    private TrayNotifier notifier;
    private boolean isInitialized = false;
    private DownloadTable table;
    
    
    /**
     * Create the panel.
     */
    @Inject
    public MainDownloadPanel(Provider<DownloadTableFactory> downloadTableFactory, 
            DownloadMediator downloadMediator,
            TrayNotifier notifier, 
            DownloadListManager downloadListManager,
            CategoryManager categoryManager) {
        this.downloadMediator = downloadMediator;
        this.downloadTableFactory = downloadTableFactory;
        this.downloadListManager = downloadListManager;
        this.categoryManager = categoryManager;
        this.notifier = notifier;

        GuiUtils.assignResources(this);
    }
    
    public void selectAndScrollTo(URN urn) {
        table.selectAndScrollTo(urn);
        if(getVisibleRect().height < table.getRowHeight()){
            alertDownloadVisibilityListeners(true);
        }
    }

    
    @Inject
    public void register() {              
        downloadMediator.getDownloadList().addListEventListener(new VisibilityListListener());
        DownloadSettings.ALWAYS_SHOW_DOWNLOADS_TRAY.addSettingListener(new SettingListener() {
           @Override
            public void settingChanged(SettingEvent evt) {
               SwingUtils.invokeLater(new Runnable() {
                   @Override
                    public void run() {
                       updateVisibility(downloadMediator.getDownloadList());
                    }
               });
            } 
        });
        
        //we have to eagerly initialize the table when the ALWAYS_SHOW_DOWNLOAD_TRAY setting
        //is set to true on startup, otherwise the table space will be empty and the lines will
        //be put in the first time a download comes in, which looks a little weird
        if(DownloadSettings.ALWAYS_SHOW_DOWNLOADS_TRAY.getValue()) {
            initialize();
        }
    }
    
    public void addDownloadVisibilityListener(DownloadVisibilityListener listener){
        downloadVisibilityListeners.add(listener);
    }
    
    public void removeDownloadVisibilityListener(DownloadVisibilityListener listener){
        downloadVisibilityListeners.remove(listener);
    }

    //Lazily initialized - initialize() is called when the first downloadItem is added to the list.  
    private void initialize() {
        isInitialized = true;
        setLayout(new BorderLayout());

        table = downloadTableFactory.get().create(downloadMediator.getDownloadList());
        table.setTableHeader(null);
        JScrollPane pane = new JScrollPane(table);
        pane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        add(pane, BorderLayout.CENTER);

        // handle individual completed downloads
        initializeDownloadListeners(downloadListManager);
    }
    
    public List<DownloadItem> getSelectedDownloadItems(){
        return table.getSelectedItems();
    }
    
    private void initializeDownloadListeners(final DownloadListManager downloadListManager) {
        // handle individual completed downloads
        downloadListManager.addPropertyChangeListener(new DownloadPropertyListener());
    }

    private class DownloadPropertyListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent event) {
            if (event.getPropertyName().equals(DownloadListManager.DOWNLOAD_COMPLETED)) {
                final DownloadItem downloadItem = (DownloadItem) event.getNewValue();
                notifier.showMessage(new Notification(I18n.tr("Download Complete"), downloadItem.getFileName(), 
                        new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        ActionMap map = Application.getInstance().getContext().getActionManager()
                                .getActionMap();
                        map.get("restoreView").actionPerformed(e);
                        
                        if (downloadItem.isLaunchable()) {
                            DownloadItemUtils.launch(downloadItem, categoryManager);
                        }
                    }
                }));
                
                // the user might be editing one of the cell's while the download completes,
                // i.e. the user might have the mouse hovering over the pause button. (Bug LWC-4317)
                // Let's manually cancel cell editing here after the download completes
                TableCellEditor editor = table.getCellEditor();
                if ( editor != null )
                {
                    editor.cancelCellEditing();
                }
            }
        }
    }

    /**
     * Initializes the download panel contents when the list changes (when a 
     * DownloadItem is added).
     */
    private class VisibilityListListener implements ListEventListener<DownloadItem> {
        private int downloadCount;
      
        @Override
        public void listChanged(ListEvent<DownloadItem> listChanges) {
            EventList sourceList = listChanges.getSourceList();
            // Update download tray setting only when items are added.
            if (sourceList.size() > downloadCount) {
                downloadCount = sourceList.size();
                if (!DownloadSettings.ALWAYS_SHOW_DOWNLOADS_TRAY.getValue()) {
                    DownloadSettings.ALWAYS_SHOW_DOWNLOADS_TRAY.setValue(true);
                } else {
                    updateVisibility(sourceList);
                }
            }
        }
    }
    
    /**
     * Updates the visibility of this download panel.  This method is called
     * when the download list changes, or when the "show downloads" setting 
     * changes.
     */
    private void updateVisibility(EventList sourceList) {
        if(!isInitialized){
            initialize();
        }
        
        if (DownloadSettings.ALWAYS_SHOW_DOWNLOADS_TRAY.getValue()) {
            alertDownloadVisibilityListeners(true);
        } else {
            alertDownloadVisibilityListeners(false);
        }
    }
    
    /**
     * Ignores DownloadSettings.ALWAYS_SHOW_DOWNLOADS_TRAY
     */
    private void alertDownloadVisibilityListeners(boolean isVisible) {
        for (DownloadVisibilityListener listener : downloadVisibilityListeners) {
            listener.updateVisibility(new DownloadVisibilityEvent(isVisible));
        }

    }
}
