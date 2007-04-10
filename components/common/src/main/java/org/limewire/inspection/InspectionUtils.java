package org.limewire.inspection;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;



public class InspectionUtils {
    /**
     * 
     * Inspects an encoded field and returns a String representation of that
     * field.  The field must
     * 
     * a) implement the Inspectable interface, or else
     * b) be annotated with @InspectablePrimitive, or else
     * c) be annotated with @InspectableForSize and have a size() method.
     * 
     * @param encodedField - name of the field we want to get, starting with
     * a fully qualified class name, and followed by comma-separated
     * field names that will help us reach the target field.
     *
     * Example:
     * "com.limegroup.gnutella.RouterService,downloadManager,active"
     * will return the list of active downloads.
     *
     * @return the object pointed to by the last field, or an Exception
     * object if such was thrown trying to get it.
     */
    public static String inspectValue(String encodedField) throws InspectionException {
        try {
            StringTokenizer t = new StringTokenizer(encodedField, ",");
            if (t.countTokens() < 2)
                throw new InspectionException();
            // the first token better be fully qualified class name
            List<Annotation> annotations = new ArrayList<Annotation>();
            Object instance =
                getValue(Class.forName(t.nextToken()), t.nextToken(), annotations);
            while (t.hasMoreTokens())
                instance = getValue(instance, t.nextToken(), annotations);
            return inspect(instance, annotations);
        } catch (Throwable e) {
            if (e instanceof InspectionException)
                throw (InspectionException)e;
            throw new InspectionException(e);
        }
    }

    /**
     * Gets a string representation of an object.
     * 
     * @param o the object to be inspected
     * @param annotations annotations that were found in the last field traversed 
     * while looking for this object
     * @return a String representation taken either from Inspectable.inspect(),
     * String.valueOf or size() depending on the annotation or type of the field.
     */
    private static String inspect(Object o, List<Annotation> annotations) throws Exception {
        if (o instanceof Inspectable) {
            Inspectable i = (Inspectable) o;
            return i.inspect();
        }
        
        for (Annotation a : annotations) {
            if (a instanceof InspectablePrimitive)
                return String.valueOf(o);

            if (a instanceof InspectableForSize) {
                Method m = o.getClass().getMethod("size", new Class[0]);
                return m.invoke(o, new Object[0]).toString();
            }
        }
        throw new InspectionException();
    }
    
    /**
     * Finds a field with the specified name in an object, storing any
     * annotations the field had in the annotations list.
     * @return the object pointed to by the field, boxed if primitive.
     */
    private static Object getValue(Object instance, String fieldName, 
            List<Annotation> annotations )     
    throws IllegalAccessException, NoSuchFieldException {
        Field field;
        if ( instance instanceof Class )
            field = getFieldImpl((Class)instance, fieldName);
        else
            field = getFieldImpl(instance.getClass(), fieldName);
        field.setAccessible(true);
        
        // clear the list of annotations and add any we find
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
