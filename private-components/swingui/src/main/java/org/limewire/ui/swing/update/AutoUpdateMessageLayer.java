package org.limewire.ui.swing.update;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.text.html.HTMLDocument;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Application;
import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.concurrent.ManagedThread;
import org.limewire.core.api.updates.AutoUpdateHelper;
import org.limewire.core.api.updates.UpdateInformation;
import org.limewire.core.settings.UpdateSettings;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.components.ImageViewPort;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.ResizeUtils;

public class AutoUpdateMessageLayer{
    
    @Resource
    private Icon backgroundIcon;
    @Resource
    private Color backgroundColor;
    @Resource
    private Color foregroundColor;
    @Resource
    private Font topFont; 
    @Resource
    private Font contentFont; 
    
    private final org.limewire.core.api.Application application;
    
    private  JXPanel messagePanel;
    
    private AutoUpdateHelper autoUpdateHelper;
    
    private volatile int updateAttemptCount = 0;
    
    private AtomicBoolean updateInProgress = new AtomicBoolean(false);
    
    public AutoUpdateMessageLayer(UpdateInformation updateInformation, 
            org.limewire.core.api.Application application, AutoUpdateHelper autoUpdateHelper) {
        GuiUtils.assignResources(this);
        
        this.application = application;
        this.messagePanel = new DownloadUpdatePanel();
        this.autoUpdateHelper = autoUpdateHelper;
        
    }
    
