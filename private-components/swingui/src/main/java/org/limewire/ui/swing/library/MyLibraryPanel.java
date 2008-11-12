/**
 * 
 */
package org.limewire.ui.swing.library;

import java.awt.AWTEvent;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.jxlayer.JXLayer;
import org.jdesktop.jxlayer.plaf.AbstractLayerUI;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.image.LibraryImagePanel;
import org.limewire.ui.swing.library.sharing.AllFriendsList;
import org.limewire.ui.swing.library.sharing.CategoryShareModel;
import org.limewire.ui.swing.library.sharing.FileShareModel;
import org.limewire.ui.swing.library.sharing.LibrarySharePanel;
import org.limewire.ui.swing.library.table.LibraryTable;
import org.limewire.ui.swing.library.table.LibraryTableFactory;
import org.limewire.ui.swing.library.table.LibraryTableModel;
import org.limewire.ui.swing.lists.CategoryFilter;
import org.limewire.ui.swing.nav.NavComponent;
import org.limewire.ui.swing.nav.NavSelectable;
import org.limewire.ui.swing.player.PlayerUtils;
import org.limewire.ui.swing.table.TableDoubleClickHandler;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.util.NativeLaunchUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

class MyLibraryPanel extends BaseLibraryPanel implements Disposable, NavComponent {
    
    @Resource private Color selectedBackground;
    @Resource private Font selectedTextFont;
    @Resource private Color selectedTextColor;
    @Resource private Font textFont;
    @Resource private Color textColor;
    
    private final LibraryHeaderPanel header;
    
    private ListEventListener<LocalFileItem> listListener;
    private EventList<LocalFileItem> eventList;
    private LibrarySelectable librarySelectable;
    
    private CardLayout cardLayout = new CardLayout();
    private JPanel cardPanel = new JPanel();
    private JPanel selectionPanel = new JPanel();
    
    private LibraryTableFactory tableFactory;
    private final CategoryIconManager categoryIconManager;
    private final FriendComboBox comboBox;
    
    private JPanel mainPanel = new JPanel();
    
    private AllFriendsList allFriendsList;
    private ShareListManager shareListManager;

    private SelectionButton currentSelected = null;
    private LibrarySharePanel shareAllPanel = null;
    
    private List<Disposable> disposableList = new ArrayList<Disposable>();
    
