package org.limewire.ui.swing.downloads.table;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.table.TableCellEditor;

import org.limewire.core.api.Category;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.util.I18n;

public class DownloadTableMenu extends JPopupMenu{

    private JMenuItem launchMenuItem;
    private JMenuItem previewMenuItem;
    private JMenuItem removeMenuItem;
    private JMenuItem pauseMenuItem;
    private JMenuItem locateMenuItem;
    private JMenuItem libraryMenuItem;
    private JMenuItem cancelMenuItem;
    private JMenuItem resumeMenuItem;
    private JMenuItem tryAgainMenuItem;
    private JMenuItem propertiesMenuItem;
    private JMenuItem shareMenuItem;
    private JMenuItem playMenuItem;
    private JMenuItem viewMenuItem;
    private JMenuItem cancelWithRemoveNameMenuItem;
    
    private MenuListener menuListener;
    private DownloadActionHandler actionHandler;
    private DownloadTable table;
    
    private List<DownloadItem> downloadItems;

    /**
     * Constructs a DownloadTableMenu using the specified action handler and
     * display table.
     */
    public DownloadTableMenu(DownloadActionHandler actionHandler, DownloadTable table) {
        this.actionHandler = actionHandler;
        this.table = table;

        menuListener = new MenuListener();

        pauseMenuItem = new JMenuItem(I18n.tr("Pause"));
        pauseMenuItem.setActionCommand(DownloadActionHandler.PAUSE_COMMAND);
        pauseMenuItem.addActionListener(new PauseListener());

        cancelMenuItem = new JMenuItem(I18n.tr("Cancel Download"));
        cancelMenuItem.setActionCommand(DownloadActionHandler.CANCEL_COMMAND);
        cancelMenuItem.addActionListener(new CancelListener());

        resumeMenuItem = new JMenuItem(I18n.tr("Resume"));
        resumeMenuItem.setActionCommand(DownloadActionHandler.RESUME_COMMAND);
        resumeMenuItem.addActionListener(new ResumeListener());

        tryAgainMenuItem = new JMenuItem(I18n.tr("Try Again"));
        tryAgainMenuItem.setActionCommand(DownloadActionHandler.TRY_AGAIN_COMMAND);
        tryAgainMenuItem.addActionListener(new TryAgainListener());

        launchMenuItem = new JMenuItem(I18n.tr("Launch File"));
        launchMenuItem.setActionCommand(DownloadActionHandler.LAUNCH_COMMAND);
        launchMenuItem.addActionListener(menuListener);

        removeMenuItem = new JMenuItem(I18n.tr("Remove from List"));
        removeMenuItem.setActionCommand(DownloadActionHandler.REMOVE_COMMAND);
        removeMenuItem.addActionListener(menuListener);

        cancelWithRemoveNameMenuItem = new JMenuItem(I18n.tr("Remove from List"));
        cancelWithRemoveNameMenuItem.setActionCommand(DownloadActionHandler.CANCEL_COMMAND);
        cancelWithRemoveNameMenuItem.addActionListener(menuListener);
        
        locateMenuItem = new JMenuItem(I18n.tr("Locate on Disk"));
        locateMenuItem.setActionCommand(DownloadActionHandler.LOCATE_COMMAND);
        locateMenuItem.addActionListener(menuListener);

        libraryMenuItem = new JMenuItem(I18n.tr("Locate in Library"));
        libraryMenuItem.setActionCommand(DownloadActionHandler.LIBRARY_COMMAND);
        libraryMenuItem.addActionListener(menuListener);

        
        propertiesMenuItem = new JMenuItem(I18n.tr("View File Info..."));
        propertiesMenuItem.setActionCommand(DownloadActionHandler.PROPERTIES_COMMAND);
        propertiesMenuItem.addActionListener(menuListener);

        previewMenuItem = new JMenuItem(I18n.tr("Preview File"));
        previewMenuItem.setActionCommand(DownloadActionHandler.PREVIEW_COMMAND);
        previewMenuItem.addActionListener(menuListener);

        shareMenuItem = new JMenuItem(I18n.tr("Share File"));
        shareMenuItem.setActionCommand(DownloadActionHandler.SHARE_COMMAND);
        shareMenuItem.addActionListener(menuListener);

        playMenuItem = new JMenuItem(I18n.tr("Play"));
        playMenuItem.setActionCommand(DownloadActionHandler.PLAY_COMMAND);
        playMenuItem.addActionListener(menuListener);

        viewMenuItem = new JMenuItem(I18n.tr("View"));
        viewMenuItem.setActionCommand(DownloadActionHandler.LAUNCH_COMMAND);
        viewMenuItem.addActionListener(menuListener);
        
        update(table.getSelectedItems());

    }

  
    public void update(List<DownloadItem> downloadItems) {
        this.downloadItems = downloadItems;
        removeAll();
        if (downloadItems.size() == 1) {
            initializeSingleItemMenu(downloadItems.get(0));
        } else {
            initializeMultiItemMenu(downloadItems);
        }
    }
    

