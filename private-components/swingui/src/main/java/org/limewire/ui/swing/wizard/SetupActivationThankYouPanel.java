package org.limewire.ui.swing.wizard;

import java.awt.Color;
import java.awt.Dimension;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.activation.api.ActivationItem;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

public class SetupActivationThankYouPanel extends JPanel {

    @Resource
    private Color thankYouColor;
    @Resource
    private Icon infoIcon;
    
    public SetupActivationThankYouPanel(final WizardPage wizardPage, List<ActivationItem> eventList, boolean userHasPreexistingLicense) {
        super(new MigLayout("fill, insets 50 15 0 15, gap 0, gapy 0", "[]", "[][][][][][]"));
        
        GuiUtils.assignResources(this);
        
        if (userHasPreexistingLicense) {
            JLabel thankYouLabel = wizardPage.createAndDecorateHeader(I18n.tr("Yay! Your license has been successfully activated."));
            thankYouLabel.setForeground(thankYouColor);
            add(thankYouLabel, "align 50% 50%, wrap");
        } else {
            JLabel thankYouLabel = wizardPage.createAndDecorateHeader(I18n.tr("Thank you! You have successfully activated your license."));
            thankYouLabel.setForeground(thankYouColor);
            add(thankYouLabel, "align 50% 50%, wrap");
        }

        add(Box.createVerticalStrut(10), "wrap");

        SetupActivationTable table = new SetupActivationTable(wizardPage, eventList);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(thankYouColor));
        int numberOfItemsVisible = (eventList.size() > 4) ? 4 : eventList.size();
        scrollPane.setMinimumSize(new Dimension(400, 27 + numberOfItemsVisible * 29 + 10));
        scrollPane.setPreferredSize(new Dimension(400, 27 + numberOfItemsVisible * 29 + 10));
        
        if (areThereProblematicModules(eventList)) {
            JPanel innerPanel = new JPanel(new MigLayout("fill, insets 0, gap 0, gapy 0", "[]", "[][][]"));
            
            innerPanel.add(scrollPane, "align 50% 50%, wrap");

            innerPanel.add(Box.createVerticalStrut(10), "wrap");

            JLabel infoTextLine1 = wizardPage.createAndDecorateMultiLine(I18n.tr("* " + "One or more of your features is currently not activated."));
            JLabel infoTextLine2a = wizardPage.createAndDecorateMultiLine(I18n.tr("Click on "));
            JLabel infoTextLine2b = wizardPage.createAndDecorateMultiLine(I18n.tr(" for more information."));
            innerPanel.add(infoTextLine1, "align 0% 50%, wrap");
            innerPanel.add(infoTextLine2a, "align 0% 50%, split, gapright 0");
            innerPanel.add(new IconButton(infoIcon), "align 0% 50%, split, gapright 0");
            innerPanel.add(infoTextLine2b, "align 0% 50%, wrap");
            
            add(innerPanel, "align 50% 0%, wrap");
        } else {
            add(scrollPane, "align 50% 0%, wrap");

            add(Box.createVerticalStrut(10), "wrap");
    
            //JPanel customerSupportPanel = new JPanel();
            JLabel questionsLabel = wizardPage.createAndDecorateMultiLine(I18n.tr("If you have any questions about your license, please contact "));
            //customerSupportPanel.add(questionsLabel);
            HyperlinkButton customerSupportButton = wizardPage.createAndDecorateHyperlink("http://www.limewire.com/support",
                                                                                          I18n.tr("Customer Support"));
            
            //customerSupportPanel.add(customerSupportButton);
            //add(customerSupportPanel, "wrap");
            add(questionsLabel, "align 50% 0%, split, gapright 0");
            add(customerSupportButton, "align 50% 0%, wrap");
        }
        add(Box.createVerticalStrut(1), "wrap, growy");
    }
    
    private static boolean areThereProblematicModules(List<ActivationItem> eventList) {
        for (ActivationItem item : eventList) {
            if (item.getStatus() != ActivationItem.Status.ACTIVE) {
                return true;
            }
        }
        return false;
    }

}
