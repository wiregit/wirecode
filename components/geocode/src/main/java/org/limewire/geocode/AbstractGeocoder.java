package org.limewire.geocode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


public abstract class AbstractGeocoder implements Geocoder {
    
    private GeocodeInformation info;
    private Throwable reasonForFailure;

    public GeocodeInformation getGeocodeInformation() {
        return info;
    }

    public boolean isReady() {
        return hasFailed() || info != null;
    }

    public boolean hasFailed() {
        return reasonForFailure != null;
    }

    public void clear() {
        info = null;
    }

    protected void setInvalid(Throwable reasonForFailure) {
        this.reasonForFailure = reasonForFailure;
    }

    Throwable getReasonForFailure() {
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
     * @param is input lines of the form
     * <blockquote>
     * T N ( String[<em>Name</em>] T String[<em>Value</em>] N )*
     * </blockquote>
     * Where <em>T</em> could be <code>'\t'</code> and <em>N</em> could be <code>'\n'</code>, for example.
     * @throws IOException 
     */
    protected void setGeocodeInformation(InputStream is) throws IOException {

        GeocodeInformation res = new GeocodeInformation();

        // The first character is key-value separator
        String separator = String.valueOf((char) is.read());

        // The second character is entry separator
        is.read(); // newline

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
    }

}