    public void showMessage(){
        if(updateInProgress.getAndSet(true)){ //if update is in progress ignore the message.
            updateAttemptCount++;
            JDialog dialog = FocusJOptionPane.createDialog(I18n.tr("New Version Available!"), null, messagePanel);
            dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE); 
            dialog.setLocationRelativeTo(GuiUtils.getMainFrame());
            dialog.setModal(true);
            dialog.pack();
            dialog.setVisible(true);
        }
    }
       
    private class DownloadUpdatePanel extends JXPanel{
        public DownloadUpdatePanel(){
            setBackground(backgroundColor);
            setLayout(new MigLayout("fill, insets 10 10 10 10, gap 6"));
                    
            add(createTopLabel(I18n.tr("<b>Your LimeWire Software is Not Up to Date</b>")), "alignx 50%, gapbottom 7, wrap");
            add(createContentArea(I18n.tr("For best possible performance, make sure you always use the latest version of LimeWire. We no longer support the version on your computer.<br/><br/>Updating to latest version is <b>FREE, quick and easy</b>, and your LimeWire library will stay completely intact."), contentFont), "grow, wrap, gapbottom 10");
            
            JButton downloadButton = new JButton();
            downloadButton.requestFocusInWindow();
            downloadButton.setAction(new AbstractAction(I18n.tr("Update Now")) {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    Thread autoUpdateThread = new ManagedThread(new Runnable() {                       
                            @Override
                            public void run() {
                                final boolean downloadSuccess = true;//TODO autoUpdateHelper.downloadUpdates();
                                SwingUtilities.invokeLater(new Runnable(){

                                    @Override
                                    public void run() {
                                        if(downloadSuccess){
                                            messagePanel = new InstallUpdatePanel();
                                            updateAttemptCount = 0;
                                            showMessage();                                              
                                        }else if(updateAttemptCount < UpdateSettings.AUTO_UPDATE_MAX_ATTEMPTS.getValue()){
                                            JFrame frame = GuiUtils.getMainFrame();
                                            FocusJOptionPane.showMessageDialog(frame, I18n.tr("Update download was interrupted. Limewire will try again."), 
                                                    I18n.tr("Error Downloading Updates"), JOptionPane.ERROR_MESSAGE );
                                            showMessage();
                                        }else{
                                            JFrame frame = GuiUtils.getMainFrame();
                                            FocusJOptionPane.showMessageDialog(frame, I18n.tr("Limewire was not able to download an important update. Limewire will close now and try to update on next launch."), 
                                                    I18n.tr("Error Downloading Updates"), JOptionPane.WARNING_MESSAGE );

                                            File updateOnNextLaunchCommand = autoUpdateHelper.getAutoUpdateCommandScript();
                                            UpdateSettings.AUTO_UPDATE_COMMAND.set(updateOnNextLaunchCommand.getAbsolutePath());
                                            UpdateSettings.DOWNLOADED_UPDATE_VERSION.set(UpdateSettings.AUTO_UPDATE_VERSION.get());
                                            exitApplication();
                                        }
                                        
                                    }
                                    }
                                );
                            }
                        }, "Auto-Update-Thread");
                    autoUpdateThread.start();
                    close();                 
                }
            });
            
            add(downloadButton, "alignx 50%");
        }
    }
    
    private class InstallUpdatePanel extends JXPanel{
        
        public InstallUpdatePanel(){
            setBackground(backgroundColor);
            setLayout(new MigLayout("fill, insets 10 10 10 10, gap 6"));
                    
            add(createTopLabel("LimeWire " + application.getVersion()), "alignx 50%, gapbottom 7, wrap");
            add(createContentArea(I18n.tr("<br/><br/><b>The new version is ready for you.</b><br/>Restart LimeWire to use it."), topFont), "grow, wrap, gapbottom 10");
            
            JButton installButton = new JButton();
            installButton.requestFocusInWindow();
            installButton.setAction(new AbstractAction(I18n.tr("Restart Now")) {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    updateInProgress.getAndSet(false);
                    close(); 
                    application.setShutdownFlag(UpdateSettings.AUTO_UPDATE_COMMAND.get());
                    exitApplication();       
                }
            });
            
            add(installButton, "alignx 50%");
        }
    }
    
    private void close() {
        Window window = SwingUtilities.getWindowAncestor(messagePanel);
        window.setVisible(false);
        window.dispose();
    }
    
    private void exitApplication(){
        ActionMap actionMap = Application.getInstance().getContext().getActionMap();
        Action exitApplication = actionMap.get("exitApplication");
        exitApplication.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "Shutdown"));
    }
    
    
    private JComponent createTopLabel(String text) {
        JEditorPane pane = new JEditorPane();
        pane.setContentType("text/html");
        pane.setEditable(false);
        pane.addHyperlinkListener(GuiUtils.getHyperlinkListener());
        pane.setOpaque(false);
        // set the color of the foreground appropriately.
        text = updateForeground(foregroundColor, text);
        pane.setText(text);
        pane.setCaretPosition(0);
        setNativeFontRenderering(pane, topFont);
        
        return pane;
    }
    
    private JComponent createContentArea(String text, Font font) {
        JEditorPane pane = new JEditorPane();
        pane.setContentType("text/html");
        pane.setEditable(false);
        pane.addHyperlinkListener(GuiUtils.getHyperlinkListener());
        // set the color of the foreground appropriately.
        text = updateForeground(foregroundColor, text);
        pane.setText(text);
        pane.setCaretPosition(0);
        
        setNativeFontRenderering(pane, font);
        
        //must be false to view the background image
        pane.setOpaque(false);
        //shift the text so as to not paint over the image
        pane.setMargin(new Insets(36, 162, 0, 30));
        ImageViewPort imageViewPort = new ImageViewPort(((ImageIcon)backgroundIcon).getImage());
        imageViewPort.setView(pane);
        
        JScrollPane scroller = new JScrollPane();
        scroller.setViewport(imageViewPort);
        ResizeUtils.forceSize(scroller, new Dimension(backgroundIcon.getIconWidth(), backgroundIcon.getIconHeight()));
        scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        return scroller;
    }
    
    private void setNativeFontRenderering(JEditorPane pane, Font font) {
        // Add a CSS rule to force body tags to use the specified font
        // instead of the value in javax.swing.text.html.default.csss
        String bodyRule = "body { font-family: " + font.getFamily() + "; " +
                "font-size: " + font.getSize() + "pt; }";
        ((HTMLDocument)pane.getDocument()).getStyleSheet().addRule(bodyRule);
    }
    
    
    private String updateForeground(Color color, String html) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        String hex = GuiUtils.toHex(r) + GuiUtils.toHex(g) + GuiUtils.toHex(b);
        return "<html><body text='#" + hex + "'>" + html + "</body></html>";
    }

}
