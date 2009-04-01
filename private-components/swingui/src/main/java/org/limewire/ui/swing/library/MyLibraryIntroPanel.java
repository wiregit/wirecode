package org.limewire.ui.swing.library;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.components.Line;
import org.limewire.ui.swing.painter.DarkButtonBackgroundPainter;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.painter.ButtonBackgroundPainter.DrawMode;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

public class MyLibraryIntroPanel extends JXPanel{
    public static final String NAME = "MyLibraryIntroPanel";
    @Resource
    private Icon libraryImage;
    
    public MyLibraryIntroPanel(Action continueAction){
        GuiUtils.assignResources(this);
        createPanel(continueAction);
    }

    
    private void createPanel(Action continueAction) {
        JXButton continueButton = new JXButton(I18n.tr("Continue"));
        continueButton.setBorderPainted(false);
        FontUtils.setSize(continueButton, 20);        
        continueButton.setBackgroundPainter(new DarkButtonBackgroundPainter(DrawMode.FULLY_ROUNDED, AccentType.NONE));
        continueButton.addActionListener(continueAction);
        
        JLabel label1 = new JLabel(I18n.tr("In LimeWire 5, the Library is the central location for all your files."));
        FontUtils.setSize(label1, 30);
        JLabel label2 = new JLabel(I18n.tr("Unlike the old LimeWire, not every file in your Library is being shared."));
        FontUtils.setSize(label2, 20);
        label2.setForeground(Color.decode("#8c8c8c"));
        JLabel label3 = new JLabel(I18n.tr("In LimeWire 5, you share and unshare from the Library, not from folders."));
        FontUtils.setSize(label3, 30);
        

        JPanel panel = new JPanel();
        panel.setLayout(new MigLayout("gap 0 0 0 0, insets 0 0 0 0, novisualpadding, nogrid, fillx"));
        panel.add(label1, "gapleft 15, gaptop 50, growx, wrap");
        panel.add(label2, "gapleft 15, gaptop 10, growx, wrap");
        panel.add(Line.createHorizontalLine(), "gaptop 60, growx, wrap");
        panel.add(label3, "gapleft 15, gaptop 40, growx, wrap");
        panel.add(new JLabel(libraryImage), "grow, gaptop 20, alignx 40%, wrap");
        panel.add(continueButton, "alignx 100%, gapright 15, gaptop 15");
                
        setLayout(new BorderLayout());
        add(new JScrollPane(panel));
    }

}
