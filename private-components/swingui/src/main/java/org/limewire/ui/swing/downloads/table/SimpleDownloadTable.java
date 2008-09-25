package org.limewire.ui.swing.downloads.table;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EventObject;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.limewire.core.api.Category;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.downloads.LimeProgressBar;
import org.limewire.ui.swing.util.GuiUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.gui.AdvancedTableFormat;
import ca.odell.glazedlists.gui.WritableTableFormat;

/**
 * Conventional table view of downloads. SimpleDownloadTable inherits popup and double
 * click handling from DownloadTable.
 */
public class SimpleDownloadTable extends DownloadTable {

    public SimpleDownloadTable(EventList<DownloadItem> downloadItems) {
        super(downloadItems);
        ((DownloadTableModel)getModel()).setTableFormat(new SimpleDownloadTableFormat());      
        setColumnControlVisible(true);
        setDefaultRenderer(DownloadItem.class, new ButtonRendererEditor(new DownloadActionHandler(downloadItems)));
        getColumnModel().getColumn(SimpleDownloadTableFormat.PERCENT).setCellRenderer(new PercentRenderer());
        getColumnModel().getColumn(SimpleDownloadTableFormat.CATEGORY).setCellRenderer(new CategoryRenderer());
        getColumnModel().getColumn(SimpleDownloadTableFormat.CURRENT_SIZE).setCellRenderer(new SizeRenderer());
        getColumnModel().getColumn(SimpleDownloadTableFormat.TOTAL_SIZE).setCellRenderer(new SizeRenderer());
        getColumnModel().getColumn(SimpleDownloadTableFormat.DOWNLOAD_SPEED).setCellRenderer(new SpeedRenderer());
        getColumnModel().getColumn(SimpleDownloadTableFormat.ACTIONS).setCellRenderer(new ButtonRendererEditor(null));
        getColumnModel().getColumn(SimpleDownloadTableFormat.ACTIONS).setCellEditor(new ButtonRendererEditor(new DownloadActionHandler(downloadItems)));
        setShowGrid(true, true);      
        setRowHeight(new PercentRenderer().getPreferredSize().height);
    }
    
