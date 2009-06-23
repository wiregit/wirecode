package org.limewire.ui.swing.options;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.NativeLaunchUtils;

public class LearnMoreButton extends HyperlinkButton {
    public LearnMoreButton(final String learnMoreUrl) {
        super(new AbstractAction(I18n.tr("Learn more")) {
                {   putValue(Action.SHORT_DESCRIPTION, learnMoreUrl);
                }

                @Override
                public void actionPerformed(ActionEvent e) {
                    NativeLaunchUtils.openURL(learnMoreUrl);
                }
            });
    }
}
