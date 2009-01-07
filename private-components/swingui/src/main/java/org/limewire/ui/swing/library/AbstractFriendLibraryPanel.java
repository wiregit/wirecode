package org.limewire.ui.swing.library;

import java.awt.Component;
import java.util.TooManyListenersException;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.FriendFileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.components.LimeHeaderBarFactory;
import org.limewire.ui.swing.dnd.GhostDragGlassPane;
import org.limewire.ui.swing.dnd.GhostDropTargetListener;
import org.limewire.ui.swing.dnd.LocalFileListTransferHandler;
import org.limewire.ui.swing.library.table.LibraryTable;
import org.limewire.ui.swing.library.table.LibraryTableFactory;
import org.limewire.ui.swing.library.table.LibraryTableModel;
import org.limewire.ui.swing.lists.CategoryFilter;
import org.limewire.ui.swing.table.TableColors;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

abstract class AbstractFriendLibraryPanel extends LibraryPanel {

    private final CategoryIconManager categoryIconManager;
    private final LibraryTableFactory tableFactory;
    private final DownloadListManager downloadListManager;
    private final LibraryManager libraryManager;
    private final Friend friend;
    
    public AbstractFriendLibraryPanel(Friend friend,
                    FriendFileList friendFileList,
                    EventList<RemoteFileItem> eventList, 
                    CategoryIconManager categoryIconManager, 
                    LibraryTableFactory tableFactory,
                    DownloadListManager downloadListManager,
                    LibraryManager libraryManager,
                    LimeHeaderBarFactory headerBarFactory,
                    GhostDragGlassPane ghostPane) {        
        super(headerBarFactory);
        this.friend = friend;
        this.categoryIconManager = categoryIconManager;
        this.tableFactory = tableFactory;
        this.downloadListManager = downloadListManager;
        this.libraryManager = libraryManager;
        
       //All friends panel gives a null friend
        if(friend != null) {
            setTransferHandler(new LocalFileListTransferHandler(friendFileList));
            
            try {
                getDropTarget().addDropTargetListener(new GhostDropTargetListener(this,ghostPane));
            } catch (TooManyListenersException ignoreException) {            
            }  
        }
    }    
    
    protected void createMyCategories(EventList<RemoteFileItem> eventList) {
        for(Category category : Category.getCategoriesInOrder()) {
            FilterList<RemoteFileItem> filtered = GlazedListsFactory.filterList(eventList, new CategoryFilter(category));
            JComponent component = createMyCategoryAction(category, filtered);
            if(component != null) {
                addCategory(categoryIconManager.getIcon(category), category, component, filtered, null);                
            }
            addDisposable(filtered);
        }
    }
    
    protected JComponent createMyCategoryAction(Category category, EventList<RemoteFileItem> filtered) {
        FilterList<RemoteFileItem> filterList = GlazedListsFactory.filterList(filtered, 
                new TextComponentMatcherEditor<RemoteFileItem>(getFilterTextField(), new LibraryTextFilterator<RemoteFileItem>()));
        addDisposable(filterList);
        
        LibraryTable table = tableFactory.createFriendTable(category, filterList, friend);
        table.enableDownloading(downloadListManager);
        addDisposable(table);
        
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setColumnHeaderView(table.getTableHeader());
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        
        if (table.isColumnControlVisible()) {
            scrollPane.setCorner(JScrollPane.UPPER_TRAILING_CORNER, table.getColumnControl());
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        }
        TableColors tableColors = new TableColors();
        table.addHighlighter(new ColorHighlighter(new AlreadyDownloadedHighlightPredicate(getTableModel(table), libraryManager.getLibraryManagedList(), downloadListManager), 
                    null, tableColors.getDisabledForegroundColor(), null, tableColors.getDisabledForegroundColor()));
        
        return scrollPane;
    }
    
    @SuppressWarnings("unchecked")
    private LibraryTableModel<RemoteFileItem> getTableModel(LibraryTable table){
        return (LibraryTableModel<RemoteFileItem>)table.getModel();
    }   
    
    @Override
    protected <T extends FileItem> void addCategorySizeListener(Category category,
            Action action, FilterList<T> filteredAllFileList, FilterList<T> filteredList) {
        // Comment this out & return null if you don't want sizes added to library panels.
        ButtonSizeListener<T> listener = new ButtonSizeListener<T>(category, action, filteredList);
        filteredList.addListEventListener(listener);
        addDisposable(listener);
    }
    
    /**
     * Hide any category that has no files in it.
     */
    private class ButtonSizeListener<T> implements Disposable, ListEventListener<T>, SettingListener {
        private final Category category;
        private final Action action;
        private final FilterList<T> list;
        
        private ButtonSizeListener(Category category, Action action, FilterList<T> list) {
            this.category = category;
            this.action = action;
            this.list = list;
            action.putValue(Action.NAME, I18n.tr(category.toString()));
            setText();
            if(category == Category.PROGRAM) {
                LibrarySettings.ALLOW_PROGRAMS.addSettingListener(this);
            }
        }

        private void setText() {
            if(category == Category.PROGRAM) { // hide program category is not enabled
                action.setEnabled(LibrarySettings.ALLOW_PROGRAMS.getValue());
            }
            //disable any category if size is 0
            action.setEnabled(list.size() > 0);
        }
        
        @Override
        public void dispose() {
            list.removeListEventListener(this);
            if(category == Category.PROGRAM) {
                LibrarySettings.ALLOW_PROGRAMS.removeSettingListener(this);
            }
        }

        @Override
        public void listChanged(ListEvent<T> listChanges) {
            setText();
        }

        @Override
        public void settingChanged(SettingEvent evt) {
            setText();
        }
    }    
    
    /**
     * Grays out a table row if a file is already downloading or
     * if the file already exists in your library.
     */
    private static class AlreadyDownloadedHighlightPredicate implements HighlightPredicate {
        private final LibraryTableModel<RemoteFileItem> libraryTableModel;
        private final LocalFileList myLibraryList;
        private final DownloadListManager downloadListManager;
        
        public AlreadyDownloadedHighlightPredicate (LibraryTableModel<RemoteFileItem> libraryTableModel, LocalFileList myLibraryList, DownloadListManager downloadListManager) {
            this.libraryTableModel = libraryTableModel;
            this.myLibraryList = myLibraryList;
            this.downloadListManager = downloadListManager;
        }
        @Override
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            RemoteFileItem fileItem = libraryTableModel.getFileItem(adapter.row);
            //TODO cache values?
            return myLibraryList.contains(fileItem.getUrn()) || downloadListManager.contains(fileItem.getUrn());
        }       
    }
}