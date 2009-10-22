package org.limewire.ui.swing.upload;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Comparator;
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
import org.limewire.core.api.Category;
import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.api.upload.UploadListManager;
import org.limewire.core.api.upload.UploadState;
import org.limewire.core.api.upload.UploadItem.UploadItemType;
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
import org.limewire.util.FileUtils;
import org.limewire.util.Objects;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.matchers.Matcher;

import com.google.inject.Inject;

/**
 * Mediator to control the interaction between the uploads table and various
 * services.
 */
@LazySingleton
public class UploadMediator implements NavMediator<JComponent> {
    public enum SortOrder {
        ORDER_STARTED, NAME, PROGRESS, TIME_REMAINING, SPEED, STATUS, 
        FILE_TYPE, FILE_EXTENSION
    }
    
    public static final String NAME = "UploadPanel";
    
    private final UploadListManager uploadListManager;
    private final UploadTableFactory uploadTableFactory;
    
    private EventList<UploadItem> activeList;
    private SortedList<UploadItem> sortedList;
    
    private boolean sortAscending;
    private SortOrder sortOrder;
    
    private JPanel uploadPanel;
    
    private JButton clearFinishedButton;
    private List<JButton> headerButtons;
    private JPopupMenu headerPopupMenu;
    
    @Inject
    public UploadMediator(UploadListManager uploadListManager,
            UploadTableFactory uploadTableFactory) {
        this.uploadListManager = uploadListManager;
        this.uploadTableFactory = uploadTableFactory;
        
        sortOrder = SortOrder.ORDER_STARTED;
        sortAscending = true;
        
        sortedList = GlazedListsFactory.sortedList(uploadListManager.getSwingThreadSafeUploads(),
                getSortComparator(sortOrder, sortAscending));
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
            activeList = GlazedListsFactory.filterList(
                    uploadListManager.getSwingThreadSafeUploads(), 
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
     * Returns a sorted list of uploads.
     */
    public EventList<UploadItem> getUploadList() {
        return sortedList;
    }
    
    /**
     * Returns true if the uploads list is sorted in ascending order.
     */
    public boolean isSortAscending() {
        return sortAscending;
    }
    
    /**
     * Returns the sort key for the uploads list.
     */
    public SortOrder getSortOrder() {
        return sortOrder;
    }
    
    /**
     * Sets the sort key and direction on the uploads list.
     */
    public void setSortOrder(SortOrder sortOrder, boolean ascending) {
        this.sortOrder = sortOrder;
        this.sortAscending = ascending;
        
        // Apply sort order.
        sortedList.setComparator(getSortComparator(sortOrder, ascending));
    }
    
    /**
     * Returns a comparator for the specified sort key and direction.
     */
    private Comparator<UploadItem> getSortComparator(SortOrder sortOrder, boolean ascending) {
        switch (sortOrder) {
        case ORDER_STARTED:
            return new OrderStartedComparator(ascending);
        case NAME:
            return new NameComparator(ascending);
        case PROGRESS:
            return new ProgressComparator(ascending);
        case TIME_REMAINING:
            return new TimeRemainingComparator(ascending);
        case SPEED:
            return new SpeedComparator(ascending);
        case STATUS:
            return new StateComparator(ascending);
        case FILE_TYPE:
            return new CategoryComparator(ascending);
        case FILE_EXTENSION:
            return new FileExtensionComparator(ascending);
        default:
            throw new IllegalArgumentException("Unknown SortOrder: " + sortOrder);
        }
    }
    
    /**
     * Returns true if any uploads may be paused.
     */
    public boolean hasPausable() {
        EventList<UploadItem> uploadList = getUploadList();
        uploadList.getReadWriteLock().writeLock().lock();
        try {
            for (UploadItem item : uploadList) {
                if (isPausable(item)) return true;
            }
        } finally {
            uploadList.getReadWriteLock().writeLock().unlock();
        }
        return false;
    }
    
    /**
     * Returns true if any uploads may be resumed.
     */
    public boolean hasResumable() {
        EventList<UploadItem> uploadList = getUploadList();
        uploadList.getReadWriteLock().writeLock().lock();
        try {
            for (UploadItem item : uploadList) {
                if (isResumable(item)) return true;
            }
        } finally {
            uploadList.getReadWriteLock().writeLock().unlock();
        }
        return false;
    }
    
    /**
     * Returns true if any uploads are in the specified state.
     */
    public boolean hasState(UploadState state) {
        EventList<UploadItem> uploadList = getUploadList();
        uploadList.getReadWriteLock().writeLock().lock();
        try {
            for (UploadItem item : uploadList) {
                if (item.getState() == state) return true;
            }
        } finally {
            uploadList.getReadWriteLock().writeLock().unlock();
        }
        return false;
    }
    
    /**
     * Returns true if any uploads are torrents.
     */
    public boolean hasTorrents() {
        EventList<UploadItem> uploadList = getUploadList();
        uploadList.getReadWriteLock().writeLock().lock();
        try {
            for (UploadItem item : uploadList) {
                if (item.getUploadItemType() == UploadItemType.BITTORRENT) return true;
            }
        } finally {
            uploadList.getReadWriteLock().writeLock().unlock();
        }
        return false;
    }
    
    /**
     * Returns true if the specified upload item is a browse item.
     */
    public static boolean isBrowseHost(UploadItem uploadItem) {
        UploadState state = uploadItem.getState();
        return (state == UploadState.BROWSE_HOST) || (state == UploadState.BROWSE_HOST_DONE);
    }
    
    /**
     * Returns true if the specified upload item may be paused.
     */
    public static boolean isPausable(UploadItem uploadItem) {
        return (uploadItem.getUploadItemType() == UploadItemType.BITTORRENT) &&
            (uploadItem.getState() == UploadState.UPLOADING);
    }
    
    /**
     * Returns true if the specified upload item may be resumed.
     */
    public static boolean isResumable(UploadItem uploadItem) {
        return (uploadItem.getUploadItemType() == UploadItemType.BITTORRENT) &&
            (uploadItem.getState() == UploadState.PAUSED);
    }
    
    /**
     * Returns true if the specified upload item may be removed.
     */
    public static boolean isRemovable(UploadItem uploadItem) {
        UploadState state = uploadItem.getState();
        return (state == UploadState.DONE) || (state == UploadState.BROWSE_HOST_DONE) ||
            (state == UploadState.CANCELED) || (state == UploadState.UNABLE_TO_UPLOAD);
    }
    
    /**
     * Cancels all uploads.
     */
    public void cancelAll() {
        List<UploadItem> uploadList = new ArrayList<UploadItem>(getUploadList());
        for (UploadItem item : uploadList) {
            item.cancel();
        }
    }
    
    /**
     * Cancels all uploads in an error state.
     */
    public void cancelAllError() {
        List<UploadItem> uploadList = new ArrayList<UploadItem>(getUploadList());
        for (UploadItem item : uploadList) {
            if (item.getState() == UploadState.UNABLE_TO_UPLOAD) item.cancel();
        }
    }
    
    /**
     * Cancels all torrent uploads.
     */
    public void cancelAllTorrents() {
        List<UploadItem> uploadList = new ArrayList<UploadItem>(getUploadList());
        for (UploadItem item : uploadList) {
            if (item.getUploadItemType() == UploadItemType.BITTORRENT) item.cancel();
        }
    }
    
    /**
     * Clears all finished uploads.
     */
    private void clearFinished() {
        uploadListManager.clearFinished();
    }
    
    /**
     * Pauses all uploads that can be paused.
     */
    public void pauseAll() {
        EventList<UploadItem> uploadList = getUploadList();
        uploadList.getReadWriteLock().writeLock().lock();
        try {
            for (UploadItem item : uploadList) {
                if (isPausable(item)) item.pause();
            }
        } finally {
            uploadList.getReadWriteLock().writeLock().unlock();
        }
    }
    
    /**
     * Resumes all uploads that can be resumed.
     */
    public void resumeAll() {
        EventList<UploadItem> uploadList = getUploadList();
        uploadList.getReadWriteLock().writeLock().lock();
        try {
            for (UploadItem item : uploadList) {
                if (isResumable(item)) item.resume();
            }
        } finally {
            uploadList.getReadWriteLock().writeLock().unlock();
        }
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
    
    private static class OrderStartedComparator implements Comparator<UploadItem> {
        private final boolean ascending;
        
        public OrderStartedComparator(boolean ascending) {
            this.ascending = ascending;
        }
        
        @Override
        public int compare(UploadItem o1, UploadItem o2) { 
            if (o1 == o2) return 0;
            return (ascending ? 1 : -1) * (int) (o1.getStartTime() - o2.getStartTime());
        }      
    }

    private static class NameComparator implements Comparator<UploadItem> {
        private final boolean ascending;
        
        public NameComparator(boolean ascending) {
            this.ascending = ascending;
        }
        
        @Override
        public int compare(UploadItem o1, UploadItem o2) {
            if (o1 == o2) return 0;
            String name1 = o1.getFileName();
            String name2 = o2.getFileName();
            return (ascending ? 1 : -1) * Objects.compareToNullIgnoreCase(name1, name2, false);
        }
    }

    private static class ProgressComparator implements Comparator<UploadItem> {
        private final boolean ascending;
        
        public ProgressComparator(boolean ascending) {
            this.ascending = ascending;
        }
        
        @Override
        public int compare(UploadItem o1, UploadItem o2) {
            if (o1 == o2) return 0;
            int pct1 = getProgressPct(o1);
            int pct2 = getProgressPct(o2);
            return (ascending ? 1 : -1) * (pct1 - pct2);
        }
        
        private int getProgressPct(UploadItem item) {
            return (int) (100 * item.getTotalAmountUploaded() / item.getFileSize());
        }
    }
    
    private static class TimeRemainingComparator implements Comparator<UploadItem> {
        private final boolean ascending;
        
        public TimeRemainingComparator(boolean ascending) {
            this.ascending = ascending;
        }
        
        @Override
        public int compare(UploadItem o1, UploadItem o2) {
            if (o1 == o2) return 0;
            long time1 = o1.getRemainingUploadTime();
            long time2 = o2.getRemainingUploadTime();
            return (ascending ? 1 : -1) * (int) (time1 - time2);
        }
    }

    private static class SpeedComparator implements Comparator<UploadItem> {
        private final boolean ascending;
        
        public SpeedComparator(boolean ascending) {
            this.ascending = ascending;
        }
        
        @Override
        public int compare(UploadItem o1, UploadItem o2) {
            if (o1 == o2) return 0;
            float speed1 = o1.getUploadSpeed();
            float speed2 = o2.getUploadSpeed();
            return (ascending ? 1 : -1) * (int) (speed1 - speed2);
        }
    }

    private static class StateComparator implements Comparator<UploadItem> {
        private final boolean ascending;
        
        public StateComparator(boolean ascending) {
            this.ascending = ascending;
        }
        
        @Override
        public int compare(UploadItem o1, UploadItem o2) {
            if (o1 == o2) return 0;
            int value1 = getSortValue(o1.getState());
            int value2 = getSortValue(o2.getState());
            return (ascending ? 1 : -1) * (value1 - value2);
        }
        
        private int getSortValue(UploadState state) {
            switch (state) {
            case DONE: return 1;
            case UPLOADING: return 2;
            case PAUSED: return 3;
            case QUEUED: return 4;
            case UNABLE_TO_UPLOAD: return 5;       
            case CANCELED: return 6;
            case BROWSE_HOST: return 7;
            case BROWSE_HOST_DONE: return 8;
            default:
                throw new IllegalArgumentException("Unknown UploadState: " + state);
            }
        }
    }
    
    private static class CategoryComparator implements Comparator<UploadItem> {
        private final boolean ascending;
        
        public CategoryComparator(boolean ascending) {
            this.ascending = ascending;
        }
        
        @Override
        public int compare(UploadItem o1, UploadItem o2) {
            if (o1 == o2) return 0;
            Category cat1 = o1.getCategory();
            Category cat2 = o2.getCategory();
            return (ascending ? 1 : -1) * cat1.compareTo(cat2);
        }
    }

    private static class FileExtensionComparator implements Comparator<UploadItem> {
        private final boolean ascending;
        
        public FileExtensionComparator(boolean ascending) {
            this.ascending = ascending;
        }
        
        @Override
        public int compare(UploadItem o1, UploadItem o2) {
            if (o1 == o2) return 0;
            
            String name1 = o1.getFileName();
            String name2 = o2.getFileName();
            if (name1 == null) {
                return (name2 == null) ? 0 : (ascending ? -1 : 1);
            } else if (name2 == null) {
                return (ascending ? 1 : -1);
            }
            
            String ext1 = FileUtils.getFileExtension(name1);
            String ext2 = FileUtils.getFileExtension(name2);
            return (ascending ? 1 : -1) * Objects.compareToNullIgnoreCase(ext1, ext2, false);
        }
    }
}
