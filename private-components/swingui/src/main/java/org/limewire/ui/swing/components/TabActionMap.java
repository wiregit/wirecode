package org.limewire.ui.swing.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.swing.Action;
import javax.swing.ActionMap;

/**
 * A typesafe extension to ActionMap for use with commands
 * specific to tabs.
 */
public class TabActionMap extends ActionMap {
    
    public static final String SELECT_COMMAND = "tab.select";
    public static final String REMOVE_COMMAND = "tab.remove";
        
    public TabActionMap(Action selectAction, Action removeAction) {
        put(SELECT_COMMAND, selectAction);
        put(REMOVE_COMMAND, removeAction);
    }

    public Action getSelectAction() {
        return get(SELECT_COMMAND);
    }

    public Action getRemoveAction() {
        return get(REMOVE_COMMAND);
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
            maps.add(new TabActionMap(action, null));
        }
        return maps;
    }

}
