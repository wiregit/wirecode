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

        if (quality1 instanceof Number) {
            if (quality2 instanceof Number) {
                int q1 = ((Number) quality1).intValue();
                int q2 = ((Number) quality2).intValue();               
                return (q1 == q2)? 0 : (q1 < q2)? -1 : 1;
            } else {
                return 1;
            }
        } else {
            return (quality2 instanceof Number) ? -1 : 0;
        }
    }
}
