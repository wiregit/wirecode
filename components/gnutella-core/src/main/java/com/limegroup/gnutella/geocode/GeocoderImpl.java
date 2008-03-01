package com.limegroup.gnutella.geocode;

import java.util.StringTokenizer;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.SuccessOrFailureCallbackConsumer;
import com.limegroup.gnutella.http.HttpExecutor;
import com.limegroup.gnutella.settings.GeocodeSettings;

@Singleton
public final class GeocoderImpl implements Geocoder {

    private final SuccessOrFailureCallbackConsumer<String> exe;

    private GeocodeInformation info;

    private Throwable reasonForFailure;

    @Inject
    public GeocoderImpl(Provider<HttpExecutor> exe) {
        this(new HttpExecutorSuccessOrFailureCallbackConsumer(exe, GeocodeSettings.GEOCODE_URL
                .getValue(), GeocodeSettings.TIMEOUT.getValue()));
    }

    GeocoderImpl(SuccessOrFailureCallbackConsumer<String> exe) {
        this.exe = exe;
    }

    // ---------------------------------------------------------------
    // Implementation of Geocoder
    // ---------------------------------------------------------------

    public synchronized GeocodeInformation getGeocodeInformation() {
        return info;
    }

    public boolean isReady() {
        return hasFailed() || info != null;
    }

    public boolean hasFailed() {
        return reasonForFailure != null;
    }

    public void initialize() {
        exe.consume(this);
    }

    public void clear() {
        info = null;
    }

    // ---------------------------------------------------------------
    // Implementation of SuccessOrFailureCallback<String>
    // ---------------------------------------------------------------

    public void setInvalid(Throwable reasonForFailure) {
        this.reasonForFailure = reasonForFailure;
    }

    public void process(String str) {
        setGeocodeInformation(str);
    }

    public Throwable getReasonForFailure() {
        return reasonForFailure;
    }

    /**
     * Read the lines and set the fields appropriately. The fields will be
     * name/value pairs separated by tabs (i.e. <code>\t</code>). The name
     * correspond to the set method on {@link GeocodeInformationImpl}.
     * <p>
     * For example: <code>countryName   United States</code> would cause a call
     * to <code>g.setCountryName("United States").
     * 
     * @param lines input lines of the form
     * <blockquote>
     * T N ( String[<em>Name</em>] T String[<em>Value</em>] N )*
     * </blockquote>
     * Where <em>T</em> could be <code>'\t'</code> and <em>N</em> could be <code>'\n'</code>, for example.
     */
    synchronized void setGeocodeInformation(String lines) {
        GeocodeInformationImpl res = new GeocodeInformationImpl();
        
        // The first character is key-value separator
        String separator = String.valueOf(lines.charAt(0));
        
        // The second character is entry separator
        String newline = String.valueOf(lines.charAt(1));
        
        for (StringTokenizer st = new StringTokenizer(lines.substring(2), newline, false); st.hasMoreTokens();) {
            String line = st.nextToken().trim();
            if (line.equals("")) {
                continue;
            }
            String[] parts = line.split(separator);
            if (parts.length < 2) {
                continue;
            }
            String name = parts[0];
            String value = parts[1];
            res.setProperty(name, value);
        }
        this.info = res;
    }

}
