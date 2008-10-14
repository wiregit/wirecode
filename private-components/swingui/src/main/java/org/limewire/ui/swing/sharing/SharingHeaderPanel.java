package org.limewire.ui.swing.sharing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.text.MessageFormat;

import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.components.HeadingLabel;
import org.limewire.ui.swing.components.PromptTextField;
import org.limewire.ui.swing.painter.SubpanelPainter;
import org.limewire.ui.swing.sharing.friends.FriendUpdate;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;

import ca.odell.glazedlists.EventList;

/**
 * This displays the header on all the sharing panels. It includes the filter
 * box and name and icon describing what this panel is for.
 */
public class SharingHeaderPanel extends JXPanel implements FriendUpdate {

    private final String staticText;
    
    private HeadingLabel titleLabel;
    private JTextField filterBox;
    
    @Resource
    private int height;
    @Resource
    private Color fontColor;
    @Resource 
    private int fontSize;
    
    /**
     * Constructs sharing header panel.
     * 
     * @param icon
     * @param staticText is expected to contain the place holder '{0}' which
     * is replaced by the argument <code>name</code>, or through 
     * {@link #setFriendName(String)}
     * @param name
     * @param viewPanel
     */
    public SharingHeaderPanel(String staticText, String name) {
        GuiUtils.assignResources(this);
        
        setBackgroundPainter(new SubpanelPainter());
    
        this.staticText = staticText;

        createComponents(MessageFormat.format(staticText, name));
        layoutComponents();
        
        setMinimumSize(new Dimension(0, height + 2));
        setMaximumSize(new Dimension(Short.MAX_VALUE, height + 2));
        setPreferredSize(new Dimension(Short.MAX_VALUE, height + 2));
    }
    
    private void createComponents(String text) {
        titleLabel = new HeadingLabel(text);
        titleLabel.setForeground(fontColor);
        FontUtils.setSize(titleLabel, fontSize);
        FontUtils.changeStyle(titleLabel, Font.PLAIN);
        filterBox = new PromptTextField();
    }
    
    protected void layoutComponents() {
        setLayout(new MigLayout("insets 0 0 0 0", "", "align 50%"));

        add(titleLabel, "gapx 10, push");
        add(filterBox, "gapafter 10");
    }
    
    public JTextField getFilterBox() {
        return filterBox;
    }
    
    @Override
    public void setFriendName(String name) {
        titleLabel.setText(MessageFormat.format(staticText, name));
    }

    @Override
    public void setEventList(EventList<LocalFileItem> model) {
    }
}
