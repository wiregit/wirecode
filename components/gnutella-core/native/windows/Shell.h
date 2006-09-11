
// Microsoft Visual Studio compiles this Windows native code into SystemUtilities.dll
// LimeWire uses these functions from the class com.limegroup.gnutella.util.SystemUtils

// Include Windows shell headers
#include <io.h>       // Unix-style file functions, like _chmod
#include <sys/stat.h> // Options for Unix-style file functions, like _S_IWRITE
#include <shlobj.h>   // Special folder names
#include <shellapi.h> // Windows shell, including the Recycle Bin

// Define types to match the signatures of functions we'll call in DLLs we load
typedef BOOL (CALLBACK *GetLastInputInfoSignature)(PLASTINPUTINFO);

// Headers generated by Java for functions Java code can call
#ifndef _Included_com_limegroup_gnutella_util_SystemUtils_Shell
#define _Included_com_limegroup_gnutella_util_SystemUtils_Shell
#ifdef __cplusplus
extern "C" {
#endif

	// Functions in Shell.cpp
	JNIEXPORT jstring JNICALL Java_com_limegroup_gnutella_util_SystemUtils_getRunningPathNative(JNIEnv *e, jclass c);
	JNIEXPORT jstring JNICALL Java_com_limegroup_gnutella_util_SystemUtils_getSpecialPathNative(JNIEnv *e, jclass c, jstring name);
	JNIEXPORT void JNICALL Java_com_limegroup_gnutella_util_SystemUtils_openURLNative(JNIEnv *e, jclass c, jstring url);
	JNIEXPORT void JNICALL Java_com_limegroup_gnutella_util_SystemUtils_openFileNative(JNIEnv *e, jclass c, jstring path);
	JNIEXPORT jboolean JNICALL Java_com_limegroup_gnutella_util_SystemUtils_recycleNative(JNIEnv *e, jclass c, jstring path);
	JNIEXPORT jint JNICALL Java_com_limegroup_gnutella_util_SystemUtils_setFileWriteable(JNIEnv *e, jclass c, jstring path);
	JNIEXPORT jlong JNICALL Java_com_limegroup_gnutella_util_SystemUtils_idleTime(JNIEnv *e, jclass c);
	JNIEXPORT jstring JNICALL Java_com_limegroup_gnutella_util_SystemUtils_setWindowIconNative(JNIEnv *e, jclass c, jobject frame, jstring bin, jstring icon);

#ifdef __cplusplus
}
#endif
#endif

// Functions in Shell.cpp
CString GetRunningPath();
CString GetSpecialPath(LPCTSTR name);
void Run(LPCTSTR path);
bool Recycle(LPCTSTR path);
int SetFileWritable(LPCTSTR path);
DWORD GetIdleTime();
CString SetWindowIcon(JNIEnv *e, jclass c, jobject frame, LPCTSTR bin, LPCTSTR icon);
void GetIcons(LPCTSTR icon);
