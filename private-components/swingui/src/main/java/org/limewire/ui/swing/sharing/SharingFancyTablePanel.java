package org.limewire.ui.swing.sharing;

import java.awt.Color;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.FileItem;
import org.limewire.ui.swing.components.Line;
import org.limewire.ui.swing.sharing.table.SharingFancyTable;
import org.limewire.ui.swing.util.GuiUtils;

import ca.odell.glazedlists.EventList;

public class SharingFancyTablePanel extends JPanel {

    private Icon panelIcon = null;
    
    @Resource
    private Icon cancelIcon;
    
    private final SharingFancyTable table;
    
    public SharingFancyTablePanel(String name, EventList<FileItem> eventList) {
        this(name, eventList, true);
    }
    
    public SharingFancyTablePanel(String name, EventList<FileItem> eventList, boolean paintTableHeader) {

        GuiUtils.assignResources(this); 
        
        setBackground(Color.WHITE);
        
        JLabel headerLabel = new JLabel(name, panelIcon, JLabel.CENTER);
        
        JLabel unShareButtonLabel = new JLabel("Unshare");
        JButton unShareButton = new JButton(cancelIcon);

        // black seperator
        Line line = new Line(Color.BLACK);
        
        table = new SharingFancyTable(eventList);
        
        // top row should never be tall than 30pixels, the bottom row(table, should fill any remainign space
        setLayout(new MigLayout("",     //layout contraints
                "[] [] ",               // column constraints
                "[::30] [] [grow][grow]" ));  // row contraints
        
        add(headerLabel, "push");       // first row
        add(unShareButtonLabel, "split 2");
        add(unShareButton, "wrap");
        
        // second row
        add(line, "span 2, growx 100, height :: 3, wrap");
        
        //third row
        if(paintTableHeader)
            add(table.getTableHeader(), "span 2, grow, wrap");
        add(table, "span 2, grow");

//        add(new JScrollPane(table), "span 2, height :10:");
    }
//    
//    public static void main(String args[]) {
//        JFrame f = new JFrame();
//        f.add(new SharingFancyTable("Music", null));
//        f.setSize(600,800);
//        f.setDefaultCloseOperation(2);
//        f.setVisible(true);
//    }

    
}
