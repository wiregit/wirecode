package org.limewire.ui.swing.search.resultpanel;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JToggleButton;
import javax.swing.ToolTipManager;
import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * This class is a panel containing three "action" buttons
 * for the actions "download", "more info" and "mark as junk".
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class ActionButtonPanel extends JXPanel {

//    private static final Point toolTipOffset = new Point(0, 25);
    private static final String[] TOOLTIPS =
        { "Download", "More Info", "Mark as Junk" };
    
    private static final int HGAP = 10;    
    private static final int VGAP = 0;    

    private static final int DOWNLOAD = 0;    
    private static final int MORE_INFO = 1;    
    private static final int MARK_AS_JUNK = 2;    

    // The icons displayed in the action column,
    // supplied by the call to GuiUtils.assignResources().
    @Resource private Icon downloadDownIcon;
    @Resource private Icon downloadOverIcon;
    @Resource private Icon downloadUpIcon;
    @Resource private Icon infoDownIcon;
    @Resource private Icon infoOverIcon;
    @Resource private Icon infoUpIcon;
    @Resource private Icon junkDownIcon;
    @Resource private Icon junkOverIcon;
    @Resource private Icon junkUpIcon;

    private Icon[][] icons;
    private JButton downloadButton;
    private JButton infoButton;
    private JToggleButton junkButton;
    private int height;

    public ActionButtonPanel() {
        // Cause the @Resource fields to be injected
        // using properties in AppFrame.properties.
        // The icon PNG file is in swingui/src/main/resources/
        // org/limewire/ui/swing/mainframe/resources/icons.
        GuiUtils.assignResources(this);

        downloadButton = new JButton() {
            @Override
            public Point getToolTipLocation(MouseEvent e) {
                return getToolTipOffset(this);
            }
        };
        
        infoButton = new JButton() {
            @Override
            public Point getToolTipLocation(MouseEvent e) {
                return getToolTipOffset(this);
            }
        };
        
        junkButton = new JToggleButton() {
            @Override
            public Point getToolTipLocation(MouseEvent e) {
                return getToolTipOffset(this);
            }
        };
        
        icons = new Icon[][] {
            { downloadUpIcon, downloadOverIcon, downloadDownIcon },
            { infoUpIcon, infoOverIcon, infoDownIcon },
            { junkUpIcon, junkOverIcon, junkDownIcon }
        };
        calculateHeight();

        setLayout(new FlowLayout(FlowLayout.CENTER, HGAP, VGAP));
        createButtons();

        // Set the tooltip delay to zero only when the mouse is over this panel.
        addMouseListener(new MouseAdapter() {
            private int delay;

            @Override
            public void mouseEntered(MouseEvent e) {
                // Save the previous delay.
                delay = ToolTipManager.sharedInstance().getInitialDelay();
                ToolTipManager.sharedInstance().setInitialDelay(0);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // Restore the previous delay.
                ToolTipManager.sharedInstance().setInitialDelay(delay);
            }
        });
    }

    private void calculateHeight() {
        for (Icon[] iconSet : icons) {
            Icon upIcon = iconSet[0];
            height = Math.max(height, upIcon.getIconHeight());
        }
    }

    private void createButtons() {
        int buttonIndex = 0;

        for (Icon[] iconSet : icons) {
            Icon upIcon = iconSet[0];
            Icon overIcon = iconSet[1];
            Icon downIcon = iconSet[2];

            AbstractButton button = getButton(buttonIndex);
            if (button == null) continue; // should never happen

            button.setIcon(upIcon);
            button.setRolloverIcon(overIcon);
            button.setPressedIcon(downIcon);
            button.setSelectedIcon(downIcon);

            button.setToolTipText(TOOLTIPS[buttonIndex]);

            button.setBorderPainted(false);
            button.setContentAreaFilled(false);

            Dimension size =
                new Dimension(upIcon.getIconWidth(), upIcon.getIconHeight());
            button.setPreferredSize(size);

            add(button);

            ++buttonIndex;
        }
    }

    private AbstractButton getButton(int buttonIndex) {
        return buttonIndex == DOWNLOAD ? downloadButton :
            buttonIndex == MORE_INFO ? infoButton :
            buttonIndex == MARK_AS_JUNK ? junkButton : null;
    }

    /**
     * Gets the height of the tallest button icon.
     * @return the height
     */
    public int getIconHeight() {
        return height;
    }

    /**
     * Gets the "Mark as Junk" button.
     * @return the button
     */
    public JToggleButton getJunkButton() {
        return junkButton;
    }

    /**
     * Gets the rollover icon of the button at a given index.
     * @param buttonIndex the button index
     * @return the rollover icon
     */
    private Icon getRolloverIcon(int buttonIndex) {
        return buttonIndex == DOWNLOAD ? downloadOverIcon :
            buttonIndex == MORE_INFO ? infoOverIcon :
            buttonIndex == MARK_AS_JUNK ? junkOverIcon : null;
    }

    private Point getToolTipOffset(JComponent component) {
        // Determine the size of the tooltip.
        Font font = component.getFont();
        FontMetrics fm = component.getFontMetrics(font);
        String text = component.getToolTipText();
        int toolTipWidth = fm.stringWidth(text);
        int toolTipHeight = fm.getHeight();

        Dimension componentSize = component.getSize();

        int x = componentSize.width/2 - toolTipWidth/2 + 1;
        int y = -toolTipHeight - 3;

        return new Point(x, y);
    }

    /**
     * Gets the up icon of the button at a given index.
     * @param buttonIndex the button index
     * @return the up icon
     */
    private Icon getUpIcon(int buttonIndex) {
        return buttonIndex == DOWNLOAD ? downloadUpIcon :
            buttonIndex == MORE_INFO ? infoUpIcon :
            buttonIndex == MARK_AS_JUNK ? junkUpIcon : null;
    }

    @Override
    public void setBackground(Color bg) {
        super.setBackground(bg);
        
        Component[] components = getComponents();
        for (Component component : components) {
            component.setBackground(bg);
        }
    }

    /**
     * Changes the "normal" icon of the specified button
     * based on whether it should simulate a mouse rollover.
     * @param buttonIndex the button index
     * @param rollover true to simulate a rollover; false otherwise
     */
    public void setRollover(int buttonIndex, boolean rollover) {
        AbstractButton button = getButton(buttonIndex);
        Icon icon = rollover ?
            getRolloverIcon(buttonIndex) : getUpIcon(buttonIndex);
        button.setIcon(icon);
    }
}