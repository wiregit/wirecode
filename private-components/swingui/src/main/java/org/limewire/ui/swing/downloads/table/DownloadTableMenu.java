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
            add(createPauseMenuItem());
            addSeparator();
        }

        switch (state) {
        case DONE:
            if (downloadItem.getCategory() != Category.PROGRAM
                    && downloadItem.getCategory() != Category.OTHER) {
                if (downloadItem.getCategory() == Category.AUDIO
                        || downloadItem.getCategory() == Category.VIDEO) {
                    add(createPlayMenuItem()).setEnabled(downloadItem.isLaunchable());
                } else if (downloadItem.getCategory() == Category.IMAGE
                        || downloadItem.getCategory() == Category.DOCUMENT) {
                    add(createViewMenuItem()).setEnabled(downloadItem.isLaunchable());
                } else {
                    add(createLaunchMenuItem()).setEnabled(downloadItem.isLaunchable());
                }
            }
            add(createShareMenuItem());
            addSeparator();
            add(createLocateMenuItem());
            add(createLibraryMenuItem());
            addSeparator();
            add(createRemoveMenuItem());
            break;

        case TRYING_AGAIN:
        case CONNECTING:
        case FINISHING:
        case LOCAL_QUEUED:
        case REMOTE_QUEUED:
            if (downloadItem.getCategory() != Category.PROGRAM
                    && downloadItem.getCategory() != Category.OTHER) {
                add(createPreviewMenuItem()).setEnabled(downloadItem.isLaunchable());
            }
            add(createLibraryMenuItem());
            add(new JSeparator());
            add(createCancelMenuItem());
            break;
        case ERROR:
            add(createCancelWithRemoveNameMenuItem());
            add(new JSeparator());
            if (downloadItem.getCategory() != Category.PROGRAM
                    && downloadItem.getCategory() != Category.OTHER) {
                add(createPreviewMenuItem()).setEnabled(downloadItem.isLaunchable());
            }
            add(createLibraryMenuItem());
            break;
        case RESUMING:
        case DOWNLOADING:
            if (downloadItem.getCategory() != Category.PROGRAM
                    && downloadItem.getCategory() != Category.OTHER) {
                add(createPreviewMenuItem()).setEnabled(downloadItem.isLaunchable());
            }
            add(createLibraryMenuItem());
            add(new JSeparator());
            add(createCancelMenuItem());
            break;

        case PAUSED:
            add(createResumeMenuItem());
            addSeparator();
            if (downloadItem.getCategory() != Category.PROGRAM
                    && downloadItem.getCategory() != Category.OTHER) {
                add(createPreviewMenuItem()).setEnabled(downloadItem.isLaunchable());
            }
            add(createLibraryMenuItem());
            add(new JSeparator());

            add(createCancelMenuItem());
            break;

        case STALLED:
            add(createTryAgainMenuItem());
            addSeparator();
            if (downloadItem.getCategory() != Category.PROGRAM
                    && downloadItem.getCategory() != Category.OTHER) {
                add(createPreviewMenuItem()).setEnabled(downloadItem.isLaunchable());
            }
            add(createLibraryMenuItem());
            add(new JSeparator());
            add(createCancelMenuItem());
            break;
        }
        
        if (state == DownloadState.LOCAL_QUEUED){
            add(new JSeparator());
            add(createRaisePriorityMenuItem());
            add(createLowerPriorityMenuItem());
        }

        add(new JSeparator());
        add(createPropertiesMenuItem());
    
    }
    
    private void initializeMultiItemMenu(List<DownloadItem> downloadItems) {
        boolean hasTryAgain = false;
        boolean hasPause = false;
        boolean hasCancel = false;
        boolean hasResume = false;
        
        //Check which menu items to include.  Items are included if they are valid
        //for any item in the list.
        for(DownloadItem item : downloadItems){
            if (hasTryAgain && hasPause && hasCancel && hasResume){
                //if all four booleans are true, we are done checking
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
            add(createPauseMenuItem());
        }
        if (hasResume){
            add(createResumeMenuItem());
        }
        if (hasTryAgain){
            add(createTryAgainMenuItem());
        }
        if (hasCancel){
            add(createCancelMenuItem());
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
    
    private JMenuItem createPauseMenuItem(){
        JMenuItem pauseMenuItem = new JMenuItem(I18n.tr("Pause"));
        pauseMenuItem.setActionCommand(DownloadActionHandler.PAUSE_COMMAND);
        pauseMenuItem.addActionListener(new PauseListener());
        return pauseMenuItem;
    }   
    
    private JMenuItem createCancelMenuItem(){
        JMenuItem cancelMenuItem = new JMenuItem(I18n.tr("Cancel Download"));
        cancelMenuItem.setActionCommand(DownloadActionHandler.CANCEL_COMMAND);
        cancelMenuItem.addActionListener(new CancelListener());
        return cancelMenuItem;
    }  
    
    private JMenuItem createResumeMenuItem(){
        JMenuItem resumeMenuItem = new JMenuItem(I18n.tr("Resume"));
        resumeMenuItem.setActionCommand(DownloadActionHandler.RESUME_COMMAND);
        resumeMenuItem.addActionListener(new ResumeListener());
        return resumeMenuItem;
    }   
    
    private JMenuItem createTryAgainMenuItem(){
        JMenuItem tryAgainMenuItem = new JMenuItem(I18n.tr("Try Again"));
        tryAgainMenuItem.setActionCommand(DownloadActionHandler.TRY_AGAIN_COMMAND);
        tryAgainMenuItem.addActionListener(new TryAgainListener());
        return tryAgainMenuItem;
    }   
    
    private JMenuItem createLaunchMenuItem(){
        JMenuItem launchMenuItem = new JMenuItem(I18n.tr("Launch File"));
        launchMenuItem.setActionCommand(DownloadActionHandler.LAUNCH_COMMAND);
        launchMenuItem.addActionListener(menuListener);
        return launchMenuItem;
    }  
    
    private JMenuItem createRemoveMenuItem(){
        JMenuItem removeMenuItem = new JMenuItem(I18n.tr("Remove from List"));
        removeMenuItem.setActionCommand(DownloadActionHandler.REMOVE_COMMAND);
        removeMenuItem.addActionListener(menuListener);
        return removeMenuItem;
    }   
    
    private JMenuItem createCancelWithRemoveNameMenuItem(){
        JMenuItem cancelWithRemoveNameMenuItem = new JMenuItem(I18n.tr("Remove from List"));
        cancelWithRemoveNameMenuItem.setActionCommand(DownloadActionHandler.CANCEL_COMMAND);
        cancelWithRemoveNameMenuItem.addActionListener(menuListener);
        return cancelWithRemoveNameMenuItem;
    } 
    
    private JMenuItem createLocateMenuItem(){
        JMenuItem locateMenuItem = new JMenuItem(I18n.tr("Locate on Disk"));
        locateMenuItem.setActionCommand(DownloadActionHandler.LOCATE_COMMAND);
        locateMenuItem.addActionListener(menuListener);
        return locateMenuItem;
    } 
    
    private JMenuItem createLibraryMenuItem(){
        JMenuItem libraryMenuItem = new JMenuItem(I18n.tr("Locate in Library"));
        libraryMenuItem.setActionCommand(DownloadActionHandler.LIBRARY_COMMAND);
        libraryMenuItem.addActionListener(menuListener);
        return libraryMenuItem;
    }   
    
    private JMenuItem createPropertiesMenuItem(){
        JMenuItem propertiesMenuItem = new JMenuItem(I18n.tr("View File Info..."));
        propertiesMenuItem.setActionCommand(DownloadActionHandler.PROPERTIES_COMMAND);
        propertiesMenuItem.addActionListener(menuListener);
        return propertiesMenuItem;
    }  
    
    private JMenuItem createShareMenuItem(){
        JMenuItem shareMenuItem = new JMenuItem(I18n.tr("Share File"));
        shareMenuItem.setActionCommand(DownloadActionHandler.SHARE_COMMAND);
        shareMenuItem.addActionListener(menuListener);
        return shareMenuItem;
    }  
    
    private JMenuItem createPlayMenuItem(){
        JMenuItem playMenuItem = new JMenuItem(I18n.tr("Play"));
        playMenuItem.setActionCommand(DownloadActionHandler.PLAY_COMMAND);
        playMenuItem.addActionListener(menuListener);
        return playMenuItem;
    }  
    
    private JMenuItem createViewMenuItem(){
        JMenuItem viewMenuItem = new JMenuItem(I18n.tr("View"));
        viewMenuItem.setActionCommand(DownloadActionHandler.LAUNCH_COMMAND);
        viewMenuItem.addActionListener(menuListener);  
        return viewMenuItem;
    }  
    
    private JMenuItem createRaisePriorityMenuItem(){
        JMenuItem raisePriorityItem = new JMenuItem(I18n.tr("Raise Priority"));
        raisePriorityItem.addActionListener(menuListener);       

        return raisePriorityItem;
    }  
    
    private JMenuItem createLowerPriorityMenuItem() {
        JMenuItem lowerPriorityItem = new JMenuItem(I18n.tr("LowerPriority"));
        lowerPriorityItem.addActionListener(menuListener);
        return lowerPriorityItem;
    }

    private JMenuItem createPreviewMenuItem() {
        JMenuItem previewMenuItem = new JMenuItem(I18n.tr("Preview File"));
        previewMenuItem.setActionCommand(DownloadActionHandler.PREVIEW_COMMAND);
        previewMenuItem.addActionListener(menuListener);
        return previewMenuItem;
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
