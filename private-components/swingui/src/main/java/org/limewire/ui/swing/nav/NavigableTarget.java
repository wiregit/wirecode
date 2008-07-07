package org.limewire.ui.swing.nav;

import javax.swing.JComponent;

public interface NavigableTarget {
    
    void showNavigablePanel(String targetKey);
    
    void addNavigablePanel(String key, JComponent panel);
    
    void removeNavigablePanel(String key);

}
