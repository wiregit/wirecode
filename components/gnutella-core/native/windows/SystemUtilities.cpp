/**
 * Handles various system utility functions that require native code.
 */

#include "com_limegroup_gnutella_util_SystemUtils.h"
#include "windows.h"

extern "C"
JNIEXPORT jlong JNICALL Java_com_limegroup_gnutella_util_SystemUtils_idleTime
  (JNIEnv *env, jclass clazz) {
    
    LASTINPUTINFO lpi;
    lpi.cbSize = sizeof(lpi);
    GetLastInputInfo(&lpi);
    DWORD dwStart = GetTickCount();
    return (jlong)(dwStart - lpi.dwTime);
}
