package org.limewire.ui.swing.properties;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadPiecesInfo;
import org.limewire.core.api.download.DownloadPiecesInfo.PieceState;
import org.limewire.core.api.library.PropertiableFile;
import org.limewire.ui.swing.properties.FileInfoDialog.FileInfoType;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.PainterUtils;
import org.limewire.ui.swing.util.ResizeUtils;

public class FileInfoPiecesPanel implements FileInfoPanel {

    private static final int NUM_COLUMNS = 10;
    private static final int MAX_NUM_ROWS = 10;
    private static final int MAX_CELL_HEIGHT = 16;
    private static final int MAX_CELL_WIDTH = 80;

    private static final PieceIntensity ACTIVE_INTENSITY = new PieceIntensity(PieceState.ACTIVE);
    private static final PieceIntensity UNAVAILABLE_INTENSITY = new PieceIntensity(PieceState.UNAVAILABLE);
    
    /**
     * The shorter the cooler, the shorter the more performance intensive.
     */
    private static final int REFRESH_DELAY = 200;
    
    @Resource private Color foreground;
    @Resource private Font smallFont;
    @Resource private Color legendBackground = PainterUtils.TRASPARENT;
    
    @Resource private Color downloadedForeground;
    @Resource private Color availableForeground;
    @Resource private Color activeForeground;
    @Resource private Color unavailableForeground;
    
    private final Color partialForegroundInitial;
    private final Color partialForegroundFinal;
    
    private DownloadPiecesInfo piecesInfo;
    private int numPieces;
    
    private final JPanel component;
    private final PiecesGrid grid;
    private final Timer refresher;
    private int coalesceFactor;
    private int cachedColumns;
    private int cachedRows;
    
