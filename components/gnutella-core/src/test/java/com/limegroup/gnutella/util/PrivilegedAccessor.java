package com.limegroup.gnutella.util;

import java.lang.reflect.*;
import java.util.Arrays;

/**
 * a.k.a. The "ObjectMolester"
 * <p>
 * This class is used to access a method or field of an object no
 * matter what the access modifier of the method or field.  The syntax
 * for accessing fields and methods is out of the ordinary because this
 * class uses reflection to peel away protection.
 * <p>
 * Here is an example of using this to access a private member.
 * <code>resolveName</code> is a private method of <code>Class</code>.
 *
 * <pre>
 * Class c = Class.class;
 * System.out.println(
 *      PrivilegedAccessor.invokeMethod( c,
 *                                       "resolveName",
 *                                       "/net/iss/common/PrivilegeAccessor" ) );
 * </pre>
 *
 * @author Charlie Hubbard (chubbard@iss.net)
 * @author Prashant Dhokte (pdhokte@iss.net)
 * @author Christopher Rohrs (added setValue)
 * @author Sam Berlin (added support for static fields/methods,
 *                     native parameters in methods, and invokeConstructor)
 */
public class PrivilegedAccessor {

    /**
     * Gets the value of the named field and returns it as an object.
     *
     * @param instance the object instance
     * @param fieldName the name of the field
     * @return an object representing the value of the field
     */
    public static Object getValue(Object instance, String fieldName )     
           throws IllegalAccessException, NoSuchFieldException {
        Field field;
        if ( instance instanceof Class )
            field = getField((Class)instance, fieldName);
        else
            field = getField(instance.getClass(), fieldName);
        field.setAccessible(true);
        return field.get(instance);
    }

    /**
     * Sets the value of the named field.
     *
     * @param instance the object instance
     * @param fieldName the name of the field
     * @param value an object representing the value of the field
     */
    public static void setValue(Object instance, 
                                String fieldName, 
                                Object value) 
            throws IllegalAccessException, NoSuchFieldException {
        Field field;
        if ( instance instanceof Class )
            field = getField((Class)instance, fieldName);
        else
            field = getField(instance.getClass(), fieldName);
        field.setAccessible(true);
        field.set(instance, value);
    }

    /**
     * Calls a method on the given object instance with the given argument.
     *
     * @param instance the object instance
     * @param methodName the name of the method to invoke
     * @param arg the argument to pass to the method
     * @see PrivilegedAccessor#invokeMethod(Object,String,Object[])
     */
    public static Object invokeMethod(Object instance, 
                                      String methodName,
                                      Object arg) 
        throws NoSuchMethodException,
               IllegalAccessException, 
               InvocationTargetException  {
        Object[] args = new Object[1];
        args[0] = arg;
        return invokeMethod(instance, methodName, args);
    }

    /**
     * Calls a method on the given object instance with the given arguments.
     *
     * @param instance the object instance
     * @param methodName the name of the method to invoke
     * @param args an array of objects to pass as arguments
     * @see PrivilegedAccessor#invokeMethod(Object,String,Object)
     */
    public static Object invokeMethod(Object instance, 
                                      String methodName, 
                                      Object[] args ) 
        throws NoSuchMethodException,
               IllegalAccessException, 
               InvocationTargetException  {
        Class[] classTypes = null;
        if( args != null) {
            classTypes = new Class[args.length];
            for( int i = 0; i < args.length; i++ ) {
                if( args[i] != null )
                    classTypes[i] = args[i].getClass();
            }
        }
        return invokeMethod(instance, methodName, args, classTypes);
    }
    
    /**
     * Calls a method on the given object instance with the given arguments
     * and types.
     * Necessary for using native-type parameters and when the arguments
     * are subclassed objects.
     *
     * @param instance the object instance
     * @param methodName the name of the method to invoke
     * @param args an array of objects to pass as arguments
     * @param classTypes an array of the types of the arguments.
     * @see PrivilegedAccessor#invokeMethod(Object,String,Object)
     */
    public static Object invokeMethod(Object instance, 
                                      String methodName, 
                                      Object[] args,
                                      Class[] classTypes ) 
        throws NoSuchMethodException,
               IllegalAccessException, 
               InvocationTargetException  {
        return getMethod(instance,methodName,classTypes).invoke(instance,args);
    }    

    /**
     *
     * @param instance the object instance
     * @param methodName the
     */
    public static Method getMethod(Object instance, 
                                   String methodName, 
                                   Class[] classTypes ) 
                                   throws NoSuchMethodException {
        Method accessMethod;
        if ( instance instanceof Class )
            accessMethod = getMethodImpl((Class)instance, methodName, classTypes);
        else
            accessMethod = getMethodImpl(instance.getClass(), methodName, classTypes);
        accessMethod.setAccessible(true);
        return accessMethod;
    }
    
    /**
     * Constructs an object with the given parameters.
     */
    public static Object invokeConstructor(Class clazz,
                                           Object[] args ) 
        throws NoSuchMethodException,
               IllegalAccessException,
               InvocationTargetException,
               InstantiationException {
        Class[] classTypes = null;
        if( args != null) {
            classTypes = new Class[args.length];
            for( int i = 0; i < args.length; i++ ) {
                if( args[i] != null )
                    classTypes[i] = args[i].getClass();
            }
        }
        return getConstructor(clazz, classTypes).newInstance(args);
    }
    
    

    /**
     * Return the named field from the given class.
     */
    private static Field getField(Class thisClass, 
                                  String fieldName) 
                                  throws NoSuchFieldException {
        if (thisClass == null)
            throw new NoSuchFieldException("Invalid field : " + fieldName);
        try {
            return thisClass.getDeclaredField( fieldName );
        }
        catch(NoSuchFieldException e) {
            return getField(thisClass.getSuperclass(), fieldName);
        }
    }

    /**
     * Return the named method with a method signature matching classTypes
     * from the given class.
     */
    private static Method getMethodImpl(Class thisClass, 
                                    String methodName, 
                                    Class[] classTypes) 
                                    throws NoSuchMethodException {
        if (thisClass == null)
            throw new NoSuchMethodException("Invalid method : " + methodName);
        try {
            return thisClass.getDeclaredMethod( methodName, classTypes );
        }
        catch(NoSuchMethodException e) {
            return getMethodImpl(thisClass.getSuperclass(), methodName, classTypes);
        }
    }
    
    /**
     * Return the constructor with the given parameters.
     */
    private static Constructor getConstructor(Class clazz, Class[] classTypes)
      throws NoSuchMethodException {
        Constructor[] cs = clazz.getDeclaredConstructors();
        for (int i = 0; i < cs.length; i++) {
            if ( Arrays.equals(classTypes, cs[i].getParameterTypes()) ) {
                cs[i].setAccessible(true);
                return cs[i];
            }
        }
        throw new NoSuchMethodException();
    }
}
