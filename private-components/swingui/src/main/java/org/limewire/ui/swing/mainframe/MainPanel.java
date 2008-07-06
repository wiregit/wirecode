package org.limewire.ui.swing.mainframe;

import java.awt.CardLayout;

import javax.swing.JPanel;

import org.limewire.ui.swing.library.DocumentPanel;
import org.limewire.ui.swing.library.ImagePanel;
import org.limewire.ui.swing.library.MusicPanel;
import org.limewire.ui.swing.library.VideoPanel;
import org.limewire.ui.swing.nav.NavigableTarget;

public class MainPanel extends JPanel implements NavigableTarget {

    private final CardLayout cardLayout;

    public MainPanel() {
        cardLayout = new CardLayout();
        setLayout(cardLayout);
        add(new HomePanel(), HomePanel.NAME);
        add(new StorePanel(), StorePanel.NAME);
        add(new DocumentPanel(), DocumentPanel.NAME);
        add(new ImagePanel(), ImagePanel.NAME);
        add(new MusicPanel(), MusicPanel.NAME);
        add(new VideoPanel(), VideoPanel.NAME);
        // add(new JLabel("main"));
        // setBackground(Color.YELLOW);
    }

    public void showNavigablePanel(String key) {
        cardLayout.show(this, key);
    }
}
