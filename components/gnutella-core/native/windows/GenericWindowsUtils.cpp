#include <jni.h>
#include <sys/stat.h>
#include <io.h>


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
