package org.limewire.ui.swing.update;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.text.html.HTMLDocument;
import javax.swing.Timer;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Application;
import org.jdesktop.application.Resource;
import org.limewire.core.api.updates.AutoUpdateHelper;
import org.limewire.core.api.updates.UpdateInformation;
import org.limewire.core.settings.UpdateSettings;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.ImageViewPort;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.ResizeUtils;

/**
 * Creates a panel to be displayed inside a Dialog for notification that a 
 * new version of LW is available.
 */
public class AutoUpdatePanel extends JPanel {
    
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
    
    private final AutoUpdateHelper updateHelper;
    
    private final UpdateInformation updateInformation;
    
    private JButton leftButton;
    private JEditorPane pane;
    private int count = 0;
    // max limit is 5 minutes
    private final int max = 5 * 60;
    private int upperLimit = 0;
    private Timer timer = null;
    
    
    public AutoUpdatePanel(UpdateInformation updateInformation, AutoUpdateHelper autoUpdateHelper) {
        GuiUtils.assignResources(this);
        this.updateInformation = updateInformation;
        this.updateHelper = autoUpdateHelper;
        setBackground(backgroundColor);
        
        setLayout(new MigLayout("fill, insets 10 10 10 10, gap 6")); 
        
        upperLimit = (int)(Math.random() * max);
        timer = new Timer(1000, new TimerListener());
        timer.setRepeats(true);
        timer.start();
        pane = createTopLabel(getTitle(upperLimit - count), topFont);
        add(pane, "alignx 50%, gapbottom 7, wrap");
        add(createContentArea(I18n.tr(updateInformation.getUpdateText()), contentFont), "grow, wrap, gapbottom 10");
        add(createLeftButton(new FirstButtonAction()), "alignx 50%");
        
        String updateCommand = updateInformation.getUpdateCommand();
        UpdateSettings.AUTO_UPDATE_COMMAND.set(updateCommand);
        UpdateSettings.DOWNLOADED_UPDATE_VERSION.set(UpdateSettings.AUTO_UPDATE_VERSION.get());
    }
    
    public JButton getDefaultButton() {
        return leftButton;
    }
    
    private JEditorPane createTopLabel(String text, Font font) {
        JEditorPane pane = new JEditorPane();
        pane.setContentType("text/html");
        pane.setEditable(false);
        pane.addHyperlinkListener(GuiUtils.getHyperlinkListener());
        pane.setOpaque(false);
        // set the color of the foreground appropriately.
        text = updateForeground(foregroundColor, text);
        pane.setText(text);
        pane.setCaretPosition(0);
        setNativeFontRenderering(pane, font);
        
        return pane;
    }
    
    private JComponent createContentArea(String text,  Font font) {
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
        pane.setMargin( new Insets(36, 162, 0, 30));
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
        // add a CSS rule to force body tags to use the default label font
        // instead of the value in javax.swing.text.html.default.csss
        String bodyRule = "body { font-family: " + font.getFamily() + "; " +
                "font-size: " + font.getSize() + "pt; }";
        ((HTMLDocument)pane.getDocument()).getStyleSheet().addRule(bodyRule);
    }
    
    private JComponent createLeftButton(Action action) {
        leftButton = new JButton(action);
        return leftButton;
    }
    
    private String updateForeground(Color color, String html) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        String hex = GuiUtils.toHex(r) + GuiUtils.toHex(g) + GuiUtils.toHex(b);
        return "<html><body text='#" + hex + "'>" + html + "</body></html>";
    }
    
    private void close() {
        Window window = SwingUtilities.getWindowAncestor(AutoUpdatePanel.this);
        window.setVisible(false);
        window.dispose();
    }
    
    private String getTitle(int count)
    {
        String title = null;
        int min = 0;
        min = count / 60;
        if(min >= 1)
        {
            title = I18n.tr("<b>LimeWire update will start in {0} {1}</b>", min + 1, "minutes.");
        }
        else
        {
            title = I18n.tr("<b>LimeWire update will start in {0} {1}</b>", count % 60, "seconds.");
        }
        return title; 
    }
    
    /**
     * The action for the button on the left. 
     */
    private class FirstButtonAction extends AbstractAction {

        public FirstButtonAction() {
            String text;
            if(updateInformation.getButton1Text() != null && updateInformation.getButton1Text().length() > 0)
                text = I18n.tr(updateInformation.getButton1Text());
            else
                text = I18n.tr("Update Now");
            
            putValue(Action.NAME, text);
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            count = 0;
            timer.stop();
            close();
            /*
            try{
                String updateCommand = updateInformation.getUpdateCommand();
                String[] cmdArray = updateCommand.split(AutoUpdateHelper.SEPARATOR);
                Runtime.getRuntime().exec(cmdArray);
            }catch(IOException io){
                // TODO: report this error to limewire.
            }
            */
           // AutoUpdateHelper updateHelper = autoUpdateHelper.get();
            updateHelper.initiateUpdateProcess();
            ActionMap actionMap = Application.getInstance().getContext().getActionMap();
            Action exitApplication = actionMap.get("exitApplication");
            exitApplication.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "Shutdown"));
        }
    }
    
    private class TimerListener implements ActionListener{
        public void actionPerformed(ActionEvent e)
        {
            count++;
            if(count == upperLimit)
            {
              leftButton.doClick();
            }
            else
            {
              pane.setText(getTitle(upperLimit - count));
            }
        }
    } 
    
}
