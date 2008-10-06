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

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;

import org.jdesktop.jxlayer.JXLayer;
import org.jdesktop.jxlayer.plaf.AbstractLayerUI;
import org.limewire.core.api.Category;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.library.sharing.LibrarySharePanel;
import org.limewire.ui.swing.library.table.AudioLibraryTable;
import org.limewire.ui.swing.library.table.DocumentTableFormat;
import org.limewire.ui.swing.library.table.ImageTableFormat;
import org.limewire.ui.swing.library.table.LibraryTable;
import org.limewire.ui.swing.library.table.OtherTableFormat;
import org.limewire.ui.swing.library.table.ProgramTableFormat;
import org.limewire.ui.swing.library.table.VideoTableFormat;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.EventTableModel;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

class MyLibraryPanel extends JPanel implements Disposable {
    private final LibraryTable<LocalFileItem> table;
    private final LibraryHeaderPanel header;
    private LibrarySharePanel sharePanel;
    
    @AssistedInject
    public MyLibraryPanel(@Assisted Category category,
                          @Assisted EventList<LocalFileItem> eventList,
                          final LibrarySharePanel sharePanel){
        this.sharePanel = sharePanel;
        
        setLayout(new BorderLayout());

        header = new LibraryHeaderPanel(category);
        //TODO: filterlist 
        table = createTable(category, eventList);
        table.enableSharing(sharePanel);
                
        final JXLayer<JTable> layer = new JXLayer<JTable>(table, new AbstractLayerUI<JTable>() {});
        final JScrollPane scrollPane = new JScrollPane(layer);
        scrollPane.setColumnHeaderView(table.getTableHeader());
        if(table.isColumnControlVisible()){
            scrollPane.setCorner(JScrollPane.UPPER_TRAILING_CORNER, table.getColumnControl());
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        }
        
        //necessary to fill table with stripes and have scrollbar appear properly
        scrollPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (table.getPreferredSize().height < scrollPane.getViewport().getSize().height) {
                    layer.setPreferredSize(scrollPane.getViewport().getSize());
                } else {
                    layer.setPreferredSize(table.getPreferredSize());
                }
            }
        });
        
        
        //for absolute positioning of LibrarySharePanel
        layer.getGlassPane().setLayout(null);
        sharePanel.setBounds(0,0, sharePanel.getPreferredSize().width, sharePanel.getPreferredSize().height);
        layer.getGlassPane().add(sharePanel);
        sharePanel.setVisible(false);
        
        //make sharePanel disappear when the user clicks elsewhere
        AWTEventListener eventListener = new AWTEventListener(){
            @Override
            public void eventDispatched(AWTEvent event) {
                if (sharePanel.isVisible() && (event.getID() == MouseEvent.MOUSE_PRESSED)){
                    MouseEvent e = (MouseEvent)event;
                    if (sharePanel != e.getComponent() && !sharePanel.contains(e.getComponent()) && !scrollPane.getVerticalScrollBar().contains(e.getPoint())) {
                        sharePanel.setVisible(false);
                    }
                }
            }
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(eventListener, AWTEvent.MOUSE_EVENT_MASK);

        add(scrollPane, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);
    }
    
    public void dispose() {
        ((EventTableModel)table.getModel()).dispose();
        if(sharePanel != null){
            sharePanel.dispose();
        }
    }
    

    private LibraryTable<LocalFileItem> createTable(Category category,
            EventList<LocalFileItem> eventList) {
        switch (category) {
        case AUDIO:
            return new AudioLibraryTable<LocalFileItem>(eventList);
        case VIDEO:
            return new LibraryTable<LocalFileItem>(eventList, new VideoTableFormat<LocalFileItem>());
        case DOCUMENT:
        	return new LibraryTable<LocalFileItem>(eventList, new DocumentTableFormat<LocalFileItem>());
        case IMAGE:
        	return new LibraryTable<LocalFileItem>(eventList, new ImageTableFormat<LocalFileItem>());
        case OTHER:
        	return new LibraryTable<LocalFileItem>(eventList, new OtherTableFormat<LocalFileItem>());
        case PROGRAM:
        	return new LibraryTable<LocalFileItem>(eventList, new ProgramTableFormat<LocalFileItem>());
        }
        
        throw new IllegalArgumentException("Unknown category: " + category);
    }

 
    
    
}
