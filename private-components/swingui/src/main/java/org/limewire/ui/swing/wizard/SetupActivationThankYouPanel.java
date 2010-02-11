package org.limewire.ui.swing.wizard;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.activation.api.ActivationItem;
import org.limewire.core.api.Application;
import org.limewire.core.settings.ActivationSettings;
import org.limewire.ui.swing.activation.ActivationUtilities;
import org.limewire.ui.swing.activation.LabelWithLinkSupport;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

public class SetupActivationThankYouPanel extends JPanel {

    @Resource
    private Color thankYouColor;
    @Resource
    private Icon noticeIcon;
    @Resource
    private Color expiredMessageColor;
    
    public SetupActivationThankYouPanel(final WizardPage wizardPage, List<ActivationItem> eventList, boolean userHasPreexistingLicense,
            Application application) {
        super(new MigLayout("fill, insets 50 15 0 15, gap 0, gapy 0", "[]", "[][][][][][]"));
        
        GuiUtils.assignResources(this);
        
        if (containsOnlyExpiredModules(eventList)) {
            JLabel expiredLabel = wizardPage.createAndDecorateHeader(I18n.tr("It appears that all of your features have expired."));
            expiredLabel.setForeground(expiredMessageColor);
            add(expiredLabel, "align 50% 50%, wrap");
        } else if (containsSomeExpiredModules(eventList)) {
            JLabel expiredLabel = wizardPage.createAndDecorateHeader(I18n.tr("It appears that some of your features have expired."));
            expiredLabel.setForeground(expiredMessageColor);
            add(expiredLabel, "align 50% 50%, wrap");
        } else if (userHasPreexistingLicense) {
            JLabel yayLabel = wizardPage.createAndDecorateHeader(I18n.tr("Yay! Your license has been successfully activated."));
            yayLabel.setForeground(thankYouColor);
            add(yayLabel, "align 50% 50%, wrap");
        } else {
            JLabel thankYouLabel = wizardPage.createAndDecorateHeader(I18n.tr("Thank you! You have successfully activated your license."));
            thankYouLabel.setForeground(thankYouColor);
            add(thankYouLabel, "align 50% 50%, wrap");
        }

        add(Box.createVerticalStrut(10), "wrap");

        SetupActivationTable table = new SetupActivationTable(eventList, application);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(thankYouColor));
        configureEnclosingScrollPane(scrollPane, table);
        int numberOfItemsVisible = (eventList.size() > 4) ? 4 : eventList.size();
        int estimatedHeaderSize = 27;
        int estimatedRowSize = 29;
        int estimatedPaddingOnEnd = 10;
        scrollPane.setMinimumSize(new Dimension(400, estimatedHeaderSize + numberOfItemsVisible * estimatedRowSize + estimatedPaddingOnEnd));
        scrollPane.setPreferredSize(new Dimension(400, estimatedHeaderSize + numberOfItemsVisible * estimatedRowSize + estimatedPaddingOnEnd));
        
        if (containsProblematicModules(eventList)) {
            JPanel innerPanel = new JPanel(new MigLayout("fill, insets 0, gap 0, gapy 0", "[]", "[][][]"));
            
            innerPanel.add(scrollPane, "align 50% 50%, wrap");

            innerPanel.add(Box.createVerticalStrut(10), "wrap");

            if (containsSomeExpiredModules(eventList)) {
                LabelWithLinkSupport renewFeaturesLabel = new LabelWithLinkSupport();
                Font font = wizardPage.createAndDecorateLabel("").getFont();
                renewFeaturesLabel.setText("<html>" + "<font size=\"3\" face=\"" + font.getFontName() + "\">" 
                                    + I18n.tr("You can renew your features at your {0}user account{1} page.", "<a href='" + ActivationSettings.ACTIVATION_ACCOUNT_SETTINGS_HOST.get() + "'>", "</a>") 
                                    + "</font></html>");
                
                innerPanel.add(Box.createVerticalStrut(10), "align 0% 50%, wrap");
                JLabel noticeLabel = new JLabel(noticeIcon);
                innerPanel.add(noticeLabel, "align 50% 50%, split");
                innerPanel.add(renewFeaturesLabel, "align 50% 50%, wrap");
            } else {
                JLabel infoTextLine1 = wizardPage.createAndDecorateMultiLine(I18n.tr("* " + "One or more of your features is currently not supported."));
                JLabel infoTextLine2 = wizardPage.createAndDecorateMultiLine("<html>" + I18n.tr("Click on {0} for more information.", "<img src='" + ActivationUtilities.getInfoIconURL() + "'>") + "</html>");
                innerPanel.add(infoTextLine1, "align 0% 50%, wrap");
                innerPanel.add(infoTextLine2, "align 0% 50%, wrap");
            }
            
            add(innerPanel, "align 50% 0%, wrap");
        } else {
            add(scrollPane, "align 50% 0%, wrap");

            add(Box.createVerticalStrut(10), "wrap");    
        }
        add(Box.createVerticalStrut(1), "wrap, growy");
    }
    
    private static boolean containsProblematicModules(List<ActivationItem> eventList) {
        for (ActivationItem item : eventList) {
            if (item.getStatus() == ActivationItem.Status.UNAVAILABLE || item.getStatus() == ActivationItem.Status.UNUSEABLE_LW 
                 || item.getStatus() == ActivationItem.Status.UNUSEABLE_OS || item.getStatus() == ActivationItem.Status.EXPIRED) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsOnlyExpiredModules(List<ActivationItem> eventList) {
        for (ActivationItem item : eventList) {
            if (item.getStatus() != ActivationItem.Status.EXPIRED) {
                return false;
            }
        }
        return true;
    }

    private static boolean containsSomeExpiredModules(List<ActivationItem> eventList) {
        for (ActivationItem item : eventList) {
            if (item.getStatus() == ActivationItem.Status.EXPIRED) {
                return true;
            }
        }
        return false;
    }

    /**
     * Fills in the top right corner if a scrollbar appears with an empty table
     * header.
     */
    protected void configureEnclosingScrollPane(JScrollPane scrollPane, JTable table) {
        JPanel cornerComponent = new JPanel();
        cornerComponent.setBackground(((SetupActivationTable)table).getHeaderColor());
        scrollPane.setCorner(JScrollPane.UPPER_RIGHT_CORNER, cornerComponent);
    }   
}
