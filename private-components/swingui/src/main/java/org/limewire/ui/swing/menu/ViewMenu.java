package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.action.MnemonicMenu;
import org.limewire.ui.swing.downloads.DownloadMediator;
import org.limewire.ui.swing.friends.chat.ChatFramePanel;
import org.limewire.ui.swing.mainframe.LeftPanel;
import org.limewire.ui.swing.util.EnabledListener;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.VisibilityListener;
import org.limewire.ui.swing.util.VisibleComponent;

import com.google.inject.Inject;

public class ViewMenu extends MnemonicMenu {

    @Inject
    public ViewMenu(final LeftPanel leftPanel,
            final ChatFramePanel friendsPanel, final DownloadMediator downloadMediator) {
        super(I18n.tr("&View"));
        add(buildShowHideAction(leftPanel, I18n.tr("Hide &Sidebar"), I18n.tr("Show &Sidebar")));
        add(buildShowHideAction(friendsPanel, I18n.tr("Hide &Chat Window"), I18n.tr("Show &Chat Window")));
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
        component.addEnabledListener(new EnabledListener() {
            @Override
            public void enabledChanged(boolean enabled) {
                action.setEnabled(enabled);
            }
        });
        action.setEnabled(component.isActionEnabled());
    }

    private void addVisibilityListener(VisibleComponent component, final Action action,
            final String visibleName, final String notVisibleName) {
        component.addVisibilityListener(new VisibilityListener() {
            @Override
            public void visibilityChanged(boolean visible) {
                if (visible) {
                    action.putValue(Action.NAME, visibleName);
                } else {
                    action.putValue(Action.NAME, notVisibleName);
                }
            }
        });
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
