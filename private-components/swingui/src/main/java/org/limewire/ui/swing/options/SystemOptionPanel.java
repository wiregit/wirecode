package org.limewire.ui.swing.options;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JRadioButton;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.settings.ApplicationSettings;
import org.limewire.core.settings.StartupSettings;
import org.limewire.setting.BooleanSetting;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * System Option View
 */
@Singleton
public class SystemOptionPanel extends OptionPanel {

    private FileAssociationPanel fileAssociationPanel;

    private StartupShutdownPanel startupShutdownPanel;

    private UpdatesBugsPanel updatesBugsPanel;

    @Inject
    public SystemOptionPanel() {
        setLayout(new MigLayout("insets 15 15 15 15, fillx, wrap", "", ""));

        setOpaque(false);

        add(getFileAssociationPanel(), "pushx, growx");
        add(getStartupShutdownPanel(), "pushx, growx");
        add(getUpdatesBugsPanel(), "pushx, growx");
    }

    private OptionPanel getFileAssociationPanel() {
        if (fileAssociationPanel == null) {
            fileAssociationPanel = new FileAssociationPanel();
        }
        return fileAssociationPanel;
    }

    private OptionPanel getStartupShutdownPanel() {
        if (startupShutdownPanel == null) {
            startupShutdownPanel = new StartupShutdownPanel();
        }
        return startupShutdownPanel;
    }

    private OptionPanel getUpdatesBugsPanel() {
        if (updatesBugsPanel == null) {
            updatesBugsPanel = new UpdatesBugsPanel();
        }
        return updatesBugsPanel;
    }

    @Override
    void applyOptions() {
        getFileAssociationPanel().applyOptions();
        getStartupShutdownPanel().applyOptions();
        getUpdatesBugsPanel().applyOptions();
    }

    @Override
    boolean hasChanged() {
        return getFileAssociationPanel().hasChanged() || getStartupShutdownPanel().hasChanged()
                || getUpdatesBugsPanel().hasChanged();
    }

    @Override
    public void initOptions() {
        getFileAssociationPanel().initOptions();
        getStartupShutdownPanel().initOptions();
        getUpdatesBugsPanel().initOptions();
    }

    private class FileAssociationPanel extends OptionPanel {

        private JCheckBox magnetCheckBox;

        private JCheckBox torrentCheckBox;

        private JCheckBox warnCheckBox;

        private JLabel warnLabel;

        public FileAssociationPanel() {
            super(I18n.tr("File Associations"));

            magnetCheckBox = new JCheckBox();
            magnetCheckBox.setContentAreaFilled(false);
            magnetCheckBox.addActionListener(new ActionListener() {
                @Override
                 public void actionPerformed(ActionEvent e) {
                    updateWarnOption();
                } 
             });
            
            
            torrentCheckBox = new JCheckBox();
            torrentCheckBox.setContentAreaFilled(false);
            torrentCheckBox.addActionListener(new ActionListener() {
               @Override
                public void actionPerformed(ActionEvent e) {
                   updateWarnOption();
               } 
            });
            warnCheckBox = new JCheckBox();
            warnCheckBox.setContentAreaFilled(false);

            add(magnetCheckBox);
            add(new JLabel(".magnet files"), "wrap");

            add(torrentCheckBox);
            add(new JLabel(".torrent files"), "push");

            add(warnCheckBox);

            warnLabel = new JLabel("Warn me when other programs take these associations");
            add(warnLabel, "wrap");
        }

        @Override
        void applyOptions() {
            // TODO integrate shell association code
            applyOption(magnetCheckBox, ApplicationSettings.HANDLE_MAGNETS);
            applyOption(torrentCheckBox, ApplicationSettings.HANDLE_TORRENTS);
            applyOption(warnCheckBox, ApplicationSettings.WARN_FILE_ASSOCIATION_CHANGES);
            // TODO check warnings on app startup and pop window to handle
            // issues.
        }

        private void applyOption(JCheckBox checkBox, BooleanSetting booleanSetting) {
            booleanSetting.setValue(checkBox.isSelected());
        }

        @Override
        boolean hasChanged() {
            return hasChanged(magnetCheckBox, ApplicationSettings.HANDLE_MAGNETS)
                    || hasChanged(torrentCheckBox, ApplicationSettings.HANDLE_TORRENTS)
                    || hasChanged(warnCheckBox, ApplicationSettings.WARN_FILE_ASSOCIATION_CHANGES);
        }

        private boolean hasChanged(JCheckBox checkBox, BooleanSetting booleanSetting) {
            return booleanSetting.getValue() != checkBox.isSelected();
        }

        @Override
        public void initOptions() {
            initOption(magnetCheckBox, ApplicationSettings.HANDLE_MAGNETS);
            initOption(torrentCheckBox, ApplicationSettings.HANDLE_TORRENTS);
            initOption(warnCheckBox, ApplicationSettings.WARN_FILE_ASSOCIATION_CHANGES);
            updateWarnOption();
        }

        private void updateWarnOption() {
            boolean warnShouldBeVisible = magnetCheckBox.isSelected()
                    || torrentCheckBox.isSelected();

            warnCheckBox.setVisible(warnShouldBeVisible);
            warnLabel.setVisible(warnShouldBeVisible);
        }

