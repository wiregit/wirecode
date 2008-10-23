package org.limewire.ui.swing.search.resultpanel;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.ui.swing.components.LimeComboBox;
import org.limewire.ui.swing.components.LimeComboBoxFactory;
import org.limewire.ui.swing.components.LimeComboBox.UpdateHandler;
import org.limewire.ui.swing.search.RemoteHostActions;
import org.limewire.util.Objects;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class SearchResultFromWidget extends JPanel implements UpdateHandler {

    private final LimeComboBox comboBox;
    private final JPopupMenu comboBoxMenu;
    
    private final RemoteHostActions fromActions;
    private List<RemoteHost> people;
    
    @AssistedInject
    SearchResultFromWidget(LimeComboBoxFactory comboBoxFactory, @Assisted RemoteHostActions fromActions) {
        this.fromActions = Objects.nonNull(fromActions, "fromActions");
        
        this.comboBox = comboBoxFactory.createMiniComboBox();
        this.comboBoxMenu = new JPopupMenu();
        this.comboBox.overrideMenu(this.comboBoxMenu);
        this.comboBox.addUpdateHandler(this);
        
        this.layoutComponents();
        this.setOpaque(false);
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
        this.setLayout(new BorderLayout());
        this.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        
        JLabel fromLabel = new JLabel(tr("From"));
        fromLabel.setFont(new Font("Arial", Font.BOLD, 10));
        this.add(fromLabel, BorderLayout.WEST);

        add(this.comboBox, BorderLayout.EAST);
    }

    
    public void setPeople(Collection<RemoteHost> people) {
        this.people = new LinkedList<RemoteHost>(people);
        this.comboBox.setText(getFromText());
    }

    private String getFromText() {
        return people.size() == 0 ? tr("nobody") : people.size() == 1 ? people.get(0)
                .getRenderName() : tr("{0} people", people.size());
    }

    private void assertParentProps(JComponent item) {
        item.setFont(this.comboBoxMenu.getFont());
        item.setForeground(this.comboBoxMenu.getForeground());
        item.setBackground(this.comboBoxMenu.getBackground());
    }
    
    private JMenuItem createItem(Action a) {
        JMenuItem item = new JMenuItem(a);
        
        assertParentProps(item);
        
        return item;
    }
    
    private void updateMenus() {
        
        this.comboBoxMenu.removeAll();
        
        if (people.size() == 0)
            return; // menu has no items

        if (people.size() == 1) {
            RemoteHost person = people.get(0);

            if (person.isChatEnabled()) {
                this.comboBoxMenu.add(createItem(getChatAction(person)));
            }
            if (person.isBrowseHostEnabled()) {
                this.comboBoxMenu.add(createItem(getLibraryAction(person)));
            }
            if (person.isSharingEnabled()) {
                this.comboBoxMenu.add(createItem(getSharingAction(person)));
            }

        } else {
            for (RemoteHost person : people) {
                if (person.isBrowseHostEnabled() || person.isChatEnabled()
                        || person.isSharingEnabled()) {

                    JMenu submenu = new JMenu(person.getRenderName());
                    assertParentProps(submenu);

                    if (person.isChatEnabled()) {
                        submenu.add(createItem(getChatAction(person)));
                    }

                    if (person.isBrowseHostEnabled()) {
                        submenu.add(createItem(getLibraryAction(person)));
                    }

                    if (person.isSharingEnabled()) {
                        submenu.add(createItem(getSharingAction(person)));
                    }
                    this.comboBoxMenu.add(submenu);
                }
            }
        }
    }

    @Override
    public void fireUpdate() {
        this.updateMenus();
    }
}