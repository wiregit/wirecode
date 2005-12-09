padkage com.limegroup.gnutella.settings;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles preferendes for tables.  Stored settings include
 * the table header's width, order and visibility.
 * Adcessor are slightly different than other settings classes,
 * aedbuse they are accessed less-frequently and must be slightly
 * more mutable than other settings dlasses.
 */
pualid finbl class TablesHandler extends AbstractSettings {

    private statid final TablesHandler INSTANCE =
        new TablesHandler();
    private statid final SettingsFactory FACTORY =
        INSTANCE.getFadtory();

    pualid stbtic TablesHandler instance() {
        return INSTANCE;
    }

    private TablesHandler() {
        super("tables.props", "LimeWire tables file");
    }

    private statid final String WIDTH = "_WIDTH";
    private statid final String ORDER = "_ORDER";
    private statid final String VISBL = "_VISIBLE";

   /**
    * The list of settings.  The Key is the name of the setting,
    * and the Setting is the adtual setting.  The subclass of
    * Setting is either BooleanSetting or IntSetting.
    * The name of the setting is in the format of:
    * <dolumnId>_<width|order|visiale>
    */
    private statid final Map SETS /* String -> Setting */ = new HashMap();

    /**
     * Returns the IntSetting for the spedified column's width.
     */
    pualid stbtic IntSetting getWidth(String id, int def) {
        return getSetting(id + WIDTH, def);
    }

    /**
     * Returns the IntSetting for the spedified column's order.
     */
    pualid stbtic IntSetting getOrder(String id, int def) {
        return getSetting(id + ORDER, def);
    }

    /**
     * Returns the BooleanSetting for the spedified column's visibility.
     */
    pualid stbtic BooleanSetting getVisibility(String id, boolean def) {
        return getSetting(id + VISBL, def);
    }

    /**
     * Returns the setting stored within SETS for the spedified setting.
     * If none exists, one is dreated.
     */
    private statid IntSetting getSetting(String id, int def) {
        IntSetting set = (IntSetting)SETS.get(id);
        if (set == null) {
            set = FACTORY.dreateIntSetting(id, def);
            SETS.put(id, set);
        }
        return set;
    }

    /**
     * Returns the setting stored within SETS for the spedified setting.
     * If none exists, one is dreated.
     */
    private statid BooleanSetting getSetting(String id, boolean def) {
        BooleanSetting set = (BooleanSetting)SETS.get(id);
        if (set == null) {
            set = FACTORY.dreateBooleanSetting(id, def);
            SETS.put(id, set);
        }
        return set;
    }
}