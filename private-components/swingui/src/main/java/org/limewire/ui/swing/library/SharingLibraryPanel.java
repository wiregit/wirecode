package org.limewire.ui.swing.library;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

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
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.util.NativeLaunchUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class SharingLibraryPanel extends LibraryPanel {
    private AllFriendsList allFriendsList;
    private ShareListManager shareListManager;
    private LibraryTableFactory tableFactory;
    private final CategoryIconManager categoryIconManager;
    private final BaseLibraryMediator basePanel;
    
    private List<Disposable> buttonFilterList = new ArrayList<Disposable>();
    
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
        super(friend);
        
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
            JButton button = createButton(categoryIconManager.getIcon(category), category,
                        createMyCategoryAction(category, eventList, friend, friendFileList));
            
            FilterList<LocalFileItem> filtered = GlazedListsFactory.filterList(friendFileList.getSwingModel(), new CategoryFilter(category));
            buttonFilterList.add(new ButtonSizeListener(category.toString(), button.getAction(), filtered));
        }
        return categories;
    }
    
    private JComponent createMyCategoryAction(Category category, EventList<LocalFileItem> eventList, Friend friend, final LocalFileList friendFileList) {
        FilterList<LocalFileItem> filtered = GlazedListsFactory.filterList(eventList, new CategoryFilter(category));

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
        } else {//Category.IMAGE
            scrollPane = new JScrollPane();
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            LibraryImagePanel imagePanel = tableFactory.createImagePanel(sortedList, scrollPane);
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
        for(Disposable disposable : buttonFilterList)
            disposable.dispose();
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
    
    private class ButtonSizeListener implements Disposable, ListEventListener<LocalFileItem> {
        private final String text;
        private final Action action;
        private final FilterList<LocalFileItem> list;
        
        public ButtonSizeListener(String text, Action action, FilterList<LocalFileItem> list) {
            this.text = text;
            this.action = action;
            this.list = list;
            
            setText();
                        
            list.addListEventListener(this);
        }

        private void setText() {
            action.putValue(Action.NAME, text + " (" + list.size() + ")");
        }
        
        @Override
        public void dispose() {
            list.removeListEventListener(this);
            list.dispose();
        }

        @Override
        public void listChanged(ListEvent<LocalFileItem> listChanges) {
            setText();
        }
    }
}
