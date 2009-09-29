package org.limewire.ui.swing.player;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;

import net.miginfocom.swing.MigLayout;

import org.limewire.player.api.PlayerState;
import org.limewire.ui.swing.components.HeaderBar;
import org.limewire.ui.swing.components.decorators.HeaderBarDecorator;
import org.limewire.ui.swing.util.I18n;

public class VideoPanel extends JPanel {

    private final HeaderBar headerBar = new HeaderBar();

    private JCheckBox fullScreenButton;

    private JButton closeButton;

    private final VideoPlayerMediator videoMediator;

    public VideoPanel(PlayerControlPanel controlPanel,
            HeaderBarDecorator headerBarDecorator, VideoPlayerMediator videoMediator) {
        super(new BorderLayout());

        setFocusable(true);
        
        this.videoMediator = videoMediator;

        setUpHeaderBar(controlPanel, headerBarDecorator);

        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK), "fullScreen");
        getActionMap().put("fullScreen", new ToggleFullScreen());
        
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "esc");
        getActionMap().put("esc", new EscAction());
        
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "space");
        getActionMap().put("space", new PlayOrPauseAction());

        add(headerBar, BorderLayout.NORTH);
    }
    
    public void addVideoComponent(Component videoComponent){
        add(videoComponent);
        setUpMouseListener(videoComponent);
    }

    private void setUpMouseListener(Component videoComponent) {
        videoComponent.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    JPopupMenu menu = new JPopupMenu();
                    if (videoMediator.getStatus() == PlayerState.PLAYING) {
                        menu.add(createPauseMenuItem());
                    } else {
                        menu.add(createPlayMenuItem());
                    }
                    menu.addSeparator();
                    menu.add(createFullScreenMenuItem());
                    menu.addSeparator();
                    menu.add(createCloseItem());
                    menu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }

    private void setUpHeaderBar(PlayerControlPanel controlPanel,
            HeaderBarDecorator headerBarDecorator) {

        fullScreenButton = new JCheckBox("Full Screen");
        fullScreenButton.addItemListener(new FullScreenListener());
        fullScreenButton.setSelected(videoMediator.isFullScreen());
        
        closeButton = new JButton("X");
        closeButton.addActionListener(new CloseAction());

        headerBarDecorator.decorateBasic(headerBar);

        headerBar.setLayout(new MigLayout());
        headerBar.add(fullScreenButton, "push");
        headerBar.add(controlPanel, "pos 0.5al 0.5al");
        headerBar.add(closeButton);

    }
    
    private JMenuItem createFullScreenMenuItem(){
        JMenuItem item = new JCheckBoxMenuItem(I18n.tr("Full Screen"));
        item.setSelected(videoMediator.isFullScreen());
        item.addItemListener(new FullScreenListener());
        return item;
    }

    private JMenuItem createPlayMenuItem() {
        JMenuItem item = new JMenuItem(I18n.tr("Play"));
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                videoMediator.resume();
            }
        });
        return item;
    }
    
    private JMenuItem createPauseMenuItem(){
        JMenuItem item = new JMenuItem(I18n.tr("Pause"));
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                videoMediator.pause();
            }
        });
        return item;
    }
    
    private JMenuItem createCloseItem(){
        JMenuItem item = new JMenuItem(I18n.tr("Close Video"));
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                videoMediator.closeVideoPanel();
            }
        });
        return item;
    }

    private class CloseAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            videoMediator.closeVideoPanel();
        }

    }
    private class FullScreenListener implements ItemListener {

        @Override
        public void itemStateChanged(ItemEvent e) {
            videoMediator.setFullScreen(e.getStateChange() == ItemEvent.SELECTED);
        }

    }
    
    private class ToggleFullScreen extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            System.out.println("TOGGLE!");
            videoMediator.setFullScreen(!videoMediator.isFullScreen());
        }
    }
    
    
    private class EscAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (videoMediator.isFullScreen()) {
                videoMediator.setFullScreen(false);
            } else {
                videoMediator.closeVideoPanel();
            }
        }
    }
    
    
    private class PlayOrPauseAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (videoMediator.getStatus() == PlayerState.PLAYING) {
                videoMediator.pause();
            } else {
                videoMediator.resume();
            }
        }
    }

}
