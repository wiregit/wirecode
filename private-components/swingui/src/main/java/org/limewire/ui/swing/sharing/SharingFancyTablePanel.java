package org.limewire.ui.swing.sharing;

import java.awt.Color;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.FileItem;
import org.limewire.ui.swing.components.Line;
import org.limewire.ui.swing.sharing.table.SharingFancyTable;
import org.limewire.ui.swing.util.GuiUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.gui.TableFormat;

public class SharingFancyTablePanel extends JPanel implements ListEventListener<FileItem> {

    private Icon panelIcon = null;
    
    @Resource
    private Icon cancelIcon;
    
    private final SharingFancyTable table;
    
    private final JButton unShareButton;
    
    public SharingFancyTablePanel(String name, EventList<FileItem> eventList, TableFormat<FileItem> tableFormat) {
        this(name, eventList, tableFormat, true);
    }
    
    public SharingFancyTablePanel(String name, EventList<FileItem> eventList, TableFormat<FileItem> tableFormat, 
            boolean paintTableHeader) {

        GuiUtils.assignResources(this); 
        
        setBackground(Color.WHITE);
        
        JLabel headerLabel = new JLabel(name, panelIcon, JLabel.CENTER);
        
        JLabel unShareButtonLabel = new JLabel("Unshare All");
        unShareButton = new JButton(cancelIcon);
        unShareButton.setEnabled(false);

        // black seperator
        Line line = new Line(Color.BLACK, 3);
        
        table = new SharingFancyTable(eventList, tableFormat);
        
        // top row should never be tall than 30pixels, the bottom row(table, should fill any remainign space
        setLayout(new MigLayout("insets 10 20 0 10",     //layout contraints
                "[] [] ",                       // column constraints
                "[::30] [] [grow][grow]" ));    // row contraints
        
        add(headerLabel, "push");       // first row
        add(unShareButtonLabel, "split 2");
        add(unShareButton, "wrap");
        
        // second row
        add(line, "span 2, growx 100, height :: 3, wrap");
        
        //third row
        if(paintTableHeader)
            add(table.getTableHeader(), "span 2, grow, wrap");
        add(table, "span 2, grow");

        eventList.addListEventListener(this);
    }

    @Override
    public void listChanged(ListEvent<FileItem> listChanges) {
        if(listChanges.getSourceList().size() == 0 ) {
            SwingUtilities.invokeLater(new Runnable(){
                @Override
                public void run() {
                    unShareButton.setEnabled(false);
                }
            });
        } else {
            SwingUtilities.invokeLater(new Runnable(){
                @Override
                public void run() {
                    unShareButton.setEnabled(true);
                }
            });
        }
    }

}
