package org.limewire.ui.swing.library.sharing;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXButton;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.PromptTextField;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class LibrarySharingEditablePanel {
    
    private final JPanel component;
    private PromptTextField filterTextField;
    private HyperlinkButton allButton;
    private HyperlinkButton noneButton;
    private JXButton applyButton;
    private HyperlinkButton cancelButton;
    
    private Provider<LibrarySharingTable> sharingTableProvider;
    private Provider<LibrarySharingEditableRendererEditor> renderer;
    private Provider<LibrarySharingEditableRendererEditor> editor;
    
    private LibrarySharingTable sharingTable;
    
    @Inject
    public LibrarySharingEditablePanel(Provider<LibrarySharingTable> sharingTableProvider,
            Provider<LibrarySharingEditableRendererEditor> renderer,
            Provider<LibrarySharingEditableRendererEditor> editor) {
        component = new JPanel(new MigLayout("insets 0, gap 0, fill", "", ""));
        
        this.sharingTableProvider = sharingTableProvider;
        this.renderer = renderer;
        this.editor = editor;
        
        component.setOpaque(false);
        
        component.add(new JLabel(I18n.tr("Share list with...")), "dock north, gapleft 5, gaptop 5");
        
        filterTextField = new PromptTextField(I18n.tr("Find..."), 10);
        
        component.add(filterTextField, "dock north, gapleft 5, gaptop 5, gapright 5");

        component.add(new JLabel(I18n.tr("Select")), "dock north, gapleft 5, gaptop 5");
        allButton = new HyperlinkButton(I18n.tr("all"));
        noneButton = new HyperlinkButton(I18n.tr("none"));
        component.add(allButton, "dock north, gapleft 15, gaptop 5");
        component.add(noneButton, "dock north, gapleft 15, gaptop 5");
        
        //TODO: this needs to be created lazily
        initTable();
        
        applyButton = new JXButton(I18n.tr("Apply"));
        cancelButton = new HyperlinkButton(I18n.tr("Cancel"));
        
        component.add(applyButton, "split 1, alignx center, growx");
        component.add(cancelButton, "alignx center, wrap");
    }
    
    private void initTable() {
        sharingTable = sharingTableProvider.get();
                
        JScrollPane scrollPane = new JScrollPane(sharingTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder()); 
        
        sharingTable.getColumnModel().getColumn(0).setCellRenderer(renderer.get());
        sharingTable.getColumnModel().getColumn(0).setCellEditor(editor.get());
        
        component.add(scrollPane, "growx, dock north, gaptop 5");
    }
    
    public JComponent getComponent() {
        return component;
    }
}
