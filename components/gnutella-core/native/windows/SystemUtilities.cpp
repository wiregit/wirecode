/**
 * Handles various system utility functions that require native code.
 */

#include "com_limegroup_gnutella_util_SystemUtils.h"
#include "windows.h"
#include <sys/stat.h>
#include <io.h>

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

/**
 * Sets a file to be writeable.  Available on all windows platforms.
 */
extern "C"
JNIEXPORT jint JNICALL Java_com_limegroup_gnutella_util_SystemUtils_setFileWriteable
  (JNIEnv *env, jclass clazz, jstring fileName) {
    const char* cFileName;
    cFileName = env->GetStringUTFChars(fileName, NULL);
    int retVal = _chmod(cFileName, _S_IWRITE);
    // free the memory for the string
    env->ReleaseStringUTFChars(fileName, cFileName);
    return retVal;
}
