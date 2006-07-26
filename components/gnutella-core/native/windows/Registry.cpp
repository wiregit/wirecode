
// Microsoft Visual Studio compiles this Windows native code into SystemUtilities.dll
// LimeWire uses these functions from the class com.limegroup.gnutella.util.SystemUtils

// Include the standard Windows DLL header which we've edited to include the Java headers and more headers
#include "stdafx.h"

// Takes a root key handle name, a key path, and a registry variable name
// Gets the information from the registry
// Returns the number, or 0 if not found or any error
JNIEXPORT jint JNICALL Java_com_limegroup_gnutella_util_SystemUtils_registryReadNumberNative(JNIEnv *e, jclass c, jstring root, jstring path, jstring name) {
	return RegistryReadNumber(GetString(e, root), GetString(e, path), GetString(e, name));
}
int RegistryReadNumber(LPCTSTR root, LPCTSTR path, LPCTSTR name) {

	// Open the key
	CRegistry registry;
	if (!registry.Open(root, path, KEY_READ)) return 0;

	// Read the number value
	DWORD d;
	DWORD size = sizeof(DWORD);
	int result = RegQueryValueEx(
		registry.key, // Handle to an open key
		name,         // Name of the value to read
		0,
		NULL,
		(LPBYTE)&d,   // Data buffer
		&size);       // Size of data buffer
	if (result != ERROR_SUCCESS) return 0;

	// Return the number
	return d;
}

// Takes a root key handle name, a key path, and a registry variable name
// Gets the information from the registry
// Returns the text, blank if not found or any error
JNIEXPORT jstring JNICALL Java_com_limegroup_gnutella_util_SystemUtils_registryReadTextNative(JNIEnv *e, jclass c, jstring root, jstring path, jstring name) {
	return MakeJavaString(e, RegistryReadText(GetString(e, root), GetString(e, path), GetString(e, name)));
}
CString RegistryReadText(LPCTSTR root, LPCTSTR path, LPCTSTR name) {

	// Open the key
	CRegistry registry;
	if (!registry.Open(root, path, KEY_READ)) return "";

	// Get the size required
	DWORD size;
	int result = RegQueryValueEx(
		registry.key, // Handle to an open key
		name,         // Name of the value to read
		0,
		NULL,
		NULL,         // No data buffer, we're requesting the size
		&size);       // The function will write the required size here
	if (result != ERROR_SUCCESS) return "";

	// Open a string
	CString buffer_string;
	LPTSTR buffer_write = buffer_string.GetBuffer(size);

	// Read the binary data
	result = RegQueryValueEx(
		registry.key,         // Handle to an open key
		name,                 // Name of the value to read
		0,
		NULL,
		(LPBYTE)buffer_write, // Data buffer
		&size);               // Size of data buffer
	buffer_string.ReleaseBuffer();
	if (result != ERROR_SUCCESS) return "";

	// Return the string
	return buffer_string;
}

// Takes a root key handle name, a key path, a registry variable name, and an integer
// Stores the information in the registry
// Returns false on error
JNIEXPORT jboolean JNICALL Java_com_limegroup_gnutella_util_SystemUtils_registryWriteNumberNative(JNIEnv *e, jclass c, jstring root, jstring path, jstring name, jint value) {
	return RegistryWriteNumber(GetString(e, root), GetString(e, path), GetString(e, name), value);
}
bool RegistryWriteNumber(LPCTSTR root, LPCTSTR path, LPCTSTR name, int value) {

	// Open the key
	CRegistry registry;
	if (!registry.Open(root, path, KEY_ALL_ACCESS)) return false;

	// Set or make and set the number value
	int result = RegSetValueEx(
		registry.key,         // Handle to an open key
		name,                 // Name of the value to set or make and set
		0,
		REG_DWORD,            // Variable type is a 32-bit number
		(const BYTE *)&value, // Address of the value data to load
		sizeof(DWORD));       // Size of the value data
	if (result != ERROR_SUCCESS) return false;
	return true;
}

