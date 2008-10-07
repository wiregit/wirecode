package org.limewire.ui.swing.search.resultpanel;

import static org.limewire.ui.swing.util.I18n.tr;
import static org.limewire.ui.swing.util.I18n.trn;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.Border;

import org.limewire.ui.swing.components.ComponentHider;
import org.limewire.ui.swing.components.RoundedBorder;
import org.limewire.ui.swing.util.FontUtils;

/**
 * This widget is used in the search results list view.
 * 
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class FromWidget extends JPanel {

    private static final char DOWN_ARROW = '\u25BC';
    private static final int R = 8; // rounded border corner radius

    private final Border border = new RoundedBorder(R);
    private final Border noBorder = BorderFactory.createEmptyBorder(R, R, R, R);
    private final FromActions fromActions = new FromActionsMockImpl();
    private final JLabel headerLabel = shrinkFontSize(new JLabel());
    private final JPanel headerPanel =
        new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    private final JPopupMenu menu;
    private final ComponentHider menuHider;
    private String[] people;

    public FromWidget() {
        menu = new JPopupMenu();
        menuHider =  new ComponentHider(menu);
        menu.setBorder(border);

        configureHeader();
        layoutComponents();
        setOpaque(false);
    }

    private void configureHeader() {
        // The label has to be in a panel so we can add a border.
        headerPanel.add(headerLabel);
        headerPanel.setBorder(noBorder);
        headerPanel.setOpaque(false);
        
        menu.addMouseListener(menuHider);

        headerLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                headerPanel.setBorder(border);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!menu.isVisible()) {
                    headerPanel.setBorder(noBorder);
                }
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
        return new AbstractAction(tr("Chat")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                fromActions.chatWith(person);
            }
        };
    }

    private Action getLibraryAction(final String person) {
        return new AbstractAction(tr("View library")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                fromActions.viewLibraryOf(person);
            }
        };
    }

    private Action getSharingAction(final String person) {
        return new AbstractAction(tr("Files I'm Sharing")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                fromActions.showFilesSharedBy(person);
            }
        };
    }

    private void layoutComponents() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.anchor = GridBagConstraints.NORTH;
        gbc.insets = new Insets(R, 0, 0, 0);
        JLabel fromLabel = new JLabel(tr("From "));
        add(shrinkFontSize(fromLabel), gbc);

        gbc.insets.top = 0;
        add(headerPanel, gbc);
    }

    private JLabel shrinkFontSize(JLabel label) {
        FontUtils.changeSize(label, -1.0F);
        return label;
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
            people.length == 0 ? tr("nobody") :
            people.length == 1 ? people[0] + DOWN_ARROW :
            trn("{1}", "{0} people", people.length , people[0]) + DOWN_ARROW;
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
            menu.add(getSharingAction(person));
        } else {
            for (String person : people) {
                JMenu submenu = new JMenu(person);
                submenu.addMouseListener(menuHider);
                JMenuItem chatItem = new JMenuItem(getChatAction(person));
                chatItem.addMouseListener(menuHider);
                submenu.add(chatItem);
                JMenuItem libraryItem = new JMenuItem(getLibraryAction(person));
                libraryItem.addMouseListener(menuHider);
                submenu.add(libraryItem);

                menu.add(submenu);
            }
        }
    }
}