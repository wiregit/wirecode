package org.limewire.ui.swing.library;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.core.api.Category;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.components.FancyTab;
import org.limewire.ui.swing.components.FancyTabList;
import org.limewire.ui.swing.components.NoOpAction;
import org.limewire.ui.swing.components.PromptTextField;
import org.limewire.ui.swing.components.TabActionMap;
import org.limewire.ui.swing.components.TextFieldClipboardControl;
import org.limewire.ui.swing.components.decorators.TextFieldDecorator;
import org.limewire.ui.swing.library.table.AbstractLibraryFormat;
import org.limewire.ui.swing.library.table.AllTableFormat;
import org.limewire.ui.swing.library.table.AudioTableFormat;
import org.limewire.ui.swing.library.table.DocumentTableFormat;
import org.limewire.ui.swing.library.table.ImageTableFormat;
import org.limewire.ui.swing.library.table.OtherTableFormat;
import org.limewire.ui.swing.library.table.ProgramTableFormat;
import org.limewire.ui.swing.library.table.VideoTableFormat;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class LibraryFilterPanel {

    @Resource Color backgroundColor;
    @Resource Color borderColor;
    @Resource Font buttonFont;
    @Resource Color fontColor;
    @Resource Color fontSelectedColor;
    
    @Resource Color tabSelectionTopGradientColor;
    @Resource Color tabSelectionBottomGradientColor;
    @Resource Color tabSelectionBorderTopGradientColor;
    @Resource Color tabSelectionBorderBottomGradientColor;
    @Resource Color tabHighlightTopGradientColor;
    @Resource Color tabHighlightBottomGradientColor;
    
    private final JPanel component;
    private final PromptTextField promptTextField;
    private final FancyTabList categoryList;
    private final List<TabActionMap> searchActionMaps;
    private final List<LibraryCategoryListener> listeners;
    
    @Inject
    public LibraryFilterPanel(Provider<AllTableFormat<LocalFileItem>> allFormat, 
            Provider<AudioTableFormat<LocalFileItem>> audioFormat,
            Provider<VideoTableFormat<LocalFileItem>> videoFormat,
            Provider<ImageTableFormat<LocalFileItem>> imageFormat,
            Provider<DocumentTableFormat<LocalFileItem>> documentFormat,
            Provider<ProgramTableFormat<LocalFileItem>> programFormat,
            Provider<OtherTableFormat<LocalFileItem>> otherFormat,
            TextFieldDecorator textFieldDecorator) {
        GuiUtils.assignResources(this);
        
        this.searchActionMaps = new ArrayList<TabActionMap>();
        
        addCategory(tr("All"), null, allFormat);
        addCategory(tr("Audio"), Category.AUDIO, audioFormat);
        addCategory(tr("Videos"), Category.VIDEO, videoFormat);
        addCategory(tr("Images"), Category.IMAGE, imageFormat);
        addCategory(tr("Documents"), Category.DOCUMENT, documentFormat);
        addCategory(tr("Programs"), Category.PROGRAM, programFormat);
        addCategory(tr("Other"), Category.OTHER, otherFormat);
        
        component = new JPanel(new MigLayout("insets 0 5 0 5, gap 0, fill", "", "[28!]"));
        promptTextField = new PromptTextField(I18n.tr("Filter"));
        categoryList = new FancyTabList(searchActionMaps);
        categoryList.setSelectionPainter(createTabSelectionPainter());
        categoryList.setHighlightPainter(createHighlightPainter());
        this.listeners = new CopyOnWriteArrayList<LibraryCategoryListener>();
        
        init(textFieldDecorator);
        
        component.setVisible(SwingUiSettings.SHOW_LIBRARY_FILTERS.getValue());
    }
    
    private void init(TextFieldDecorator textFieldDecorator) {
        component.setBackground(backgroundColor);
        component.setBorder(BorderFactory.createMatteBorder(0,0,1,0, borderColor));
        
        TextFieldClipboardControl.install(promptTextField);
        textFieldDecorator.decorateClearablePromptField(promptTextField, AccentType.NONE);
        
        categoryList.setTabTextColor(fontColor);
        categoryList.setTextFont(buttonFont);
        categoryList.setTabTextSelectedColor(fontSelectedColor);
        categoryList.setUnderlineEnabled(false);

        component.add(categoryList, "");
        component.add(promptTextField, "alignx right");
    }
    
    public JComponent getComponent() {
        return component;
    }
    
    void addSearchTabListener(LibraryCategoryListener listener) {
        listeners.add(listener);
    }
    
    public AbstractLibraryFormat<LocalFileItem> getSelectedTableFormat() {
        return ((LibraryCategoryAction)categoryList.getSelectedTab().getTabActionMap().getMainAction()).getTableFormat();
    }
    
    public void clearFilters() {
        promptTextField.setText("");
        //TODO: select all action
    }
    
    public JTextField getFilterField() {
        return promptTextField;
    }
    
    public Category getSelectedCategory() {
        return ((LibraryCategoryAction)categoryList.getSelectedTab().getTabActionMap().getMainAction()).getCategory();
    }
    
    private void addCategory(String title, Category category, Provider<? extends AbstractLibraryFormat<LocalFileItem>> tableFormat) {
        LibraryCategoryAction action = new LibraryCategoryAction(title, category, tableFormat);
        if(category == null)
            action.putValue(Action.SELECTED_KEY, true);
        TabActionMap map = newTabActionMap(action);
        searchActionMaps.add(map);
    }
    
    private TabActionMap newTabActionMap(LibraryCategoryAction action) {
        Action moreText = new NoOpAction();
        moreText.putValue(Action.NAME, "");
        return new TabActionMap(action, null, moreText, null);
    }
    
    private class LibraryCategoryAction extends AbstractAction {
        private final Category category;
        private final Provider<? extends AbstractLibraryFormat<LocalFileItem>> tableFormat;

        public LibraryCategoryAction(String name, Category category, Provider<? extends AbstractLibraryFormat<LocalFileItem>> tableFormat) {
            super(name);
            this.category = category;
            this.tableFormat = tableFormat;
        }

        Category getCategory() {
            return category;
        }
        
        public AbstractLibraryFormat<LocalFileItem> getTableFormat() {
            return tableFormat.get();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            for(LibraryCategoryListener listener : listeners) {
                listener.categorySelected(category);
            }
        }
    }
    
    static interface LibraryCategoryListener {
        void categorySelected(Category category);
    }
    
    /**
     * Creates a Painter used to render the selected category tab.
     */
    private AbstractPainter<FancyTab> createTabSelectionPainter() {
        RectanglePainter<FancyTab> painter = new RectanglePainter<FancyTab>();
        
        painter.setFillPaint(new GradientPaint(0, 0, tabSelectionTopGradientColor, 
                0, 1, tabSelectionBottomGradientColor));
        painter.setBorderPaint(new GradientPaint(0, 0, tabSelectionBorderTopGradientColor, 
                0, 1, tabSelectionBorderBottomGradientColor));
        
        painter.setRoundHeight(10);
        painter.setRoundWidth(10);
        painter.setRounded(true);
        painter.setPaintStretched(true);
        painter.setInsets(new Insets(0,0,6,0));
                
        painter.setAntialiasing(true);
        painter.setCacheable(true);
        
        return painter;
    }
    
    private AbstractPainter createHighlightPainter() {
        RectanglePainter tabHighlight = new RectanglePainter();
        tabHighlight.setFillPaint(new GradientPaint(20.0f, 0.0f, tabHighlightTopGradientColor, 
                                                    20.0f, 33.0f, tabHighlightBottomGradientColor));

        tabHighlight.setInsets(new Insets(0,0,1,0));
        tabHighlight.setBorderPaint(null);
        return tabHighlight;
    }
}
