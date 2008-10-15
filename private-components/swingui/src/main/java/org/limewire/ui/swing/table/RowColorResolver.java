package org.limewire.ui.swing.table;

import java.awt.Color;

public interface RowColorResolver<E> {
    /**
     * Determine background color for the supplied item. This is meant for use
     * in a stripe-highlighted table that alternates background colors for rows. 
     * @param item
     * @return
     */
    Color getColorForItemRow(E item);
}
