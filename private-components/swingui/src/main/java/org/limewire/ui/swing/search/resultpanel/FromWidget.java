package org.limewire.ui.swing.search.resultpanel;

import java.awt.Color;
import java.awt.Component;
import org.limewire.ui.swing.FancyPopupMenu;
import org.limewire.ui.swing.RoundedBorder;
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

/**
 * Note that we can't use JPopupMenu for this because it doesn't have
 * the look called for in the spec.
 * 
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class FromWidget extends JPanel {

    private static final char DOWN_ARROW = '\u25BC';

    private Border border;
    private Border noBorder;

    private FancyPopupMenu menu;
    private FancyPopupMenu submenu;
    private JLabel headerLabel;
    private JPanel headerPanel =
        new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    private String[] people;

    public FromWidget() {
        this(new String[] {});
    }

    public FromWidget(List<String> people) {
        this(people.toArray(new String[]{}));
    }

    public FromWidget(String[] people) {
        this.people = people;

        int r = FancyPopupMenu.CORNER_RADIUS;
        border = new RoundedBorder(r);
        noBorder = BorderFactory.createEmptyBorder(r, r, r, r);

        createHeaderLabel();
        createMenus();
        layoutComponents();
        setOpaque(false);
    }

    private void createHeaderLabel() {
        String text =
            people.length == 0 ? "nobody" :
            people.length == 1 ? people[0] + DOWN_ARROW :
            people.length + " people " + DOWN_ARROW;

        headerLabel = new JLabel(text);

        headerLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                headerPanel.setBorder(border);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!menu.isVisible()) headerPanel.setBorder(noBorder);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (people.length == 0) return;
                if (menu.isVisible()) {
                    menu.setVisible(false);
                } else {
                    menu.showOver(headerLabel);
                }
            }
        });
    }

    private void createMenus() {
        if (people.length == 0) return;

        Window owner = (Window) getTopLevelAncestor();
        menu = new FancyPopupMenu(owner, headerLabel.getText());

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
            submenu = new FancyPopupMenu(menu);
            menu.addItems(people);
            populateLastMenu(submenu);
            menu.setSubmenu(submenu);
        }
    }

    private String getSelectedPerson() {
        return people.length > 1 ? menu.getSelectedText() : people[0];
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

    private void layoutComponents() {
        headerPanel.setBorder(noBorder);
        headerPanel.add(headerLabel);

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.anchor = GridBagConstraints.NORTH;
        gbc.insets = new Insets(FancyPopupMenu.CORNER_RADIUS, 0, 0, 0);
        add(new JLabel("From "), gbc);

        gbc.insets.top = 0;
        add(headerPanel, gbc);
    }

    @Override
    public void setBackground(Color color) {
        super.setBackground(color);
        for (Component component : getComponents()) {
            component.setBackground(color);
        }
    }

    public void setPeople(List<String> people) {
        setPeople(people.toArray(new String[]{}));
    }

    public void setPeople(String[] people) {
        this.people = people;
        createHeaderLabel();
        createMenus();
    }
}