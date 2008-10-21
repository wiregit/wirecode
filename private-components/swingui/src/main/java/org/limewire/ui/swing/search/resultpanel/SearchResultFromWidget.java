package org.limewire.ui.swing.search.resultpanel;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.ui.swing.components.LimeComboBox;
import org.limewire.ui.swing.components.LimeComboBoxFactory;
import org.limewire.ui.swing.search.RemoteHostActions;
import org.limewire.util.Objects;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class SearchResultFromWidget extends JPanel implements MouseListener {

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
        this.comboBox.addMouseListener(this);
        
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

    
    public void setPeople(List<RemoteHost> people) {
        this.people = people;
        this.comboBox.setText(getFromText());
    }

    private String getFromText() {
        return people.size() == 0 ? tr("nobody") : people.size() == 1 ? people.get(0)
                .getRenderName() : tr("{0} people", people.size());
    }

    private void updateMenus() {
        
        this.comboBoxMenu.removeAll();
        
        if (people.size() == 0)
            return; // menu has no items

        if (people.size() == 1) {
            RemoteHost person = people.get(0);

            if (person.isChatEnabled()) {
                this.comboBoxMenu.add(getChatAction(person));
            }
            if (person.isBrowseHostEnabled()) {
                this.comboBoxMenu.add(getLibraryAction(person));
            }
            if (person.isSharingEnabled()) {
                this.comboBoxMenu.add(getSharingAction(person));
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
                    this.comboBoxMenu.add(submenu);
                }
            }
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }
    @Override
    public void mouseEntered(MouseEvent e) {
        updateMenus();
    }
    @Override
    public void mouseExited(MouseEvent e) {
    }
    @Override
    public void mousePressed(MouseEvent e) {
    }
    @Override
    public void mouseReleased(MouseEvent e) {
    }
}