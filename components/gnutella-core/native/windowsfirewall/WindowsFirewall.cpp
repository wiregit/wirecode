
// Include the standard Windows DLL header which we've edited to include the Java headers and more headers
#include "stdafx.h"

// A Windows DLL has a DllMain method
BOOL APIENTRY DllMain(HANDLE hModule, DWORD ul_reason_for_call, LPVOID lpReserved) {

	// Indicate success
	return TRUE;
}

// Takes the JNI environment object, and a Java string
// Safely gets the text from the Java string, copies it into a new CString, and frees the Java string
// Returns the CString
CString GetString(JNIEnv *e, jstring j) {

	// If j is a null pointer, just return a blank string
	if (j == NULL) return CString("");

	// Get a character pointer into the text characters of the Java string
	const char *c = e->GetStringUTFChars(j, FALSE);

	// Make a new Windows CString object with that text
	CString s = c; // The CString constructor copies the characters it reads at c into memory it manages

	// Tell the JNI environment that we're done with the character pointer
	e->ReleaseStringUTFChars(j, c);

	// Return the CString we made
	return s;
}

// Takes the JNI environment object, and a pointer to text, like a CString cast to LPCTSTR
// Makes a new Java string from the text that we can return back to Java
// Returns the jstring object
jstring MakeJavaString(JNIEnv *e, LPCTSTR t) {

	// Have the JNI environment make a new Java string from the given text, and return it
	return e->NewStringUTF(t);
}

// Returns the path of this running program, like "C:\Folder\Program.exe", or blank if error
JNIEXPORT jstring JNICALL Java_com_limegroup_gnutella_util_SystemUtils_getRunningPathNative(JNIEnv *e, jclass c) {
	return MakeJavaString(e, GetRunningPath());
}
CString GetRunningPath() {

	// Ask Windows for our path
	TCHAR bay[MAX_PATH];
	if (!GetModuleFileName(NULL, bay, MAX_PATH)) return("");
	return bay;
}

// Takes the JNI environment and class
// The jobject frame is a AWT Component like a JFrame that is backed by a real Windows window
// bin is the path to the folder that has the file "jawt.dll", like "C:\Program Files\Java\jre1.5.0_05\bin"
// icon is the path to a Windows .ico file on the disk
// Gets the window handle, and uses it to set the icon.
// Returns blank on success, or a text message about what didn't work
// Do not call this function repeatedly, it creates two icon resources for each call
JNIEXPORT jstring JNICALL Java_com_limegroup_gnutella_util_SystemUtils_setWindowIconNative(JNIEnv *e, jclass c, jobject frame, jstring bin, jstring icon) {
	return MakeJavaString(e, SetWindowIcon(e, c, frame, GetString(e, bin), GetString(e, icon)));
}
CString SetWindowIcon(JNIEnv *e, jclass c, jobject frame, LPCTSTR bin, LPCTSTR icon) {

	// Make variables for the window handle we'll get, and the message we'll return
	HWND handle = NULL;
	CString message = "Start of method";

	// Make sure neither of the paths are blank
	if (bin == CString("") || icon == CString("")) return CString("Blank paths");

	// Make a JAWT structure that will tell Java we're using Java 1.4
	JAWT awt;
	awt.version = JAWT_VERSION_1_4;

	// Load jawt.dll into our process space
	CString path = CString(bin) + CString("\\jawt.dll"); // Compose the complete path to the DLL, like "C:\Program Files\Java\jre1.5.0_05\bin\jawt.dll"
	HMODULE module = GetModuleHandle(path); // The DLL may already by in our process space
	if (!module) module = LoadLibrary(path);
	if (module) {
		message = "Got module";

		// Get a function pointer to JAWT_GetAWT() in the DLL
		JawtGetAwtSignature JawtGetAwt = (JawtGetAwtSignature)GetProcAddress(module, "_JAWT_GetAWT@8");
		if (JawtGetAwt) {
			message = "Got signature";

			// Access Java's Active Widget Toolkit
			jboolean result = JawtGetAwt(e, &awt);
			if (result != JNI_FALSE) {
				message = "Got AWT";

				// Get the drawing surface
				JAWT_DrawingSurface *surface = awt.GetDrawingSurface(e, frame);
				if (surface) {
					message = "Got surface";

					// Lock the drawing surface
					jint lock = surface->Lock(surface);
					if ((lock & JAWT_LOCK_ERROR) == 0) { // If the error bit is not set, keep going
						message = "Locked surface";

						// Get the drawing surface information
						JAWT_DrawingSurfaceInfo *info = surface->GetDrawingSurfaceInfo(surface);
						if (info) {
							message = "Got surface information";

							// Get the Windows-specific drawing surface information
							JAWT_Win32DrawingSurfaceInfo *win = (JAWT_Win32DrawingSurfaceInfo*)info->platformInfo;
							if (win) {
								message = "Got platform-specific surface information";

								// Get the window handle
								handle = win->hwnd;
							}
						}

						// Unlock the drawing surface
						surface->Unlock(surface);
					}

					// Free the drawing surface
					awt.FreeDrawingSurface(surface);
				}
			}
		}
	}

	// Make sure we were able to get the handle
	if (!handle) return message;

	// Open the .ico file, getting handles to the large and small icons inside it
	HICON bigicon   = (HICON)LoadImage(NULL, icon, IMAGE_ICON, 32, 32, LR_LOADFROMFILE);
	HICON smallicon = (HICON)LoadImage(NULL, icon, IMAGE_ICON, 16, 16, LR_LOADFROMFILE);
	if (!bigicon || !smallicon) return CString("Unable to open icon file");

	/*
	 * It is important that you do not call this function repeatedly.
	 * Each LoadImage call above creates a HICON that leads to an icon resource.
	 * Windows graphics resources like icons take up a lot of memory.
	 * A Windows program should free icons with a call to DestroyIcon(HICON).
	 * We can't free them now, because they are on display in our window.
	 * When the Windows process exits, Windows will free the two icons.
	 */

	// Set both sizes of the window's icon
	SendMessage(handle, WM_SETICON, ICON_BIG,   (LPARAM)bigicon);
	SendMessage(handle, WM_SETICON, ICON_SMALL, (LPARAM)smallicon);
	return CString(""); // Return blank on success
}

