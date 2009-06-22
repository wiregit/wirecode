package org.limewire.ui.swing.library.sharing;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.NativeLaunchUtils;

import com.google.inject.Inject;

/**
 * Gives feedback about the Public Shared panel
 */
public class PublicSharedFeedbackPanel {

    @Resource Icon publicSharedIcon;
    @Resource Color backgroundColor;
    @Resource Color foregroundColor;
    @Resource Color borderColor;
    @Resource Font labelFont;
    
    private final JPanel component;
    
    @Inject
    public PublicSharedFeedbackPanel() {
        GuiUtils.assignResources(this);
        
        component = new JPanel(new MigLayout("insets 0 5 0 0", "", "[25!]"));
        component.setVisible(false);
        
        init();
    }
    
    private void init() {
        component.setBackground(backgroundColor);
        component.setBorder(BorderFactory.createMatteBorder(0,0,1,0, borderColor));
        JLabel label = new JLabel(I18n.tr("Files in this list are shared anonymously with the world."), publicSharedIcon, SwingConstants.LEFT);
        label.setForeground(foregroundColor);
        label.setFont(labelFont);
        component.add(label);
        
        final String learnMoreUrl = "http://www.limewire.com/client_redirect/?page=publicSharing";
        HyperlinkButton learnMoreButton = new HyperlinkButton(new AbstractAction(I18n.tr("Learn more")) {
            {
                putValue(Action.SHORT_DESCRIPTION, learnMoreUrl);
            }
            @Override
            public void actionPerformed(ActionEvent e) {
                NativeLaunchUtils.openURL(learnMoreUrl);
            }
        });
        component.add(learnMoreButton);
    }
    
    public JComponent getComponent() {
        return component;
    }
}
