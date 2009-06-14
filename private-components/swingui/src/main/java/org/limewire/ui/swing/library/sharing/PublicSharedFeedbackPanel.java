package org.limewire.ui.swing.library.sharing;

import java.awt.Color;
import java.awt.Font;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

/**
 * Gives feedback about the Public Shared panel
 */
public class PublicSharedFeedbackPanel {

    @Resource Icon publicSharedIcon;
    @Resource Color backgroundColor;
    @Resource Color foregroundColor;
    @Resource Font labelFont;
    
    private final JPanel component;
    
    @Inject
    public PublicSharedFeedbackPanel() {
        GuiUtils.assignResources(this);
        
        component = new JPanel(new MigLayout(""));
        component.setVisible(false);
        
        init();
    }
    
    private void init() {
        component.setBackground(backgroundColor);
        JLabel label = new JLabel(I18n.tr("This list is shared anonymously with the world"), publicSharedIcon, SwingConstants.LEFT);
        label.setForeground(foregroundColor);
        label.setFont(labelFont);
        component.add(label);
    }
    
    public JComponent getComponent() {
        return component;
    }
}
