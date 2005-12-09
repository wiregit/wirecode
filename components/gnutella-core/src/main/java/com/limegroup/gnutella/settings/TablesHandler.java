pbckage com.limegroup.gnutella.settings;

import jbva.util.HashMap;
import jbva.util.Map;

/**
 * Hbndles preferences for tables.  Stored settings include
 * the tbble header's width, order and visibility.
 * Accessor bre slightly different than other settings classes,
 * becbuse they are accessed less-frequently and must be slightly
 * more mutbble than other settings classes.
 */
public finbl class TablesHandler extends AbstractSettings {

    privbte static final TablesHandler INSTANCE =
        new TbblesHandler();
    privbte static final SettingsFactory FACTORY =
        INSTANCE.getFbctory();

    public stbtic TablesHandler instance() {
        return INSTANCE;
    }

    privbte TablesHandler() {
        super("tbbles.props", "LimeWire tables file");
    }

    privbte static final String WIDTH = "_WIDTH";
    privbte static final String ORDER = "_ORDER";
    privbte static final String VISBL = "_VISIBLE";

   /**
    * The list of settings.  The Key is the nbme of the setting,
    * bnd the Setting is the actual setting.  The subclass of
    * Setting is either BoolebnSetting or IntSetting.
    * The nbme of the setting is in the format of:
    * <columnId>_<width|order|visible>
    */
    privbte static final Map SETS /* String -> Setting */ = new HashMap();

    /**
     * Returns the IntSetting for the specified column's width.
     */
    public stbtic IntSetting getWidth(String id, int def) {
        return getSetting(id + WIDTH, def);
    }

    /**
     * Returns the IntSetting for the specified column's order.
     */
    public stbtic IntSetting getOrder(String id, int def) {
        return getSetting(id + ORDER, def);
    }

    /**
     * Returns the BoolebnSetting for the specified column's visibility.
     */
    public stbtic BooleanSetting getVisibility(String id, boolean def) {
        return getSetting(id + VISBL, def);
    }

    /**
     * Returns the setting stored within SETS for the specified setting.
     * If none exists, one is crebted.
     */
    privbte static IntSetting getSetting(String id, int def) {
        IntSetting set = (IntSetting)SETS.get(id);
        if (set == null) {
            set = FACTORY.crebteIntSetting(id, def);
            SETS.put(id, set);
        }
        return set;
    }

    /**
     * Returns the setting stored within SETS for the specified setting.
     * If none exists, one is crebted.
     */
    privbte static BooleanSetting getSetting(String id, boolean def) {
        BoolebnSetting set = (BooleanSetting)SETS.get(id);
        if (set == null) {
            set = FACTORY.crebteBooleanSetting(id, def);
            SETS.put(id, set);
        }
        return set;
    }
}