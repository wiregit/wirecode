package org.limewire.ui.swing.search.resultpanel;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.*;
import javax.swing.border.Border;
import org.limewire.ui.swing.FancyPopupMenu;
import org.limewire.ui.swing.RoundedBorder;

/**
 * Note that we can't use JPopupMenu for this because it doesn't have
 * the look called for in the spec.
 * 
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class FromWidget extends JPanel {

    private static final char DOWN_ARROW = '\u25BC';
    private static final int R = FancyPopupMenu.CORNER_RADIUS;

    private Border border = new RoundedBorder(R);
    private Border noBorder = BorderFactory.createEmptyBorder(R, R, R, R);
    private FancyPopupMenu menu;
    private FancyPopupMenu submenu;
    private JLabel headerLabel = new JLabel("no header yet");
    private JPanel headerPanel =
        new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    private String[] people;

    public FromWidget() {
        Window owner = (Window) getTopLevelAncestor();
        menu = new FancyPopupMenu(owner);

        configureHeader();
        layoutComponents();
        setOpaque(false);
    }

    private void configureHeader() {
        headerPanel.setOpaque(false);
        headerPanel.setBorder(noBorder);
        headerPanel.add(headerLabel);

        headerLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                System.out.println("FromWidget: got mouseEntered");
                headerPanel.setBorder(border);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                System.out.println("FromWidget: got mouseExited");
                if (!menu.isVisible()) headerPanel.setBorder(noBorder);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                System.out.println("FromWidget: got mousePressed");
                if (people.length == 0) return;
                if (menu.isVisible()) {
                    menu.setVisible(false);
                } else {
                    menu.showOver(headerLabel);
                }
            }
        });
    }

    private String getSelectedPerson() {
        return people.length > 1 ? menu.getSelectedText() : people[0];
    }

    private void layoutComponents() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.anchor = GridBagConstraints.NORTH;
        gbc.insets = new Insets(FancyPopupMenu.CORNER_RADIUS, 0, 0, 0);
        add(new JLabel("From "), gbc);

        gbc.insets.top = 0;
        add(headerPanel, gbc);
    }

    private void populateLastMenu(FancyPopupMenu lastMenu) {
        Action action = new AbstractAction("Chat") {
            public void actionPerformed(ActionEvent e) {
                System.out.println(
                    "starting chat with " + getSelectedPerson());
            }
        };
        lastMenu.addItem(action);
        
        action = new AbstractAction("View Library") {
            public void actionPerformed(ActionEvent e) {
                System.out.println(
                    "viewing library of " + getSelectedPerson());
            }
        };
        lastMenu.addItem(action);
    }

    public void setPeople(List<String> people) {
        setPeople(people.toArray(new String[]{}));
    }

    public void setPeople(String[] people) {
        System.out.println("FromWidget: setting people to:");
        for (String person : people) System.out.println("  " + person);

        this.people = people;
        menu.clear();
        //if (submenu != null) submenu.clear();
        submenu = null;
        updateHeaderLabel();
        updateMenus();
    }

    private void updateHeaderLabel() {
        String text =
            people.length == 0 ? "nobody" :
            people.length == 1 ? people[0] + DOWN_ARROW :
            people.length + " people " + DOWN_ARROW;
        headerLabel.setText(text);
        menu.setHeader(text);
    }

    private void updateMenus() {
        // TODO: RMV Why is the next line needed?
        // The next line removes headerLabel from panel
        // because it can't be in two containers!
        headerPanel.add(headerLabel);

        if (people.length == 0) return; // menus have no items

        if (people.length == 1) {
            populateLastMenu(menu);

            menu.addSeparator();

            Action action = new AbstractAction("Files I'm Sharing") {
                public void actionPerformed(ActionEvent e) {
                    System.out.println(
                        "showing files shared by " + getSelectedPerson());
                }
            };
            menu.addItem(action);
        } else {
            menu.addItems(people);
            submenu = new FancyPopupMenu(menu);
            menu.setSubmenu(submenu);
            populateLastMenu(submenu);
        }
    }
}