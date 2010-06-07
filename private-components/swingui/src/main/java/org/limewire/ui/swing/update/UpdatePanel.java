package org.limewire.ui.swing.update;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.html.HTMLDocument;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Application;
import org.jdesktop.application.Resource;
import org.limewire.core.api.updates.UpdateInformation;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.components.ImageViewPort;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.ui.swing.util.ResizeUtils;

/**
 * Creates a panel to be displayed inside a Dialog for notification that a 
 * new version of LW is available.
 */
public class UpdatePanel extends JPanel {
    
    @Resource
    private Icon backgroundIcon;
    @Resource
    private Color backgroundColor;
    @Resource
    private Color foregroundColor;
    
    private final org.limewire.core.api.Application application;
    private final UpdateInformation updateInformation;
    
    private JButton leftButton;
    private JButton rightButton;
    
    public UpdatePanel(UpdateInformation updateInformation, org.limewire.core.api.Application application) {
        GuiUtils.assignResources(this);
        
        this.updateInformation = updateInformation;
        this.application = application;
        
        setBackground(backgroundColor);
        
        setLayout(new MigLayout("fill, insets 10 10 10 10, gap 6"));
                
        add(createTopLabel(updateInformation.getUpdateTitle()), "alignx 50%, gapbottom 7, wrap");
        add(createContentArea(updateInformation.getUpdateText()), "grow, wrap, gapbottom 10");
        add(createLeftButton(new FirstButtonAction()), "alignx 50%, split, gapright 10");
        add(createRightButton(new SecondButtonAction()), "alignx 50%");
    }
    
    public JButton getDefaultButton() {
        return leftButton;
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
        pane.setMargin( new Insets(padding,140, 0,0));
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
    
    private JComponent createLeftButton(Action action) {
        leftButton = new JButton(action);
        leftButton.requestFocusInWindow();
        return leftButton;
    }
    
    private JComponent createRightButton(Action action) {
        rightButton = new JButton(action);
        return rightButton;
    }
    
    private String updateForeground(Color color, String html) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        String hex = GuiUtils.toHex(r) + GuiUtils.toHex(g) + GuiUtils.toHex(b);
        return "<html><body text='#" + hex + "'>" + html + "</body></html>";
    }
    
    private void close() {
        Window window = SwingUtilities.getWindowAncestor(UpdatePanel.this);
        window.setVisible(false);
        window.dispose();
    }
    
    /**
     * The action for the button on the left. 
     */
    private class FirstButtonAction extends AbstractAction {

        public FirstButtonAction() {
            String text;
            if(updateInformation.getButton1Text() != null && updateInformation.getButton1Text().length() > 0)
                text = updateInformation.getButton1Text();
            else
                text = I18n.tr("Update Now");
            
            putValue(Action.NAME, text);
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Visit http://www.limewire.com to update!"));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            String updateCommand = updateInformation.getUpdateCommand();

            if (updateCommand != null) {
                application.setShutdownFlag(updateCommand);
    
                int restartNow = FocusJOptionPane.showYesNoMessage(I18n.tr("LimeWire needs to be restarted to install the update. If you choose not to update now, LimeWire will update automatically when you close. Would you like to update now?"),
                      I18n.tr("Update Ready"), JOptionPane.YES_OPTION, UpdatePanel.this);
            
                if (restartNow == JOptionPane.YES_OPTION)
                      Application.getInstance().exit(e);
                
            } else {
                NativeLaunchUtils.openURL(updateInformation.getUpdateURL());
            }
            
            close();
        }
    }
    
    /**
     * Action for the button on the right.
     */
    private class SecondButtonAction extends AbstractAction {
        public SecondButtonAction() {
            String text;
            if(updateInformation.getButton2Text() != null && updateInformation.getButton2Text().length() > 0)
                text = updateInformation.getButton2Text();
            else
                text = I18n.tr("Update Later");
            
            putValue(Action.NAME, text);
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Visit http://www.limewire.com to update!"));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            close();
        }
    }
}
