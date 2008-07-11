package org.limewire.ui.swing.search.resultpanel;

import javax.swing.JComponent;
import javax.swing.JScrollPane;

public class SearchScrollPane extends JScrollPane {
    
    public SearchScrollPane(JComponent component) {
        super(component);
        setOpaque(false);
        getViewport().setOpaque(false);
        setBorder(null);
        setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    }

}
