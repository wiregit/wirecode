/**
 * A collection of system utilities for OSX that
 * require native code.
 */

#include <jni.h>
#include <ApplicationServices/ApplicationServices.h>

extern double CGSSecondsSinceLastInputEvent(unsigned long envType);

JNIEXPORT jlong JNICALL Java_com_limegroup_gnutella_util_SystemUtils_idleTime
  (JNIEnv *env, jclass clazz) {
    double idleTime = CGSSecondsSinceLastInputEvent(-1);
        
    //On MDD Powermacs, the above function will return a large value when the machine is active (-1?).
    //Here we check for that value and correctly return a 0 idle time.
    if(idleTime >= 18446744000.0) idleTime = 0.0; //18446744073.0

    return (jlong)idleTime*1000;
}