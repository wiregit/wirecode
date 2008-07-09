package org.limewire.ui.swing.nav;

import javax.swing.JComponent;

public interface NavigableTarget {
    
    void showNavigablePanel(Object targetKey);
    
    void addNavigablePanel(Object key, JComponent panel);
    
    void removeNavigablePanel(Object key);

}
