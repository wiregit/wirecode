/**
 * 
 */
package org.limewire.ui.swing.library;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

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

        table = new LibraryTable<LocalFileItem>(libraryManager.getLibraryList().getModel()); 
        table.enableSharing(sharePanel);
        
        JScrollPane scrollPane = new JScrollPane(table);
        
        JXLayer<JComponent> layer = new JXLayer<JComponent>(scrollPane);
        //necessary if the layer is to paint 
        layer.setUI(new AbstractLayerUI<JComponent>() {});
        
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
                    if (!sharePanel.contains(e.getComponent())) {
                        sharePanel.setVisible(false);
                    }
                }
            }};
        Toolkit.getDefaultToolkit().addAWTEventListener(eventListener, AWTEvent.MOUSE_EVENT_MASK);


        add(layer, BorderLayout.CENTER);
        
      //  add(scrollPane, BorderLayout.CENTER);
    }
}
