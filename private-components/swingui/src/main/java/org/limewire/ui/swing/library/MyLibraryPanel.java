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
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.library.sharing.LibrarySharePanel;
import org.limewire.ui.swing.library.table.LibraryTable;

import com.google.inject.Inject;

/**
 *
 */
public class MyLibraryPanel extends JPanel {
    public static final String NAME = "My Library";
    public final LibraryTable table;
    
    @Inject
    public MyLibraryPanel(LibraryManager libraryManager, final LibrarySharePanel sharePanel){
        setLayout(new BorderLayout());

        table = new LibraryTable<LocalFileItem>(libraryManager.getLibraryManagedList().getSwingModel()); 
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
            }};
        Toolkit.getDefaultToolkit().addAWTEventListener(eventListener, AWTEvent.MOUSE_EVENT_MASK);

        add(scrollPane, BorderLayout.CENTER);
    }
}