    private boolean initialized = false;
    //overridden to pack columns
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (!initialized) {
            initialized = true;
            packAll();
        }
    }
    
    private static class SimpleDownloadTableFormat
    implements AdvancedTableFormat<DownloadItem>, WritableTableFormat<DownloadItem> {
        private String[] columns = new String[] {"Category", "Title", "State", "Percent", 
                "Current Size", "Total Size", "DownloadSpeed", "Actions"};
        private static final int CATEGORY = 0;
        private static final int TITLE = CATEGORY + 1;
        private static final int STATE = TITLE + 1;
        private static final int PERCENT = STATE + 1;
        private static final int CURRENT_SIZE = PERCENT + 1;
        private static final int TOTAL_SIZE = CURRENT_SIZE + 1;
        private static final int DOWNLOAD_SPEED = TOTAL_SIZE + 1;
        private static final int ACTIONS = DOWNLOAD_SPEED + 1;
        

        public int getColumnCount() {
            return columns.length;
        }

        public String getColumnName(int column) {
            if(column < 0 || column >= columns.length)
                throw new IllegalStateException("Column "+ column + " out of bounds");

            return columns[column];
        }

        @Override
        public Object getColumnValue(DownloadItem baseObject, int column) {
            switch (column) {
            case CATEGORY:
                return baseObject.getCategory();
            case TITLE:
                return baseObject.getTitle();
            case STATE:
                return baseObject.getState();
            case PERCENT:
                return baseObject.getPercentComplete();
            case CURRENT_SIZE:
                return baseObject.getCurrentSize();
            case TOTAL_SIZE:
                return baseObject.getTotalSize();
            case DOWNLOAD_SPEED:
                return baseObject.getDownloadSpeed();
            case ACTIONS:
                return baseObject;

            }
            
            throw new IllegalStateException("Column "+ column + " out of bounds");
        }

        @Override
        public Comparator getColumnComparator(int column) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean isEditable(DownloadItem baseObject, int column) {
            if(column < 0 || column >= columns.length)
                throw new IllegalStateException("Column "+ column + " out of bounds");
            return true;
        }

        @Override
        public DownloadItem setColumnValue(DownloadItem baseObject, Object editedValue, int column) {
            if(column < 0 || column >= columns.length)
                throw new IllegalStateException("Column "+ column + " out of bounds");
            return baseObject;
        }

        @Override
        public Class getColumnClass(int column) {
            switch (column) {
            case CATEGORY:
                return Category.class;
            case TITLE:
                return String.class;
            case STATE:
                return DownloadState.class;
            case PERCENT:
                return Integer.class;
            case CURRENT_SIZE:
                return Long.class;
            case TOTAL_SIZE:
                return Long.class;
            case DOWNLOAD_SPEED:
                return Float.class;
            case ACTIONS:
                return DownloadItem.class;

            }
            
            throw new IllegalStateException("Column "+ column + " out of bounds");
        }

    }
    
    private static class PercentRenderer extends JPanel implements TableCellRenderer{
        JProgressBar progressBar;
        public PercentRenderer(){
            progressBar = new LimeProgressBar(0, 100);
            add(progressBar);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            if(value instanceof Number){
                progressBar.setValue(((Number)value).intValue());
                return this;
            }
            throw new IllegalArgumentException("Value must be a number: "+ value.getClass());
        }
        
    }
    
    private static class CategoryRenderer extends JPanel implements TableCellRenderer {        
        private CategoryIconLabel label = new CategoryIconLabel(CategoryIconLabel.Size.SMALL);
        public CategoryRenderer(){
            add(label);
        }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            if (value instanceof Category) {
                label.setIcon((Category) value);
                return this;
            }
            throw new IllegalArgumentException("Value must be a Category: " + value.getClass());
        }
        
    }
    
    private static class SpeedRenderer extends JLabel implements TableCellRenderer {
        
        public SpeedRenderer(){
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setText(GuiUtils.rate2speed((Float)value));
            return this;
        }
    }
    
    private static class SizeRenderer extends JLabel implements TableCellRenderer {
        
        public SizeRenderer(){
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setText(GuiUtils.toUnitbytes((Long)value));
            return this;
        }
        
    }
    
    private static class ButtonRendererEditor extends JPanel implements TableCellRenderer, TableCellEditor {
        private final List<CellEditorListener> listeners = new ArrayList<CellEditorListener>();
        private DownloadItem editItem;
        private DownloadActionHandler downloadActionHandler;
        private DownloadButtonPanel buttonPanel;

        /**
         * @param downloadActionHandler can be null for renderer since the
         *        renderer will not receive events.
         */
        public ButtonRendererEditor(final DownloadActionHandler downloadActionHandler) {
            super(new BorderLayout());
            this.downloadActionHandler = downloadActionHandler;
            
            ActionListener listener = downloadActionHandler == null ? null : new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    handleAction(e);
                }
            };
            
            buttonPanel = new DownloadButtonPanel(listener);
            buttonPanel.setOpaque(false);
            add(buttonPanel);
        }
        
        private void handleAction(ActionEvent e){
            downloadActionHandler.performAction(e.getActionCommand(), editItem);
            cancelCellEditing();
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            buttonPanel.updateButtons(((DownloadItem)value).getState());
            return this;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            editItem = (DownloadItem) value;
            buttonPanel.updateButtons(editItem.getState());
            return this;
        }

        @Override
        public final void addCellEditorListener(CellEditorListener lis) {
            synchronized (listeners) {
                if (!listeners.contains(lis))
                    listeners.add(lis);
            }
        }

        @Override
        public final void cancelCellEditing() {
            synchronized (listeners) {
                for (int i = 0, N = listeners.size(); i < N; i++) {
                    listeners.get(i).editingCanceled(new ChangeEvent(this));
                }
            }
        }

        @Override
        public final Object getCellEditorValue() {
            return null;
        }

        @Override
        public boolean isCellEditable(EventObject e) {
            return true;
        }

        @Override
        public final void removeCellEditorListener(CellEditorListener lis) {
            synchronized (listeners) {
                if (listeners.contains(lis))
                    listeners.remove(lis);
            }
        }

        @Override
        public final boolean shouldSelectCell(EventObject e) {
            return true;
        }

        @Override
        public final boolean stopCellEditing() {
            synchronized (listeners) {
                for (int i = 0, N = listeners.size(); i < N; i++) {
                    listeners.get(i).editingStopped(new ChangeEvent(this));
                }
            }
            return true;
        }
    }
}