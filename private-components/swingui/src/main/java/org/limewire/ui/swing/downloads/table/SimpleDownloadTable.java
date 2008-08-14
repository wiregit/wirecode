package org.limewire.ui.swing.downloads.table;

import java.awt.Component;
import java.util.Comparator;

import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.table.MouseableTable;
import org.limewire.ui.swing.util.GuiUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.gui.AdvancedTableFormat;
import ca.odell.glazedlists.gui.WritableTableFormat;

public class SimpleDownloadTable extends MouseableTable {

    public SimpleDownloadTable(EventList<DownloadItem> downloadItems) {
        super(new DownloadTableModel(downloadItems));
        ((DownloadTableModel)getModel()).setTableFormat(new SimpleDownloadTableFormat());
        GuiUtils.assignResources(this);
        setColumnControlVisible(true);
        getColumnModel().getColumn(SimpleDownloadTableFormat.PERCENT).setCellRenderer(new PercentRenderer());
    }


    
    private static class SimpleDownloadTableFormat implements AdvancedTableFormat<DownloadItem>, WritableTableFormat<DownloadItem> {
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
                return DownloadItem.Category.class;
            case TITLE:
                return String.class;
            case STATE:
                return DownloadState.class;
            case PERCENT:
                return Integer.class;
            case CURRENT_SIZE:
                return Integer.class;
            case TOTAL_SIZE:
                return Integer.class;
            case DOWNLOAD_SPEED:
                return Float.class;
            case ACTIONS:
                return DownloadItem.class;

            }
            
            throw new IllegalStateException("Column "+ column + " out of bounds");
        }

    }
    
    private class PercentRenderer extends JProgressBar implements TableCellRenderer{
        
        public PercentRenderer(){
            super(0, 100);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            if(value instanceof Number){
                setValue(((Number)value).intValue());
                return this;
            }
            throw new IllegalArgumentException("Value must be a number: "+ value.getClass());
        }
        
    }

}
