package org.limewire.ui.swing.downloads;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.jdesktop.application.Application;
import org.jdesktop.swingx.JXTable;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.ui.swing.dnd.DownloadableTransferHandler;
import org.limewire.ui.swing.dock.DockIconFactory;
import org.limewire.ui.swing.downloads.table.DownloadTableFactory;
import org.limewire.ui.swing.tray.Notification;
import org.limewire.ui.swing.tray.TrayNotifier;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;
import org.limewire.ui.swing.util.SwingUtils;

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class MainDownloadPanel extends JPanel {  	
    
    public static final String NAME = "MainDownloadPanel";    
    
    private TrayNotifier notifier;

    private DownloadMediator downloadMediator;
    
    private boolean isInitialized = false;
    private final DownloadTableFactory downloadTableFactory;
    private final SaveLocationExceptionHandler saveLocationExceptionHandler;
    private final DownloadListManager downloadListManager;
    
    
	/**
	 * Create the panel
	 */
	@Inject
	public MainDownloadPanel(DownloadTableFactory downloadTableFactory, 
	        DownloadMediator downloadMediator,
	        DockIconFactory dockIconFactory,
	        TrayNotifier notifier, 
	        DownloadListManager downloadListManager,
	        SaveLocationExceptionHandler saveLocationExceptionHandler) {
	    this.downloadMediator = downloadMediator;
	    this.downloadTableFactory = downloadTableFactory;
	    this.downloadListManager = downloadListManager;
		this.notifier = notifier;
		this.saveLocationExceptionHandler = saveLocationExceptionHandler;
    }

    
    @Inject
    public void register() {              
        downloadMediator.getDownloadList().addListEventListener(new VisibilityListListener());
    }

    //initialize() is called when the first downloadItem is added to the list
    private void initialize() {
        isInitialized = true;
        setLayout(new BorderLayout());

        JXTable table = downloadTableFactory.create(downloadMediator.getDownloadList());
        table.setTableHeader(null);
        JScrollPane pane = new JScrollPane(table);
        pane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        add(pane, BorderLayout.CENTER);

        setTransferHandler(new DownloadableTransferHandler(downloadListManager, saveLocationExceptionHandler));

        // handle individual completed downloads
        initializeDownloadListeners(downloadListManager);
    }

    
    private void initializeDownloadListeners(final DownloadListManager downloadListManager) {
        // handle individual completed downloads
        downloadListManager.addPropertyChangeListener(new DownloadPropertyListener());

        downloadListManager.getDownloads().addListEventListener(
                new ListEventListener<DownloadItem>() {
                    @Override
                    public void listChanged(ListEvent<DownloadItem> listChanges) {
                        // only show the notification messages if the tray
                        // downloads are forced to be invisible
                        if (shouldShowNotification()) {
                            while (listChanges.nextBlock()) {
                                if (listChanges.getType() == ListEvent.INSERT) {
                                    int index = listChanges.getIndex();
                                    final DownloadItem downloadItem = listChanges.getSourceList().get(index);
                                    SwingUtils.invokeLater(new Runnable() {
                                        @Override
                                        public void run() {
                                            notifier.showMessage(new Notification(I18n.tr("Download Started"), downloadItem.getFileName(), 
                                                    new AbstractAction(I18n.tr("Show")) {
                                                @Override
                                                public void actionPerformed(ActionEvent e) {
                                                    setSize(getPreferredSize().width, 80);
                                                }
                                            }));
                                        }
                                    });
                                }
                            }
                        }
                    }
                });
    }
    
    private boolean shouldShowNotification(){
        return !isShowing() || getVisibleRect().height < 5;
    }
    


    private class DownloadPropertyListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent event) {
            if (event.getPropertyName().equals(DownloadListManager.DOWNLOAD_COMPLETED)) {
                final DownloadItem downloadItem = (DownloadItem) event.getNewValue();
                notifier.showMessage(new Notification(I18n.tr("Download Complete"), downloadItem
                        .getFileName(), new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        ActionMap map = Application.getInstance().getContext().getActionManager()
                                .getActionMap();
                        map.get("restoreView").actionPerformed(e);

                        if (downloadItem.isLaunchable()) {
                            DownloadItemUtils.launch(downloadItem);
                        }
                    }
                }));
            }
        }
    }

/**
 * Initializes the download panel contents the first time the list changes (when the first DownloadItem is added).  
 * Adjusts visibility of the panel depending on whether or not the list is empty.
 */
    private class VisibilityListListener implements ListEventListener<DownloadItem> {
      
        @Override
        public void listChanged(ListEvent<DownloadItem> listChanges) {
            if(!isInitialized){
                initialize();
            }
            setVisible(listChanges.getSourceList().size() > 0);
        }
    }
    
    
}
