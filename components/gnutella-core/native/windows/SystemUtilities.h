
// Microsoft Visual Studio compiles this Windows native code into SystemUtilities.dll
// LimeWire uses these functions from the class com.limegroup.gnutella.util.SystemUtils

// Define types to match the signatures of functions we'll call in DLLs we load
typedef jboolean(JNICALL *JawtGetAwtSignature)(JNIEnv*, JAWT*);

// The program's single CSystemUtilities object holds window and icon handles
class CSystemUtilities {
public:

	// Program
	HINSTANCE Instance;

	// Window
	HWND Window;

	// Icons
	HICON Icon, SmallIcon;

	// Java objects
	JNIEnv *Environment;
	jclass Class;
	jmethodID Method;

	// Constructor
	CSystemUtilities() {

		// Set handles to NULL
		Instance = NULL;
		Window = NULL;
		Icon = SmallIcon = NULL;
		Environment = NULL;
		Class = NULL;
		Method = 0;
	}
};

// Functions in SystemUtilities.cpp
CString GetJavaString(JNIEnv *e, jstring j);
jstring MakeJavaString(JNIEnv *e, LPCTSTR t);
HWND GetJavaWindowHandle(JNIEnv *e, jclass c, jobject frame, LPCTSTR bin, CString *message);
