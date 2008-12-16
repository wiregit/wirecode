package org.limewire.ui.swing.search.resultpanel;

import java.util.Comparator;

import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.table.AbstractColumnStateFormat;
import org.limewire.ui.swing.table.ColumnStateInfo;

import ca.odell.glazedlists.GlazedLists;


/**
 * This class is the base class for each of the TableFormat classes
 * that describe the various table views of search results.
 */
public abstract class ResultsTableFormat<T> extends AbstractColumnStateFormat<T> {

    protected VisualSearchResult vsr;
    private final int nameColumn;
    private final int fromColumn;

    public ResultsTableFormat(ColumnStateInfo... columnInfo) {
        this(-1, -1, columnInfo);
    }
    
    public ResultsTableFormat(int nameColumn, int fromColumn, ColumnStateInfo... columnInfo) {
        super(columnInfo);
        this.nameColumn = nameColumn;
        this.fromColumn = fromColumn;
    }
    
    @Override
    public Class getColumnClass(int index) {
        return String.class;
    }
    
    @Override
    public boolean isColumnHideable(int column) {
        return true;
    }

    public VisualSearchResult setColumnValue(
        VisualSearchResult vsr, Object value, int index) {
        // do nothing with the new value
        return vsr;
    }
    
    public boolean isEditable(VisualSearchResult vsr, int column) {
        return column == fromColumn;
    }

    public int getNameColumn() {
        return nameColumn;
    }

    /**
     * If the FromColumn is sorted, use a custom column sorter
     * otherwise it is assumed the column returns a value that 
     * implements the Comparable interface
     */
    @Override
    public Comparator getColumnComparator(int index) {
        if(index == fromColumn) 
            return getFromComparator();
        else
            return GlazedLists.comparableComparator();
    }
    
    public FromComparator getFromComparator() {
        return new FromComparator();
    }
    
    /**
     * Compares the number of files being shared. 
     */
    public static class FromComparator implements Comparator<VisualSearchResult> {
        @Override
        public int compare(VisualSearchResult o1, VisualSearchResult o2) {
            int size1 = o1.getSources().size();
            int size2 = o2.getSources().size();
            
            if(size1 == size2)
                return 0;
            else if(size1 > size2)
                return 1;
            else 
                return -1;
        }
    }

}