package org.limewire.ui.swing.mainframe;

import java.awt.CardLayout;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.limewire.ui.swing.nav.NavigableTarget;

class MainPanel extends JPanel implements NavigableTarget {

    private final Map<String, JComponent> keyToComponents = new HashMap<String, JComponent>();
    private final CardLayout cardLayout;

    public MainPanel() {
        cardLayout = new CardLayout();
        setLayout(cardLayout);
    }

    public void showNavigablePanel(Object key) {
        cardLayout.show(this, asString(key));
        keyToComponents.get(asString(key)).requestFocusInWindow();
    }
    
    @Override
    public void addNavigablePanel(Object key, JComponent panel) {
        keyToComponents.put(asString(key), panel);
        add(panel, asString(key));
    }
    
    @Override
    public void removeNavigablePanel(Object key) {
        remove(keyToComponents.remove(asString(key)));
    }
    
    private String asString(Object key) {
        return System.identityHashCode(key) + "";
    }
}
