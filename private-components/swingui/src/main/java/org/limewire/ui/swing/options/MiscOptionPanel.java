package org.limewire.ui.swing.options;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.Locale;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.Application;
import org.limewire.core.settings.ApplicationSettings;
import org.limewire.ui.swing.action.UrlAction;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.LanguageComboBox;
import org.limewire.ui.swing.search.resultpanel.LicenseWarningDownloadPreprocessor;
import org.limewire.ui.swing.settings.QuestionsHandler;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.LanguageUtils;

import com.google.inject.Inject;

/**
 * Misc Option View.
 */
public class MiscOptionPanel extends OptionPanel {

    // backwards compatibility
    private static final int SKIP_WARNING_VALUE = LicenseWarningDownloadPreprocessor.SKIP_WARNING_VALUE;
    private static final int SHOW_WARNING_VALUE = LicenseWarningDownloadPreprocessor.SHOW_WARNING_VALUE;
    
    private static final String TRANSLATE_URL = "http://wiki.limewire.org/index.php?title=Translate";
    
    private NotificationsPanel notificationsPanel;
    
    private Locale currentLanguage;
    private JLabel comboLabel;
    private final JComboBox languageDropDown;
    private final HyperlinkButton translateButton;
    private final JCheckBox shareUsageDataCheckBox;

    @Inject
    public MiscOptionPanel(Application application) {
        
        GuiUtils.assignResources(this);

        setLayout(new MigLayout("nogrid, insets 15 15 15 15, fillx, gap 4"));

        comboLabel = new JLabel(I18n.tr("Language:"));
        languageDropDown = new LanguageComboBox();
        
        translateButton = new HyperlinkButton(new UrlAction(I18n.tr("Help translate LimeWire!"), TRANSLATE_URL, application));
        
        add(comboLabel);
        add(languageDropDown);
        add(translateButton, "wrap");
        
        add(getNotificationsPanel(), "growx, wrap");

        shareUsageDataCheckBox = new JCheckBox((I18n.tr("Help improve LimeWire by sending us anonymous usage data")));
        shareUsageDataCheckBox.setOpaque(false);
        add(shareUsageDataCheckBox);
        add(new LearnMoreButton("http://www.limewire.com/client_redirect/?page=anonymousDataCollection", application), "wrap");
    }

    private OptionPanel getNotificationsPanel() {
        if(notificationsPanel == null) {
            notificationsPanel = new NotificationsPanel();
        }
        
        return notificationsPanel;
    }

    @Override
    ApplyOptionResult applyOptions() {
        
        
        ApplicationSettings.ALLOW_ANONYMOUS_STATISTICS_GATHERING.setValue(shareUsageDataCheckBox.isSelected());
        
        Locale selectedLocale = (Locale) languageDropDown.getSelectedItem();
        
        ApplyOptionResult result = getNotificationsPanel().applyOptions();
                
        // if the language changed, always notify about a required restart
        if(selectedLocale != null && !currentLanguage.equals(selectedLocale) && result.isSuccessful()) {
            currentLanguage = selectedLocale;
            LanguageUtils.setLocale(selectedLocale);
            result.updateRestart(true);
        }
        return result;
    }

    @Override
    void setOptionTabItem(OptionTabItem tab) {
        super.setOptionTabItem(tab);
        getNotificationsPanel().setOptionTabItem(tab);
    }

    @Override
    boolean hasChanged() {
        Locale selectedLocale = (Locale) languageDropDown.getSelectedItem();
        
        return getNotificationsPanel().hasChanged() ||
                selectedLocale != currentLanguage ||
                ApplicationSettings.ALLOW_ANONYMOUS_STATISTICS_GATHERING.getValue() 
                    != shareUsageDataCheckBox.isSelected();
    }

    @Override
    public void initOptions() {
        shareUsageDataCheckBox.setSelected(ApplicationSettings.ALLOW_ANONYMOUS_STATISTICS_GATHERING.getValue());
        getNotificationsPanel().initOptions();
        currentLanguage = LanguageUtils.getCurrentLocale();
        // if language got corrupted somehow, resave it
        // this shouldn't be possible but somehow currentLanguage can be 
        // null on OSX.
        if(currentLanguage == null) {
            LanguageUtils.setLocale(Locale.ENGLISH);
            currentLanguage = Locale.ENGLISH;
        }
        languageDropDown.setSelectedItem(currentLanguage);
    }

    private class NotificationsPanel extends OptionPanel {

        private JCheckBox showNotificationsCheckBox;
        private JButton resetWarningsButton;

        public NotificationsPanel() {
            super(tr("Notifications and Warnings"));

            showNotificationsCheckBox = new JCheckBox(tr("Show popup system notifications"));
            showNotificationsCheckBox.setContentAreaFilled(false);
            resetWarningsButton = new JButton(new AbstractAction(I18n.tr("Reset")){
                @Override
                public void actionPerformed(ActionEvent e) {
                    resetWarnings();
                }
            });
            
            add(showNotificationsCheckBox, "wrap");
            add(new JLabel(I18n.tr("Reset warning messages")));
            add(resetWarningsButton, "wrap");
        }

        @Override
        ApplyOptionResult applyOptions() {
            SwingUiSettings.SHOW_NOTIFICATIONS.setValue(showNotificationsCheckBox.isSelected());
            return new ApplyOptionResult(false, true);
        }

        @Override
        boolean hasChanged() {
            return showNotificationsCheckBox.isSelected() != SwingUiSettings.SHOW_NOTIFICATIONS.getValue();
        }

        @Override
        public void initOptions() {
            showNotificationsCheckBox.setSelected(SwingUiSettings.SHOW_NOTIFICATIONS.getValue());
        }
    }
    
    private static void resetWarnings() {
        int skipWarningSettingValue = getLicenseSettingValueFromCheckboxValue(true);
        QuestionsHandler.SKIP_FIRST_DOWNLOAD_WARNING.setValue(skipWarningSettingValue);
        QuestionsHandler.WARN_TORRENT_SEED_MORE.revertToDefault();
        QuestionsHandler.CONFIRM_BLOCK_HOST.revertToDefault();
        SwingUiSettings.WARN_DOWNLOAD_DANGEROUS.revertToDefault();
        SwingUiSettings.WARN_DOWNLOAD_SCAN_FAILED.revertToDefault();
        SwingUiSettings.WARN_DOWNLOAD_THREAT_FOUND.revertToDefault();
    }
    
    private static int getLicenseSettingValueFromCheckboxValue(boolean isSelected) {
        return isSelected ? SHOW_WARNING_VALUE : SKIP_WARNING_VALUE;
    }
}
