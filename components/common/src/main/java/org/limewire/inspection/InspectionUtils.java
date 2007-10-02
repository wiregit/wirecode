package org.limewire.inspection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import com.google.inject.Injector;
import com.google.inject.Singleton;


/**
 * Gets the value of an object that implements {@link Inspectable} or an object
 * with an annotation of {@link InspectableForSize @InspectableForSize} or 
 * {@link InspectablePrimitive @InspectablePrimitive}. 
 * <p>
 * See the Lime Wire Wiki for sample code using the <a href="http://www.limewire.org/wiki/index.php?title=Org.limewire.inspection">
 * org.limewire.inspection</a> package.
 * 
 */

public class InspectionUtils {
    /**
     * 
     * Inspects a field and returns a representation of that field. The field must:
     * <p>
     * a) implement the <code>Inspectable</code> interface, in which case the
     * return value of the <code>inspect</code> method is returned, 
     * or else
     * <p>
     * b) be annotated with <code>@InspectablePrimitive</code>, in 
     * which case the <code>String.valueOf</code> is returned, or else
     * <p>
     * c) be annotated with {@link InspectableForSize @InspectableForSize} and have a 
     * <code>size</code> method in which case the return value of the <code>size</code>
     * method call is returned.
     * 
     * @param encodedField - name of the field we want to get, starting with
     * a fully qualified class name, and followed by comma-separated
     * field names that will help us reach the target field.
     * <p>
     * For example:
     * "com.limegroup.gnutella.gui.GUIMediator,DOWNLOAD_MEDIATOR,resumeClicks"
     * will return the count of resume clicks.
     *
     * @return the object pointed to by the last field, or an Exception
     * object if such was thrown trying to get it.
     */
    public static Object inspectValue(String encodedField, Injector injector) throws InspectionException {
        try {
            List<Annotation> annotations = new ArrayList<Annotation>();
            return inspect(getTargetObject(encodedField, annotations, injector), annotations);
        } catch (Throwable e) {
            if (e instanceof InspectionException)
                throw (InspectionException)e;
            throw new InspectionException(e);
        }
    }
    
    private static Object getTargetObject(String encodedField, List<Annotation> annotations, Injector injector) 
    throws Throwable {

        // see if this is old-style path
        if (encodedField.contains(":"))
            return getTargetStaticObject(encodedField, annotations);
        
        StringTokenizer t = new StringTokenizer(encodedField, ",");
        if (t.countTokens() < 2)
            throw new InspectionException();
        
        // the first token better be fully qualified class name
        Class<?> clazz = Class.forName(t.nextToken());

        // try to find an instance of the object
        Object instance;
        
        // check if this is an enclosed class
        Class<?> enclosing = clazz.getEnclosingClass();
        if (enclosing == null) {
            if (clazz.getAnnotation(Singleton.class) == null)
                throw new InspectionException();
            instance = injector.getInstance(clazz);
        } else {
            
            // inner classes must be annoated properly
            if (enclosing.getAnnotation(Singleton.class) == null)
                throw new InspectionException();
            if (clazz.getAnnotation(InspectableContainer.class) == null)
                throw new InspectionException();
            
            // if inner, create one
            Object enclosingObj = injector.getInstance(enclosing);
            Constructor []constructors  = clazz.getDeclaredConstructors();
            if (constructors.length != 1)
                throw new InspectionException();
            Class [] parameters = constructors[0].getParameterTypes();
            if (parameters.length != 1 || !parameters[0].equals(enclosing)) 
                throw new InspectionException();
            constructors[0].setAccessible(true);
            instance = constructors[0].newInstance(new Object[]{enclosingObj});
        }
        
        while (t.hasMoreTokens())
            instance = getValue(instance, t.nextToken(), annotations);
        return instance;
    }

    private static Object getTargetStaticObject(String encodedField, List<Annotation> annotations ) 
    throws Throwable {
        StringTokenizer t = new StringTokenizer(encodedField, ":,");
        if (t.countTokens() < 2)
            throw new InspectionException();
        
        // do it old style
        Object instance =
            getValue(Class.forName(t.nextToken()), t.nextToken(), annotations);
        while (t.hasMoreTokens())
            instance = getValue(instance, t.nextToken(), annotations);
        return instance;
        
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
    private static Object inspect(Object o, List<Annotation> annotations) throws Exception {
        if (o instanceof Inspectable) {
            Inspectable i = (Inspectable) o;
            return i.inspect();
        }
        
        for (Annotation a : annotations) {
            if (a instanceof InspectablePrimitive)
                return String.valueOf(o);

            if (a instanceof InspectableForSize) {
                Method m = o.getClass().getMethod("size", new Class[0]);
                m.setAccessible(true);
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
        if (annotations != null) {
            annotations.clear();
            for (Annotation a : field.getAnnotations())
                annotations.add(a);
        }
        
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
