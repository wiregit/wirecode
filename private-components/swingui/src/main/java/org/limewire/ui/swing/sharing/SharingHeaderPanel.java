package org.limewire.ui.swing.sharing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.components.HeadingLabel;
import org.limewire.ui.swing.components.PromptTextField;
import org.limewire.ui.swing.painter.SubpanelPainter;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Singleton;

/**
 * This displays the header on all the sharing panels. It includes the filter
 * box and name and icon describing what this panel is for.
 */
@Singleton
public class SharingHeaderPanel extends JXPanel{
    
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
    public SharingHeaderPanel() {
        GuiUtils.assignResources(this);
        
        setBackgroundPainter(new SubpanelPainter());

        createComponents();
        layoutComponents();
        
        setMinimumSize(new Dimension(0, height + 2));
        setMaximumSize(new Dimension(Short.MAX_VALUE, height + 2));
        setPreferredSize(new Dimension(Short.MAX_VALUE, height + 2));
    }
    
    private void createComponents() {
        titleLabel = new HeadingLabel(I18n.tr("Sharing with the LimeWire Network"));
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
}