// Determines if this copy of Windows has Windows Firewall
// Returns true if it does, false if not or there was an error
JNIEXPORT jboolean JNICALL Java_com_limegroup_gnutella_util_SystemUtils_firewallPresentNative(JNIEnv *e, jclass c) {
	return WindowsFirewallPresent();
}
bool WindowsFirewallPresent() {

	// Make a Windows Firewall object and have it access the COM interfaces of Windows Firewall
	CWindowsFirewall firewall;
	if (!firewall.Access()) return false;
	return true;
}

// Determines if Windows Firewall is off or on
// Returns true if the firewall is on, false if it's off or there was an error
JNIEXPORT jboolean JNICALL Java_com_limegroup_gnutella_util_SystemUtils_firewallEnabledNative(JNIEnv *e, jclass c) {
	return WindowsFirewallEnabled();
}
bool WindowsFirewallEnabled() {

	// Make a Windows Firewall object and have it access the COM interfaces of Windows Firewall
	CWindowsFirewall firewall;
	if (!firewall.Access()) return false;

	// Determine if Windows Firewall is off or on
	bool enabled = false;
	if (!firewall.FirewallEnabled(&enabled)) return false;
	return enabled;
}

// Determines if the Exceptions not allowed check box in Windows Firewall is checked
// Returns true if the exceptions not allowed box is checked, false if it's not checked or there was an error
JNIEXPORT jboolean JNICALL Java_com_limegroup_gnutella_util_SystemUtils_firewallExceptionsNotAllowedNative(JNIEnv *e, jclass c) {
	return WindowsFirewallExceptionsNotAllowed();
}
bool WindowsFirewallExceptionsNotAllowed() {

	// Make a Windows Firewall object and have it access the COM interfaces of Windows Firewall
	CWindowsFirewall firewall;
	if (!firewall.Access()) return false;

	// Determine if the Exceptions not allowed box is checked
	bool notallowed = false;
	if (!firewall.ExceptionsNotAllowed(&notallowed)) return false;
	return notallowed;
}

// Takes a program path and file name, like "C:\Folder\Program.exe"
// Determines if it's listed in Windows Firewall
// Returns true if is listed, false if it's not or there was an error
JNIEXPORT jboolean JNICALL Java_com_limegroup_gnutella_util_SystemUtils_firewallIsProgramListedNative(JNIEnv *e, jclass c, jstring j) {
	return WindowsFirewallIsProgramListed(GetString(e, j));
}
bool WindowsFirewallIsProgramListed(LPCTSTR path) {

	// Make a Windows Firewall object and have it access the COM interfaces of Windows Firewall
	CWindowsFirewall firewall;
	if (!firewall.Access()) return false;

	// Determine if the program has a listing on the Exceptions tab
	bool listed = false;
	if (!firewall.IsProgramListed(path, &listed)) return false;
	return listed;
}

