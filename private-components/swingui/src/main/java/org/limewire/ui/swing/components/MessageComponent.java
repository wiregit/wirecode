package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Font;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.text.html.HTMLDocument;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.painter.GreenMessagePainter;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * Creates a Lime Message Component. Currently
 * this is painted green and has a triangle, chat arrow
 * attached to the bottom of it.
 */
public class MessageComponent extends JPanel {
    @Resource
    private Icon arrowIcon;
    @Resource
    private Font headingFont;
    @Resource
    private Color fontColor;
    @Resource
    private Font subFont;
    
    /**
     * Contains the actual subComponents.
     */
    private JXPanel messageContainer;
    
    public MessageComponent() {
        this(18, 22, 18, 22);
    }
    
    // TODO: top, left, right, bottom does not seem to map correctly
    public MessageComponent(int topInset, int leftInset, int rightInset, int bottomInset) {
        GuiUtils.assignResources(this);
        
        setLayout(new MigLayout("insets 0 0 " + (arrowIcon.getIconHeight()-2) + " 0, gap 0"));
        setOpaque(false);
        
        messageContainer = new JXPanel(new MigLayout("insets " + topInset + " " + leftInset + " " + rightInset + " " + bottomInset + ", hidemode 3"));
        messageContainer.setOpaque(false);
        messageContainer.setBackgroundPainter(new GreenMessagePainter());
        
        add(new JLabel(arrowIcon), "pos (messageContainer.x + 25) 0.99al");
        add(messageContainer, "wrap");
    }
    
    public void addComponent(JComponent component, String layout) {
        messageContainer.add(component, layout);
    }
    
    public void decorateHeaderLabel(JComponent component) {
        component.setFont(headingFont);
        component.setForeground(fontColor);
    }
    
    public void decorateSubLabel(JLabel component) {
        component.setFont(subFont);
        component.setForeground(fontColor);
    }
    
    public void decorateSubLabel(JTextPane component) {
        ((HTMLDocument)component.getDocument()).getStyleSheet().addRule(createCSS(subFont, fontColor));
    }
    
    private String createCSS(Font font, Color color) {
        StringBuilder builder = new StringBuilder("body {");
        builder.append("font-family: ").append(font.getFamily()).append(";");
        builder.append("font-size: ").append(font.getSize()).append("pt;");
        builder.append("color: ").append(GuiUtils.colorToHex(color)).append(";");
        builder.append("}");
        return builder.toString();
    }
    
    public void decorateFont(JComponent component) {
        component.setFont(subFont);
    }
}
