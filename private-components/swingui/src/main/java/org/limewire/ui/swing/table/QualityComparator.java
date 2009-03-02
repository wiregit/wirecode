package org.limewire.ui.swing.table;

import java.util.Comparator;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.PropertiableFile;

/**
 * Compares the quality value for a pair of PropertiableFile objects.
 */
public class QualityComparator implements Comparator<PropertiableFile> {
    @Override
    public int compare(PropertiableFile o1, PropertiableFile o2) {
        Object quality1 = o1.getProperty(FilePropertyKey.QUALITY);
        Object quality2 = o2.getProperty(FilePropertyKey.QUALITY);

        if (quality1 instanceof Long) {
            if (quality2 instanceof Long) {
                return ((Long) quality1).compareTo((Long) quality2);
            } else {
                return 1;
            }
        } else {
            return (quality2 instanceof Long) ? -1 : 0;
        }
    }
}
