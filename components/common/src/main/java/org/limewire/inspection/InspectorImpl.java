package org.limewire.inspection;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
        InputStream fis = null;
        Properties p = new Properties();
        try {
            fis = new BufferedInputStream(new FileInputStream(props));
            p.load(fis);
            this.props = p;
        } catch(IllegalArgumentException ignored) {
        } catch(StringIndexOutOfBoundsException sioobe) {
        } catch(IOException iox) {}
        finally {
            if (fis != null)
                try {fis.close();} catch (IOException ignore){}
        }
    }

}
