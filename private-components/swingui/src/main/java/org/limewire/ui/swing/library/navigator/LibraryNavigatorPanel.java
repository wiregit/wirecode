package org.limewire.ui.swing.library.navigator;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.inject.LazySingleton;
import org.limewire.ui.swing.dnd.GhostDragGlassPane;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;

@LazySingleton
public class LibraryNavigatorPanel extends JXPanel {

    @Resource private Color backgroundColor;
    @Resource private Color borderColor;
    
    private final LibraryNavigatorTable table;
    
    @Inject
    public LibraryNavigatorPanel(LibraryNavigatorTable table, LibraryNavTableRenderer renderer,
            SharedFileListManager sharedFileListManager, GhostDragGlassPane ghostGlassPane) {
        super(new MigLayout("insets 0, gap 0, fillx", "[100!]", ""));
        
        this.table = table;
        table.getColumnModel().getColumn(0).setCellRenderer(renderer);
        
        GuiUtils.assignResources(this);
        setBackground(backgroundColor);        
        setBorder(BorderFactory.createMatteBorder(0,0,0,1, borderColor));
        
        JPanel panel = new JPanel(new MigLayout("fill, gap 0, insets 0"));
        panel.setBackground(backgroundColor);
        panel.add(table, "grow, wrap");        
        add(panel, "growx, growy, wrap");

        initData();
    }
    
    private void initData() {
        table.getSelectionModel().setSelectionInterval(0, 0);
    }
    
    public void selectLocalFileList(LocalFileList localFileList) {
        table.selectLibraryNavItem(localFileList);
    }
    
    public void addTableSelectionListener(ListSelectionListener listener) {
        table.getSelectionModel().addListSelectionListener(listener);
    }
    
    public LibraryNavItem getSelectedNavItem() {
        return table.getSelectedItem();
    }
}
