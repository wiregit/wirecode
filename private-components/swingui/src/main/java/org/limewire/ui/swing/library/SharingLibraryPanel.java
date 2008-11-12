package org.limewire.ui.swing.library;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.image.LibraryImagePanel;
import org.limewire.ui.swing.library.table.LibraryTable;
import org.limewire.ui.swing.library.table.LibraryTableFactory;
import org.limewire.ui.swing.library.table.LibraryTableModel;
import org.limewire.ui.swing.lists.CategoryFilter;
import org.limewire.ui.swing.player.PlayerUtils;
import org.limewire.ui.swing.table.TableDoubleClickHandler;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.util.NativeLaunchUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class SharingLibraryPanel extends JPanel implements Disposable {

    @Resource private Color selectedBackground;
    @Resource private Font selectedTextFont;
    @Resource private Color selectedTextColor;
    @Resource private Font textFont;
    @Resource private Color textColor;
    
    private final LibraryHeaderPanel header;
    
    private CardLayout cardLayout = new CardLayout();
    private JPanel cardPanel = new JPanel();
    private JPanel selectionPanel = new JPanel();
    
    private LibraryTableFactory tableFactory;
    private final CategoryIconManager categoryIconManager;
    private final BaseLibraryPanel basePanel;
    
    private List<FilterList<LocalFileItem>> lists = new ArrayList<FilterList<LocalFileItem>>();
    private List<Disposable> disposableList = new ArrayList<Disposable>();
    
    private SelectionButton currentSelected = null;
    
    @AssistedInject
    public SharingLibraryPanel( @Assisted BaseLibraryPanel basePanel,
                                @Assisted Friend friend,
                                @Assisted EventList<LocalFileItem> eventList,
                                @Assisted LocalFileList friendFileList,
                                IconManager iconManager,
                                CategoryIconManager categoryIconManager,
                                LibraryTableFactory tableFactory,
                                ShareListManager shareListManager) {
        this.categoryIconManager = categoryIconManager;
        this.tableFactory = tableFactory;
        this.basePanel = basePanel;
        
        GuiUtils.assignResources(this);
        
        Category category = Category.AUDIO;
        header = new LibraryHeaderPanel(category, friend);
        header.setFriend(friend);
        header.setBackgroundPainter(null);
        header.setBackground(Color.pink.darker());
        
        createSelectionPanel();
        createMyCategories(eventList, friend, friendFileList);
        
        setLayout(new MigLayout("fill, gap 0, insets 0 0 0 0", "[120!][]", "[][]"));
        
        add(header, "span, growx, wrap");
        add(selectionPanel, "growy");
        add(cardPanel, "grow");
    }
    
    private void createSelectionPanel() {
        selectionPanel.setLayout(new MigLayout("insets 0, gap 0, fillx, wrap", "[120!]", ""));
        
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
        cardPanel.setLayout(cardLayout);
        
        Map<Category, JComponent> categories = new LinkedHashMap<Category, JComponent>();
        for(Category category : Category.getCategoriesInOrder()) {
            JComponent component = createMyCategoryAction(category, eventList, friend, friendFileList);
            cardPanel.add(component, category.toString());
            Icon icon = categoryIconManager.getIcon(category);
            
            SelectionButton button = new SelectionButton(new SelectionAction(icon, category, cardLayout, friend));
            selectionPanel.add(button, "growx");
            
            if(currentSelected == null) {
                currentSelected  = button;
                currentSelected.select(true);
            }
        }
        return categories;
    }
    
    private JComponent createMyCategoryAction(Category category, EventList<LocalFileItem> eventList, Friend friend, final LocalFileList friendFileList) {
        FilterList<LocalFileItem> filtered = GlazedListsFactory.filterList(eventList, new CategoryFilter(category));
        lists.add(filtered);
        
        
        EventList<LocalFileItem> filterList = GlazedListsFactory.filterList(filtered, 
                new TextComponentMatcherEditor<LocalFileItem>(header.getFilterTextField(), new LibraryTextFilterator<LocalFileItem>()));

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
            LibraryTable table = tableFactory.createSharingTable(category, sortedList, friend, friendFileList);
            table.setDoubleClickHandler(new MyLibraryDoubleClickHandler(getTableModel(table)));
            disposableList.add(table);
            
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
            disposableList.add(imagePanel);
            
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
        for(FilterList<LocalFileItem> item : lists) 
            item.dispose();
        for(Disposable disposable : disposableList)
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
    
    private class SelectionAction extends AbstractAction {
        private Category category;
        private CardLayout cardLayout;
        private Friend friend;
        
        public SelectionAction(Icon icon, Category category, CardLayout cardLayout, Friend friend) {
            super(category.toString(), icon);
            
            this.category = category;
            this.cardLayout = cardLayout;
            this.friend = friend;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            cardLayout.show(cardPanel, category.toString());
            header.setCategory(category, friend);
        }
    }
    
    private class SelectionButton extends JButton {
        public SelectionButton(AbstractAction action) {
            super(action);

            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setHorizontalAlignment(SwingConstants.LEFT);
            
            addMouseListener(new MouseListener(){
                @Override
                public void mouseClicked(MouseEvent e) {}
                @Override
                public void mouseEntered(MouseEvent e) {}
                @Override
                public void mouseExited(MouseEvent e) {}
                @Override
                public void mouseReleased(MouseEvent e) {}
                @Override
                public void mousePressed(MouseEvent e) {
                    select(true);
                }                
            });
        }
        
        public void select(boolean selected) {
            if(currentSelected != null && currentSelected != this) {
                currentSelected.select(false);
            }
            currentSelected = this;
            if(selected) {
                setBackground(selectedBackground);
                setForeground(selectedTextColor);
                setFont(selectedTextFont);
                setOpaque(true);
            } else {
                setOpaque(false);
                setForeground(textColor);
                setFont(textFont);
            }
        }
    }
}
