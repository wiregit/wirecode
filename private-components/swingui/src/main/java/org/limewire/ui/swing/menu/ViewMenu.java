package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import org.limewire.listener.EventListener;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.action.MnemonicMenu;
import org.limewire.ui.swing.downloads.DownloadMediator;
import org.limewire.ui.swing.friends.chat.ChatFrame;
import org.limewire.ui.swing.mainframe.LeftPanel;
import org.limewire.ui.swing.util.EnabledType;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.VisibilityType;
import org.limewire.ui.swing.util.VisibleComponent;

import com.google.inject.Inject;

public class ViewMenu extends MnemonicMenu {

    @Inject
    public ViewMenu(final LeftPanel leftPanel,
            final ChatFrame chatFrame, final DownloadMediator downloadMediator) {
        super(I18n.tr("&View"));
        add(buildShowHideAction(leftPanel, I18n.tr("Hide &Sidebar"), I18n.tr("Show &Sidebar")));
        add(buildShowHideAction(chatFrame, I18n.tr("Hide &Chat Window"), I18n.tr("Show &Chat Window")));
    }

    private Action buildShowHideAction(final VisibleComponent component, final String visibleName,
            final String notVisibleName) {
        final Action action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                component.toggleVisibility();
            }
        };

        addVisibilityListener(component, action, visibleName, notVisibleName);
        setInitialText(component, action, visibleName, notVisibleName);
        addEnabledListener(component, action);

        return action;
    }

    /**
     * Adds a listener to the specified component to update the enabled state
     * of the specified action, and initializes its enabled state. 
     */
    private void addEnabledListener(VisibleComponent component, final Action action) {
        component.addEnabledListener(new EventListener<EnabledType>() {
            @Override
            public void handleEvent(EnabledType enabledType) {
                action.setEnabled(enabledType.isEnabled());
            }
        });
        action.setEnabled(component.isActionEnabled());
    }

    private void addVisibilityListener(VisibleComponent component, final Action action,
            final String visibleName, final String notVisibleName) {
        EventListener<VisibilityType> listener = new EventListener<VisibilityType>() {
            @Override
            public void handleEvent(VisibilityType visibilityType) {
                if (visibilityType == VisibilityType.VISIBLE) {
                    action.putValue(Action.NAME, visibleName);
                } else {
                    action.putValue(Action.NAME, notVisibleName);
                }
            }
        };
        component.addVisibilityListener(listener);
    }

    private void setInitialText(VisibleComponent component, final Action action,
            final String visibleName, final String notVisibleName) {
        if (component.isVisible()) {
            action.putValue(Action.NAME, visibleName);
        } else {
            action.putValue(Action.NAME, notVisibleName);
        }
    }
}
