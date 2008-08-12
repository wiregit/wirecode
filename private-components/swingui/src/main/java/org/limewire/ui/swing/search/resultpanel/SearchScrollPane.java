package org.limewire.ui.swing.search.resultpanel;

import java.awt.Component;

import javax.swing.JComponent;
import javax.swing.JScrollPane;

public class SearchScrollPane extends JScrollPane {
    
    public SearchScrollPane() {
        super();
        init();
    }

    public SearchScrollPane(Component view, int vsbPolicy, int hsbPolicy) {
        super(view, vsbPolicy, hsbPolicy);
        init();
    }

    public SearchScrollPane(Component view) {
        super(view);
        init();
    }

    public SearchScrollPane(int vsbPolicy, int hsbPolicy) {
        super(vsbPolicy, hsbPolicy);
        init();
    }

    public SearchScrollPane(JComponent component) {
        super(component);
        init();
    }
    
    private void init() {
        setBorder(null);
        setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    }

}