// Takes a program path and file name like "C:\Folder\Program.exe"
// Determines if the listing for that program in Windows Firewall is checked or unchecked
// Returns true if it is enabled, false if it's not or there was an error
JNIEXPORT jboolean JNICALL Java_com_limegroup_gnutella_util_SystemUtils_firewallIsProgramEnabledNative(JNIEnv *e, jclass c, jstring j) {
	return WindowsFirewallIsProgramEnabled(GetString(e, j));
}
bool WindowsFirewallIsProgramEnabled(LPCTSTR path) {

	// Make a Windows Firewall object and have it access the COM interfaces of Windows Firewall
	CWindowsFirewall firewall;
	if (!firewall.Access()) return false;

	// Determine if the program has a listing on the Exceptions tab, and that listing is checked
	bool enabled = false;
	if (!firewall.IsProgramEnabled(path, &enabled)) return false;
	return enabled;
}

// Takes a path like "C:\Folder\Program.exe" and a name like "My Program"
// Adds the program's listing in Windows Firewall to make sure it is listed and checked
// Returns false on error
JNIEXPORT jboolean JNICALL Java_com_limegroup_gnutella_util_SystemUtils_firewallAddNative(JNIEnv *e, jclass c, jstring j1, jstring j2) {
	return WindowsFirewallAdd(GetString(e, j1), GetString(e, j2));
}
bool WindowsFirewallAdd(LPCTSTR path, LPCTSTR name) {

	// Make a Windows Firewall object and have it access the COM interfaces of Windows Firewall
	CWindowsFirewall firewall;
	if (!firewall.Access()) return false;

	// Add the program's listing
	if (!firewall.AddProgram(path, name)) return false;
	return true;
}

// Takes a path and file name like "C:\Folder\Program.exe"
// Removes the program's listing from the Windows Firewall exceptions list
// Returns false on error
JNIEXPORT jboolean JNICALL Java_com_limegroup_gnutella_util_SystemUtils_firewallRemoveNative(JNIEnv *e, jclass c, jstring j) {
	return WindowsFirewallRemove(GetString(e, j));
}
bool WindowsFirewallRemove(LPCTSTR path) {

	// Make a Windows Firewall object and have it access the COM interfaces of Windows Firewall
	CWindowsFirewall firewall;
	if (!firewall.Access()) return false;

	// Remove the program's listing
	if (!firewall.RemoveProgram(path)) return false;
	return true;
}

// Get access to the COM objects
// Returns true if it works, false if there was an error
bool CWindowsFirewall::Access() {

	// Initialize COM itself so this thread can use it
	HRESULT result = CoInitialize(NULL); // Must be NULL
	if (FAILED(result)) return false;

	// Create an instance of the firewall settings manager
	result = CoCreateInstance(__uuidof(NetFwMgr), NULL, CLSCTX_INPROC_SERVER, __uuidof(INetFwMgr), (void **)&Manager);
	if (FAILED(result) || !Manager) return false;

	// Retrieve the local firewall policy
	result = Manager->get_LocalPolicy(&Policy);
	if (FAILED(result) || !Policy) return false;

	// Retrieve the firewall profile currently in effect
	result = Policy->get_CurrentProfile(&Profile);
	if (FAILED(result) || !Profile) return false;

	// Retrieve the authorized application collection
	result = Profile->get_AuthorizedApplications(&ProgramList);
	if (FAILED(result) || !ProgramList) return false;

	// Everything worked
	return true;
}

// Determines if Windows Firewall is off or on
// Returns true if it works, and writes the answer in enabled
bool CWindowsFirewall::FirewallEnabled(bool *enabled) {

	// Find out if the firewall is enabled
	VARIANT_BOOL v;
	HRESULT result = Profile->get_FirewallEnabled(&v);
	if (FAILED(result)) return false;
	if (v == VARIANT_FALSE) {

		// The Windows Firewall setting is "Off (not recommended)"
		*enabled = false;
		return true;

	} else {

		// The Windows Firewall setting is "On (recommended)"
		*enabled = true;
		return true;
	}
}

