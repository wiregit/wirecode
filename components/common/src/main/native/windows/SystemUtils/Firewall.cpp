
// Microsoft Visual Studio compiles this Windows native code into SystemUtilities.dll
// LimeWire uses these functions from the class com.limegroup.gnutella.util.SystemUtils

// Include the standard Windows DLL header and more headers
#include "stdafx.h"
#include "SystemUtilities.h"
#include "Firewall.h"

// Determines if this copy of Windows has Windows Firewall
// Returns true if it does, false if not or there was an error
JNIEXPORT jboolean JNICALL Java_org_limewire_util_SystemUtils_firewallPresentNative(JNIEnv *e, jclass c) {
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
JNIEXPORT jboolean JNICALL Java_org_limewire_util_SystemUtils_firewallEnabledNative(JNIEnv *e, jclass c) {
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
JNIEXPORT jboolean JNICALL Java_org_limewire_util_SystemUtils_firewallExceptionsNotAllowedNative(JNIEnv *e, jclass c) {
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
JNIEXPORT jboolean JNICALL Java_org_limewire_util_SystemUtils_firewallIsProgramListedNative(JNIEnv *e, jclass c, jstring path) {
	return WindowsFirewallIsProgramListed(GetJavaString(e, path));
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
JNIEXPORT jboolean JNICALL Java_org_limewire_util_SystemUtils_firewallIsProgramEnabledNative(JNIEnv *e, jclass c, jstring path) {
	return WindowsFirewallIsProgramEnabled(GetJavaString(e, path));
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
JNIEXPORT jboolean JNICALL Java_org_limewire_util_SystemUtils_firewallAddNative(JNIEnv *e, jclass c, jstring path, jstring name) {
	return WindowsFirewallAdd(GetJavaString(e, path), GetJavaString(e, name));
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
JNIEXPORT jboolean JNICALL Java_org_limewire_util_SystemUtils_firewallRemoveNative(JNIEnv *e, jclass c, jstring path) {
	return WindowsFirewallRemove(GetJavaString(e, path));
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
