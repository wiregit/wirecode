package org.limewire.ui.swing.components;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.SwingConstants;

import org.limewire.ui.swing.util.GuiUtils;

public class IconButton extends JButton {

    public IconButton(Action a) {
        super(a);
        init();
    }

    public IconButton(Icon icon) {
        super(icon);
        init();
    }

    public IconButton(String text, Icon icon) {
        super(text, icon);
        init();
    }
    
    private void init() {
        setBorder(null);
        setContentAreaFilled(false);
        setOpaque(false);
        setHorizontalTextPosition(SwingConstants.CENTER);
        setVerticalTextPosition(SwingConstants.BOTTOM);
        setIconTextGap(2);
        addMouseListener(GuiUtils.getActionHandListener(null));
    }
    

}