    private void initializeSingleItemMenu(DownloadItem downloadItem){
        
        DownloadState state = downloadItem.getState();

        // add pause to all pausable states
        if (state.isPausable()) {
            add(pauseMenuItem);
            addSeparator();
        }

        switch (state) {

        case DONE:
            if (downloadItem.getCategory() != Category.PROGRAM
                    && downloadItem.getCategory() != Category.OTHER) {
                if (downloadItem.getCategory() == Category.AUDIO
                        || downloadItem.getCategory() == Category.VIDEO) {
                    add(playMenuItem).setEnabled(downloadItem.isLaunchable());
                } else if (downloadItem.getCategory() == Category.IMAGE
                        || downloadItem.getCategory() == Category.DOCUMENT) {
                    add(viewMenuItem).setEnabled(downloadItem.isLaunchable());
                } else {
                    add(launchMenuItem).setEnabled(downloadItem.isLaunchable());
                }
            }
            add(shareMenuItem);
            addSeparator();
            add(locateMenuItem);
            add(libraryMenuItem);
            addSeparator();
            add(removeMenuItem);
            break;

        case TRYING_AGAIN:
        case CONNECTING:
        case FINISHING:
        case LOCAL_QUEUED:
        case REMOTE_QUEUED:
            if (downloadItem.getCategory() != Category.PROGRAM
                    && downloadItem.getCategory() != Category.OTHER) {
                add(previewMenuItem).setEnabled(downloadItem.isLaunchable());
            }
            add(libraryMenuItem);
            add(new JSeparator());
            add(cancelMenuItem);
            break;
        case ERROR:
            add(cancelWithRemoveNameMenuItem);
            add(new JSeparator());
            if (downloadItem.getCategory() != Category.PROGRAM
                    && downloadItem.getCategory() != Category.OTHER) {
                add(previewMenuItem).setEnabled(downloadItem.isLaunchable());
            }
            add(libraryMenuItem);
            break;
        case RESUMING:
        case DOWNLOADING:
            if (downloadItem.getCategory() != Category.PROGRAM
                    && downloadItem.getCategory() != Category.OTHER) {
                add(previewMenuItem).setEnabled(downloadItem.isLaunchable());
            }
            add(libraryMenuItem);
            add(new JSeparator());
            add(cancelMenuItem);
            break;

        case PAUSED:
            add(resumeMenuItem);
            addSeparator();
            if (downloadItem.getCategory() != Category.PROGRAM
                    && downloadItem.getCategory() != Category.OTHER) {
                add(previewMenuItem).setEnabled(downloadItem.isLaunchable());
            }
            add(libraryMenuItem);
            add(new JSeparator());

            add(cancelMenuItem);
            break;

        case STALLED:
            add(tryAgainMenuItem);
            addSeparator();
            if (downloadItem.getCategory() != Category.PROGRAM
                    && downloadItem.getCategory() != Category.OTHER) {
                add(previewMenuItem).setEnabled(downloadItem.isLaunchable());
            }
            add(libraryMenuItem);
            add(new JSeparator());
            add(cancelMenuItem);
            break;
        }

        add(new JSeparator());
        add(propertiesMenuItem);
    
    }
    
