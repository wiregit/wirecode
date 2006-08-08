
// Microsoft Visual Studio compiles this Windows native code into SystemUtilities.dll
// LimeWire uses these functions from the class com.limegroup.gnutella.util.SystemUtils

// Define types to match the signatures of functions we'll call in DLLs we load
typedef jboolean(JNICALL *JawtGetAwtSignature)(JNIEnv*, JAWT*);

// Functions in SystemUtilities.cpp
CString GetJavaString(JNIEnv *e, jstring j);
jstring MakeJavaString(JNIEnv *e, LPCTSTR t);
HWND GetJavaWindowHandle(JNIEnv *e, jclass c, jobject frame, LPCTSTR bin, CString *message);
