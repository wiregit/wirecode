package org.limewire.ui.swing.options;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JRadioButton;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.updates.UpdateStyle;
import org.limewire.core.settings.UpdateSettings;
import org.limewire.setting.BooleanSetting;
import org.limewire.ui.swing.options.actions.CancelDialogAction;
import org.limewire.ui.swing.options.actions.DialogDisplayAction;
import org.limewire.ui.swing.options.actions.OKDialogAction;
import org.limewire.ui.swing.settings.BugSettings;
import org.limewire.ui.swing.settings.StartupSettings;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.shell.LimeAssociationOption;
import org.limewire.ui.swing.shell.LimeAssociations;
import org.limewire.ui.swing.tray.TrayNotifier;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.MacOSXUtils;
import org.limewire.ui.swing.util.WindowsUtils;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;

/**
 * System Option View.
 */
public class SystemOptionPanel extends OptionPanel {

    private final TrayNotifier trayNotifier;
    private final FileAssociationPanel fileAssociationPanel;
    private final StartupShutdownPanel startupShutdownPanel;
    private final BugsAndUpdatesPanel bugsAndUpdatesPanel;

    @Inject
    public SystemOptionPanel(TrayNotifier trayNotifier) {
        this.trayNotifier = trayNotifier;
        setLayout(new MigLayout("hidemode 2, insets 15 15 15 15, fillx, wrap", "", ""));

        setOpaque(false);

        fileAssociationPanel = new FileAssociationPanel();
        startupShutdownPanel = new StartupShutdownPanel();
        bugsAndUpdatesPanel = new BugsAndUpdatesPanel();

        add(fileAssociationPanel, "pushx, growx");
        add(startupShutdownPanel, "pushx, growx");
        add(bugsAndUpdatesPanel, "pushx, growx");
    }

    @Override
    boolean applyOptions() {
        boolean restart = fileAssociationPanel.applyOptions();
        restart |= startupShutdownPanel.applyOptions();
        restart |= bugsAndUpdatesPanel.applyOptions();
        return restart;
    }

    @Override
    boolean hasChanged() {
        return fileAssociationPanel.hasChanged() || startupShutdownPanel.hasChanged()
                || bugsAndUpdatesPanel.hasChanged();
    }

    @Override
    public void initOptions() {
        fileAssociationPanel.initOptions();
        startupShutdownPanel.initOptions();
        bugsAndUpdatesPanel.initOptions();
    }

    private static class FileAssociationPanel extends OptionPanel {

        private JCheckBox magnetCheckBox;
        private JCheckBox torrentCheckBox;
        private JCheckBox warnCheckBox;

