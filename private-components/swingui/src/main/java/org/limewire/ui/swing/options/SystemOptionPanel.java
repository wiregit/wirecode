package org.limewire.ui.swing.options;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JRadioButton;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.settings.StartupSettings;
import org.limewire.ui.swing.util.I18n;

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
        if(fileAssociationPanel == null) {
            fileAssociationPanel = new FileAssociationPanel();
        }
        return fileAssociationPanel;
    }
    
    private OptionPanel getStartupShutdownPanel() {
        if(startupShutdownPanel == null) {
            startupShutdownPanel = new StartupShutdownPanel();
        }
        return startupShutdownPanel;
    }
    
    private OptionPanel getUpdatesBugsPanel() {
        if(updatesBugsPanel == null) {
            updatesBugsPanel = new UpdatesBugsPanel();
        }
        return updatesBugsPanel;
    }

    @Override
    void applyOptions() {
        // TODO Auto-generated method stub
        
    }

    @Override
    boolean hasChanged() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    void initOptions() {
        // TODO Auto-generated method stub
        
    }
    
    private class FileAssociationPanel extends OptionPanel {

        private JCheckBox magnetCheckBox;
        private JCheckBox torrentCheckBox;
        private JCheckBox warnCheckBox;
        
        public FileAssociationPanel() {
            super(I18n.tr("File Associations"));
            
            magnetCheckBox = new JCheckBox();
            torrentCheckBox = new JCheckBox();
            warnCheckBox = new JCheckBox();
            
            add(magnetCheckBox);
            add(new JLabel(".magnet files"), "wrap");
            
            add(torrentCheckBox);
            add(new JLabel(".torrent files"), "push");
            
            add(warnCheckBox);
            add(new JLabel("Warn me when other programs take these associations"), "wrap");
        }
        
        @Override
        void applyOptions() {

        }

        @Override
        boolean hasChanged() {
            return false;
        }

        @Override
        void initOptions() {
            
        }
    }
    
    /**
     * When I press X is not shown for OSX, OSX automatically minimizes on an X
     * If Run at startup || minimize to try is selected, set System tray icon to true
     */
    private class StartupShutdownPanel extends OptionPanel {

        private JCheckBox runAtStartupCheckBox;
        private JRadioButton minimizeButton;
        private JRadioButton exitButton;
        private ButtonGroup buttonGroup;
        
        public StartupShutdownPanel() {
            super(I18n.tr("Startup and Shutdown"));
            
            runAtStartupCheckBox = new JCheckBox();
            minimizeButton = new JRadioButton();
            exitButton = new JRadioButton();
            
            buttonGroup = new ButtonGroup();
            buttonGroup.add(runAtStartupCheckBox);
            buttonGroup.add(exitButton);
            
            add(runAtStartupCheckBox, "split");
            add(new JLabel("Run LimeWire on System Startup"), "wrap");

            add(new JLabel("When I press X:"), "wrap");
            
            add(minimizeButton, "gapleft 25, split");
            add(new JLabel("Minimize to system tray"), "gapafter 20");
            add(exitButton);
            add(new JLabel("Exit program"));
        }
        
        @Override
        void applyOptions() {
            //TODO: fix this
        }

        @Override
        boolean hasChanged() {
            return StartupSettings.RUN_ON_STARTUP.getValue() != runAtStartupCheckBox.isSelected();
        }

        @Override
        void initOptions() {
            runAtStartupCheckBox.setSelected(StartupSettings.RUN_ON_STARTUP.getValue());
        }
    }
    
    private class UpdatesBugsPanel extends OptionPanel {

        private JCheckBox betaCheckBox;
        private JCheckBox bugCheckBox;
        private JCheckBox bugMessageCheckBox;
        
        public UpdatesBugsPanel() {
            super(I18n.tr("Updates and Bugs"));
            
            betaCheckBox = new JCheckBox();
            bugCheckBox = new JCheckBox();
            bugMessageCheckBox = new JCheckBox();
            
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
        void initOptions() {

        }
    }
}
