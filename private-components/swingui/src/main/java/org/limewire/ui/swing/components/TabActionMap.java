package org.limewire.ui.swing.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.Action;

/**
 * A collection of actions for use with {@link FancyTabList}.
 */
public class TabActionMap {
    
    /** An action command to be used for the select action. */
    public static final String SELECT_COMMAND = "tab.select";
    /** An action command to be used for the remove action. */
    public static final String REMOVE_COMMAND = "tab.remove";
    
    private final Action select;
    private final Action remove;
    private final Action moreText;
    private final List<Action> rightClick;
    
    private Action removeOthers;
    private Action removeAll;
        
    public TabActionMap(Action selectAction, Action removeAction, Action moreTextAction,
            List<Action> rightClickActions) {
        this.select = selectAction;
        this.remove = removeAction;
        this.moreText = moreTextAction;
        if(rightClickActions == null) {
            this.rightClick = Collections.emptyList();
        } else {
            this.rightClick = rightClickActions;
        }
    }
    
    Action getRemoveOthers() {
        return removeOthers;
    }
    
    void setRemoveOthers(Action removeOthers) {
        this.removeOthers = removeOthers;
    }

    Action getRemoveAll() {
        return removeAll;
    }

    void setRemoveAll(Action removeAll) {
        this.removeAll = removeAll;
    }

    public Action getSelectAction() {
        return select;
    }

    public Action getRemoveAction() {
        return remove;
    }
    
    public Action getMoreTextAction() {
        return moreText;
    }
    
    public List<Action> getRightClickActions() {
        return rightClick;
    }
    
    /**
     * Wraps the given {@link Action} for select within a list {@link TabActionMap},
     * suitable for constructing a {@link FancyTabList}.
     */
    public static List<TabActionMap> createMapForSelectActions(Action... selectActions) {
        return createMapForSelectActions(Arrays.asList(selectActions));
    }
    
    /**
     * Wraps the given {@link Action} for select within a list {@link TabActionMap},
     * suitable for constructing a {@link FancyTabList}.
     */
    public static List<TabActionMap> createMapForSelectActions(Collection<? extends Action> selectActions) {
        List<TabActionMap> maps = new ArrayList<TabActionMap>();
        for(Action action : selectActions) {
            maps.add(new TabActionMap(action, null, null, null));
        }
        return maps;
    }

}
