/**
 * 
 */
package org.limewire.ui.swing.library;

import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import org.jdesktop.jxlayer.JXLayer;
import org.jdesktop.jxlayer.plaf.AbstractLayerUI;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.library.image.LibraryImagePanel;
import org.limewire.ui.swing.library.sharing.AllFriendsList;
import org.limewire.ui.swing.library.sharing.CategoryShareModel;
import org.limewire.ui.swing.library.sharing.FileShareModel;
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
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

class MyLibraryPanel extends LibraryPanel {
    private AllFriendsList allFriendsList;
    private ShareListManager shareListManager;
    private LibraryTableFactory tableFactory;
    private final CategoryIconManager categoryIconManager;
    private final FriendComboBox comboBox;

    private LibrarySharePanel shareAllPanel = null;
    
    @AssistedInject
    public MyLibraryPanel( @Assisted EventList<LocalFileItem> eventList,
                          IconManager iconManager,
                          LibraryTableFactory tableFactory,
                          CategoryIconManager categoryIconManager,
                          ShareListManager shareListManager,
                          AllFriendsList allFriendsList,
                          final FriendComboBox comboBox){
        super(null);
        
        this.shareListManager = shareListManager;
        this.allFriendsList = allFriendsList;
        this.tableFactory = tableFactory;
        this.categoryIconManager = categoryIconManager;
        this.comboBox = comboBox;
        
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                comboBox.reset();
            }
        });
       
        loadHeader();
        loadSelectionPanel();
        createMyCategories(eventList);
        
        selectFirst();
    }

    @Override
    public void loadHeader() {
        shareAllPanel = new LibrarySharePanel(allFriendsList.getAllFriends());
        shareAllPanel.setShareModel(new CategoryShareModel(shareListManager));
        headerPanel.enableShareAll(shareAllPanel);
    }

    @Override
    public void loadSelectionPanel() {
        selectionPanel.add(new JLabel(I18n.tr("Show:")), "gapleft 5, gaptop 5, gapbottom 5");
        selectionPanel.add(comboBox, "wmax 110, alignx 50%, gapbottom 15");
    }
    
    private Map<Category, JComponent> createMyCategories(EventList<LocalFileItem> eventList) {
        Map<Category, JComponent> categories = new LinkedHashMap<Category, JComponent>();
        for(Category category : Category.getCategoriesInOrder()) {
            createButton(categoryIconManager.getIcon(category), 
                    category, createMyCategoryAction(category, eventList));
        }
        return categories;
    }

    private JComponent createMyCategoryAction(Category category, EventList<LocalFileItem> eventList) {
        FilterList<LocalFileItem> filtered = GlazedListsFactory.filterList(eventList, new CategoryFilter(category));
        
        //TODO: can this be a singleton??? 
        final LibrarySharePanel sharePanel = new LibrarySharePanel(allFriendsList.getAllFriends());
        addDisposable(sharePanel);
        
        final JXLayer<JComponent> layer;
        
        sharePanel.setShareModel(new FileShareModel(shareListManager));
        
        final JComponent scrollComponent;
        final JScrollPane scrollPane;
        
        EventList<LocalFileItem> filterList = GlazedListsFactory.filterList(filtered, 
                new TextComponentMatcherEditor<LocalFileItem>(getFilterTextField(), new LibraryTextFilterator<LocalFileItem>()));
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
            addDisposable(table);
//            librarySelectable = table;

        } else {//Category.IMAGE 
            scrollPane = new JScrollPane();
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            LibraryImagePanel imagePanel = tableFactory.createImagePanel(filterList, scrollPane);
            imagePanel.enableSharing(sharePanel);
            
            scrollPane.setViewportView(imagePanel);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            layer = new JXLayer<JComponent>(scrollPane, new AbstractLayerUI<JComponent>());
            
            scrollComponent = imagePanel;
            addDisposable(imagePanel);
//            librarySelectable = imagePanel;
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
}
