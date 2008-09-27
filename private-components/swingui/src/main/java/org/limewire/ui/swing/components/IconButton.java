package org.limewire.ui.swing.components;

import java.awt.Insets;

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
    
    public IconButton(Icon icon, Icon rolloverIcon, Icon pressedIcon) {
        super(icon);
        init();
        setRolloverIcon(rolloverIcon);
        setPressedIcon(pressedIcon);
    }
    
    private void init() {
        setMargin(new Insets(0, 0, 0, 0));
        setBorderPainted(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setRolloverEnabled(true);
        setHideActionText(true);
        setBorder(null);
        setOpaque(false);
        setHorizontalTextPosition(SwingConstants.CENTER);
        setVerticalTextPosition(SwingConstants.BOTTOM);
        setIconTextGap(2);
        addMouseListener(GuiUtils.getActionHandListener(null));
    }
    

}
