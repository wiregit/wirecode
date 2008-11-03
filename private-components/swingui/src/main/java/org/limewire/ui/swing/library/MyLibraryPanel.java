/**
 * 
 */
package org.limewire.ui.swing.library;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.io.File;

import javax.swing.JComponent;
import javax.swing.JPanel;
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
import org.limewire.ui.swing.nav.NavComponent;
import org.limewire.ui.swing.nav.NavSelectable;
import org.limewire.ui.swing.player.PlayerUtils;
import org.limewire.ui.swing.table.TableDoubleClickHandler;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.util.NativeLaunchUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

class MyLibraryPanel extends JPanel implements Disposable, NavComponent {
   // private LibraryTable<LocalFileItem> table;
    private final LibraryHeaderPanel header;
    private LibrarySharePanel sharePanel;
    private LibrarySharePanel shareAllPanel;
    private JXLayer<JComponent> layer;
    private final JScrollPane scrollPane;
    private ListEventListener<LocalFileItem> listListener;
    private EventList<LocalFileItem> eventList;
    private JComponent scrollComponent;
    private Disposable disposable;
    private LibrarySelectable librarySelectable;
    
    @AssistedInject
    public MyLibraryPanel(@Assisted Category category,
                          @Assisted EventList<LocalFileItem> eventList,
                          IconManager iconManager,
                          LibraryTableFactory tableFactory,
                          ShareListManager shareListManager,
                          AllFriendsList allFriendsList){
        this.sharePanel = new LibrarySharePanel(allFriendsList.getAllFriends());
        sharePanel.setShareModel(new FileShareModel(shareListManager));
        this.eventList = eventList;
        
        setLayout(new BorderLayout());

        header = new LibraryHeaderPanel(category, null);
        
        if (category == Category.AUDIO || category == Category.VIDEO || category == Category.IMAGE){
            shareAllPanel = new LibrarySharePanel(allFriendsList.getAllFriends());
            shareAllPanel.setShareModel(new CategoryShareModel(shareListManager));
            header.enableShareAll(shareAllPanel);
        }
        
        EventList<LocalFileItem> filterList = GlazedListsFactory.filterList(eventList, 
                new TextComponentMatcherEditor<LocalFileItem>(header.getFilterTextField(), new LibraryTextFilterator<LocalFileItem>()));
        if (category != Category.IMAGE) {
            LibraryTable table = tableFactory.createTable(category, filterList, null);
            table.enableSharing(sharePanel);
            table.setDoubleClickHandler(new MyLibraryDoubleClickHandler(getTableModel(table)));
            
            layer = new JXLayer<JComponent>(table, new AbstractLayerUI<JComponent>());
            scrollPane = new JScrollPane(layer);
            scrollPane.setColumnHeaderView(table.getTableHeader());
            if (table.isColumnControlVisible()) {
                scrollPane.setCorner(JScrollPane.UPPER_TRAILING_CORNER, table.getColumnControl());
                scrollPane
                        .setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            }

            scrollComponent = table;
            disposable = table;
            librarySelectable = table;

        } else {//Category.IMAGE
            //TODO merge with table for disposing and enabling sharing
            LibraryImagePanel imagePanel = tableFactory.createImagePanel(eventList);
            imagePanel.enableSharing(sharePanel);
            layer = new JXLayer<JComponent>(imagePanel, new AbstractLayerUI<JComponent>());
            scrollPane = new JScrollPane(layer);
            
            scrollComponent = imagePanel;
            disposable = imagePanel;
            librarySelectable = imagePanel;
        }


        // necessary to fill table with stripes and have scrollbar appear
        // properly        
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                adjustSize();
            }
            
            @Override
            public void componentShown(ComponentEvent e){
                adjustSize();
            }
        });

        listListener = new ListEventListener<LocalFileItem>() {
            @Override
            public void listChanged(ListEvent<LocalFileItem> listChanges) {
                adjustSize();
            }
        };

        eventList.addListEventListener(listListener);

        // for absolute positioning of LibrarySharePanel
        layer.getGlassPane().setLayout(null);
        sharePanel.setBounds(0, 0, sharePanel.getPreferredSize().width, sharePanel
                .getPreferredSize().height);
        layer.getGlassPane().add(sharePanel);
        sharePanel.setVisible(false);

        // make sharePanel disappear when the user clicks elsewhere
        AWTEventListener eventListener = new AWTEventListener() {
            @Override
            public void eventDispatched(AWTEvent event) {
                if (sharePanel.isVisible() && (event.getID() == MouseEvent.MOUSE_PRESSED)) {
                    MouseEvent e = (MouseEvent) event;
                    if (sharePanel != e.getComponent()
                            && !sharePanel.contains(e.getComponent())
                            && !scrollPane.getVerticalScrollBar().contains(e.getPoint())) {
                        sharePanel.setVisible(false);
                    }
                }
            }
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(eventListener,
                AWTEvent.MOUSE_EVENT_MASK);
        add(scrollPane, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);
    }
    
    public void dispose() {
        disposable.dispose();
        eventList.removeListEventListener(listListener);
        if(sharePanel != null){
            sharePanel.dispose();
        }
        if(shareAllPanel != null){
            shareAllPanel.dispose();
        }
    }
    
    private void adjustSize(){
        if (scrollComponent.getPreferredSize().height < scrollPane.getViewport().getSize().height) {
            //force layer to take up entire scrollpane so stripes are shown
            layer.setPreferredSize(scrollPane.getViewport().getSize());
        } else {
            //layer can handle its own sizing
            layer.setPreferredSize(null);
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
   
}
