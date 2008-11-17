package org.limewire.ui.swing.library;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.library.image.LibraryImagePanel;
import org.limewire.ui.swing.library.sharing.AllFriendsList;
import org.limewire.ui.swing.library.sharing.CategoryShareModel;
import org.limewire.ui.swing.library.sharing.LibrarySharePanel;
import org.limewire.ui.swing.library.table.LibraryTable;
import org.limewire.ui.swing.library.table.LibraryTableFactory;
import org.limewire.ui.swing.library.table.LibraryTableModel;
import org.limewire.ui.swing.lists.CategoryFilter;
import org.limewire.ui.swing.player.PlayerUtils;
import org.limewire.ui.swing.table.TableDoubleClickHandler;
import org.limewire.ui.swing.table.MouseableTable.TableColors;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.util.NativeLaunchUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class SharingLibraryPanel extends LibraryPanel {
    private AllFriendsList allFriendsList;
    private ShareListManager shareListManager;
    private LibraryTableFactory tableFactory;
    private final CategoryIconManager categoryIconManager;
    private final BaseLibraryMediator basePanel;
    
    private LibrarySharePanel shareAllPanel = null;
    
    @AssistedInject
    public SharingLibraryPanel( @Assisted BaseLibraryMediator basePanel,
                                @Assisted Friend friend,
                                @Assisted EventList<LocalFileItem> eventList,
                                @Assisted LocalFileList friendFileList,
                                IconManager iconManager,
                                CategoryIconManager categoryIconManager,
                                LibraryTableFactory tableFactory,
                                AllFriendsList allFriendsList,
                                ShareListManager shareListManager) {
        super(friend, false);
        
        this.shareListManager = shareListManager;
        this.allFriendsList = allFriendsList;
        this.categoryIconManager = categoryIconManager;
        this.tableFactory = tableFactory;
        this.basePanel = basePanel;
        
        loadHeader();
        loadSelectionPanel();
        createMyCategories(eventList, friend, friendFileList);
    }
        
    @Override
    public void loadHeader() {
        headerPanel.setBackgroundPainter(null);
        headerPanel.setBackground(Color.pink.darker());
        
        shareAllPanel = new LibrarySharePanel(allFriendsList.getAllFriends());
        shareAllPanel.setShareModel(new CategoryShareModel(shareListManager));
        headerPanel.enableShareAll(shareAllPanel);
    }

    @Override
    public void loadSelectionPanel() {
        JButton showAll = new JButton(I18n.tr("Show All Files"));
        showAll.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                basePanel.showMainCard();
            }
        });
        
        selectionPanel.add(showAll, "gaptop 15, gapbottom 15, alignx 50%");
    }
    
    private Map<Category, JComponent> createMyCategories(EventList<LocalFileItem> eventList, Friend friend, LocalFileList friendFileList) {
        Map<Category, JComponent> categories = new LinkedHashMap<Category, JComponent>();
        for(Category category : Category.getCategoriesInOrder()) {
            FilterList<LocalFileItem> filteredAll = GlazedListsFactory.filterList(eventList, new CategoryFilter(category));
            FilterList<LocalFileItem> filteredShared = GlazedListsFactory.filterList(friendFileList.getSwingModel(), new CategoryFilter(category));
            createButton(categoryIconManager.getIcon(category), category,
                        createMyCategoryAction(category, filteredAll, friend, friendFileList), filteredAll, filteredShared);
        }
        return categories;
    }
    
    private JComponent createMyCategoryAction(Category category, EventList<LocalFileItem> filtered, Friend friend, final LocalFileList friendFileList) {
        EventList<LocalFileItem> filterList = GlazedListsFactory.filterList(filtered, 
                new TextComponentMatcherEditor<LocalFileItem>(getFilterTextField(), new LibraryTextFilterator<LocalFileItem>()));

        Comparator<LocalFileItem> c = new java.util.Comparator<LocalFileItem>() {
            @Override
            public int compare(LocalFileItem fileItem1, LocalFileItem fileItem2) {
                boolean containsF1 = friendFileList.contains(fileItem1.getFile());
                boolean containsF2 = friendFileList.contains(fileItem2.getFile());
                if(containsF1 && containsF2)
                    return 0;
                else if(containsF1 && !containsF2)
                    return -1;
                else
                    return 1;
            }
        };
        
        SortedList<LocalFileItem> sortedList = new SortedList<LocalFileItem>(filterList, c);

        JScrollPane scrollPane;
        
        if (category != Category.IMAGE) {
            LibraryTable table = tableFactory.createSharingTable(category, sortedList, friendFileList);
            table.setDoubleClickHandler(new MyLibraryDoubleClickHandler(getTableModel(table)));
            addDisposable(table);
            
            scrollPane = new JScrollPane(table);
            scrollPane.setColumnHeaderView(table.getTableHeader());
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            
            if (table.isColumnControlVisible()) {
                scrollPane.setCorner(JScrollPane.UPPER_TRAILING_CORNER, table.getColumnControl());
                scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            }
			TableColors tableColors = new TableColors();
            table.addHighlighter(new ColorHighlighter(new UnsharedHighlightPredicate(getTableModel(table), friendFileList), null, tableColors.getDisabledForegroundColor(), null, tableColors.getDisabledForegroundColor()));
        } else {//Category.IMAGE
            scrollPane = new JScrollPane();
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            LibraryImagePanel imagePanel = tableFactory.createSharingImagePanel(sortedList, scrollPane, friendFileList);
            addDisposable(imagePanel);
            
            scrollPane.setViewportView(imagePanel);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
        }
        return scrollPane;
    }
    
    @SuppressWarnings("unchecked")
    private LibraryTableModel<LocalFileItem> getTableModel(LibraryTable table){
        return (LibraryTableModel<LocalFileItem>)table.getModel();
    }   
    
    @Override
    public void dispose() {
        super.dispose();
        
        shareAllPanel.dispose();
    }
    
    private static class MyLibraryDoubleClickHandler implements TableDoubleClickHandler{
        private LibraryTableModel<LocalFileItem> model;

        public MyLibraryDoubleClickHandler(LibraryTableModel<LocalFileItem> model){
            this.model = model;
        }

        @Override
        public void handleDoubleClick(int row) {
            File file = model.getFileItem(row).getFile();
            switch (model.getFileItem(row).getCategory()){
            case AUDIO:
                if (PlayerUtils.isPlayableFile(file)){
                    PlayerUtils.play(file);
                } else {                
                    NativeLaunchUtils.launchFile(file);
                }
                break;
            case OTHER:
            case PROGRAM:
                NativeLaunchUtils.launchExplorer(file);
                break;
            case IMAGE:
                //TODO: image double click
            case VIDEO:
            case DOCUMENT:
                NativeLaunchUtils.launchFile(file);
            }
        }
    }
    
    private static class UnsharedHighlightPredicate implements HighlightPredicate {
        LibraryTableModel<LocalFileItem> libraryTableModel;
        private LocalFileList friendFileList;
        public UnsharedHighlightPredicate (LibraryTableModel<LocalFileItem> libraryTableModel, LocalFileList friendFileList) {
            this.libraryTableModel = libraryTableModel;
            this.friendFileList = friendFileList;
        }
        @Override
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            LocalFileItem fileItem = libraryTableModel.getFileItem(adapter.row);
            //TODO cache values?
            return !(friendFileList.contains(fileItem.getUrn()));
        }       
    }    
}
