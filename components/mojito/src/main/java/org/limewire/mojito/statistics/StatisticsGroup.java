package org.limewire.mojito.statistics;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class StatisticsGroup {
    
    private final Log log = LogFactory.getLog(getClass());
    
    public void write(Writer out) throws IOException {
        List<Field> fields = new ArrayList<Field>();

        Class<?> superClass = getClass().getSuperclass();
        Class<?> declaringClass = getClass().getDeclaringClass();
        
        if (superClass != null) {
            fields.addAll(Arrays.asList(superClass.getFields()));
        }
        
        if (declaringClass != null) {
            fields.addAll(Arrays.asList(declaringClass.getFields()));
        }
        
        fields.addAll(Arrays.asList(getClass().getFields()));
        
        for (Field field : fields) {
            try {
                if (Modifier.isTransient(field.getModifiers())) {
                    continue;
                }
                
                Object value = field.get(this);
                if (!(value instanceof Statistic)) {
                    continue;
                }
                
                Statistic statistic = (Statistic) value;
                out.write(field.getName() + "\t");
                statistic.write(out);
                out.write("\n");
                
            } catch (IllegalAccessException err) {
                log.error("IllegalAccessException", err);
                continue;
            }
        }
    }
}