// Determines if the Exceptions not allowed check box is checked
// Returns true if it works, and writes the answer in enabled
bool CWindowsFirewall::ExceptionsNotAllowed(bool *notallowed) {

	// Find out if the exceptions box is checked
	VARIANT_BOOL v;
	HRESULT result = Profile->get_ExceptionsNotAllowed(&v);
	if (FAILED(result)) return false;
	if (v == VARIANT_FALSE) {

		// The "Don't allow exceptions" box is checked
		*notallowed = false;
		return true;

	} else {

		// The "Don't allow exceptions" box is not checked
		*notallowed = true;
		return true;
	}
}

// Takes a program path and file name, like "C:\Folder\Program.exe"
// Determines if it's listed in Windows Firewall
// Returns true if it works, and writes the answer in listed
bool CWindowsFirewall::IsProgramListed(LPCTSTR path, bool *listed) {

	// Look for the program in the list
	if (Program) { Program->Release(); Program = NULL; }
	CBstr p(path); // Express the name as a BSTR
	HRESULT result = ProgramList->Item(p.B, &Program); // Try to get the interface for the program with the given name
	if (SUCCEEDED(result)) {

		// The program is in the list
		*listed = true;
		return true;

	// The ProgramList->Item call failed
	} else {

		// The error is not found
		if (result == HRESULT_FROM_WIN32(ERROR_FILE_NOT_FOUND)) {

			// The program is not in the list
			*listed = false;
			return true;

		// Some other error occurred
		} else {

			// Report it
			return false;
		}
	}
}

// Takes a program path and file name like "C:\Folder\Program.exe"
// Determines if the listing for that program in Windows Firewall is checked or unchecked
// Returns true if it works, and writes the answer in enabled
bool CWindowsFirewall::IsProgramEnabled(LPCTSTR path, bool *enabled) {

	// First, make sure the program is listed
	bool listed;
	if (!IsProgramListed(path, &listed)) return false; // This sets the Program interface we can use here
	if (!listed) return false; // The program isn't in the list at all

	// Find out if the program is enabled
	VARIANT_BOOL v;
	HRESULT result = Program->get_Enabled(&v);
	if (FAILED(result)) return false;
	if (v == VARIANT_FALSE) {

		// The program is on the list, but the checkbox next to it is cleared
		*enabled = false;
		return true;

	} else {

		// The program is on the list and the checkbox is checked
		*enabled = true;
		return true;
	}
}

// Takes a path and file name like "C:\Folder\Program.exe" and a name like "My Program"
// Lists and checks the program on Windows Firewall, so now it can listed on a socket without a warning popping up
// Returns false on error
bool CWindowsFirewall::AddProgram(LPCTSTR path, LPCTSTR name) {

	// Create an instance of an authorized application, we'll use this to add our new application
	if (Program) { Program->Release(); Program = NULL; }
	HRESULT result = CoCreateInstance(__uuidof(NetFwAuthorizedApplication), NULL, CLSCTX_INPROC_SERVER, __uuidof(INetFwAuthorizedApplication), (void **)&Program);
	if (FAILED(result)) return false;

	// Set the text
	CBstr p(path);                                   // Express the text as BSTRs
	result = Program->put_ProcessImageFileName(p.B); // Set the process image file name
	if (FAILED(result)) return false;
	CBstr n(name);
	result = Program->put_Name(n.B);                 // Set the program name
	if (FAILED(result)) return false;

	// Get the program on the Windows Firewall exceptions list
	result = ProgramList->Add(Program); // Add the application to the collection
	if (FAILED(result)) return false;
	return true;
}

// Takes a program path and file name like "C:\Folder\Program.exe"
// Checks the checkbox next to its listing in Windows Firewall
// Returns false on error
bool CWindowsFirewall::EnableProgram(LPCTSTR path) {

	// First, make sure the program is listed
	bool listed;
	if (!IsProgramListed(path, &listed)) return false; // This sets the Program interface we can use here
	if (!listed) return false; // The program isn't on the list at all

	// Check the box next to the program
	VARIANT_BOOL v = true;
	HRESULT result = Program->put_Enabled(v);
	if (FAILED(result)) return false;
	return true;
}

// Takes a path like "C:\Folder\Program.exe"
// Removes the program from Windows Firewall
// Returns false on error
bool CWindowsFirewall::RemoveProgram(LPCTSTR path) {

	// Remove the program from the Windows Firewall exceptions list
	CBstr p(path); // Express the text as a BSTR
	HRESULT result = ProgramList->Remove(p.B);
	if (FAILED(result)) return false;
	return true;
}