        public FileAssociationPanel() {
            super(I18n.tr("File Associations"));
            setLayout(new MigLayout("insets 0, gap 0"));
            setOpaque(false);

            magnetCheckBox = new JCheckBox(I18n.tr("\".magnet\" files"));
            magnetCheckBox.setContentAreaFilled(false);
            magnetCheckBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    updateView();
                }
            });

            torrentCheckBox = new JCheckBox(I18n.tr("\".torrent\" files"));
            torrentCheckBox.setContentAreaFilled(false);
            torrentCheckBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    updateView();
                }
            });
            warnCheckBox = new JCheckBox(I18n
                    .tr("Warn me when other programs want to automatically open these types"));
            warnCheckBox.setContentAreaFilled(false);

            add(magnetCheckBox, "gapleft 5, gapbottom 5, wrap");
            add(torrentCheckBox, "gapleft 5, push");
            add(warnCheckBox);
        }

        @Override
        boolean applyOptions() {
            if (hasChanged(magnetCheckBox, SwingUiSettings.HANDLE_MAGNETS)) {
                applyOption(magnetCheckBox, SwingUiSettings.HANDLE_MAGNETS);
                LimeAssociationOption magnetAssociationOption = LimeAssociations
                        .getMagnetAssociation();
                if (magnetAssociationOption != null) {
                    magnetAssociationOption.setEnabled(magnetCheckBox.isSelected());
                }
            }

            if (hasChanged(torrentCheckBox, SwingUiSettings.HANDLE_TORRENTS)) {
                applyOption(torrentCheckBox, SwingUiSettings.HANDLE_TORRENTS);
                LimeAssociationOption torrentAssociationOption = LimeAssociations
                        .getTorrentAssociation();
                if (torrentAssociationOption != null) {
                    torrentAssociationOption.setEnabled(torrentCheckBox.isSelected());
                }
            }

            if (hasChanged(warnCheckBox, SwingUiSettings.WARN_FILE_ASSOCIATION_CHANGES)) {
                applyOption(warnCheckBox, SwingUiSettings.WARN_FILE_ASSOCIATION_CHANGES);
            }
            return false;
        }

        private void applyOption(JCheckBox checkBox, BooleanSetting booleanSetting) {
            booleanSetting.setValue(checkBox.isSelected());
        }

        @Override
        boolean hasChanged() {
            return hasChanged(magnetCheckBox, SwingUiSettings.HANDLE_MAGNETS)
                    || hasChanged(torrentCheckBox, SwingUiSettings.HANDLE_TORRENTS)
                    || hasChanged(warnCheckBox, SwingUiSettings.WARN_FILE_ASSOCIATION_CHANGES);
        }

        private boolean hasChanged(JCheckBox checkBox, BooleanSetting booleanSetting) {
            return booleanSetting.getValue() != checkBox.isSelected();
        }

        @Override
        public void initOptions() {
            initOption(magnetCheckBox, SwingUiSettings.HANDLE_MAGNETS.getValue()
                    && LimeAssociations.isMagnetAssociationSupported()
                    && LimeAssociations.getMagnetAssociation().isEnabled());
            initOption(torrentCheckBox, SwingUiSettings.HANDLE_TORRENTS.getValue()
                    && LimeAssociations.isTorrentAssociationSupported()
                    && LimeAssociations.getTorrentAssociation().isEnabled());
            initOption(warnCheckBox, SwingUiSettings.WARN_FILE_ASSOCIATION_CHANGES.getValue());
            updateView();
        }

        private void updateView() {
            boolean warnShouldBeVisible = magnetCheckBox.isSelected()
                    || torrentCheckBox.isSelected();
            warnCheckBox.setVisible(warnShouldBeVisible);

            boolean torrentShouldBeVisible = LimeAssociations.isTorrentAssociationSupported();
            torrentCheckBox.setVisible(torrentShouldBeVisible);

            boolean magnetShouldBeVisible = LimeAssociations.isMagnetAssociationSupported();
            magnetCheckBox.setVisible(magnetShouldBeVisible);

            setVisible(torrentShouldBeVisible || magnetShouldBeVisible);
        }

        private void initOption(JCheckBox checkBox, boolean value) {
            checkBox.setSelected(value);
        }
    }

    /**
     * When I press X is not shown for OSX, OSX automatically minimizes on an X
     * If Run at startup || minimize to try is selected, set System tray icon to
     * true
     */
    private class StartupShutdownPanel extends OptionPanel {

        private JCheckBox runAtStartupCheckBox;
        private JRadioButton minimizeButton;
        private JRadioButton exitButton;
        private boolean displaySystemTrayIcon = false;

        public StartupShutdownPanel() {
            super(I18n.tr("Startup and Shutdown"));

            runAtStartupCheckBox = new JCheckBox(I18n.tr("Run LimeWire on System Startup"));
            runAtStartupCheckBox.setContentAreaFilled(false);
            minimizeButton = new JRadioButton(I18n.tr("Minimize to system tray"));
            minimizeButton.setContentAreaFilled(false);
            exitButton = new JRadioButton(I18n.tr("Exit program"));
            exitButton.setContentAreaFilled(false);

            ButtonGroup buttonGroup = new ButtonGroup();
            buttonGroup.add(minimizeButton);
            buttonGroup.add(exitButton);

            if (OSUtils.isWindows() || OSUtils.isMacOSX()) {
                add(runAtStartupCheckBox, "wrap");
            }
            if (trayNotifier.supportsSystemTray()) {
                add(new JLabel(I18n.tr("When I press X:")), "wrap");
                add(minimizeButton, "gapleft 10");
                add(exitButton);
            }
        }

        @Override
        boolean applyOptions() {
            if (OSUtils.isMacOSX()) {
                MacOSXUtils.setLoginStatus(runAtStartupCheckBox.isSelected());
            } else if (WindowsUtils.isLoginStatusAvailable()) {
                BackgroundExecutorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        WindowsUtils.setLoginStatus(runAtStartupCheckBox.isSelected());
                    }
                });
            }
            StartupSettings.RUN_ON_STARTUP.setValue(runAtStartupCheckBox.isSelected());
            SwingUiSettings.MINIMIZE_TO_TRAY.setValue(minimizeButton.isSelected());
            if (SwingUiSettings.MINIMIZE_TO_TRAY.getValue()) {
                trayNotifier.showTrayIcon();
            } else {
                trayNotifier.hideTrayIcon();
            }
            return false;
        }

        @Override
        boolean hasChanged() {
            return StartupSettings.RUN_ON_STARTUP.getValue() != runAtStartupCheckBox.isSelected()
                    || SwingUiSettings.MINIMIZE_TO_TRAY.getValue() != minimizeButton.isSelected()
                    || isIconDisplayed();
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
            runAtStartupCheckBox.setSelected(StartupSettings.RUN_ON_STARTUP.getValue());
            minimizeButton.setSelected(SwingUiSettings.MINIMIZE_TO_TRAY.getValue());
            exitButton.setSelected(!SwingUiSettings.MINIMIZE_TO_TRAY.getValue());
        }
    }

    private static class BugsAndUpdatesPanel extends OptionPanel {
        private final BugsPanel bugsPanel;
        private final JCheckBox betaCheckBox;

        public BugsAndUpdatesPanel() {
            super(I18n.tr("Bugs and Updates"));
            betaCheckBox = new JCheckBox(I18n.tr("Tell me about Beta updates"));
            betaCheckBox.setContentAreaFilled(false);
            bugsPanel = new BugsPanel();

            add(betaCheckBox, "wrap");
            add(new JButton(new DialogDisplayAction(this, bugsPanel, I18n.tr("Bug Reports"), 
                    I18n.tr("Bug Reports..."), I18n.tr("Configure bug report settings."))), "wrap");
        }

        @Override
        public boolean applyOptions() {
            boolean restart = bugsPanel.applyOptions();
            if (betaCheckBox.isSelected()) {
                UpdateSettings.UPDATE_STYLE.setValue(UpdateStyle.STYLE_BETA);
            } else {
                UpdateSettings.UPDATE_STYLE.setValue(UpdateStyle.STYLE_MINOR);
            }
            return restart;
        }

        @Override
        public boolean hasChanged() {
            int expectedUpdateStyle = betaCheckBox.isSelected() ? UpdateStyle.STYLE_BETA : UpdateStyle.STYLE_MINOR;
            return bugsPanel.hasChanged() || UpdateSettings.UPDATE_STYLE.getValue() != expectedUpdateStyle;
        }

        @Override
        public void initOptions() {
            bugsPanel.initOptions();
            betaCheckBox.setSelected(UpdateSettings.UPDATE_STYLE.getValue() == 0);
        }
    }

    private static class BugsPanel extends OptionPanel {

        private JRadioButton showBugsBeforeSendingButton;
        private JRadioButton alwaysSendBugsButton;
        private JRadioButton neverSendBugsButton;

        public BugsPanel() {
            setLayout(new MigLayout("fill"));
            setOpaque(false);

            showBugsBeforeSendingButton = new JRadioButton(I18n.tr("Let me know about bugs before sending them"));
            showBugsBeforeSendingButton.setContentAreaFilled(false);
            alwaysSendBugsButton = new JRadioButton(I18n.tr("Always send bugs to Lime Wire"));
            alwaysSendBugsButton.setContentAreaFilled(false);
            neverSendBugsButton = new JRadioButton(I18n.tr("Never send bugs to Lime Wire"));
            neverSendBugsButton.setContentAreaFilled(false);

            ButtonGroup bugsButtonGroup = new ButtonGroup();
            bugsButtonGroup.add(showBugsBeforeSendingButton);
            bugsButtonGroup.add(alwaysSendBugsButton);
            bugsButtonGroup.add(neverSendBugsButton);
            
            add(showBugsBeforeSendingButton, "wrap");
            add(alwaysSendBugsButton, "wrap");
            add(neverSendBugsButton, "gapbottom 5, wrap");
            add(new JButton(new OKDialogAction()), "tag ok, alignx right, split 2");
            add(new JButton(new CancelDialogAction()), "tag cancel");
        }

        @Override
        boolean applyOptions() {
            if (showBugsBeforeSendingButton.isSelected()) {
                BugSettings.SHOW_BUGS.setValue(true);
                BugSettings.REPORT_BUGS.setValue(true);
            } else if (alwaysSendBugsButton.isSelected()) {
                BugSettings.SHOW_BUGS.setValue(false);
                BugSettings.REPORT_BUGS.setValue(true);
            } else {
                BugSettings.SHOW_BUGS.setValue(false);
                BugSettings.REPORT_BUGS.setValue(false);
            }
            return false;
        }

        @Override
        boolean hasChanged() {
            return hasChanged(alwaysSendBugsButton, BugSettings.REPORT_BUGS)
                    || hasChanged(showBugsBeforeSendingButton, BugSettings.SHOW_BUGS);
        }

        private boolean hasChanged(JRadioButton radioButton, BooleanSetting setting) {
            return setting.getValue() != radioButton.isSelected();
        }

        @Override
        public void initOptions() {
            if (BugSettings.SHOW_BUGS.getValue()) {
                showBugsBeforeSendingButton.setSelected(true);
            } else if (BugSettings.REPORT_BUGS.getValue()) {
                alwaysSendBugsButton.setSelected(true);
            } else {
                neverSendBugsButton.setSelected(true);
            }
        }
    }
}
