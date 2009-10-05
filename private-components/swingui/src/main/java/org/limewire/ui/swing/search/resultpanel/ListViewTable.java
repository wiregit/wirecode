package org.limewire.ui.swing.search.resultpanel;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.event.ChangeEvent;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.limewire.core.api.search.store.StoreStyle;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.model.VisualStoreResult;
import org.limewire.ui.swing.search.resultpanel.list.ListViewRowHeightRule;
import org.limewire.ui.swing.search.resultpanel.list.ListViewTableEditorRenderer;
import org.limewire.ui.swing.search.resultpanel.list.ListViewRowHeightRule.RowDisplayResult;
import org.limewire.ui.swing.util.GuiUtils;

import ca.odell.glazedlists.swing.DefaultEventTableModel;

/**
 * Table component to display search results in a vertical list.
 */
public class ListViewTable extends ResultsTable<VisualSearchResult> {
    
    @Resource private Color similarResultParentBackgroundColor;
    
    private final ListViewRowHeightRule rowHeightRule;
    
    private final ColorHighlighter storeHighlighter;
    
    private boolean ignoreRepaints;
    
    private ListViewTableEditorRenderer listEditor;
    
    private ListViewTableEditorRenderer listRenderer;
    
    /**
     * Constructs a ListViewTable using the specified row height rule and
     * store style.
     */
    public ListViewTable(ListViewRowHeightRule rowHeightRule) {
        super();
        this.rowHeightRule = rowHeightRule;
        
        GuiUtils.assignResources(this);
        
        storeHighlighter = new ColorHighlighter(new HighlightPredicate() {
            public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
                Object value = getValueAt(adapter.row, 0);
                return (value instanceof VisualStoreResult);
            }
        }, getBackground(), null, getTableColors().selectionColor, null);
        
        setGridColor(Color.decode("#EBEBEB"));
        setHighlighters(
                new ColorHighlighter(getBackground(), null, getTableColors().selectionColor, null),                    
                new ColorHighlighter(new HighlightPredicate() {
                    public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
                        VisualSearchResult vsr = (VisualSearchResult)getValueAt(adapter.row, 0);
                        return vsr != null && vsr.isChildrenVisible();
                    }
                }, similarResultParentBackgroundColor, null, getTableColors().selectionColor, null),
                storeHighlighter);
    }
    
    @Override
    protected void paintEmptyRows(Graphics g) {
        // do nothing.
    }
    
    @Override
    protected void updateViewSizeSequence() {
        if (ignoreRepaints) {
            return;
        }
        super.updateViewSizeSequence();
    }

    @Override
    protected void resizeAndRepaint() {
        if (ignoreRepaints) {
            return;
        }
        super.resizeAndRepaint();
    }
    
    private void setIgnoreRepaints(boolean ignore) {
        this.ignoreRepaints = ignore;
    }
    
    /**
     * Sets the cell editor for the list.
     */
    public void setListEditor(ListViewTableEditorRenderer editor) {
        this.listEditor = editor;
        
        TableColumnModel tcm = getColumnModel();
        int columnCount = getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            TableColumn tc = tcm.getColumn(i);
            tc.setCellEditor(editor);
        }
        
        setDefaultEditor(VisualSearchResult.class, editor);
    }
    
    /**
     * Sets the cell renderer for the list.
     */
    public void setListRenderer(ListViewTableEditorRenderer renderer) {
        this.listRenderer = renderer;
        
        TableColumnModel tcm = getColumnModel();
        int columnCount = getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            TableColumn tc = tcm.getColumn(i);
            tc.setCellRenderer(renderer);
        }
    }
    
    /**
     * Sets the store style for the list.
     */
    public void setStoreStyle(StoreStyle storeStyle) {
        // Update background color.
        storeHighlighter.setBackground(storeStyle.getBackground());
        
        // Set style in editor/renderer components.
        if (listRenderer != null) {
            listRenderer.setStoreStyle(storeStyle);
        }
        if (listEditor != null) {
            listEditor.setStoreStyle(storeStyle);
        }
        
        // Update store row heights and repaint.
        updateStoreRowSizes();
    }
    
    /**
     * Calculates the display height for the specified search result by 
     * preparing the list renderer.
     */
    private int calculateRowHeight(VisualSearchResult vsr) {
        // Determine row height by preparing renderer to use the search 
        // result, and retrieving its preferred height.
        if (listRenderer != null) {
            Component renderer = listRenderer.getTableCellRendererComponent(this, vsr, false, false, 0, 0);
            renderer.validate();
            return renderer.getPreferredSize().height;
        }
        
        // Return -1 if renderer not yet defined.
        return -1;
    }
    
    /**
     * Updates row heights for all rows in the list.
     */
    public void updateRowSizes() {
        DefaultEventTableModel model = getEventTableModel();
        if (model == null) return;
        
        setIgnoreRepaints(true);
        
        boolean setRowSize = false;
        for (int row = 0; row < model.getRowCount(); row++) {
            VisualSearchResult vsr = (VisualSearchResult) model.getElementAt(row);
            RowDisplayResult result = vsr.getRowDisplayResult();
            if (result == null || result.isStale(vsr)) {
                if (vsr instanceof VisualStoreResult) {
                    result = rowHeightRule.createDisplayResult(vsr, calculateRowHeight(vsr));
                } else {
                    result = rowHeightRule.createDisplayResult(vsr);
                }
                vsr.setRowDisplayResult(result);
            }
            
            int newRowHeight = result.getRowHeight();
            if (vsr.getSimilarityParent() == null) {
                //only resize rows that belong to parent visual results.
                //this will prevent the jumping when expanding child results as mentioned in
                //https://www.limewire.org/jira/browse/LWC-2545
                if (getRowHeight(row) != newRowHeight) {
                    setRowHeight(row, newRowHeight);
                    setRowSize = true;
                }
            }
        }
        
        setIgnoreRepaints(false);
        
        if (setRowSize) {
            if (isEditing()) {
                editingCanceled(new ChangeEvent(this));
            }
            updateViewSizeSequence();
            resizeAndRepaint();
        }
    }
    
    /**
     * Updates row heights for store results, and repaints the list.
     */
    public void updateStoreRowSizes() {
        // Get table model
        DefaultEventTableModel<VisualSearchResult> model = getEventTableModel();
        if (model == null) return;
        
        // Ignore repaint requests.
        setIgnoreRepaints(true);
        
        // Process all store result rows.  This is only called when the style
        // is updated, which happens when store results are received, or on
        // store login/logout.
        boolean setRowSize = false;
        for (int row = 0; row < model.getRowCount(); row++) {
            VisualSearchResult vsr = model.getElementAt(row);
            if (vsr instanceof VisualStoreResult) {
                // Create new RowDisplayResult for store result.
                RowDisplayResult result = rowHeightRule.createDisplayResult(vsr, calculateRowHeight(vsr));
                vsr.setRowDisplayResult(result);

                // Set row height if changed.
                int newRowHeight = result.getRowHeight();
                if (getRowHeight(row) != newRowHeight) {
                    setRowHeight(row, newRowHeight);
                    setRowSize = true;
                }
            }
        }

        // Restore repaint requests.
        setIgnoreRepaints(false);
        
        // Update size sequence if any row heights changed.
        if (setRowSize) {
            if (isEditing()) {
                editingCanceled(new ChangeEvent(this));
            }
            updateViewSizeSequence();
        }
        
        // Repaint list.
        resizeAndRepaint();
    }
}
