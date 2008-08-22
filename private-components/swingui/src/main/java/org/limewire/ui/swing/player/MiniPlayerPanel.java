package org.limewire.ui.swing.player;

import java.awt.Color;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JToggleButton;
import javax.swing.JPanel;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.HorizontalLayout;
import org.limewire.ui.swing.util.GuiUtils;

public class MiniPlayerPanel extends JPanel {

    @Resource
    private Icon pauseIcon;
    @Resource
    private Icon pauseIconPressed;
    @Resource
    private Icon pauseIconRollover;
    @Resource
    private Icon playIcon;
    @Resource
    private Icon playIconPressed;
    @Resource
    private Icon playIconRollover;

    private JToggleButton playPauseButton;

    private JButton statusButton;

    public MiniPlayerPanel() {
        super(new HorizontalLayout());
        GuiUtils.assignResources(this);
        
        setOpaque(false);

        // TODO: all icon JToggleButton
        playPauseButton = new JToggleButton();
        playPauseButton.setSelected(false);
        playPauseButton.setMargin(new Insets(0, 0, 0, 0));
        playPauseButton.setBorderPainted(false);
        playPauseButton.setContentAreaFilled(false);
        playPauseButton.setFocusPainted(false);
        playPauseButton.setRolloverEnabled(true);
        playPauseButton.setIcon(playIcon);
        playPauseButton.setRolloverIcon(playIconRollover);
        playPauseButton.setPressedIcon(playIconPressed);
        playPauseButton.setHideActionText(true);
        playPauseButton.setSelectedIcon(pauseIcon);

        statusButton = new JButton("Clean Your Room - The Your Moms");
        statusButton.addActionListener(new ShowPlayerListener());

        add(playPauseButton);
        add(statusButton);

        setMaximumSize(getPreferredSize());
    }

    private class ShowPlayerListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            new DisplayPlayerEvent().publish();
        }
    }
}
