package org.limewire.ui.swing.library;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
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
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.table.LibraryTable;
import org.limewire.ui.swing.library.table.LibraryTableFactory;
import org.limewire.ui.swing.lists.CategoryFilter;
import org.limewire.ui.swing.nav.NavComponent;
import org.limewire.ui.swing.nav.NavSelectable;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

class FriendLibraryPanel extends BaseLibraryPanel implements Disposable, NavComponent {
   
    @Resource private Color selectedBackground;
    @Resource private Font selectedTextFont;
    @Resource private Color selectedTextColor;
    @Resource private Font textFont;
    @Resource private Color textColor;
    
    private LibrarySelectable librarySelectable;
    
    private CardLayout cardLayout = new CardLayout();
    private JPanel cardPanel = new JPanel();
    
    private LibraryTableFactory tableFactory;
    private final CategoryIconManager categoryIconManager;
    private DownloadListManager downloadListManager;
    
    private final Friend friend;
    
    private FriendEmptyLibrary noLibraryPanel;
    private MainPanel mainPanel;
    
    private List<FilterList<RemoteFileItem>> lists = new ArrayList<FilterList<RemoteFileItem>>();
    private List<Disposable> disposableList = new ArrayList<Disposable>();
    
    @AssistedInject
    public FriendLibraryPanel(@Assisted Friend friends,
                              CategoryIconManager categoryIconManager,
                              DownloadListManager downloadListManager,
                              RemoteLibraryManager remoteLibraryManager,
                              LibraryTableFactory tableFactory) {
        this.friend = friends;
        
        this.tableFactory = tableFactory;
        this.categoryIconManager = categoryIconManager;
        this.downloadListManager = downloadListManager;
        
        GuiUtils.assignResources(this);

        noLibraryPanel = new FriendEmptyLibrary(friend);
        setMainCard(noLibraryPanel);
    }
    
    public void createLibraryPanel(EventList<RemoteFileItem> eventList) {
        mainPanel = new MainPanel(eventList);      
        setAuxCard(mainPanel);
        showAuxCard();
    }
    
    @Override
    public void select(NavSelectable selectable) {
        librarySelectable.selectAndScroll(selectable.getNavSelectionId());
    }
       
    public void dispose() {
        for(FilterList<RemoteFileItem> item : lists)
            item.dispose();
        for(Disposable disposable : disposableList)
            disposable.dispose();
        if(mainPanel != null)
            mainPanel.dispose();
    }
    
    class MainPanel extends JPanel implements Disposable {
        private LibraryHeaderPanel header;
        private JPanel selectionPanel = new JPanel();
        private SelectionButton currentSelected = null;
        
        public MainPanel(EventList<RemoteFileItem> eventList) {
            Category category = Category.AUDIO;
            header = new LibraryHeaderPanel(category, friend);

            createSelectionPanel();
            createMyCategories(eventList, friend);    
          
            setLayout(new MigLayout("fill, gap 0, insets 0 0 0 0", "[120!][]", "[][]"));
          
            add(header, "span, growx, wrap");
            add(selectionPanel, "growy");
            add(cardPanel, "grow");
        }
        
        private void createSelectionPanel() {
            selectionPanel.setLayout(new MigLayout("insets 0, gap 0, fillx, wrap", "[120!]", ""));
        }
        
        private Map<Category, JComponent> createMyCategories(EventList<RemoteFileItem> eventList, Friend friend) {
            cardPanel.setLayout(cardLayout);
            
            Map<Category, JComponent> categories = new LinkedHashMap<Category, JComponent>();
            for(Category category : Category.getCategoriesInOrder()) {
                JComponent component = createMyCategoryAction(category, eventList, friend);
                if(component != null) {
                    cardPanel.add(component, category.toString());
                    Icon icon = categoryIconManager.getIcon(category);
                    SelectionButton button = new SelectionButton(new SelectionAction(icon, category, cardLayout));
                    selectionPanel.add(button, "growx");
                    
                    if(currentSelected == null) {
                        currentSelected = button;
                        currentSelected.select(true);
                    }
                }
            }
            return categories;
        }
        
        private JComponent createMyCategoryAction(Category category, EventList<RemoteFileItem> eventList, Friend friend) {
            FilterList<RemoteFileItem> filtered = GlazedListsFactory.filterList(eventList, new CategoryFilter(category));
            
            //don't bother creating anything if there's nothing to display
            if(filtered.size() == 0) {
                filtered.dispose();
                return null;
            }
            lists.add(filtered);
            EventList<RemoteFileItem> filterList = GlazedListsFactory.filterList(filtered, 
                    new TextComponentMatcherEditor<RemoteFileItem>(header.getFilterTextField(), new LibraryTextFilterator<RemoteFileItem>()));

            LibraryTable table = tableFactory.createTable(category, filterList, friend);
            table.enableDownloading(downloadListManager);
            disposableList.add(table);
            
            JScrollPane scrollPane = new JScrollPane(table);
            scrollPane.setColumnHeaderView(table.getTableHeader());
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            
            if (table.isColumnControlVisible()) {
                scrollPane.setCorner(JScrollPane.UPPER_TRAILING_CORNER, table.getColumnControl());
                scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            }

            librarySelectable = table;
            
            return scrollPane;
        }
        
        private class SelectionAction extends AbstractAction {
            
            private Category category;
            private CardLayout cardLayout;
            
            public SelectionAction(Icon icon, Category category, CardLayout cardLayout) {
                super(category.toString(), icon);
                
                this.category = category;
                this.cardLayout = cardLayout;
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(cardPanel, category.toString());
                header.setCategory(category);
            }
        }
        
        @Override
        public void dispose() {
            // TODO Auto-generated method stub
            
        }
        
        
        class SelectionButton extends JButton {
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
}
