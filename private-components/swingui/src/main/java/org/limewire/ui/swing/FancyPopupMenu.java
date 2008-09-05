package org.limewire.ui.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.border.Border;

/**
 * This class implements a popup menu that has rounded corners
 * and supports cascading menus.
 * TODO: The area outside the rounded corners isn't yet transparent.
 * TODO: Java 6 update 10 will add support for that.
 * TODO: See com.sun.awt.AWTUtilities.setWindowShape(Window, Shape).
 * TODO: @author R. Mark Volkmann, Object Computing, Inc.
 */
public class FancyPopupMenu extends JWindow {

    private static final Color HIGHLIGHT_COLOR = new Color(220, 220, 255);
    public static final int CORNER_RADIUS = 8;

    private static int instanceCounter;

    private Border border = new RoundedBorder(CORNER_RADIUS);
    private FancyPopupMenu submenu;
    private JLabel headerLabel;
    private JLabel lastLabel;
    private JLabel selectedLabel;
    private JPanel panel = new JPanel(new GridLayout(0, 1));
    private Map<JLabel, Action> labelToActionMap =
        new HashMap<JLabel, Action>();
    private Window owner;
    private int instanceNumber;

    public FancyPopupMenu(Window owner) {
        super(owner);
        instanceNumber = ++instanceCounter;

        this.owner = owner;
        panel.setOpaque(false);
        panel.setBorder(border);
        add(panel, BorderLayout.CENTER);
    }

    public FancyPopupMenu(Window owner, String header) {
        this(owner);
        addItem(header);
    }

    public JLabel addItem(Action action) {
        String text = getActionName(action);
        JLabel label = addItem(text);
        labelToActionMap.put(label, action);
        return label;
    }

    public JLabel addItem(final JLabel label) {
        label.setOpaque(true); // required for background colors
        panel.add(label);

        final Color defaultBackground = label.getBackground();
        
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                // If another name label was previously selected,
                // deselect it.
                if (selectedLabel != null) {
                    selectedLabel.setBackground(defaultBackground);
                    if (submenu != null) submenu.setVisible(false);
                }
                
                if (label == headerLabel) {
                    selectedLabel = null;
                } else {
                    label.setBackground(HIGHLIGHT_COLOR);
                    selectedLabel = label;
                }
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                FancyPopupMenu fpm = FancyPopupMenu.this;

                if (label == headerLabel) {
                    fpm.setVisible(false);
                } else if (submenu == null) {
                    Action action = labelToActionMap.get(label);
                    if (action != null) {
                        ActionEvent event =
                            new ActionEvent(label, 0, getActionName(action));
                        action.actionPerformed(event);
                        fpm.setVisible(false);
                        if (fpm.owner instanceof FancyPopupMenu) {
                            FancyPopupMenu ownerFPM =
                                (FancyPopupMenu) fpm.owner;
                            ownerFPM.setVisible(false);
                        }
                    }
                } else {
                    submenu.showRight(label);
                }
            }
        });

        lastLabel = label;
        return label;
    }

    public JLabel addItem(String item) {
        return addItem(new JLabel(item));
    }

    public void addItems(String[] items) {
        for (String item : items) addItem(item);
    }

    public void addSeparator() {
        if (lastLabel == null) return;
        lastLabel.setBorder(new BottomBorder());
    }

    public void clear() {
        panel.removeAll();
        headerLabel = null;
        selectedLabel = null;
        labelToActionMap.clear();
    }

    /**
     * This method is useful for debuggin.
     * @param title the title to be displayed at the beginning of the output
     */
    public void dump(String title) {
        System.out.println("FancyPopupMenu.dump: " + title);
        System.out.println("  instance #" + instanceNumber);
        System.out.println("  header: " + (headerLabel == null ? "none" : headerLabel.getText()));
        //System.out.println("  submenu: " + (submenu == null ? "none" : submenu.instanceNumber));
        //System.out.println("  selectedLabel: " + (selectedLabel == null ? "none" : selectedLabel.getText()));
        System.out.println("  component count: " + panel.getComponentCount());
    }

    private static String getActionName(Action action) {
        return action.getValue(Action.NAME).toString();
    }

    public String getSelectedText() {
        return selectedLabel == null ? null : selectedLabel.getText();
    }

    public void paintComponent(Graphics g) {
        // do nothing
    }

    public void setHeader(String text) {
        headerLabel = addItem(text);
    }

    public void setSubmenu(FancyPopupMenu submenu) {
        this.submenu = submenu;
    }

    public void showBelow(Component component) {
        Point location = component.getLocationOnScreen();
        location.y += CORNER_RADIUS + component.getHeight();
        show(location);
    }

    public void showOver(Component component) {
        Point location = component.getLocationOnScreen();
        location.x -= CORNER_RADIUS;
        location.y -= CORNER_RADIUS;
        show(location);
    }

    public void showRight(Component component) {
        Point location = component.getLocationOnScreen();
        location.x += CORNER_RADIUS + component.getWidth();
        show(location);
    }

    private void show(Point location) {
        pack();
        setLocation(location);
        setVisible(true);
    }
}