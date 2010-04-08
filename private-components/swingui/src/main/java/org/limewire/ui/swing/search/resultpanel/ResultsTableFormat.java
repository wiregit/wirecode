package org.limewire.ui.swing.search.resultpanel;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.jdesktop.swingx.decorator.SortKey;
import org.jdesktop.swingx.decorator.SortOrder;
import org.limewire.ui.swing.search.model.BasicDownloadState;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.settings.TablesHandler;
import org.limewire.ui.swing.table.AbstractColumnStateFormat;
import org.limewire.ui.swing.table.ColumnStateInfo;
import org.limewire.ui.swing.table.QualityComparator;
import org.limewire.ui.swing.util.EventListTableSortFormat;

/**
 * This class is the base class for each of the TableFormat classes
 * that describe the various table views of search results.
 */
public abstract class ResultsTableFormat<T> extends AbstractColumnStateFormat<T> 
implements EventListTableSortFormat {

    protected VisualSearchResult vsr;
    private final int nameColumn;
    private final int relevanceColumn;
    private final int spamColumn;

    private final String sortID;

    /**
     * Constructs a ResultsTableFormat with the specified array of column
     * descriptors.
     */
    public ResultsTableFormat(ColumnStateInfo... columnInfo) {
        this("", -1, -1, -1, columnInfo);
    }

    /**
     * Constructs a ResultsTableFormat with the specified sort identifier,
     * Name/Relevance/Spam column indices, and array of column descriptors.
     */
    public ResultsTableFormat(String sortID, int nameColumn, int relevanceColumn, 
            int spamColumn, ColumnStateInfo... columnInfo) {
        super(columnInfo);
        this.sortID = sortID;
        this.nameColumn = nameColumn;
        this.relevanceColumn = relevanceColumn;
        this.spamColumn = spamColumn;
    }

    @Override
    public Class getColumnClass(int index) {
        return String.class;
    }

    public VisualSearchResult setColumnValue(
            VisualSearchResult vsr, Object value, int index) {
        // do nothing with the new value
        return vsr;
    }

    public boolean isEditable(VisualSearchResult vsr, int column) {
        return false;
    }

    public int getNameColumn() {
        return nameColumn;
    }

    @Override
    public List<SortKey> getPreSortColumns() {
        return Arrays.asList(new SortKey(SortOrder.ASCENDING, spamColumn));
    }

    @Override
    public boolean getSortOrder() {// always descending for search results
        return false;
    }

    @Override
    public String getSortOrderID() {
        return sortID;
    }

    @Override
    public int getSortedColumn() { // always from column for search results
        return relevanceColumn;
    }

    @Override
    public List<SortKey> getDefaultSortKeys() {
        return Arrays.asList(
                new SortKey(((TablesHandler.getSortedOrder(getSortOrderID(), getSortOrder()).getValue() == true) ?
                        SortOrder.ASCENDING : SortOrder.DESCENDING ),
                        TablesHandler.getSortedColumn(getSortOrderID(), getSortedColumn()).getValue()));
    }

    /**
     * If the FromColumn is sorted, use a custom column sorter
     * otherwise it is assumed the column returns a value that 
     * implements the Comparable interface.
     */
    @Override
    public Comparator getColumnComparator(int index) {
        if(index == relevanceColumn) 
            return getRelevanceComparator();
        else if(index == nameColumn) 
            return getNameComparator();
        else if(index == spamColumn)
            return getSpamComparator();
        else
            return getLimeComparator();
    }

    /**
     * Returns a comparator for the relevance column.
     */
    public Comparator getRelevanceComparator() {
        return new RelevanceComparator();
    }

    /**
     * Returns a comparator for the Name column.
     */
    public Comparator getNameComparator() {
        return new NameComparator(false);
    }

    /**
     * Returns a comparator for the Quality column.
     */
    public Comparator getQualityComparator() {
        return new QualityComparator();
    }

    /**
     * Returns a comparator for the Spam column.
     */
    public Comparator getSpamComparator() {
        return new IsSpamComparator();
    }

    /**
     * Compares the relevance.
     */
    public static class RelevanceComparator implements Comparator<VisualSearchResult> {
        @Override
        public int compare(VisualSearchResult o1, VisualSearchResult o2) {
            return Float.compare(o1.getRelevance(), o2.getRelevance());
        }
    }

    /**
     * Compares the name column. This is essentially a string compare but
     * VSR are returned in this column to also display an icon so we need
     * a custom comparator.
     */
    public static class NameComparator implements Comparator<VisualSearchResult> {
        private final boolean useAudioArtist;

        public NameComparator(boolean useAudioArtist) {
            this.useAudioArtist = useAudioArtist;
        }

        @Override
        public int compare(VisualSearchResult o1, VisualSearchResult o2) {
            String name1 = getName(o1);
            String name2 = getName(o2);

            return name1.compareToIgnoreCase(name2);
        }

        private String getName(VisualSearchResult result) {
            return result.getNameProperty(useAudioArtist);
        }
    }

    /**
     * Compares the Spam Column. This column is never displayed to the user;
     * it's used to push spam results and previously downloaded files to the
     * bottom in table view.
     */
    public static class IsSpamComparator implements Comparator<VisualSearchResult> {
        @Override
        public int compare(VisualSearchResult o1, VisualSearchResult o2) {
            boolean spam1 = o1.isSpam();
            boolean spam2 = o2.isSpam();

            if(!spam1 && spam2)
                return -1;
            if(spam1 && !spam2)
                return 1;

            boolean downloaded1 = o1.getDownloadState() == BasicDownloadState.LIBRARY;
            boolean downloaded2 = o2.getDownloadState() == BasicDownloadState.LIBRARY;

            if(!downloaded1 && downloaded2)
                return -1;
            if(downloaded1 && !downloaded2)
                return 1;

            return 0;
        }
    }
}