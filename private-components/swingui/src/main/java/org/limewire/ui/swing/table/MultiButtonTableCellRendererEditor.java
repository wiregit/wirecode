package org.limewire.ui.swing.table;

import java.awt.Component;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

/**
 * Generic Renderer/Editor that allows for one or more buttons to 
 * be drawn in a table cell. To create clickable buttons, a list of
 * actions is passed into the renderer/editor.
 * <p>
 * This class can be used for both renderering and editing but there must
 * be a unique instance for the renderer and editor. 
 * <p>
 * For the editor to properlly cancel, an instance of the editor must be passed
 * to each button and upon actionPerfomed, the button must call cancelCellEditing.
 * 
 *      public void actionPerformed(ActionEvent e) {
 *          editor.cancelCellEditing();
 *      }
 */
public class MultiButtonTableCellRendererEditor extends JPanel implements TableCellRenderer, TableCellEditor {

    /**
     *  The horizontal gap in the layout. 
     */
    private final static int HGAP = 5;
    
    /** 
     * The vertical gap in the layout. 
     */
    private final static int VGAP = 1;

    /**
     * Height for the rows
     */
    protected int height;
    
    private final List<CellEditorListener> listeners = new ArrayList<CellEditorListener>();

    public MultiButtonTableCellRendererEditor(int minRowHeight) {
        this(new ArrayList<Action>(), minRowHeight);
    }
    
    public MultiButtonTableCellRendererEditor(List<Action> actions, int minRowHeight) {
        setLayout(new FlowLayout(FlowLayout.CENTER, HGAP, VGAP));
        
        //default size paint the rows
        height = minRowHeight;
        
        addActions(actions);
    }
    
    public void addActions(List<Action> actions) {
        for(Action action : actions) {
            JButton button = new JButton(action);
            button.setBorder(null);
            button.setFocusable(false);
            button.setBorderPainted(false);
            
            if(button.getIcon() != null)
                height = Math.max(height, button.getHeight());
            
            add(button);
        }
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {

        if(table.getSelectedRow() == row) {
            this.setBackground(table.getSelectionBackground());
            this.setForeground(table.getSelectionForeground());
        }
        else {
            this.setBackground(table.getBackground());
            this.setForeground(table.getForeground());
        }
        
        table.setRowHeight(height + 2 * table.getRowMargin());

        return this;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
            int row, int column) {
        this.setBackground(table.getSelectionBackground());
        this.setForeground(table.getSelectionForeground());
        table.setRowHeight(height + 2 * table.getRowMargin());
        return this;
    }

    @Override
    public void addCellEditorListener(CellEditorListener lis) {
        synchronized (listeners) {
            if (!listeners.contains(lis)) listeners.add(lis);
        }
    }

    @Override
    public void cancelCellEditing() {
        synchronized (listeners) {
            for (int i=0, N=listeners.size(); i<N; i++) {
                listeners.get(i).editingCanceled(new ChangeEvent(this));
            }
        }
    }

    @Override
    public Object getCellEditorValue() {
        return null;
    }

    @Override
    public boolean isCellEditable(EventObject anEvent) {
        return true;
    }

    @Override
    public void removeCellEditorListener(CellEditorListener lis) {
        synchronized (listeners) {
            if (listeners.contains(lis)) listeners.remove(lis);
        }
    }

    @Override
    public boolean shouldSelectCell(EventObject anEvent) {
        return true;
    }

    @Override
    public boolean stopCellEditing() {
        cancelCellEditing();
        return true;
    }
}
