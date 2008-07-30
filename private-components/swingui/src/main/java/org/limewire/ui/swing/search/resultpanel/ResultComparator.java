package org.limewire.ui.swing.search.resultpanel;

import java.util.Comparator;

import org.limewire.ui.swing.search.model.VisualSearchResult;

/**
 * This class compares VisualSearchResult objects by their descriptions.
 * 
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class ResultComparator implements Comparator {

    @Override
    public int compare(Object obj1, Object obj2) {
        VisualSearchResult vsr1 = (VisualSearchResult) obj1;
        VisualSearchResult vsr2 = (VisualSearchResult) obj2;
        return vsr1.getDescription().compareTo(vsr2.getDescription());
    }
}