    private void initializeMultiItemMenu(List<DownloadItem> downloadItems) {
        boolean hasTryAgain = false;
        boolean hasPause = false;
        boolean hasCancel = false;
        boolean hasResume = false;
        
        for(DownloadItem item : downloadItems){
            if (hasTryAgain && hasPause && hasCancel){
                break;
            }
            if(isResumable(item.getState())){
                hasResume = true;
            }
            if(isPausable(item.getState())){
                hasPause = true;
            }
            if(isTryAgainable(item.getState())){
                hasTryAgain = true;
            }
            if(isCancelable(item.getState())){
                hasCancel = true;
            }   
        }

        if (hasPause){
            add(pauseMenuItem);
        }
        if (hasResume){
            add(resumeMenuItem);
        }
        if (hasTryAgain){
            add(tryAgainMenuItem);
        }
        if (hasCancel){
            add(cancelMenuItem);
        }
        
    }

    private boolean isPausable(DownloadState state) {
        return state.isPausable();
    }
    
    private boolean isResumable(DownloadState state) {
        return state == DownloadState.PAUSED;
    }

    private boolean isTryAgainable(DownloadState state) {
        return state == DownloadState.STALLED;
    }

    private boolean isCancelable(DownloadState state) {
        return state != DownloadState.DONE;
    }
    
    private void cancelEditing(){
        Component comp = table.getEditorComponent();
        if (comp != null && comp instanceof TableCellEditor) {
            ((TableCellEditor) comp).cancelCellEditing();
        }
    }
   
    /**
     * An ActionListener for the menu items in the popup menu.  
     */
    private class MenuListener implements ActionListener {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            // Get download item and perform action.
            for (DownloadItem item : downloadItems) {
                actionHandler.performAction(e.getActionCommand(), item);
            }
            
            // must cancel editing
            cancelEditing();
        }
    }
    
    /**
     * An ActionListener for the menu items in the popup menu.  
     */
    private class PauseListener implements ActionListener {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            // Get download item and perform action.
            for (DownloadItem item : downloadItems) {
                if (isPausable(item.getState())) {
                    actionHandler.performAction(DownloadActionHandler.PAUSE_COMMAND, item);
                }
            }
            
            // must cancel editing
            cancelEditing();
        }
    }
    
    /**
     * An ActionListener for the menu items in the popup menu.  
     */
    private class CancelListener implements ActionListener {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            // Get download item and perform action.
            for (DownloadItem item : downloadItems) {
                if (isCancelable(item.getState())) {
                    actionHandler.performAction(DownloadActionHandler.CANCEL_COMMAND, item);
                }
            }
            
            // must cancel editing
            cancelEditing();
        }
    }
    
    /**
     * An ActionListener for the menu items in the popup menu.  
     */
    private class TryAgainListener implements ActionListener {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            // Get download item and perform action.
            for (DownloadItem item : downloadItems) {
                if (isTryAgainable(item.getState())) {
                    actionHandler.performAction(DownloadActionHandler.TRY_AGAIN_COMMAND, item);
                }
            }
            
            // must cancel editing
            cancelEditing();
        }
    }
    
    /**
     * An ActionListener for the menu items in the popup menu.  
     */
    private class ResumeListener implements ActionListener {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            // Get download item and perform action.
            for (DownloadItem item : downloadItems) {
                if (isResumable(item.getState())) {
                    actionHandler.performAction(DownloadActionHandler.RESUME_COMMAND, item);
                }
            }
            
            // must cancel editing
            cancelEditing();
        }
    }
    
   
}
