package org.limewire.ui.swing.search.resultpanel;

import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * This class is a panel containing three "action" buttons
 * for the actions "download", "more info" and "mark as junk".
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class ActionButtonPanel extends JPanel {

    private static final String[] TOOLTIPS =
        { "Download", "More Info", "Mark as Junk" };
    
    private static final int HGAP = 10;    
    private static final int VGAP = 0;    

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
    private JToggleButton junkButton = new JToggleButton();
    private int height;

    public ActionButtonPanel() {
        // Cause the @Resource fields to be injected
        // using properties in AppFrame.properties.
        // The icon PNG file is in swingui/src/main/resources/
        // org/limewire/ui/swing/mainframe/resources/icons.
        GuiUtils.assignResources(this);
        
        icons = new Icon[][] {
            { downloadUpIcon, downloadOverIcon, downloadDownIcon },
            { infoUpIcon, infoOverIcon, infoDownIcon },
            { junkUpIcon, junkOverIcon, junkDownIcon }
        };
        calculateHeight();

        setLayout(new FlowLayout(FlowLayout.CENTER, HGAP, VGAP));
        createButtons();
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

            AbstractButton button =
                buttonIndex == MARK_AS_JUNK ? junkButton : new JButton();
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
}