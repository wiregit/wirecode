package org.limewire.ui.swing.upload.table;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.limewire.ui.swing.table.MouseableTable;
import org.limewire.ui.swing.table.TableColors;
import org.limewire.ui.swing.util.GuiUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.DefaultEventTableModel;

/**
 * Base class for transfer tables like the Uploads table.
 */
public abstract class TransferTable<E> extends MouseableTable {

    private final DefaultEventTableModel<E> model;
    private final TransferTableResources resources;
    
    /**
     * Constructs a new TransferTable with the specified table format.
     */
    public TransferTable(EventList<E> eventList, TransferTableFormat<E> tableFormat) {
        this.model = new DefaultEventTableModel<E>(eventList, tableFormat);
        this.resources = new TransferTableResources();
        
        setModel(model);
        setShowGrid(true, false);      
        setEmptyRowsPainted(true);
        setRowHeight(resources.rowHeight);
        
        TableColors colors = new TableColors();
        setHighlighters(
                new ColorHighlighter(HighlightPredicate.EVEN, colors.evenColor,
                        colors.evenForeground, colors.selectionColor,
                        colors.selectionForeground),
                new ColorHighlighter(HighlightPredicate.ODD, colors.evenColor,
                        colors.evenForeground, colors.selectionColor,
                        colors.selectionForeground));
        
        initializeColumns();
    }
    
    /**
     * Initializes the columns in the table.
     */
    private void initializeColumns() {
        setColumnWidths(TransferTableFormat.TITLE_COL, resources.titleMinWidth, resources.titlePrefWidth, resources.titleMaxWidth);
        setColumnWidths(TransferTableFormat.TITLE_GAP, resources.gapMinWidth, resources.gapPrefWidth, resources.gapMaxWidth);
        setColumnWidths(TransferTableFormat.PROGRESS_COL, resources.progressMinWidth, resources.progressPrefWidth, resources.progressMaxWidth);
        setColumnWidths(TransferTableFormat.PROGRESS_GAP, resources.gapMinWidth, resources.gapPrefWidth, resources.gapMaxWidth);
        setColumnWidths(TransferTableFormat.MESSAGE_COL, resources.messageMinWidth, resources.messagePrefWidth, resources.messageMaxWidth);
        setColumnWidths(TransferTableFormat.MESSAGE_GAP, resources.gapMinWidth, resources.gapPrefWidth, resources.gapMaxWidth);
        setColumnWidths(TransferTableFormat.ACTION_COL, resources.actionMinWidth, resources.actionPrefWidth, resources.actionMaxWidth);
        setColumnWidths(TransferTableFormat.ACTION_GAP, resources.gapMinWidth, resources.gapPrefWidth, resources.gapMaxWidth);
        setColumnWidths(TransferTableFormat.CANCEL_COL, resources.cancelMinWidth, resources.cancelPrefWidth, resources.cancelMaxWidth);
        
        // Set gap column renderers.
        TableCellRenderer gapRenderer = new GapRenderer();
        setColumnRenderer(TransferTableFormat.TITLE_GAP, gapRenderer);
        setColumnRenderer(TransferTableFormat.PROGRESS_GAP, gapRenderer);
        setColumnRenderer(TransferTableFormat.MESSAGE_GAP, gapRenderer);
        setColumnRenderer(TransferTableFormat.ACTION_GAP, gapRenderer);
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
    private void setColumnWidths(int index, int minWidth, int prefWidth, int maxWidth) {
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
     * Resources for the transfer table.
     */
    private static class TransferTableResources {
        @Resource(key="DownloadTable.rowHeight") public int rowHeight;  
        @Resource(key="DownloadTable.gapMinWidth") public int gapMinWidth;  
        @Resource(key="DownloadTable.gapPrefWidth") public int gapPrefWidth; 
        @Resource(key="DownloadTable.gapMaxWidth") public int gapMaxWidth; 
        @Resource(key="DownloadTable.titleMinWidth") public int titleMinWidth;  
        @Resource(key="DownloadTable.titlePrefWidth") public int titlePrefWidth; 
        @Resource(key="DownloadTable.titleMaxWidth") public int titleMaxWidth;  
        @Resource(key="DownloadTable.progressMinWidth") public int progressMinWidth;
        @Resource(key="DownloadTable.progressPrefWidth") public int progressPrefWidth; 
        @Resource(key="DownloadTable.progressMaxWidth") public int progressMaxWidth;  
        @Resource(key="DownloadTable.messageMinWidth") public int messageMinWidth; 
        @Resource(key="DownloadTable.messagePrefWidth") public int messagePrefWidth; 
        @Resource(key="DownloadTable.messageMaxWidth") public int messageMaxWidth;  
        @Resource(key="DownloadTable.actionMinWidth") public int actionMinWidth; 
        @Resource(key="DownloadTable.actionPrefWidth") public int actionPrefWidth;  
        @Resource(key="DownloadTable.actionMaxWidth") public int actionMaxWidth; 
        @Resource(key="DownloadTable.cancelMinWidth") public int cancelMinWidth; 
        @Resource(key="DownloadTable.cancelPrefWidth") public int cancelPrefWidth;
        @Resource(key="DownloadTable.cancelMaxWidth") public int cancelMaxWidth;
        
        TransferTableResources() {
            GuiUtils.assignResources(this);
        }
    }
    
    /**
     * Cell renderer for gap columns in the table.
     */
    private static class GapRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            return super.getTableCellRendererComponent(table, null, isSelected, false, row, column);
        }
    }
}
