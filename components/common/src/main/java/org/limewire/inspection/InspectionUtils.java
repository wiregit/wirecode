package org.limewire.inspection;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.limewire.inspection.Inspectable.InspectableForSize;
import org.limewire.inspection.Inspectable.InspectablePrimitive;


public class InspectionUtils {
    
    public static String inspectValue(String encodedField) {
        try {
            StringTokenizer t = new StringTokenizer(encodedField, ",");
            if (t.countTokens() < 2)
                return "invalid field";
            // the first token better be fully qualified class name
            List<Annotation> annotations = new ArrayList<Annotation>();
            Object instance =
                getValue(Class.forName(t.nextToken()), t.nextToken(), annotations);
            while (t.hasMoreTokens())
                instance = getValue(instance, t.nextToken(), annotations);
            return inspect(instance, annotations);
        } catch (Throwable e) {
            return e.toString();
        }
    }
    
    private static String inspect(Object o, List<Annotation> annotations) {
        if (o instanceof Inspectable) {
            Inspectable i = (Inspectable) o;
            return i.inspect();
        }
        
        for (Annotation a : annotations) {
            if (a instanceof InspectablePrimitive)
                return String.valueOf(o);

            if (a instanceof InspectableForSize) {
                try {
                    Method m = o.getClass().getMethod("size", new Class[0]);
                    return m.invoke(o, new Object[0]).toString();
                }  catch (NoSuchMethodException nsme) {
                    return "cannot find method size() for class "+o.getClass();
                } catch (IllegalAccessException iae) {
                    return "cannot invoke size on class "+o.getClass();
                } catch (InvocationTargetException ite) {
                    return "invoking size failed on class "+o.getClass();
                }
            }
        }
        return "object of class "+o.getClass()+" is not inspectable";
    }
    
    private static Object getValue(Object instance, String fieldName, 
            List<Annotation> annotations )     
    throws IllegalAccessException, NoSuchFieldException {
        Field field;
        if ( instance instanceof Class )
            field = getFieldImpl((Class)instance, fieldName);
        else
            field = getFieldImpl(instance.getClass(), fieldName);
        field.setAccessible(true);
        annotations.clear();
        for (Annotation a : field.getAnnotations())
            annotations.add(a);
        return field.get(instance);
    }

    private static Field getFieldImpl(Class thisClass, 
            String fieldName) 
    throws NoSuchFieldException {
        if (thisClass == null)
            throw new NoSuchFieldException("Invalid field : " + fieldName);
        try {
            return thisClass.getDeclaredField( fieldName );
        }
        catch(NoSuchFieldException e) {
            return getFieldImpl(thisClass.getSuperclass(), fieldName);
        }
    }
}
