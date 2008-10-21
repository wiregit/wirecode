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

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;

import org.jdesktop.jxlayer.JXLayer;
import org.jdesktop.jxlayer.plaf.AbstractLayerUI;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.library.image.LibraryImagePanel;
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
import ca.odell.glazedlists.swing.EventTableModel;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

class MyLibraryPanel extends JPanel implements Disposable, NavComponent {
    private LibraryTable<LocalFileItem> table;
    private final LibraryHeaderPanel header;
    private LibrarySharePanel sharePanel;
    private JXLayer<JTable> layer;
    private final JScrollPane scrollPane;
    private ListEventListener<LocalFileItem> listListener;
    private EventList<LocalFileItem> eventList;
    
    @AssistedInject
    public MyLibraryPanel(@Assisted Category category,
                          @Assisted EventList<LocalFileItem> eventList,
                          final LibrarySharePanel sharePanel, 
                          IconManager iconManager, 
                          LibraryTableFactory tableFactory){
        this.sharePanel = sharePanel;
        this.eventList = eventList;
        
        setLayout(new BorderLayout());

        header = new LibraryHeaderPanel(category, null);
        
        EventList<LocalFileItem> filterList = GlazedListsFactory.filterList(eventList, 
                new TextComponentMatcherEditor<LocalFileItem>(header.getFilterTextField(), new LibraryTextFilterator<LocalFileItem>()));
        if (category != Category.IMAGE) {
            table = tableFactory.createTable(category, filterList, null);
            table.enableSharing(sharePanel);
            table.setDoubleClickHandler(new MyLibraryDoubleClickHandler(getTableModel()));

            layer = new JXLayer<JTable>(table, new AbstractLayerUI<JTable>());
            scrollPane = new JScrollPane(layer);
            scrollPane.setColumnHeaderView(table.getTableHeader());
            if (table.isColumnControlVisible()) {
                scrollPane.setCorner(JScrollPane.UPPER_TRAILING_CORNER, table.getColumnControl());
                scrollPane
                        .setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            }

            // necessary to fill table with stripes and have scrollbar appear
            // properly
            scrollPane.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
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

        } else {//Category.IMAGE
            //TODO merge with table for disposing and enabling sharing
            LibraryImagePanel imagePanel = tableFactory.createImagePanel(eventList);
            scrollPane = new JScrollPane(imagePanel);
        }

        add(scrollPane, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);
    }
    
    public void dispose() {
        table.dispose();
        eventList.removeListEventListener(listListener);
        ((EventTableModel)table.getModel()).dispose();
        if(sharePanel != null){
            sharePanel.dispose();
        }
    }
    
    private void adjustSize(){
        if (table.getPreferredSize().height < scrollPane.getViewport().getSize().height) {
            layer.setPreferredSize(scrollPane.getViewport().getSize());
        } else {
            layer.setPreferredSize(table.getPreferredSize());
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
    private LibraryTableModel<LocalFileItem> getTableModel(){
        return (LibraryTableModel<LocalFileItem>)table.getModel();
    }

    @Override
    public void select(NavSelectable selectable) {
        LibraryTableModel<LocalFileItem> model = getTableModel();
        for(int y=0; y < model.getRowCount(); y++) {
            LocalFileItem localFileItem = model.getElementAt(y);
            if(selectable.getNavSelectionId().equals(localFileItem.getUrn())) {
                table.getSelectionModel().setSelectionInterval(y, y);
                break;
            }
        }
        table.ensureSelectionVisible();
    }
   
}
