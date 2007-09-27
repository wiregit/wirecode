package org.limewire.inspection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

@Singleton
public class InspectorImpl implements Inspector {

    private volatile Properties props;
    private final Injector injector;
    
    @Inject
    InspectorImpl(Injector injector) {
        this.injector = injector;
    }
    
    public Object inspect(String key) throws InspectionException {
        if (props == null || !props.containsKey(key))
            throw new InspectionException();
        return InspectionUtils.inspectValue(props.getProperty(key), injector);
    }

    public void load(File props) {
        /*
         * Ignore any and all errors as inspection is not critical functionality
         */
        BufferedReader in = null;
        Properties p = new Properties();
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(props)));
            String current = null;
            while((current = in.readLine()) != null) {
                String [] k = current.split("=");
                if (k.length != 2)
                    continue;
                p.setProperty(k[0], k[1]);
            }
            this.props = p;
        } catch(IllegalArgumentException ignored) {
        } catch(StringIndexOutOfBoundsException sioobe) {
        } catch(IOException iox) {}
        finally {
            if (in != null)
                try {in.close();} catch (IOException ignore){}
        }
    }

}
