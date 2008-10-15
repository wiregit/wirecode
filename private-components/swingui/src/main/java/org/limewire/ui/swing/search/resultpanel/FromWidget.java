package org.limewire.ui.swing.search.resultpanel;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.Border;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.ui.swing.components.RoundedBorder;
import org.limewire.ui.swing.search.RemoteHostActions;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.util.Objects;

/**
 * This widget is used in the search results list view.
 * 
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class FromWidget extends JPanel {

    private static final int R = 8; // rounded border corner radius

    private final Border border = new RoundedBorder(R);

    private final Border noBorder = BorderFactory.createEmptyBorder(R, R, R, R);

    private final RemoteHostActions fromActions;

    private final JLabel headerLabel = shrinkFontSize(new JLabel());

    private final JPanel headerPanel = new JPanel();

    private final JPopupMenu menu;

    private List<RemoteHost> people;

    public FromWidget(RemoteHostActions fromActions) {
        menu = new JPopupMenu();
        menu.setBorder(border);
        this.fromActions = Objects.nonNull(fromActions, "fromActions");
        configureHeader();
        layoutComponents();
        setOpaque(false);
    }

    private void configureHeader() {
        headerPanel.setLayout(new MigLayout("insets 0 0 0 0", "0[]", "[]"));
        
        // The label has to be in a panel so we can add a border.
        headerPanel.add(headerLabel, "wmin 125");
        headerPanel.setBorder(noBorder);
        headerPanel.setOpaque(false);

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
                if (people.size() > 0) {
                    updateMenus();
                    menu.show((Component) e.getSource(), -R, -R);
                }
            }
        });
    }

    private Action getChatAction(final RemoteHost person) {
        return new AbstractAction(tr("Chat")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                fromActions.chatWith(person);
            }
        };
    }

    private Action getLibraryAction(final RemoteHost person) {
        return new AbstractAction(tr("View library")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                fromActions.viewLibraryOf(person);
            }
        };
    }

    private Action getSharingAction(final RemoteHost person) {
        int numberOfSharedFile = fromActions.getNumberOfSharedFiles(person);
        return new AbstractAction(tr("Files I'm Sharing ({0})", numberOfSharedFile)) {
            @Override
            public void actionPerformed(ActionEvent e) {
                fromActions.showFilesSharedBy(person);
            }
        };
    }

    private void layoutComponents() {
        setLayout(new MigLayout("insets 8 0 0 0", "0[]0[]", "0[]"));
        JLabel fromLabel = new JLabel(tr("From"));
        add(shrinkFontSize(fromLabel));

        add(headerPanel);
    }

    private JLabel shrinkFontSize(JLabel label) {
        FontUtils.changeSize(label, -1.0F);
        return label;
    }

    public void setPeople(List<RemoteHost> people) {
        this.people = people;
        headerLabel.setText(getFromText());
    }

    private String getFromText() {
        return people.size() == 0 ? tr("nobody") : people.size() == 1 ? people.get(0)
                .getRenderName() : tr("{0} people", people.size());
    }

    private void updateMenus() {
        menu.removeAll();
        String text = getFromText();
        menu.setLabel(text);
        menu.add(text);
        if (people.size() == 0)
            return; // menu has no items

        if (people.size() == 1) {
            RemoteHost person = people.get(0);

            if (person.isChatEnabled()) {
                menu.add(getChatAction(person));
            }
            if (person.isBrowseHostEnabled()) {
                menu.add(getLibraryAction(person));
            }
            if (person.isSharingEnabled()) {
                menu.add(getSharingAction(person));
            }

        } else {
            for (RemoteHost person : people) {
                if (person.isBrowseHostEnabled() || person.isChatEnabled()
                        || person.isSharingEnabled()) {

                    JMenu submenu = new JMenu(person.getRenderName());

                    if (person.isChatEnabled()) {
                        submenu.add(getChatAction(person));
                    }

                    if (person.isBrowseHostEnabled()) {
                        submenu.add(getLibraryAction(person));
                    }

                    if (person.isSharingEnabled()) {
                        submenu.add(getSharingAction(person));
                    }
                    menu.add(submenu);
                }
            }
        }
    }
}