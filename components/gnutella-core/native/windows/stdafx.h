
// Exclude rarely-used types from the Windows headers
#define WIN32_LEAN_AND_MEAN

// Access Win32 API calls availiable in Windows 2000
#define _WIN32_WINNT 0x0500

// Include Java and Windows headers
#include <jni.h>      // Java types for native code like jstring and JNIEnv
#include <jawt_md.h>  // Access Java's Active Widget Toolkit
#include <windows.h>  // Win32 types, like DWORD
#include <io.h>       // Unix-style file functions, like _chmod
#include <sys/stat.h> // Options for Unix-style file functions, like _S_IWRITE
#include <atlstr.h>   // CString, the Windows MFC and ATL string type
#include <shellapi.h> // Windows shell, including the Recycle Bin
#include <netfw.h>    // Windows Firewall, only available in Windows XP Service Pack 2 or later

// Headers generated by Java
#ifndef _Included_com_limegroup_gnutella_util_SystemUtils
#define _Included_com_limegroup_gnutella_util_SystemUtils
#ifdef __cplusplus
extern "C" {
#endif
JNIEXPORT jstring JNICALL Java_com_limegroup_gnutella_util_SystemUtils_getRunningPathNative(JNIEnv *e, jclass c);
JNIEXPORT jstring JNICALL Java_com_limegroup_gnutella_util_SystemUtils_setWindowIconNative(JNIEnv *e, jclass c, jobject frame, jstring bin, jstring icon);
JNIEXPORT jboolean JNICALL Java_com_limegroup_gnutella_util_SystemUtils_firewallPresentNative(JNIEnv *e, jclass c);
JNIEXPORT jboolean JNICALL Java_com_limegroup_gnutella_util_SystemUtils_firewallEnabledNative(JNIEnv *e, jclass c);
JNIEXPORT jboolean JNICALL Java_com_limegroup_gnutella_util_SystemUtils_firewallExceptionsNotAllowedNative(JNIEnv *e, jclass c);
JNIEXPORT jboolean JNICALL Java_com_limegroup_gnutella_util_SystemUtils_firewallIsProgramListedNative(JNIEnv *e, jclass c, jstring j);
JNIEXPORT jboolean JNICALL Java_com_limegroup_gnutella_util_SystemUtils_firewallIsProgramEnabledNative(JNIEnv *e, jclass c, jstring j);
JNIEXPORT jboolean JNICALL Java_com_limegroup_gnutella_util_SystemUtils_firewallAddNative(JNIEnv *e, jclass c, jstring j1, jstring j2);
JNIEXPORT jboolean JNICALL Java_com_limegroup_gnutella_util_SystemUtils_firewallRemoveNative(JNIEnv *e, jclass c, jstring j);
#ifdef __cplusplus
}
#endif
#endif

// Define types to match the signatures of functions we'll call in DLLs we load
typedef jboolean(JNICALL *JawtGetAwtSignature)(JNIEnv*, JAWT*);
typedef BOOL (CALLBACK *GetLastInputInfoSignature)(PLASTINPUTINFO);

// Wraps a registry key, taking care of closing it
class CRegistry {
public:

	// The handle to the registry key
	HKEY key;

	// Open a registry key and store its handle in this object
	bool Open(HKEY root, LPCTSTR path, DWORD access);

	// Make a new local CRegistry object, and delete it when it goes out of scope
	CRegistry() { key = NULL; }
	~CRegistry() { if (key) RegCloseKey(key); key = NULL; }
};

// Wraps a BSTR, a COM string, taking care of memory allocation
class CBstr {
public:

	// The BSTR
	BSTR B;

	// Make a new CBstr object
	CBstr()          { B = NULL; }         // With no BSTR allocated
	CBstr(LPCTSTR t) { B = NULL; Set(t); } // From the given text
	~CBstr()         { Clear(); }          // It frees its memory when you delete it

	// Use AllocSysString and SysFreeString to allocate and free the BSTR
	void Set(CString s) { Clear(); B = s.AllocSysString(); }
	void Clear() { if (B) { SysFreeString(B); B = NULL; } }
};

// Add an application to the Windows Firewall exceptions list
class CWindowsFirewall {
public:

	// COM interfaces
	INetFwMgr*                    Manager;
	INetFwPolicy*                 Policy;
	INetFwProfile*                Profile;
	INetFwAuthorizedApplications* ProgramList;
	INetFwAuthorizedApplication*  Program;

	// Make a new CWindowsFirewall object
	CWindowsFirewall() {

		// Set the COM interface pointers to NULL so we'll know if we've initialized them
		Manager     = NULL;
		Policy      = NULL;
		Profile     = NULL;
		ProgramList = NULL;
		Program     = NULL;
	}

	// Delete the CWindowsFirewall object
	~CWindowsFirewall() {

		// Release the COM interfaces that we got access to
		if (Program)     { Program->Release();     Program     = NULL; } // Release them in reverse order
		if (ProgramList) { ProgramList->Release(); ProgramList = NULL; }
		if (Profile)     { Profile->Release();     Profile     = NULL; }
		if (Policy)      { Policy->Release();      Policy      = NULL; }
		if (Manager)     { Manager->Release();     Manager     = NULL; }
	}

	// Methods
	bool Access();
	bool FirewallEnabled(bool *enabled);
	bool ExceptionsNotAllowed(bool *notallowed);
	bool IsProgramListed(LPCTSTR path, bool *listed);
	bool IsProgramEnabled(LPCTSTR path, bool *enabled);
	bool AddProgram(LPCTSTR path, LPCTSTR name);
	bool EnableProgram(LPCTSTR path);
	bool RemoveProgram(LPCTSTR path);
};

// Functions in SystemUtilities.cpp
CString GetString(JNIEnv *e, jstring j);
jstring MakeJavaString(JNIEnv *e, LPCTSTR t);

// Functions in Shell.cpp
CString GetRunningPath();
void Run(LPCTSTR path);
bool Recycle(LPCTSTR path);
int SetFileWritable(LPCTSTR path);
DWORD GetIdleTime();
CString SetWindowIcon(JNIEnv *e, jclass c, jobject frame, LPCTSTR bin, LPCTSTR icon);

// Functions in Registry.cpp
HKEY RegistryName(LPCTSTR name);
bool RegistryReadNumber(HKEY root, LPCTSTR path, LPCTSTR name, int *i);
bool RegistryReadText(HKEY root, LPCTSTR path, LPCTSTR name, CString *s);
bool RegistryWriteNumber(HKEY root, LPCTSTR path, LPCTSTR name, int i);
bool RegistryWriteText(HKEY root, LPCTSTR path, LPCTSTR name, LPCTSTR t);
bool RegistryDelete(HKEY root, LPCTSTR path, LPCTSTR name);

// Functions in Firewall.cpp
bool WindowsFirewallPresent();
bool WindowsFirewallEnabled();
bool WindowsFirewallExceptionsNotAllowed();
bool WindowsFirewallIsProgramListed(LPCTSTR path);
bool WindowsFirewallIsProgramEnabled(LPCTSTR path);
bool WindowsFirewallAdd(LPCTSTR path, LPCTSTR name);
bool WindowsFirewallRemove(LPCTSTR path);
