package org.limewire.ui.swing.update;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.File;

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
import javax.swing.UIManager;
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
    
    private final org.limewire.core.api.Application application;
    
    private  JXPanel messagePanel;
    
    private AutoUpdateHelper autoUpdateHelper;
    
    private int updateAttemptCount = 0;
    
    public AutoUpdateMessageLayer(UpdateInformation updateInformation, 
            org.limewire.core.api.Application application, AutoUpdateHelper autoUpdateHelper) {
        GuiUtils.assignResources(this);
        
        this.application = application;
        this.messagePanel = new DownloadUpdatePanel();
        this.autoUpdateHelper = autoUpdateHelper;
        
    }
    
    public void showMessage(){
        updateAttemptCount++;
        JDialog dialog = FocusJOptionPane.createDialog(I18n.tr("New Version Available!"), null, messagePanel);
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE); 
        dialog.setLocationRelativeTo(GuiUtils.getMainFrame());
        dialog.setModal(true);
        dialog.pack();
        dialog.setVisible(true);
        
    }
       
    private class DownloadUpdatePanel extends JXPanel{
        public DownloadUpdatePanel(){
            setBackground(backgroundColor);
            setLayout(new MigLayout("fill, insets 10 10 10 10, gap 6"));
                    
            add(createTopLabel("<b>YOUR LIMEWIRE SOFTWARE IS NOT UP TO DATE</b>"), "alignx 50%, gapbottom 7, wrap");
            add(createContentArea("For best possible performance, make sure you always use the latest version of LimeWire, We no longer support the version on your computer, and <b>you're missing out on some great new features.</b> <br/><br/> Updating to latest version is <b>FREE quick and easy</b>, and your LimeWire library will stay completely intact."), "grow, wrap, gapbottom 10");
            
            JButton downloadButton = new JButton();
            downloadButton.requestFocusInWindow();
            downloadButton.setAction(new AbstractAction(I18n.tr("Update Now")) {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    Thread autoUpdateThread = 
                        new ManagedThread(new Runnable() {                                                  
                            @Override
                            public void run() {
                                final boolean downloadSuccess = autoUpdateHelper.downloadUpdates();
                                SwingUtilities.invokeLater(new Runnable(){

                                    @Override
                                    public void run() {
                                        if(downloadSuccess){
                                            messagePanel = new InstallUpdatePanel();
                                            updateAttemptCount = 0;
                                            showMessage();                                              
                                        }else if(updateAttemptCount < UpdateSettings.AUTO_UPDATE_MAX_ATTEMTPS.getValue()){
                                            JFrame frame = GuiUtils.getMainFrame();
                                            FocusJOptionPane.showMessageDialog(frame, I18n.tr("Update Download was interrupted. Limewire will try again."), 
                                                    I18n.tr("Error Downloading Updates"), JOptionPane.ERROR_MESSAGE );
                                            showMessage();
                                        }else{
                                            JFrame frame = GuiUtils.getMainFrame();
                                            FocusJOptionPane.showMessageDialog(frame, I18n.tr("Limewire was not able to download an important update. Limewire will close now and try to update on next launch."), 
                                                    I18n.tr("Error Downloading Updates"), JOptionPane.WARNING_MESSAGE );

                                            File updateOnNextLaunchCommand = autoUpdateHelper.getAutoUpdateCommandScript();
                                            UpdateSettings.AUTO_UPDATE_COMMAND.set(updateOnNextLaunchCommand.getAbsolutePath());
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
            add(createContentArea("<br/><br/><br/>The new version is ready for you. <br/><br/> Restart LimeWire to use it."), "grow, wrap, gapbottom 10");
            
            JButton installButton = new JButton();
            installButton.requestFocusInWindow();
            installButton.setAction(new AbstractAction(I18n.tr("Restart Now")) {
                
                @Override
                public void actionPerformed(ActionEvent e) {
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
        setNativeFontRenderering(pane);
        
        return pane;
    }
    
    private JComponent createContentArea(String text) {
        JEditorPane pane = new JEditorPane();
        pane.setContentType("text/html");
        pane.setEditable(false);
        pane.addHyperlinkListener(GuiUtils.getHyperlinkListener());
        // set the color of the foreground appropriately.
        text = updateForeground(foregroundColor, text);
        pane.setText(text);
        pane.setCaretPosition(0);
        
        setNativeFontRenderering(pane);
        
        int htmlHeight = pane.getPreferredSize().height;        
        int padding = Math.max(15, (backgroundIcon.getIconHeight() - htmlHeight)/2);
        //must be false to view the background image
        pane.setOpaque(false);
        //shift the text so as to not paint over the image
        pane.setMargin( new Insets(5,140, 0,0));
        ImageViewPort imageViewPort = new ImageViewPort(((ImageIcon)backgroundIcon).getImage());
        imageViewPort.setView(pane);
        
        JScrollPane scroller = new JScrollPane();
        scroller.setViewport(imageViewPort);
        ResizeUtils.forceSize(scroller, new Dimension(backgroundIcon.getIconWidth(), backgroundIcon.getIconHeight()));
        scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        return scroller;
    }
    
    private void setNativeFontRenderering(JEditorPane pane) {
        // add a CSS rule to force body tags to use the default label font
        // instead of the value in javax.swing.text.html.default.csss
        Font font = UIManager.getFont("Label.font");
        String bodyRule = "body { font-family: " + font.getFamily() + "; " +
                "font-size: " + 14 + "pt; }";
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
