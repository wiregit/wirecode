package org.limewire.ui.swing.sharing.fancy;

import java.awt.Color;
import java.awt.Component;
import java.awt.dnd.DropTarget;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.components.Line;
import org.limewire.ui.swing.sharing.actions.SharingRemoveAllAction;
import org.limewire.ui.swing.sharing.actions.SharingRemoveTableAction;
import org.limewire.ui.swing.sharing.components.ConfirmationUnshareButton;
import org.limewire.ui.swing.sharing.table.SharingFancyMultiButtonTableCellRendererEditor;
import org.limewire.ui.swing.sharing.table.SharingFancyTable;
import org.limewire.ui.swing.table.MultiButtonTableCellRendererEditor;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.gui.TableFormat;

public class SharingFancyTablePanel extends JPanel implements ListEventListener<LocalFileItem> {
    
    @Resource
    private Icon cancelIcon;
    
    private SharingFancyTable table;
    
    private final TableFormat<LocalFileItem> tableFormat;
    
    private final ConfirmationUnshareButton unShareAllButton;
    
    FancyCellRenderer fancyRenderer;
    MultiButtonTableCellRendererEditor editor;
    MultiButtonTableCellRendererEditor renderer;
    
    private SharingRemoveAllAction removeAction;
    private EventList<LocalFileItem> currentEventList;
    
    public SharingFancyTablePanel(String name, EventList<LocalFileItem> eventList, TableFormat<LocalFileItem> tableFormat, DropTarget dropTarget, LocalFileList fileList, Icon panelIcon) {
        this(name, eventList, tableFormat, true, dropTarget, fileList, panelIcon);
    }
    
    public SharingFancyTablePanel(String name, EventList<LocalFileItem> eventList, TableFormat<LocalFileItem> tableFormat, 
            boolean paintTableHeader, DropTarget dropTarget, LocalFileList fileList, Icon panelIcon) {
        
        this.tableFormat = tableFormat;
        this.currentEventList = eventList;
        
        GuiUtils.assignResources(this); 
        
        setBackground(Color.WHITE);
        
        JLabel headerLabel = new JLabel(name, panelIcon, JLabel.CENTER);
        
        JLabel unShareButtonLabel = new JLabel(I18n.tr("Unshare All"));
        removeAction = new SharingRemoveAllAction(fileList, eventList);
        unShareAllButton = new ConfirmationUnshareButton(removeAction);
        unShareAllButton.setEnabled(false);

        // black seperator
        Line line = Line.createHorizontalLine(Color.BLACK, 3);
        
        // create the table
        table = createTable(eventList, fileList, tableFormat, dropTarget);
        
        // top row should never be tall than 30pixels, the bottom row(table, should fill any remainign space
        setLayout(new MigLayout("insets 10 25 0 10",     //layout contraints
                "[] [] ",                       // column constraints
                "[::30] [] [grow][grow]" ));    // row contraints
        
        add(headerLabel, "push");       // first row
        add(unShareButtonLabel, "split 2");
        add(unShareAllButton, "wrap");
        
        // second row
        add(line, "span 2, growx 100, height :: 3, wrap");
        
        //third row
        if(paintTableHeader)
            add(table.getTableHeader(), "span 2, grow, wrap");
        add(table, "span 2, grow");

        eventList.addListEventListener(this);
        
        setVisible(false);
    }
    
    private SharingFancyTable createTable(EventList<LocalFileItem> eventList, LocalFileList fileList, TableFormat<LocalFileItem> tableFormat, DropTarget dropTarget) {
        if( table == null) {
            table = new SharingFancyTable(eventList, fileList, tableFormat);
            table.setDropTarget(dropTarget);
            table.setSortable(false);
            
            TableMouseListener tableMouseListener = new TableMouseListener(table);
            fancyRenderer = new FancyCellRenderer(tableMouseListener);
            
            editor = new SharingFancyMultiButtonTableCellRendererEditor(tableMouseListener);
            editor.addActions(createActions());
            renderer = new SharingFancyMultiButtonTableCellRendererEditor(tableMouseListener);
            renderer.addActions(createActions());
            table.setRowHeight(20);
            setRenderers();
        }
        return table;
    }
    
    private void setRenderers() {
        table.setDefaultRenderer(Object.class, fancyRenderer);
        
        TableColumn tc = table.getColumn("");
        tc.setPreferredWidth(25);
        tc.setMaxWidth(25);
        tc.setResizable(false);
        tc.setCellEditor(editor);
        tc.setCellRenderer(renderer);
    }
    
    public SharingFancyTable getTable() {
        return table;
    }
    
    public void setModel(EventList<LocalFileItem> eventList, LocalFileList fileList) {
        currentEventList.removeListEventListener(this);
        currentEventList = eventList;
        currentEventList.addListEventListener(this);
        removeAction.setEventList(eventList);
        removeAction.setFileList(fileList);
        
        table.setModel(eventList, fileList, tableFormat);
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
    
    public class TableMouseListener implements MouseListener, MouseMotionListener {

        private final JTable table;
        private int mouseOverRow = -1;
        
        public TableMouseListener(JTable table) {
            this.table = table;
            
            table.addMouseListener(this);
            table.addMouseMotionListener(this);
        }
        
        public int getMouseOverRow() {
            return mouseOverRow;
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            mouseOverRow = table.rowAtPoint(e.getPoint()); 
            table.repaint();
        }
        
        @Override
        public void mouseExited(MouseEvent e) {
            mouseOverRow = -1;
            table.repaint();
        }
        
        @Override
        public void mouseClicked(MouseEvent e) {}
        @Override
        public void mouseEntered(MouseEvent e) {}
        @Override
        public void mousePressed(MouseEvent e) {}
        @Override
        public void mouseReleased(MouseEvent e) {}
        @Override
        public void mouseDragged(MouseEvent e) { }      
    }
    
    private class FancyCellRenderer extends JLabel implements TableCellRenderer {

        private final TableMouseListener tableListener;
        
        public FancyCellRenderer(TableMouseListener tableListener) {
            this.tableListener = tableListener;
            setOpaque(true);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            if(row == tableListener.getMouseOverRow()) {
                this.setBackground(Color.BLUE);
                setForeground(Color.WHITE);
            } else {
                this.setBackground(Color.WHITE);
                setForeground(Color.BLACK);
            }
            
            if(value == null)
                setText("");
            else
                setText(value.toString());
            
            return this;
        }
        
    }
    
    private List<Action> createActions() {
        List<Action> list = new ArrayList<Action>();
        list.add(new SharingRemoveTableAction(table, cancelIcon));
        return list;
    }

}
