package org.limewire.ui.swing.components;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.GroupLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.GroupLayout.Group;
import javax.swing.GroupLayout.SequentialGroup;

import org.jdesktop.swingx.JXBusyLabel;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.painter.MoreButtonPainter;
import org.limewire.ui.swing.util.I18n;

public class FancyTabMoreButton extends JXButton {
    
//    @Resource private int width;
//    @Resource private int height;
    
    private final FancyTabProperties props;
    private JPopupMenu menu = new JPopupMenu(I18n.tr("more"));
    
    public FancyTabMoreButton(List<FancyTab> tabs,
            Icon triangle,
            FancyTabProperties props) {

        super(I18n.tr("more"), triangle);

        this.props = props;
        
//        setMinimumSize(new Dimension(70, 21));
//        setMaximumSize(new Dimension(68, 21));
//        setPreferredSize(new Dimension(68, 21));
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setHorizontalTextPosition(SwingConstants.LEFT);        
        setVerticalTextPosition(SwingConstants.CENTER);
        setHorizontalAlignment(SwingConstants.RIGHT);
        setBackgroundPainter(new MoreButtonPainter());
        MoreListener listener = new MoreListener(tabs);
        this.addMouseListener(listener);
        this.addActionListener(listener);
    }
    
    private JComponent createMenuItemFor(
            final JPopupMenu menu, final FancyTab tab) {

        JXPanel jp = new JXPanel();
        jp.setOpaque(false);
        jp.setBackgroundPainter(props.getNormalPainter());
        
        final AbstractButton selectButton = tab.createMainButton();
        selectButton.setHorizontalAlignment(SwingConstants.LEADING);
        selectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                menu.setVisible(false);
            }
        });
        
        JButton removeButton = tab.createRemoveButton();
        if(props.isRemovable()) {
            removeButton.setVisible(true);
            removeButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    menu.setVisible(false);
                }
            });
        }
        
        JXBusyLabel busyLabel = tab.createBusyLabel();
        
        GroupLayout layout = new GroupLayout(jp);
        jp.setLayout(layout);
        
        SequentialGroup horGroup = layout.createSequentialGroup();
        layout.setHorizontalGroup(horGroup);
        
        Group verGroup = layout.createParallelGroup(GroupLayout.Alignment.CENTER);
        layout.setVerticalGroup(verGroup);
        
        layout.setAutoCreateGaps(true);
        
        horGroup.addGap(5)
                .addComponent(busyLabel)
                .addComponent(selectButton, 0, 120, 120)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(removeButton, 20, 20, 20);
        
        verGroup.addComponent(busyLabel)
                .addComponent(selectButton)
                .addComponent(removeButton, 20, 20, 20);
        
        layout.setHonorsVisibility(busyLabel, false);
        
        Highlighter highlighter = new Highlighter(jp, selectButton, removeButton);
        jp.addMouseListener(highlighter);
        selectButton.addMouseListener(highlighter);
        removeButton.addMouseListener(highlighter);
        
        return jp;
    }
    
    private class Highlighter extends MouseAdapter {
        private final JXPanel panel;
        private final AbstractButton selectButton;
        private final AbstractButton removeButton;
        
        public Highlighter(JXPanel panel, AbstractButton selectButton, AbstractButton removeButton) {
            this.panel = panel;
            this.selectButton = selectButton;
            this.removeButton = removeButton;
        }
        
        @Override
        public void mouseEntered(MouseEvent e) {
            panel.setBackgroundPainter(props.getHighlightPainter());
            removeButton.setIcon(removeButton.getSelectedIcon());
        }
        
        @Override
        public void mouseExited(MouseEvent e) {
            panel.setBackgroundPainter(props.getNormalPainter());
            removeButton.setIcon(null);
        }
        
        @Override
        public void mouseClicked(MouseEvent e) {
            // Forward the click to selection if it wasn't already a button
            if(!(e.getSource() instanceof AbstractButton) && SwingUtilities.isLeftMouseButton(e)) {
                selectButton.doClick(0);
            }
        }
    }
    
    private class MoreListener implements MouseListener, ActionListener {
        private final List<FancyTab> tabs;
        
        public MoreListener(List<FancyTab> tabs) {
            this.tabs = tabs;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            menu.removeAll();

            for (FancyTab tab : tabs) {
                menu.add(createMenuItemFor(menu, tab));
            }

            JComponent source = (JComponent) e.getSource();
            menu.show(source, 3, source.getBounds().height);
        }
        
        @Override
        public void mouseEntered(MouseEvent e) {
            getTopLevelAncestor().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        
        @Override
        public void mouseExited(MouseEvent e) {
            getTopLevelAncestor().setCursor(Cursor.getDefaultCursor());
        }
        
        @Override public void mouseClicked(MouseEvent e) {}
        @Override public void mousePressed(MouseEvent e) {}
        @Override public void mouseReleased(MouseEvent e) {}
    }

}
