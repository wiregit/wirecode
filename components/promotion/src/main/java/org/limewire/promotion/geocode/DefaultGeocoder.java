package org.limewire.promotion.geocode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.inject.Singleton;

@Singleton
public class DefaultGeocoder implements Geocoder {
    
    private final static Log LOG = LogFactory.getLog(DefaultGeocoder.class);    
    private final SuccessOrFailureCallbackConsumer<InputStream> exe;
    private GeocodeInformation info;
    private Throwable reasonForFailure;

    protected DefaultGeocoder(SuccessOrFailureCallbackConsumer<InputStream> exe) {
        this.exe = exe;
    }

    // ---------------------------------------------------------------
    // Implementation of Geocoder
    // ---------------------------------------------------------------

    public final synchronized GeocodeInformation getGeocodeInformation() {
        return info;
    }

    public final boolean isReady() {
        return hasFailed() || info != null;
    }

    public final boolean hasFailed() {
        return reasonForFailure != null;
    }

    public final void initialize() {
        exe.consume(this);
    }

    public final void clear() {
        info = null;
    }

    // ---------------------------------------------------------------
    // Implementation of SuccessOrFailureCallback<String>
    // ---------------------------------------------------------------

    public final void setInvalid(Throwable reasonForFailure) {
        this.reasonForFailure = reasonForFailure;
    }

    public final void process(InputStream str) {
        try {
            setGeocodeInformation(str);
        } catch (IOException e) {
            LOG.error(e);
        }
    }

    public final Throwable getReasonForFailure() {
        return reasonForFailure;
    }

    /**
     * Read the lines and set the fields appropriately. The fields will be
     * name/value pairs separated by tabs (i.e. <code>\t</code>). The name
     * correspond to the set method on {@link GeocodeInformation}.
     * <p>
     * For example: <code>countryName   United States</code> would cause a call
     * to <code>g.setCountryName("United States").
     * 
     * @param is input lines of the form
     * <blockquote>
     * T N ( String[<em>Name</em>] T String[<em>Value</em>] N )*
     * </blockquote>
     * Where <em>T</em> could be <code>'\t'</code> and <em>N</em> could be <code>'\n'</code>, for example.
     * @throws IOException 
     */
    final synchronized void setGeocodeInformation(InputStream is) throws IOException {

        GeocodeInformation res = new GeocodeInformation();

        // The first character is key-value separator
        String separator;

        separator = String.valueOf((char) is.read());

        // The second character is entry separator
        // This won't matter now
        String newline = String.valueOf((char) is.read());

        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        String line;
        while ((line = in.readLine()) != null) {
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
        in.close();
    }

}
