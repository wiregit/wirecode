package org.limewire.ui.swing.search.filter;

import java.awt.Dimension;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;

import org.limewire.ui.swing.components.RangeSlider;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.matchers.Matcher;

/**
 * Filter to select search results according to a range of values.
 */
class RangeFilter extends AbstractFilter {

    private final JPanel panel = new JPanel();
    private final JLabel headerLabel = new JLabel();
    private final JLabel valueLabel = new JLabel();
    private final RangeSlider slider = new RangeSlider();
    
    private final RangeFilterFormat rangeFormat;
    
    private boolean resetAdjusting;
    
    /**
     * Constructs a RangeFilter with the specified range format.
     */
    public RangeFilter(RangeFilterFormat rangeFormat) {
        this.rangeFormat = rangeFormat;
        
        FilterResources resources = getResources();
        
        panel.setLayout(new MigLayout("insets 0 0 0 0, gap 0!", 
                "[left,grow]",
                "[top][top]3[top]"));
        panel.setOpaque(false);

        headerLabel.setFont(resources.getHeaderFont());
        headerLabel.setForeground(resources.getHeaderColor());
        headerLabel.setText(rangeFormat.getHeaderText());
        
        valueLabel.setFont(resources.getRowFont());
        valueLabel.setForeground(resources.getRowColor());
        
        slider.setMinimum(0);
        slider.setMaximum(rangeFormat.getValues().length - 1);
        slider.setOpaque(false);
        slider.setPreferredSize(new Dimension(resources.getFilterWidth(), slider.getPreferredSize().height));
        slider.setRequestFocusEnabled(false);
        slider.setUpperThumbEnabled(rangeFormat.isUpperLimitEnabled());
        
        // Set initial values.
        resetSliderRange();

        // Add listener to update filter.
        slider.addChangeListener(new SliderListener());
        
        panel.add(headerLabel, "wrap");
        panel.add(valueLabel , "wrap");
        panel.add(slider     , "growx");
    }
    
    @Override
    public JComponent getComponent() {
        return panel;
    }

    @Override
    public void reset() {
        // Reset slider values.  We temporarily set resetAdjusting to true so 
        // the slider listener can avoid handling the events.
        resetAdjusting = true;
        resetSliderRange();
        resetAdjusting = false;
        
        // Deactivate filter.
        deactivate();
    }

    @Override
    public void dispose() {
        // Do nothing.
    }

    /**
     * Creates a text string describing the current selected range.
     */
    protected String createRangeText() {
        if (rangeFormat.isUpperLimitEnabled()) {
            // Get text for lower and upper values.
            String minText = rangeFormat.getValueText(slider.getValue());
            String maxText = rangeFormat.getValueText(slider.getUpperValue());
            
            if (slider.getUpperValue() == slider.getMaximum()) {
                return I18n.tr("{0} or above", minText);
            } else {
                return I18n.tr("{0} to {1}", minText, maxText);
            }

        } else {
            // Get text for lower value only.
            String minText = rangeFormat.getValueText(slider.getValue());
            
            if (slider.getValue() == slider.getMinimum()) {
                return I18n.tr("all");
            } else {
                return I18n.tr("{0} or above", minText);
            }
        }
    }
    
    /**
     * Resets the range values to the minimum and maximum. 
     */
    private void resetSliderRange() {
        // Set lower value to minimum.
        slider.setValue(slider.getMinimum());
        
        // Set upper value to maximum if enabled.
        if (rangeFormat.isUpperLimitEnabled()) {
            slider.setUpperValue(slider.getMaximum());
        }
        
        // Set range text.
        valueLabel.setText(createRangeText());
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
            
            // Update range display.
            RangeSlider slider = (RangeSlider) e.getSource();
            String rangeText = createRangeText();
            valueLabel.setText(rangeText);
            
            // When slider movement has ended, apply filter for selected range.
            if (!slider.getValueIsAdjusting()) {
                long[] values = rangeFormat.getValues();
                
                // Determine if filter is activated.  This is true when the 
                // slider values are set to anything other than the min/max.
                boolean activate = false;
                if (rangeFormat.isUpperLimitEnabled()) {
                    activate = (slider.getValue() > 0) || (slider.getUpperValue() < values.length - 1);
                } else {
                    activate = (slider.getValue() > 0);
                }
                
                if (activate) {
                    // Get selected range.
                    long minValue = values[slider.getValue()];
                    long maxValue = values[slider.getUpperValue()];

                    // Create new matcher and activate.
                    Matcher<VisualSearchResult> newMatcher = rangeFormat.getMatcher(minValue, maxValue);
                    activate(rangeText, newMatcher);
                    
                } else {
                    // Deactivate to clear matcher.
                    deactivate();
                }
                
                // Notify filter listeners.
                fireFilterChanged(RangeFilter.this);
            }
        }
    }
}