    public FileInfoPiecesPanel(FileInfoType type, final DownloadItem propertiableFile) {
        
        GuiUtils.assignResources(this);
        
        partialForegroundInitial = createShade(availableForeground, downloadedForeground, .1);
        partialForegroundFinal = createShade(downloadedForeground, availableForeground, .1);
        
        component = new JPanel(new MigLayout("insets 6 0 0 0"));
        component.setOpaque(false);
        
        piecesInfo = propertiableFile.getPieceInfo();
        
        if (piecesInfo == null) {
            component.add(new JLabel(I18n.tr("No piece data available.")));
            refresher = null;
            grid = null;
            return;
        }
        
        numPieces = piecesInfo.getNumPieces();
        grid = new PiecesGrid(0, 0);
        setupGrid();

        final JPanel infoPanel = new JPanel(new MigLayout("nogrid, insets 0, gap 0"));
        infoPanel.setOpaque(false);
        
        JPanel legendPanel = new JPanel(new MigLayout("insets 8, gap 3"));
        legendPanel.setBackground(legendBackground);
        
        legendPanel.add(createLegendBox(activeForeground));
        legendPanel.add(createLabel(I18n.tr("Active")), "gapright 5");
        legendPanel.add(createLegendBox(availableForeground));
        legendPanel.add(createLabel(I18n.tr("Available")), "wrap");
        legendPanel.add(createLegendBox(downloadedForeground));
        legendPanel.add(createLabel(I18n.tr("Done")), "gapright 5");
        legendPanel.add(createLegendBox(unavailableForeground));
        legendPanel.add(createLabel(I18n.tr("Unavailable")), "wrap");
        legendPanel.add(createLegendBox(partialForegroundInitial));
        legendPanel.add(createLabel(I18n.tr("Partially Done")));
        
        // ....
        infoPanel.add(legendPanel);
        
        component.add(grid, "wrap");
        component.add(infoPanel, "growx");
        
        refresher = new Timer(REFRESH_DELAY, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                piecesInfo = propertiableFile.getPieceInfo();
                int newNum = piecesInfo.getNumPieces();
                if(newNum != numPieces) {
                    // if the number of pieces has changed, resize the grid.
                    numPieces = newNum;
                    setupGrid();
                }            
                updateTable();
                grid.repaint();
                
                // Hack to get the legends margin to line up with the table edge despite
                //  resizing due to rounding.  NOTE: there will be a lag of refresh cycle. 
                if (grid.isMarginUpdated()) {
                    int margin = grid.getInnerMargin();
                    if (margin != 0) {
                        infoPanel.setBorder(BorderFactory.createEmptyBorder(0, margin, 0, margin));
                    }
                }
            }
        });
        refresher.start();
        updateTable();

    }
    
    private void setupGrid() {        
        int requiredRows = (int)Math.ceil((double)numPieces / NUM_COLUMNS);
        int numRows = requiredRows;
        
        coalesceFactor = 1;
        if (requiredRows > MAX_NUM_ROWS) {
            coalesceFactor = (int)Math.ceil((double)numPieces / (MAX_NUM_ROWS*NUM_COLUMNS));
            numRows = (int)Math.ceil((double)numPieces / (coalesceFactor*NUM_COLUMNS));
        }
        
        grid.resizeGrid(numRows, NUM_COLUMNS);
        grid.setAlignmentX(Component.CENTER_ALIGNMENT);
        grid.setAlignmentY(Component.CENTER_ALIGNMENT);
        ResizeUtils.forceSize(grid, new Dimension(MAX_CELL_WIDTH*NUM_COLUMNS, 
                                                  MAX_CELL_HEIGHT*numRows));
    }
    
    private static Component createLegendBox(Paint foreground) {
        JXPanel panel = new JXPanel();
        RectanglePainter<JXPanel> painter = new RectanglePainter<JXPanel>();
        painter.setPaintStretched(true);
        painter.setFillPaint(foreground);
        painter.setBorderPaint(Color.BLACK);
        painter.setAntialiasing(true);
        painter.setCacheable(true);
        panel.setBackgroundPainter(painter);
        return panel;
    }
    
    private Component createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setOpaque(false);
        label.setForeground(foreground);
        label.setFont(smallFont);
        return label;
    }
    
    private void updateTable() {
        
        // Correction if per chance the number of cells or rows is changed.
        if (cachedRows != grid.getRows() || cachedColumns != grid.getColumns()) {
            cachedRows = grid.getRows();
            cachedColumns = grid.getColumns();
            
            int cellsAvailable = grid.getCells();
            coalesceFactor = (int)Math.ceil((double)numPieces / cellsAvailable);
        }
        
        boolean allDownloaded = true;
        int gridSlot = 0;
        
        for ( int i=0 ; i<numPieces ; i+=coalesceFactor ) {
            Paint pieceForeground = Color.BLACK;
        
            int numPiecesLeft = numPieces-coalesceFactor*gridSlot;
            int piecesToCoalesce = coalesceFactor;
            if (numPiecesLeft < coalesceFactor) {
                piecesToCoalesce = numPiecesLeft;
            }
            
            PieceState[] states = new PieceState[piecesToCoalesce];
            for ( int coalesceIndex=0 ; coalesceIndex < piecesToCoalesce ; coalesceIndex++ ) {
                states[coalesceIndex] = piecesInfo.getPieceState(i+coalesceIndex);
            }
            
            PieceIntensity cumulativePieceIntensity = coalescePieceStates(states); 
            PieceState cumulativeState = cumulativePieceIntensity.getState();
            
            switch (cumulativeState) {
                case ACTIVE :
                    pieceForeground = activeForeground;
                    break;
                case PARTIAL :
                    pieceForeground = createShade(partialForegroundInitial, partialForegroundFinal,
                            cumulativePieceIntensity.getIntensity());
                    break;
                case AVAILABLE :
                    pieceForeground = availableForeground;
                    break;
                case DOWNLOADED :
                    pieceForeground = downloadedForeground;
                    break;
                case UNAVAILABLE :
                    pieceForeground = unavailableForeground;
                    break;
                default:
                    throw new IllegalStateException(cumulativeState.toString());
            }
            
            // TODO: this might need to get smarter if not all pieces are shown on complete
            if (cumulativeState != PieceState.DOWNLOADED) {
                allDownloaded = false;
            }
            
            grid.setPaint(gridSlot++, pieceForeground);
        }
        
        if (allDownloaded) {
            refresher.stop();
        }
    }
    
    @Override
    public JComponent getComponent() {
        return component;
    }

    @Override
    public boolean hasChanged() {
        return false;
    }

    @Override
    public void save() {
    }

    @Override
    public void updatePropertiableFile(PropertiableFile file) {
    }

    @Override
    public void dispose() {
        refresher.stop();
    }
    
    private static PieceIntensity coalescePieceStates(PieceState ... states) {
        
        // +1 for partial, +2 for done, 0 for available
        int completedScore = 0;
                
        PieceState workingState = null;
        
        for ( PieceState state : states ) {
            if (state == PieceState.ACTIVE) {
                return ACTIVE_INTENSITY;
            }
            if (state == PieceState.UNAVAILABLE) {
                return UNAVAILABLE_INTENSITY;
            }
            
            switch (state) {
                case PARTIAL :
                    completedScore+=1;
                    break;
                case DOWNLOADED :
                    completedScore+=2;
                    break;
            }
            
            if (workingState != null && workingState != state) {
                workingState = PieceState.PARTIAL;                   
            } 
            else {            
                workingState = state;
            }
        }
        
        if (workingState == PieceState.PARTIAL) {
            int completedScoreMax = states.length*2;
            return new PieceIntensity(workingState, (double)completedScore/completedScoreMax);
        } else {
            return new PieceIntensity(workingState);
        }
        
    }
    
    private static class PieceIntensity {
        private final PieceState state;
        private final double intensity;
        
        public PieceIntensity(PieceState state) {
            this.state = state;
            this.intensity = 1;
        }
        
        public PieceIntensity(PieceState state, double intensity) {
            this.state = state;
            this.intensity = intensity;
        }
        
        public PieceState getState() {
            return state;
        }
        
        public double getIntensity() {
            return intensity;
        }
    }   
    
    private static Color createShade(Color initialShade, Color finalShade, double intensity) {
        int redDelta = finalShade.getRed() - initialShade.getRed();
        int greenDelta = finalShade.getGreen() - initialShade.getGreen();
        int blueDelta = finalShade.getBlue() - initialShade.getBlue();
        
        redDelta *= intensity;
        greenDelta *= intensity;
        blueDelta *= intensity;
        
        return new Color(initialShade.getRed() + redDelta,
                initialShade.getGreen() + greenDelta,
                initialShade.getBlue() + blueDelta);
    }
}