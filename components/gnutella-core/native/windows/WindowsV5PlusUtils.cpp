#include <jni.h>
#include <windows.h>

/**
 * Retrieves the idle time.  Available only on Windows2000+.
 */
extern "C"
JNIEXPORT jlong JNICALL Java_com_limegroup_gnutella_util_SystemUtils_idleTime
  (JNIEnv *env, jclass clazz) {
    
    LASTINPUTINFO lpi;
    lpi.cbSize = sizeof(lpi);
    GetLastInputInfo(&lpi);
    DWORD dwStart = GetTickCount();
    return (jlong)(dwStart - lpi.dwTime);
}
