package org.limewire.ui.swing.search.filter;

import java.awt.Font;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;

import org.limewire.ui.swing.components.RangeSlider;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.matchers.Matcher;

/**
 * Filter component to select search results according to their quality.
 */
class QualityFilter extends AbstractFilter {
    /** Array of quality options. */
    private static final long[] QUALITIES = {
        0, // spam
        1, // poor
        2, // good
        3  // excellent
    };
    
    private final JPanel panel = new JPanel();
    private final JLabel qualityLabel = new JLabel();
    private final JLabel qualityValue = new JLabel();
    private final RangeSlider qualitySlider = new RangeSlider();
    
    private boolean resetAdjusting;

    /**
     * Constructs a QualityFilter.
     */
    public QualityFilter() {
        panel.setLayout(new MigLayout("insets 1 1 0 0", 
                "[left]3[left]",
                "[top]3[top]"));
        panel.setOpaque(false);

        qualityLabel.setText(I18n.tr("Quality:"));
        qualityLabel.setFont(qualityLabel.getFont().deriveFont(Font.BOLD));
        
        qualitySlider.setMinimum(0);
        qualitySlider.setMaximum(QUALITIES.length - 1);
        qualitySlider.setOpaque(false);
        qualitySlider.setRequestFocusEnabled(false);
        qualitySlider.setUpperThumbEnabled(false);
        
        // Set initial values.
        qualitySlider.setValue(qualitySlider.getMinimum());
        qualityValue.setText(createRangeText());

        // Add listener to update filter.
        qualitySlider.addChangeListener(new SliderListener());
        
        panel.add(qualityLabel , "");
        panel.add(qualityValue , "wrap");
        panel.add(qualitySlider, "spanx 2, growx");
    }

    @Override
    public JComponent getComponent() {
        return panel;
    }

    @Override
    public void reset() {
        // Reset slider values.
        resetAdjusting = true;
        qualitySlider.setValue(qualitySlider.getMinimum());
        qualityValue.setText(createRangeText());
        resetAdjusting = false;
        
        // Deactivate filter.
        deactivate();
    }

    @Override
    public void dispose() {
        // Do nothing.
    }

    /**
     * Creates a text string describing the current quality.
     */
    private String createRangeText() {
        int value = qualitySlider.getValue();
        if (value == qualitySlider.getMinimum()) {
            return I18n.tr("all"); 
        } else {
            return GuiUtils.toQualityStringShort(QUALITIES[value]);
        }
    }
    
    /**
     * Listener to handle slider change events.
     */
    private class SliderListener implements ChangeListener {

        @Override
        public void stateChanged(ChangeEvent e) {
            // Skip event if filter is being reset.
            if (resetAdjusting) {
                return;
            }
            
            // Update value display.
            RangeSlider slider = (RangeSlider) e.getSource();
            String rangeText = createRangeText();
            qualityValue.setText(rangeText);
            
            // When slider movement has ended, apply filter for selected value.
            if (!slider.getValueIsAdjusting()) {
                if (slider.getValue() > 0) {
                    // Get quality.
                    long quality = QUALITIES[slider.getValue()];

                    // Create new matcher and activate.
                    Matcher<VisualSearchResult> newMatcher = new QualityMatcher(quality);
                    activate(rangeText, newMatcher);
                    
                } else {
                    // Deactivate to clear matcher.
                    deactivate();
                }
                
                // Notify filter listeners.
                fireFilterChanged(QualityFilter.this);
            }
        }
    }
}
