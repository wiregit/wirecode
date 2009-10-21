package org.limewire.ui.swing.upload.table;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.limewire.ui.swing.table.MouseableTable;
import org.limewire.ui.swing.table.TableColors;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.DefaultEventTableModel;

/**
 * Base class for transfer tables like the Uploads table.
 */
public abstract class TransferTable<E> extends MouseableTable {

    private final DefaultEventTableModel<E> model;
    
    /**
     * Constructs a new TransferTable with the specified event list and table
     * format.
     */
    public TransferTable(EventList<E> eventList, TableFormat<E> tableFormat) {
        this.model = new DefaultEventTableModel<E>(eventList, tableFormat);
        
        setModel(model);
        setShowGrid(true, false);      
        setEmptyRowsPainted(true);
        
        TableColors colors = new TableColors();
        setHighlighters(
                new ColorHighlighter(HighlightPredicate.EVEN, colors.evenColor,
                        colors.evenForeground, colors.selectionColor,
                        colors.selectionForeground),
                new ColorHighlighter(HighlightPredicate.ODD, colors.evenColor,
                        colors.evenForeground, colors.selectionColor,
                        colors.selectionForeground));
        
    }

    /**
     * Sets the column editor for the specified column index.
     */
    public void setColumnEditor(int column, TableCellEditor editor) {
        getColumnModel().getColumn(column).setCellEditor(editor);
    }

    /**
     * Sets the column renderer for the specified column index.
     */
    public void setColumnRenderer(int column, TableCellRenderer renderer) {
        getColumnModel().getColumn(column).setCellRenderer(renderer);
    }
    
    /**
     * Sets the column widths for the specified column index.
     */
    public void setColumnWidths(int index, int minWidth, int prefWidth, int maxWidth) {
        TableColumn column = getColumnModel().getColumn(index);
        column.setMinWidth(minWidth);
        column.setPreferredWidth(prefWidth);
        column.setMaxWidth(maxWidth);
    }

    /**
     * Returns the element at the specified table model row.
     */
    public E getElementAt(int index) {
        return model.getElementAt(index);
    }
    
    /**
     * Cell renderer for gap columns in the table.
     */
    public static class GapRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            return super.getTableCellRendererComponent(table, null, isSelected, false, row, column);
        }
    }
}
