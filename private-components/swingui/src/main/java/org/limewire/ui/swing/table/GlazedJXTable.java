package org.limewire.ui.swing.table;

import java.util.Arrays;
import java.util.Vector;

import javax.swing.ListSelectionModel;
import javax.swing.SizeSequence;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.jdesktop.swingx.decorator.FilterPipeline;
import org.jdesktop.swingx.decorator.SelectionMapper;
import org.jdesktop.swingx.decorator.SizeSequenceMapper;
import org.jdesktop.swingx.decorator.SortController;

/**
 * A JXTable for use with glazed lists event models. See
 * http://sites.google.com/site/glazedlists/documentation/swingx for issues with
 * SwingX. 
 */
public class GlazedJXTable extends BasicJXTable {
    
    private SizeSequenceMapper simpleRowModelMapper;
    private SelectionMapper simpleSelectionMapper;
    private SortController sortController;

    public GlazedJXTable() {
        super();
        initialize();
    }

    public GlazedJXTable(int numRows, int numColumns) {
        super(numRows, numColumns);
        initialize();
    }

    public GlazedJXTable(Object[][] rowData, Object[] columnNames) {
        super(rowData, columnNames);
        initialize();
    }

    public GlazedJXTable(TableModel dm, TableColumnModel cm, ListSelectionModel sm) {
        super(dm, cm, sm);
        initialize();
    }

    public GlazedJXTable(TableModel dm, TableColumnModel cm) {
        super(dm, cm);
        initialize();
    }

    public GlazedJXTable(TableModel dm) {
        super(dm);
        initialize();
    }

    public GlazedJXTable(Vector rowData, Vector columnNames) {
        super(rowData, columnNames);
        initialize();
    }

    private void initialize() {
        // Add initialization steps here.
    }
    
    @Override
    protected SizeSequenceMapper getRowModelMapper() {
        if(simpleRowModelMapper == null) {
            simpleRowModelMapper = new SimpleSizeSequenceMapper();
        }
        return simpleRowModelMapper;
    }
    
    @Override
    public SelectionMapper getSelectionMapper() {
        if(simpleSelectionMapper == null) {
            simpleSelectionMapper = new SimpleSelectionMapper();
        }
        return simpleSelectionMapper;
    }
    
    @Override
    protected boolean shouldSortOnChange(TableModelEvent e) {
        return false;
    }
    
    @Override
    public void setFilters(FilterPipeline pipeline) {
        if(pipeline != null) {
            throw new UnsupportedOperationException("do not use filters.");
        }
    }
    
    @Override
    public FilterPipeline getFilters() {
        return null;
    }
    
    @Override
    public SortController getSortController() {
        return sortController;
    }
    
    public void setSortController(SortController sortController) {
        this.sortController = sortController;
    }
    
    private static class SimpleSizeSequenceMapper extends SizeSequenceMapper {

        private SizeSequence viewSizes;
        private SizeSequence modelSizes;
        private int defaultHeight;

        @Override
        public void setViewSizeSequence(SizeSequence selection, int height) {
            SizeSequence old = this.viewSizes;
            if (old != null) {
                clearModelSizes();
            }
            this.viewSizes = selection;
            this.defaultHeight = height;
            mapTowardsModel();
        }

        @Override
        public SizeSequence getViewSizeSequence() {
            return viewSizes;
        }

        @Override
        public void setFilters(FilterPipeline pipeline) {
//            restoreSelection();
        }

        @Override
        public void clearModelSizes() {
            modelSizes = null;
        }

        @Override
        public void insertIndexInterval(int start, int length, int value) {
            if (modelSizes == null) return;
            modelSizes.insertEntries(start, length, value);
        }

        @Override
        public void removeIndexInterval(int start, int length) {
            if (modelSizes == null) return;
            modelSizes.removeEntries(start, length);
        }
        
        @Override
        public void restoreSelection() {
            if (viewSizes == null) return;
            int[] sizes = new int[getOutputSize()];
            Arrays.fill(sizes, defaultHeight);
            viewSizes.setSizes(sizes);
//            viewSizes.setSizes(new int[0]);
//            viewSizes.insertEntries(0, getOutputSize(), defaultHeight);

            int[] selected = modelSizes.getSizes();
            for (int i = 0; i < selected.length; i++) {
              int index = convertToView(i);
              // index might be -1, ignore. 
              if (index >= 0) {
                  viewSizes.setSize(index, selected[i]);
              }
            }
        }

        private void mapTowardsModel() {
            if (viewSizes == null) return;
            modelSizes = new SizeSequence(getInputSize(), defaultHeight);
            int[] selected = viewSizes.getSizes(); 
            for (int i = 0; i < selected.length; i++) {
                int modelIndex = convertToModel(i);
                modelSizes.setSize(modelIndex, selected[i]); 
            }
        }

        private int getInputSize() {
            return 0;
        }

        private int getOutputSize() {
            return 0;
        }

        private int convertToModel(int index) {
            return index;
        }
        
        private int convertToView(int index) {
            return index;
        }        

        @Override
        protected void updateFromPipelineChanged() {
//            restoreSelection();
        }

    }
    
    private static class SimpleSelectionMapper implements SelectionMapper {

        @Override
        public ListSelectionModel getViewSelectionModel() {
            return null;
        }

        @Override
        public boolean isEnabled() {
            return false;
        }

        @Override
        public void setViewSelectionModel(ListSelectionModel viewSelectionModel) {}
        @Override
        public void setFilters(FilterPipeline pipeline) {}
        @Override
        public void setEnabled(boolean enabled) {}
        @Override
        public void clearModelSelection() {}
        @Override
        public void insertIndexInterval(int start, int length, boolean before) {}
        @Override
        public void removeIndexInterval(int start, int end) {}
    }


}
