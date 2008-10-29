package org.limewire.ui.swing.sharing.fancy;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.components.Line;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.sharing.actions.SharingRemoveAllAction;
import org.limewire.ui.swing.sharing.actions.SharingRemoveTableAction;
import org.limewire.ui.swing.sharing.components.ConfirmationUnshareButton;
import org.limewire.ui.swing.sharing.table.CustomTableCellHeaderRenderer;
import org.limewire.ui.swing.sharing.table.SharingTable;
import org.limewire.ui.swing.sharing.table.SharingTableModel;
import org.limewire.ui.swing.table.MultiButtonTableCellRendererEditor;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.gui.TableFormat;

public class SharingFancyTablePanel extends JPanel implements ListEventListener<LocalFileItem> {

    @Resource
    private Color lineColor;
    @Resource
    private int lineSize;
    @Resource
    private Color backgroundColor;
    @Resource
    private Color mainLabelColor;
    @Resource
    private int mainLabelFontSize;
    
    private SharingTable table;
    
    private final TableFormat<LocalFileItem> tableFormat;
    
    private final ConfirmationUnshareButton unShareAllButton;
    
    private SharingRemoveAllAction removeAction;
    private EventList<LocalFileItem> currentEventList;
    
    public SharingFancyTablePanel(String name, EventList<LocalFileItem> eventList, TableFormat<LocalFileItem> tableFormat, 
            TransferHandler transferHandler, LocalFileList fileList, Icon panelIcon, PropertiesFactory<LocalFileItem> localFileItemPropsFactory) {
        this(name, eventList, tableFormat, true, transferHandler, fileList, panelIcon, localFileItemPropsFactory);
    }
    
    public SharingFancyTablePanel(String name, EventList<LocalFileItem> eventList, TableFormat<LocalFileItem> tableFormat, 
            boolean paintTableHeader, TransferHandler transferHandler, LocalFileList fileList, Icon panelIcon, PropertiesFactory<LocalFileItem> localFileItemPropsFactory) {
        GuiUtils.assignResources(this); 
        
        this.tableFormat = tableFormat;
        this.currentEventList = eventList;
               
        setBackground(backgroundColor);
        
        JLabel headerLabel = new JLabel(name, panelIcon, JLabel.CENTER);
        headerLabel.setForeground(mainLabelColor);
        FontUtils.setSize(headerLabel, mainLabelFontSize);
        FontUtils.bold(headerLabel);
        
        removeAction = new SharingRemoveAllAction(fileList, eventList);
        unShareAllButton = new ConfirmationUnshareButton(removeAction);
        unShareAllButton.setEnabled(false);

        // black seperator
        Line line = Line.createHorizontalLine(lineColor, lineSize);
        
        // create the table
        table = createTable(eventList, fileList, tableFormat, transferHandler, localFileItemPropsFactory);
        
        // top row should never be tall than 30pixels, the bottom row(table, should fill any remaining space
        setLayout(new MigLayout("gap 0, insets 18 6 10 6",     //layout constraints
                "[] [] ",                       // column constraints
                "[::30] [] [grow][grow]" ));    // row constraints
        
        add(headerLabel, "gapbottom 4, push");       // first row
        add(new JLabel(I18n.tr("Unshare All")), "gapbottom 2, split 2");
        add(unShareAllButton, "gapbottom 2, gapright 9, wrap");
        
        // second row
        add(line, "span 2, growx 100, height :: 3, wrap");
        
        //third row
        if(paintTableHeader)
            add(table.getTableHeader(), "span 2, grow, wrap");
        add(table, "span 2, grow");

        eventList.addListEventListener(this);
        
        setVisible(false);
    }
    
    private SharingTable createTable(EventList<LocalFileItem> eventList, LocalFileList fileList, TableFormat<LocalFileItem> tableFormat, 
            TransferHandler transferHandler, PropertiesFactory<LocalFileItem> localFileItemPropsFactory) {
        if( table == null) {
            table = new SharingTable(eventList, fileList, tableFormat, localFileItemPropsFactory);
            table.setTransferHandler(transferHandler);
            table.setSortable(false);
            table.setRowSelectionAllowed(true);
            table.setRowHeight(20);
            setRenderers();
        }
        return table;
    }
    
    private void setRenderers() {
        //create renders/editors
        MultiButtonTableCellRendererEditor editor = new MultiButtonTableCellRendererEditor(createActions());
        MultiButtonTableCellRendererEditor renderer = new MultiButtonTableCellRendererEditor(createActions());
        
        TableColumn tc = table.getColumn("");
        tc.setPreferredWidth(25);
        tc.setMaxWidth(25);
        tc.setResizable(false);
        tc.setCellEditor(editor);
        tc.setCellRenderer(renderer);
        
        JTableHeader th = table.getTableHeader();
        th.setDefaultRenderer(new CustomTableCellHeaderRenderer());
    }
    
    public SharingTable getTable() {
        return table;
    }
    
    public void setModel(EventList<LocalFileItem> eventList, LocalFileList fileList) {
        currentEventList.removeListEventListener(this);
        currentEventList = eventList;
        currentEventList.addListEventListener(this);
        removeAction.setEventList(eventList);
        removeAction.setFileList(fileList);
        
        table.setModel(new SharingTableModel(eventList, fileList, tableFormat));
        setRenderers();

        int size = eventList.size();
        if( size == 0 ) {
            unShareAllButton.setEnabled(false);
            SharingFancyTablePanel.this.setVisible(false);
        } else {
            unShareAllButton.setEnabled(true);
            SharingFancyTablePanel.this.setVisible(true);
        }
    }

    @Override
    public void listChanged(ListEvent<LocalFileItem> listChanges) {
        final int size = listChanges.getSourceList().size();
        SwingUtilities.invokeLater(new Runnable(){
            public void run() {
                if( size == 0 ) {
                    unShareAllButton.setEnabled(false);
                    SharingFancyTablePanel.this.setVisible(false);
                } else {
                    unShareAllButton.setEnabled(true);
                    SharingFancyTablePanel.this.setVisible(true);
                }
            }
        });
    }
    
    private List<Action> createActions() {
        List<Action> list = new ArrayList<Action>();
        list.add(new SharingRemoveTableAction(table));
        return list;
    }
}
