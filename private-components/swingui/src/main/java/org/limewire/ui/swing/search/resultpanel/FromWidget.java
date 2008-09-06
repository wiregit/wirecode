package org.limewire.ui.swing.search.resultpanel;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.*;
import javax.swing.border.Border;
import org.limewire.ui.swing.RoundedBorder;

/**
 * Note that we can't use JPopupMenu for this because it doesn't have
 * the look called for in the spec.
 * 
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class FromWidget extends JPanel {

    private static final char DOWN_ARROW = '\u25BC';
    private static final int R = 8; // rounded border corner radius

    private Border border = new RoundedBorder(R);
    private Border noBorder = BorderFactory.createEmptyBorder(R, R, R, R);
    private JPopupMenu menu;
    private JLabel headerLabel = new JLabel();
    private JPanel headerPanel =
        new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    private String[] people;

    public FromWidget() {
        menu = new JPopupMenu();
        menu.setBorder(border);
        //setInsets(menu);

        configureHeader();
        layoutComponents();
        setOpaque(false);
    }

    private void configureHeader() {
        // The label has to be in a panel so we can add a border.
        headerPanel.add(headerLabel);
        headerPanel.setBorder(noBorder);
        headerPanel.setOpaque(false);

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
                if (people.length > 0) {
                    menu.show((Component) e.getSource(), -R, -R);
                }
            }
        });
    }

    private Action getChatAction(final String person) {
        return new AbstractAction("Chat") {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("starting chat with " + person);
            }
        };
    }

    private Action getLibraryAction(final String person) {
        return new AbstractAction("View library") {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("viewing library of " + person);
            }
        };
    }

    private Action getSharingAction(final String person) {
        return new AbstractAction("Files I'm Sharing") {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("showing files shared by " + person);
            }
        };
    }

    private void layoutComponents() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.anchor = GridBagConstraints.NORTH;
        gbc.insets = new Insets(R, 0, 0, 0);
        add(new JLabel("From "), gbc);

        gbc.insets.top = 0;
        add(headerPanel, gbc);
    }

    private void setInsets(Component component) {
        JComponent jc = (JComponent) component;
        Insets insets = jc.getInsets();
        insets.left = insets.right = 0;
        insets.top = insets.bottom = 2;
    }

    public void setPeople(List<String> people) {
        setPeople(people.toArray(new String[]{}));
    }

    public void setPeople(String[] people) {
        this.people = people;
        menu.removeAll();
        updateHeaderLabel();
        updateMenus();
    }

    private void updateHeaderLabel() {
        String text =
            people.length == 0 ? "nobody" :
            people.length == 1 ? people[0] + DOWN_ARROW :
            people.length + " people " + DOWN_ARROW;
        headerLabel.setText(text);
        menu.setLabel(text);
        menu.add(text);
    }

    private void updateMenus() {
        if (people.length == 0) return; // menu has no items

        if (people.length == 1) {
            String person = people[0];
            menu.add(getChatAction(person));
            menu.add(getLibraryAction(person));
            menu.addSeparator();
            menu.add(getSharingAction(person));
        } else {
            for (String person : people) {
                JMenu submenu = new JMenu(person);
                submenu.setBorder(border);
                submenu.add(getChatAction(person));
                submenu.add(getLibraryAction(person));

                menu.add(submenu);
            }
        }
    }
}