// Takes a root key handle, a key path, a registry variable name, and value text
// Stores the information in the registry
// Returns false on error
JNIEXPORT jboolean JNICALL Java_com_limegroup_gnutella_util_SystemUtils_registryWriteTextNative(JNIEnv *e, jclass c, jstring root, jstring path, jstring name, jstring value) {
	return RegistryWriteText(GetString(e, root), GetString(e, path), GetString(e, name), GetString(e, value));
}
bool RegistryWriteText(LPCTSTR root, LPCTSTR path, LPCTSTR name, LPCTSTR value) {

	// Open the key
	CRegistry registry;
	if (!registry.Open(root, path, KEY_ALL_ACCESS)) return false;

	// Set or make and set the text value
	int result = RegSetValueEx(
		registry.key,        // Handle to an open key
		name,                // Name of the valeu to set or make and set
		0,
		REG_SZ,              // Variable type is a null-terminated string
		(const BYTE *)value, // Address of the value data to load
		lstrlen(value) + 1); // Size of the value data, add 1 to write the null terminator
	if (result != ERROR_SUCCESS) return false;
	return true;
}

// Takes a root key handle, the path to a key that has no subkeys, and a registry variable name, or blank to delete the key
// Deletes the registry variable or key from the registry
// Returns false on error
JNIEXPORT jboolean JNICALL Java_com_limegroup_gnutella_util_SystemUtils_registryDeleteNative(JNIEnv *e, jclass c, jstring root, jstring path, jstring name) {
	return RegistryDelete(GetString(e, root), GetString(e, path), GetString(e, name));
}
bool RegistryDelete(LPCTSTR root, LPCTSTR path, LPCTSTR name) {

	// Delete the registry key variable
	int result;
	if (CString(name) != "") {

		// Open the key
		CRegistry registry;
		if (!registry.Open(root, path, KEY_ALL_ACCESS)) return false;

		// Delete the variable
		result = RegDeleteValue(registry.key, name);
		if (result != ERROR_SUCCESS && result != ERROR_FILE_NOT_FOUND) return false;
		return true;

	// Delete the registry key
	} else {

		// Look at the text name to get the matching registry root key handle value
		HKEY key = RegistryName(root);
		if (!key) return false;

		// Delete the key
		result = RegDeleteKey(key, path);
		if (result != ERROR_SUCCESS && result != ERROR_FILE_NOT_FOUND) return false;
		return true;
	}
}

// Takes a root key handle and a key path, and the desired level of access
// Opens or creates and opens the key with full access
// Returns false on error
bool CRegistry::Open(LPCTSTR root, LPCTSTR path, DWORD access) {

	// Look at the text name to get the matching registry root key handle value
	HKEY key = RegistryName(root);
	if (!key) return false;

	// Open or create and open the key
	HKEY k;
	DWORD info;
	int result = RegCreateKeyEx(
		key,                     // Handle to open root key
		path,                    // Subkey name
		0,
		"",
		REG_OPTION_NON_VOLATILE, // Save information in the registry file
		access,                  // Given access flags
		NULL,
		&k,                      // The opened or created key handle is put here
		&info);                  // Tells if the key was opened or created and opened
	if (result != ERROR_SUCCESS) return false;

	// Save the open key in this CRegistry object
	key = k;
	return true;
}

// Takes a text name of a registry root key, like "HKEY_LOCAL_MACHINE"
// Returns the HKEY value Windows defines for it, or NULL if not found
HKEY RegistryName(LPCTSTR name) {

	// Look at the text name to return the matching registry root key handle value
	CString s = name;
	if      (s == "HKEY_CLASSES_ROOT")   return HKEY_CLASSES_ROOT;
	else if (s == "HKEY_CURRENT_CONFIG") return HKEY_CURRENT_CONFIG;
	else if (s == "HKEY_CURRENT_USER")   return HKEY_CURRENT_USER;
	else if (s == "HKEY_LOCAL_MACHINE")  return HKEY_LOCAL_MACHINE;
	else if (s == "HKEY_USERS")          return HKEY_USERS;
	else return NULL;
}
