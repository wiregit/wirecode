package org.limewire.ui.swing.library;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.Category;
import org.limewire.ui.swing.painter.ButtonPainter;
import org.limewire.ui.swing.painter.SubpanelPainter;
import org.limewire.ui.swing.search.FilteredTextField;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

public class LibraryHeaderPanel extends JXPanel {
    
    @Resource
    private Color fontColor;
    
    @Resource
    private Icon shareIcon;
    @Resource
    private Font headerFont;
    @Resource
    private Font buttonFont;

    private JLabel titleLabel;

    private FilteredTextField filterField;

    private JXButton shareAllButton;

    public LibraryHeaderPanel(Category category) {
        super(new MigLayout());
        
        GuiUtils.assignResources(this);
        
        titleLabel = new JLabel(getTitle(category));
        titleLabel.setForeground(fontColor);
        titleLabel.setFont(headerFont);

        filterField = new FilteredTextField();
        filterField.setPromptText(I18n.tr("Filter"));
        filterField.setPreferredSize(new Dimension(148, 17));
        filterField.setSize(filterField.getPreferredSize());
        filterField.setMaximumSize(filterField.getPreferredSize());

        shareAllButton = new JXButton(I18n.tr("Share All"), shareIcon); 
        shareAllButton.setForeground(fontColor);
        shareAllButton.setHorizontalTextPosition(SwingConstants.LEFT);
        shareAllButton.setBackgroundPainter(new ButtonPainter());
        shareAllButton.setFont(buttonFont);

        add(titleLabel);
        add(filterField, "pushx, right");
        add(shareAllButton);
        
        setBackgroundPainter(new SubpanelPainter());
    }

    private String getTitle(Category category) {
        switch (category) {
        case AUDIO:
            return I18n.tr("My Library - Audio");
        case DOCUMENT:
            return I18n.tr("My Library - Documents");
        case IMAGE:
            return I18n.tr("My Library - Images");
        case OTHER:
            return I18n.tr("My Library - Other");
        case PROGRAM:
            return I18n.tr("My Library - Program");
        case VIDEO:
            return I18n.tr("My Library - Video");
        }
        throw new IllegalArgumentException("Unknown category: " + category);
    }

}
