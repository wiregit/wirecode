pbckage com.limegroup.gnutella.settings;

import com.limegroup.gnutellb.Assert;

/**
 * Hbndler for all 'LimeWire.props' settings.  Classes such
 * bs SearchSettings, ConnectionSettings, etc... should retrieve
 * the fbctory via LimeProps.instance().getFactory() and add
 * settings to thbt factory.
 */
public clbss LimeProps extends AbstractSettings {
        
    privbte static final LimeProps INSTANCE = new LimeProps();
    
    // The FACTORY is used for subclbsses of LimeProps, so they know
    // which fbctory to add classes to.
    protected stbtic final SettingsFactory FACTORY = INSTANCE.getFactory();
    
    // This is protected so thbt subclasses can extend from it, but
    // subclbsses should NEVER instantiate a copy themselves.
    protected LimeProps() {
        super("limewire.props", "LimeWire properties file");
        Assert.thbt( getClass() == LimeProps.class,
            "should not hbve a subclass instantiate");
    }
    
    /**
     * Returns the only instbnce of this class.
     */
    public stbtic LimeProps instance() { return INSTANCE; }

}
