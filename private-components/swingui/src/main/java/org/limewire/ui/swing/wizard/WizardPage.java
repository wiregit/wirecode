package org.limewire.ui.swing.wizard;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.NativeLaunchUtils;

public abstract class WizardPage extends JPanel {
    
    private final SetupComponentDecorator decorator;
    public WizardPage(SetupComponentDecorator decorator) {
        this.decorator = decorator;
    }
    
    public abstract void applySettings();
    public abstract String getLine1();
    public abstract String getLine2();
    public abstract String getFooter();
    
    protected JLabel createAndDecorateHeader(String text) {
        JLabel label = new JLabel(text);
        decorator.decorateHeadingText(label);
        return label;
    }
    
    protected JLabel createAndDecorateMultiLine(String text, JCheckBox checkBox) {
        JLabel label = new MultiLineLabel(text, 500);
        label.addMouseListener(new SetupComponentDecorator.ToggleExtenderListener(checkBox));
        decorator.decorateNormalText(label); 
        return label;
    }
    
    protected JCheckBox createAndDecorateCheckBox(boolean isSelected) {
        JCheckBox checkBox = new JCheckBox();
        checkBox.setSelected(isSelected);
        decorator.decorateLargeCheckBox(checkBox);
        return checkBox;
    }
    
    protected HyperlinkButton createAndDecorateHyperlink(final String url) {
        HyperlinkButton learnMoreButton = new HyperlinkButton(new AbstractAction(I18n.tr("Learn more")) {
            {
                putValue(Action.SHORT_DESCRIPTION, url);
            }
            
            @Override
            public void actionPerformed(ActionEvent e) {
                NativeLaunchUtils.openURL(url);
            }
        });
        decorator.decorateNormalText(learnMoreButton);
        decorator.decorateLink(learnMoreButton);
        return learnMoreButton;
    }
}