    @AssistedInject
    public MyLibraryPanel( @Assisted EventList<LocalFileItem> eventList,
                          IconManager iconManager,
                          LibraryTableFactory tableFactory,
                          CategoryIconManager categoryIconManager,
                          ShareListManager shareListManager,
                          AllFriendsList allFriendsList,
                          final FriendComboBox comboBox){
        
        this.shareListManager = shareListManager;
        this.allFriendsList = allFriendsList;
        this.eventList = eventList;
        this.tableFactory = tableFactory;
        this.categoryIconManager = categoryIconManager;
        
        GuiUtils.assignResources(this);
        
        Category category = Category.AUDIO;

        header = new LibraryHeaderPanel(category, null);
        this.comboBox = comboBox;
        
        this.comboBox.setBasePanel(this);
        
        
        shareAllPanel = new LibrarySharePanel(allFriendsList.getAllFriends());
        shareAllPanel.setShareModel(new CategoryShareModel(shareListManager));
        header.enableShareAll(shareAllPanel);
   
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                comboBox.reset();
            }
        });
        
        createSelectionPanel();
        createMyCategories(eventList);

        eventList.addListEventListener(listListener);
        
        mainPanel.setLayout(new MigLayout("fill, gap 0, insets 0 0 0 0", "[120!][]", "[][]"));
        
        mainPanel.add(header, "span, growx, wrap");
        mainPanel.add(selectionPanel, "growy");
        mainPanel.add(cardPanel, "grow");
        
        setMainCard(mainPanel);
    }
    
    @Override
    public void showMainCard() {        
        comboBox.reset();
        
        super.showMainCard();
    }
    
    private void createSelectionPanel() {
        selectionPanel.setLayout(new MigLayout("insets 0, gap 0, fillx, wrap", "[120!]", ""));
        
        selectionPanel.add(new JLabel(I18n.tr("Show:")), "gapleft 5, gaptop 5, gapbottom 5");
        selectionPanel.add(comboBox, "wmax 110, alignx 50%, gapbottom 15");
    }
    
    private Map<Category, JComponent> createMyCategories(EventList<LocalFileItem> eventList) {
        cardPanel.setLayout(cardLayout);
        
        Map<Category, JComponent> categories = new LinkedHashMap<Category, JComponent>();
        for(Category category : Category.getCategoriesInOrder()) {
            JComponent component = createMyCategoryAction(category, eventList);
            cardPanel.add(component, category.toString());
            Icon icon = categoryIconManager.getIcon(category);
            
            SelectionButton button = new SelectionButton(new SelectionAction(icon, category, cardLayout));
            selectionPanel.add(button, "growx");
            
            if(currentSelected == null) {
                currentSelected  = button;
                currentSelected.select(true);
            }
        }
        return categories;
    }

    private JComponent createMyCategoryAction(Category category, EventList<LocalFileItem> eventList) {
        FilterList<LocalFileItem> filtered = GlazedListsFactory.filterList(eventList, new CategoryFilter(category));
        
        //TODO: can this be a singleton??? 
        final LibrarySharePanel sharePanel = new LibrarySharePanel(allFriendsList.getAllFriends());
        disposableList.add(sharePanel);
        
        final JXLayer<JComponent> layer;
        
        sharePanel.setShareModel(new FileShareModel(shareListManager));
        
        final JComponent scrollComponent;
        final JScrollPane scrollPane;
        
        EventList<LocalFileItem> filterList = GlazedListsFactory.filterList(filtered, 
                new TextComponentMatcherEditor<LocalFileItem>(header.getFilterTextField(), new LibraryTextFilterator<LocalFileItem>()));
        if (category != Category.IMAGE) {
            LibraryTable table = tableFactory.createTable(category, filterList, null);
            table.enableSharing(sharePanel);
            table.setDoubleClickHandler(new MyLibraryDoubleClickHandler(getTableModel(table)));
            
            layer = new JXLayer<JComponent>(table, new AbstractLayerUI<JComponent>());
            scrollPane = new JScrollPane(layer);
            scrollPane.setColumnHeaderView(table.getTableHeader());
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            
            if (table.isColumnControlVisible()) {
                scrollPane.setCorner(JScrollPane.UPPER_TRAILING_CORNER, table.getColumnControl());
                scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            }

            scrollComponent = table;
            disposableList.add(table);
            librarySelectable = table;

        } else {//Category.IMAGE 
            scrollPane = new JScrollPane();
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            LibraryImagePanel imagePanel = tableFactory.createImagePanel(filterList, scrollPane);
            imagePanel.enableSharing(sharePanel);
            
            scrollPane.setViewportView(imagePanel);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            layer = new JXLayer<JComponent>(scrollPane, new AbstractLayerUI<JComponent>());
            
            scrollComponent = imagePanel;
            disposableList.add(imagePanel);
            librarySelectable = imagePanel;
        }
        
        // for absolute positioning of LibrarySharePanel
        layer.getGlassPane().setLayout(null);
        sharePanel.setBounds(0, 0, sharePanel.getPreferredSize().width, sharePanel
                .getPreferredSize().height);
        layer.getGlassPane().add(sharePanel);
        sharePanel.setVisible(false);
        
        layer.addComponentListener(new ComponentListener(){
            @Override
            public void componentHidden(ComponentEvent e) {
            }

            @Override
            public void componentMoved(ComponentEvent e) {
            }

            @Override
            public void componentResized(ComponentEvent e) {
                  if (scrollComponent.getPreferredSize().height < scrollPane.getViewport().getSize().height) {
                  //force layer to take up entire scrollpane so stripes are shown
                      layer.setPreferredSize(scrollPane.getViewport().getSize());
                  } else {
                  //layer can handle its own sizing
                      layer.setPreferredSize(null);
                  }
            }

            @Override
            public void componentShown(ComponentEvent e) {
                if (scrollComponent.getPreferredSize().height < scrollPane.getViewport().getSize().height) {
                //force layer to take up entire scrollpane so stripes are shown
                    layer.setPreferredSize(scrollPane.getViewport().getSize());
                } else {
                //layer can handle its own sizing
                    layer.setPreferredSize(null);
                }
            }
            
        });

        // make sharePanel disappear when the user clicks elsewhere
        AWTEventListener eventListener = new AWTEventListener() {
            @Override
            public void eventDispatched(AWTEvent event) {
                if (sharePanel.isVisible() && (event.getID() == MouseEvent.MOUSE_PRESSED)) {
                    MouseEvent e = (MouseEvent) event;
                    if (sharePanel != e.getComponent()
                            && !sharePanel.contains(e.getComponent()) ){
//                            && !scrollPane.getVerticalScrollBar().contains(e.getPoint())) {
                        sharePanel.setVisible(false);
                    }
                }
            }
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(eventListener,
                AWTEvent.MOUSE_EVENT_MASK);
        
        return scrollPane;
    }
    
    public void dispose() {
        eventList.removeListEventListener(listListener);
        for(Disposable disposable : disposableList) {
            disposable.dispose();
        }
        if(shareAllPanel != null){
            shareAllPanel.dispose();
        }
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
    
    @SuppressWarnings("unchecked")
    private LibraryTableModel<LocalFileItem> getTableModel(LibraryTable table){
        return (LibraryTableModel<LocalFileItem>)table.getModel();
    }

    @Override
    public void select(NavSelectable selectable) {
        librarySelectable.selectAndScroll(selectable.getNavSelectionId());
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
