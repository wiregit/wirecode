package org.limewire.ui.swing.components;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.limewire.ui.swing.components.LimeComboBox.SelectionListener;
import org.limewire.util.BaseTestCase;

/**
 * JUnit test case for LimeComboBox.
 */
public class LimeComboBoxTest extends BaseTestCase {
    /** Instance of class being tested. */
    private LimeComboBox limeComboBox;
    
    private Action testAction1;
    private Action testAction2;
    
    private boolean action1Performed;
    private boolean action2Performed;
    private boolean selectionChanged;
    
    /**
     * Constructs a test case for the specified method name.
     */
    public LimeComboBoxTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        // Create test actions.
        testAction1 = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action1Performed = true;
            }
        };
        testAction2 = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action2Performed = true;
            }
        };
        action1Performed = false;
        action2Performed = false;
        selectionChanged = false;
        
        // Create instance using test actions.
        List<Action> testActions = new ArrayList<Action>();
        testActions.add(testAction1);
        testActions.add(testAction2);
        limeComboBox = new LimeComboBox(testActions);
    }

    @Override
    protected void tearDown() throws Exception {
        limeComboBox = null;
        testAction1 = null;
        testAction2 = null;
        super.tearDown();
    }

    /** Tests method to get selected action. */
    public void testGetSelectedAction() {
        // Verify initial selected action.
        assertEquals("selected action", testAction1, limeComboBox.getSelectedAction());
        assertFalse("selection changed", selectionChanged);
    }

    /** Tests method to select an action. */
    public void testSelectAction() {
        // Add selection change listener.
        limeComboBox.addSelectionListener(new SelectionListener() {
            @Override
            public void selectionChanged(Action item) {
                selectionChanged = true;
            }
        });
        
        // Select action.
        limeComboBox.selectAction(testAction2);
        
        // Verify action performed.
        assertFalse("action 1 performed", action1Performed);
        assertTrue("action 2 performed", action2Performed);
        assertTrue("selection changed", selectionChanged);
    }
}
