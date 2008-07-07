package org.limewire.ui.swing.mainframe;

import java.awt.CardLayout;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.limewire.ui.swing.nav.NavigableTarget;

public class MainPanel extends JPanel implements NavigableTarget {

    private final Map<String, JComponent> keyToComponents = new HashMap<String, JComponent>();
    private final CardLayout cardLayout;

    public MainPanel() {
        cardLayout = new CardLayout();
        setLayout(cardLayout);
    }

    public void showNavigablePanel(String key) {
        cardLayout.show(this, key);
        keyToComponents.get(key).requestFocusInWindow();
    }
    
    @Override
    public void addNavigablePanel(String key, JComponent panel) {
        keyToComponents.put(key, panel);
        add(panel, key);
    }
    
    @Override
    public void removeNavigablePanel(String key) {
        remove(keyToComponents.remove(key));
    }
}
