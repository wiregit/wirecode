package org.limewire.ui.swing.wizard;

import java.awt.Color;
import java.util.List;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.activation.api.ActivationItem;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

public class SetupActivationThankYouPanel extends JPanel {

    @Resource
    private Color thankYouColor;
    
    public SetupActivationThankYouPanel(final WizardPage wizardPage, List<ActivationItem> eventList) {
        super(new MigLayout("fill, insets 80, gap 0, gapy 0", "[]", "[][][][][]"));
        
        GuiUtils.assignResources(this);
        
        JLabel thankYouLabel = wizardPage.createAndDecorateHeader(I18n.tr("Thank you! You've successfully activated your license."));
        thankYouLabel.setForeground(thankYouColor);
        add(thankYouLabel, "align 50% 50%, wrap");

        add(Box.createVerticalStrut(18), "wrap");

        SetupActivationTable table = new SetupActivationTable(wizardPage, eventList);
        add(table, "align 50% 50%, wrap");

        add(Box.createVerticalStrut(18), "wrap");

        //JPanel customerSupportPanel = new JPanel();
        JLabel questionsLabel = wizardPage.createAndDecorateMultiLine(I18n.tr("If you have any questions about your license, please contact "));
        //customerSupportPanel.add(questionsLabel);
        HyperlinkButton customerSupportButton = wizardPage.createAndDecorateHyperlink("http://www.limewire.com/support",
                                                                                      I18n.tr("Customer Support"));
        //customerSupportPanel.add(customerSupportButton);
        //add(customerSupportPanel, "wrap");
        add(questionsLabel, "align 50% 50%, split, gapright 0");
        add(customerSupportButton, "align 50% 50%, wrap");
    }
}
