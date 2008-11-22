/**
 * 
 */
package org.limewire.ui.swing.menu.actions;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.friends.DisplayFriendsToggleEvent;
import org.limewire.ui.swing.friends.SignoffEvent;
import org.limewire.ui.swing.friends.XMPPConnectionEstablishedEvent;
import org.limewire.ui.swing.util.I18n;

public class SignInOutAction extends AbstractAction {
    private static final String SIGN_INTO_FRIENDS_TEXT = I18n.tr("&Sign into Friends");

    private static final String SIGN_OUT_OF_FRIENDS_TEXT = I18n.tr("&Sign out of Friends");

    boolean signedIn = false;

    public SignInOutAction() {
        super(SIGN_INTO_FRIENDS_TEXT);
        EventAnnotationProcessor.subscribe(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!signedIn) {
            new DisplayFriendsToggleEvent(true).publish();
        } else {
            new SignoffEvent().publish();
        }
    }

    @EventSubscriber
    public void handleSignon(XMPPConnectionEstablishedEvent event) {
        putValue(Action.NAME, SIGN_OUT_OF_FRIENDS_TEXT);
        signedIn = true;
    }

    @EventSubscriber
    public void handleSignoff(SignoffEvent event) {
        putValue(Action.NAME, SIGN_INTO_FRIENDS_TEXT);
        signedIn = false;
    }
}