        private void initOption(JCheckBox checkBox, BooleanSetting booleanSetting) {
            checkBox.setSelected(booleanSetting.getValue());
        }
    }

    /**
     * When I press X is not shown for OSX, OSX automatically minimizes on an X
     * If Run at startup || minimize to try is selected, set System tray icon to
     * true
     * 
     * TODO: revisit this and check
     */
    private class StartupShutdownPanel extends OptionPanel {

        private JCheckBox runAtStartupCheckBox;

        private JRadioButton minimizeButton;

        private JRadioButton exitButton;

        private ButtonGroup buttonGroup;

        private boolean displaySystemTrayIcon = false;

        private boolean shutdownAfterTransfer = false;

        public StartupShutdownPanel() {
            super(I18n.tr("Startup and Shutdown"));

            runAtStartupCheckBox = new JCheckBox();
            runAtStartupCheckBox.setContentAreaFilled(false);
            minimizeButton = new JRadioButton();
            minimizeButton.setContentAreaFilled(false);
            exitButton = new JRadioButton();
            exitButton.setContentAreaFilled(false);

            buttonGroup = new ButtonGroup();
            buttonGroup.add(minimizeButton);
            buttonGroup.add(exitButton);

            add(runAtStartupCheckBox, "split");
            add(new JLabel("Run LimeWire on System Startup"), "wrap");

            if (!OSUtils.isAnyMac()) {
                add(new JLabel("When I press X:"), "wrap");

                add(minimizeButton, "gapleft 25, split");
                add(new JLabel("Minimize to system tray"), "gapafter 20");
                add(exitButton);
                add(new JLabel("Exit program"));
            }
        }

        @Override
        void applyOptions() {
            StartupSettings.RUN_ON_STARTUP.setValue(runAtStartupCheckBox.isSelected());
            ApplicationSettings.MINIMIZE_TO_TRAY.setValue(minimizeButton.isSelected());

            // if minimize or run at startup is selected, system tray icon must
            // be shown
            if ((minimizeButton.isSelected() || runAtStartupCheckBox.isSelected())
                    && OSUtils.supportsTray()) {
                ApplicationSettings.DISPLAY_TRAY_ICON.setValue(true);
            } else {
                ApplicationSettings.DISPLAY_TRAY_ICON.setValue(false);
            }
            ApplicationSettings.SHUTDOWN_AFTER_TRANSFERS.setValue(false);
        }

        @Override
        boolean hasChanged() {
            return StartupSettings.RUN_ON_STARTUP.getValue() != runAtStartupCheckBox.isSelected()
                    || ApplicationSettings.MINIMIZE_TO_TRAY.getValue() != minimizeButton
                            .isSelected() || isIconDisplayed() || shutdownAfterTransfer != false;
        }

        private boolean isIconDisplayed() {
            if ((runAtStartupCheckBox.isSelected() || minimizeButton.isSelected())
                    && OSUtils.supportsTray())
                return displaySystemTrayIcon != true;
            else
                return displaySystemTrayIcon != false;
        }

        @Override
        public void initOptions() {
            // TODO: should shutdown after transfer be set to false here??
            shutdownAfterTransfer = ApplicationSettings.SHUTDOWN_AFTER_TRANSFERS.getValue();
            if (shutdownAfterTransfer) {
                ApplicationSettings.SHUTDOWN_AFTER_TRANSFERS.setValue(false);
                ApplicationSettings.MINIMIZE_TO_TRAY.setValue(true);
                shutdownAfterTransfer = false;
            }

            runAtStartupCheckBox.setSelected(StartupSettings.RUN_ON_STARTUP.getValue());
            minimizeButton.setSelected(ApplicationSettings.MINIMIZE_TO_TRAY.getValue());
            exitButton.setSelected(!ApplicationSettings.MINIMIZE_TO_TRAY.getValue());

            // load these to ensure that we override them correctly since they
            // aren't
            // directly accessable in 5.0
            displaySystemTrayIcon = ApplicationSettings.DISPLAY_TRAY_ICON.getValue();
        }
    }

    private class UpdatesBugsPanel extends OptionPanel {

        private JCheckBox betaCheckBox;

        private JCheckBox bugCheckBox;

        private JCheckBox bugMessageCheckBox;

        public UpdatesBugsPanel() {
            super(I18n.tr("Updates and Bugs"));

            betaCheckBox = new JCheckBox();
            betaCheckBox.setContentAreaFilled(false);
            bugCheckBox = new JCheckBox();
            bugCheckBox.setContentAreaFilled(false);
            bugMessageCheckBox = new JCheckBox();
            bugMessageCheckBox.setContentAreaFilled(false);

            add(betaCheckBox, "split");
            add(new JLabel("Tell me about Beta updates"), "wrap");

            add(bugCheckBox, "split");
            add(new JLabel("Report bugs to LimeWire"), "wrap");

            add(bugMessageCheckBox, "gapleft 25, split");
            add(new JLabel("Show me the bug report before sending it"), "wrap");
        }

        @Override
        void applyOptions() {

        }

        @Override
        boolean hasChanged() {
            return false;
        }

        @Override
        public void initOptions() {

        }
    }
}
