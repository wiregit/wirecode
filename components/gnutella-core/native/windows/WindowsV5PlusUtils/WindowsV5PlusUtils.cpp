// WindowsV5PlusUtils.cpp : Defines the entry point for the DLL application.
//

//*****************************************************************************
#define NEW_CODE		//	Turns on the new WinXP ICF firewall detection code
//*****************************************************************************

#include <jni.h>
#include <assert.h>

#define _WIN32_WINNT	0x0500		//	Required windows version: Win2K or better
#define MIN_ICF_VER		0x00050102	//	WinXP SP2 (5.1.2) 
#include <windows.h>

/**
 * Retrieves the idle time.  Available only on Windows2000+.
 */
extern "C"
JNIEXPORT jlong JNICALL Java_com_limegroup_gnutella_util_SystemUtils_idleTime(
	JNIEnv		*	env, 
	jclass			clazz ) 
{
    
    LASTINPUTINFO lpi;
    lpi.cbSize = sizeof(lpi);
    GetLastInputInfo(&lpi);
    DWORD dwStart = GetTickCount();
    return (jlong)(dwStart - lpi.dwTime);
}

//*****************************************************************************
//	Windows XP SP2 - ICF (COM) code
//*****************************************************************************
//	NOTE:  netfw.h is available from the XP SP2 PSDK.  This *WAS* avaiable at:
//	http://www.microsoft.com/msdownload/platformsdk/sdkupdate/XPSP2FULLInstall.htm
//	and may still be.
//*****************************************************************************
#ifdef NEW_CODE

#ifndef __WINDOWSV5PLUSUTILS__
#include "WindowsV5PlusUtils.h"
#endif

#include <netfw.h>
#include <objbase.h>
#include <oleauto.h>
#include <crtdbg.h>

#define APP_NAME L"%ProgramFiles%\\LimeWire\\LimeWire.exe"

//	Initialization code from:
//	http://msdn.microsoft.com/library/default.asp?url=/library/en-us/ics/ics/using_windows_firewall.asp

//*****************************************************************************

HRESULT WindowsFirewallInitialize(OUT INetFwProfile** fwProfile, unsigned short *nReturn)
{
    HRESULT hr = S_OK;
    INetFwMgr* fwMgr = NULL;
    INetFwPolicy* fwPolicy = NULL;

    _ASSERT(fwProfile != NULL);

    *fwProfile = NULL;

    // Create an instance of the firewall settings manager.
    hr = CoCreateInstance(
            __uuidof(NetFwMgr),
            NULL,
            CLSCTX_INPROC_SERVER,
            __uuidof(INetFwMgr),
            (void**)&fwMgr
            );
    if (FAILED(hr))
    {
		*nReturn|=ICF::eErrCoCreateInstance;
        //printf("CoCreateInstance failed: 0x%08lx\n", hr);
        goto error;
    }

    // Retrieve the local firewall policy.
    hr = fwMgr->get_LocalPolicy(&fwPolicy);
    if (FAILED(hr))
    {
		*nReturn|=ICF::eErrGetLocalPolicy;
        //printf("get_LocalPolicy failed: 0x%08lx\n", hr);
        goto error;
    }

    // Retrieve the firewall profile currently in effect.
    hr = fwPolicy->get_CurrentProfile(fwProfile);
    if (FAILED(hr))
    {
		*nReturn|=ICF::eErrGetCurProfile;
        //printf("get_CurrentProfile failed: 0x%08lx\n", hr);
        goto error;
    }

error:

    // Release the local firewall policy.
    if (fwPolicy != NULL)
    {
        fwPolicy->Release();
    }

    // Release the firewall settings manager.
    if (fwMgr != NULL)
    {
        fwMgr->Release();
    }

    return hr;
}

//-----------------------------------------------------------------------------

void WindowsFirewallCleanup(IN INetFwProfile* fwProfile)
{
    // Release the firewall profile.
    if (fwProfile != NULL)
    {
        fwProfile->Release();
    }
}

//-----------------------------------------------------------------------------

HRESULT WindowsFirewallIsOn(IN INetFwProfile* fwProfile, OUT BOOL* fwOn, unsigned short *nReturn)
{
    HRESULT hr = S_OK;
    VARIANT_BOOL fwEnabled;

    _ASSERT(fwProfile != NULL);
    _ASSERT(fwOn != NULL);

    *fwOn = FALSE;

    // Get the current state of the firewall.
    hr = fwProfile->get_FirewallEnabled(&fwEnabled);
    if (FAILED(hr))
    {
		*nReturn|=ICF::eErrGetFwEnabled;
        //printf("get_FirewallEnabled failed: 0x%08lx\n", hr);
        goto error;
    }

    // Check to see if the firewall is on.
    if (fwEnabled != VARIANT_FALSE)
    {
        *fwOn = TRUE;
		*nReturn|= ICF::eFirewallEnabled;
        //printf("The firewall is on.\n");
    }
    else
    {
		*nReturn&=~ICF::eFirewallEnabled;
        //printf("The firewall is off.\n");
    }

error:

    return hr;
}

//-----------------------------------------------------------------------------

HRESULT WindowsFirewallTurnOn(IN INetFwProfile* fwProfile,unsigned short *nReturn)
{
    HRESULT hr = S_OK;
    BOOL fwOn;

    _ASSERT(fwProfile != NULL);

    // Check to see if the firewall is off.
    hr = WindowsFirewallIsOn(fwProfile, &fwOn, nReturn);
    if (FAILED(hr))
    {
        printf("WindowsFirewallIsOn failed: 0x%08lx\n", hr);
        goto error;
    }

    // If it is, turn it on.
    if (!fwOn)
    {
        // Turn the firewall on.
        hr = fwProfile->put_FirewallEnabled(VARIANT_TRUE);
        if (FAILED(hr))
        {
            printf("put_FirewallEnabled failed: 0x%08lx\n", hr);
            goto error;
        }

        printf("The firewall is now on.\n");
    }

error:

    return hr;
}

//-----------------------------------------------------------------------------

HRESULT WindowsFirewallTurnOff(IN INetFwProfile* fwProfile, unsigned short *nReturn)
{
    HRESULT hr = S_OK;
    BOOL fwOn;

    _ASSERT(fwProfile != NULL);

    // Check to see if the firewall is on.
    hr = WindowsFirewallIsOn(fwProfile, &fwOn, nReturn);
    if (FAILED(hr))
    {
        printf("WindowsFirewallIsOn failed: 0x%08lx\n", hr);
        goto error;
    }

    // If it is, turn it off.
    if (fwOn)
    {
        // Turn the firewall off.
        hr = fwProfile->put_FirewallEnabled(VARIANT_FALSE);
        if (FAILED(hr))
        {
            printf("put_FirewallEnabled failed: 0x%08lx\n", hr);
            goto error;
        }

        printf("The firewall is now off.\n");
    }

error:

    return hr;
}

//-----------------------------------------------------------------------------

HRESULT WindowsFirewallAppIsEnabled(
    IN INetFwProfile* fwProfile,
    IN const wchar_t* fwProcessImageFileName,
    OUT BOOL* fwAppEnabled,
	unsigned short *nReturn )
{
    HRESULT hr = S_OK;
    BSTR fwBstrProcessImageFileName = NULL;
    VARIANT_BOOL fwEnabled;
    INetFwAuthorizedApplication* fwApp = NULL;
    INetFwAuthorizedApplications* fwApps = NULL;

    _ASSERT(fwProfile != NULL);
    _ASSERT(fwProcessImageFileName != NULL);
    _ASSERT(fwAppEnabled != NULL);

    *fwAppEnabled = FALSE;

    // Retrieve the authorized application collection.
    hr = fwProfile->get_AuthorizedApplications(&fwApps);
    if (FAILED(hr))
    {
		*nReturn|=ICF::eErrGetAuthApps;
        //printf("get_AuthorizedApplications failed: 0x%08lx\n", hr);
        goto error;
    }

    // Allocate a BSTR for the process image file name.
    fwBstrProcessImageFileName = SysAllocString(fwProcessImageFileName);
    if (SysStringLen(fwBstrProcessImageFileName) == 0)
    {
        hr = E_OUTOFMEMORY;
		*nReturn|=ICF::eErrAllocation;
        //printf("SysAllocString failed: 0x%08lx\n", hr);
        goto error;
    }

    // Attempt to retrieve the authorized application.
    hr = fwApps->Item(fwBstrProcessImageFileName, &fwApp);
    if (SUCCEEDED(hr))
    {
        // Find out if the authorized application is enabled.
        hr = fwApp->get_Enabled(&fwEnabled);
        if (FAILED(hr))
        {
			*nReturn|=ICF::eErrGetEnabled;
            //printf("get_Enabled failed: 0x%08lx\n", hr);
            goto error;
        }

        if (fwEnabled != VARIANT_FALSE)
        {
            // The authorized application is enabled.
            *fwAppEnabled = TRUE;
			*nReturn|= ICF::eProcessEnabled;
			/*
            printf(
                "Authorized application %lS is enabled in the firewall.\n",
                fwProcessImageFileName
                );
			*/
        }
        else
        {
			*nReturn&=~ICF::eProcessEnabled;
			/*
            printf(
                "Authorized application %lS is disabled in the firewall.\n",
                fwProcessImageFileName
                );
			*/
        }
    }
    else
    {
        // The authorized application was not in the collection.
        hr = S_OK;

		*nReturn&=~ICF::eProcessEnabled;
		/*
        printf(
            "Authorized application %lS is disabled in the firewall.\n",
            fwProcessImageFileName
            );
		*/
    }

error:

    // Free the BSTR.
    SysFreeString(fwBstrProcessImageFileName);

    // Release the authorized application instance.
    if (fwApp != NULL)
    {
        fwApp->Release();
    }

    // Release the authorized application collection.
    if (fwApps != NULL)
    {
        fwApps->Release();
    }

    return hr;
}

//-----------------------------------------------------------------------------

HRESULT WindowsFirewallAddApp(
    IN INetFwProfile* fwProfile,
    IN const wchar_t* fwProcessImageFileName,
    IN const wchar_t* fwName,
	unsigned short *nReturn )
{
    HRESULT hr = S_OK;
    BOOL fwAppEnabled;
    BSTR fwBstrName = NULL;
    BSTR fwBstrProcessImageFileName = NULL;
    INetFwAuthorizedApplication* fwApp = NULL;
    INetFwAuthorizedApplications* fwApps = NULL;

    _ASSERT(fwProfile != NULL);
    _ASSERT(fwProcessImageFileName != NULL);
    _ASSERT(fwName != NULL);

    // First check to see if the application is already authorized.
    hr = WindowsFirewallAppIsEnabled(
		 fwProfile,
		 fwProcessImageFileName,
        &fwAppEnabled,
		 nReturn );

    if (FAILED(hr))
    {
#pragma message( __FILE__ ":DAVE should we add an error code here?" )
        //printf("WindowsFirewallAppIsEnabled failed: 0x%08lx\n", hr);
        goto error;
    }

    // Only add the application if it isn't already authorized.
    if (!fwAppEnabled)
    {
        // Retrieve the authorized application collection.
        hr = fwProfile->get_AuthorizedApplications(&fwApps);
        if (FAILED(hr))
        {
			*nReturn|=ICF::eErrGetAuthApps;
            //printf("get_AuthorizedApplications failed: 0x%08lx\n", hr);
            goto error;
        }

        // Create an instance of an authorized application.
        hr = CoCreateInstance(
                __uuidof(NetFwAuthorizedApplication),
                NULL,
                CLSCTX_INPROC_SERVER,
                __uuidof(INetFwAuthorizedApplication),
                (void**)&fwApp
                );
        if (FAILED(hr))
        {
			*nReturn|=ICF::eErrCoCreateInstance;
            //printf("CoCreateInstance failed: 0x%08lx\n", hr);
            goto error;
        }

        // Allocate a BSTR for the process image file name.
        fwBstrProcessImageFileName = SysAllocString(fwProcessImageFileName);
        if (SysStringLen(fwBstrProcessImageFileName) == 0)
        {
            hr = E_OUTOFMEMORY;
			*nReturn|=ICF::eErrAllocation;
            //printf("SysAllocString failed: 0x%08lx\n", hr);
            goto error;
        }

        // Set the process image file name.
        hr = fwApp->put_ProcessImageFileName(fwBstrProcessImageFileName);
        if (FAILED(hr))
        {
			*nReturn|=ICF::eErrPut;
            //printf("put_ProcessImageFileName failed: 0x%08lx\n", hr);
            goto error;
        }

        // Allocate a BSTR for the application friendly name.
        fwBstrName = SysAllocString(fwName);
        if (SysStringLen(fwBstrName) == 0)
        {
            hr = E_OUTOFMEMORY;
			*nReturn|=ICF::eErrAllocation;
            //printf("SysAllocString failed: 0x%08lx\n", hr);
            goto error;
        }

        // Set the application friendly name.
        hr = fwApp->put_Name(fwBstrName);
        if (FAILED(hr))
        {
			*nReturn|=ICF::eErrPut;
            //printf("put_Name failed: 0x%08lx\n", hr);
            goto error;
        }

        // Add the application to the collection.
        hr = fwApps->Add(fwApp);
        if (FAILED(hr))
        {
			*nReturn|=ICF::eErrAdd;
            //printf("Add failed: 0x%08lx\n", hr);
            goto error;
        }
/*
        printf(
            "Authorized application %lS is now enabled in the firewall.\n",
            fwProcessImageFileName
            );
*/
    }

error:

    // Free the BSTRs.
    SysFreeString(fwBstrName);
    SysFreeString(fwBstrProcessImageFileName);

    // Release the authorized application instance.
    if (fwApp != NULL)
    {
        fwApp->Release();
    }

    // Release the authorized application collection.
    if (fwApps != NULL)
    {
        fwApps->Release();
    }

    return hr;
}

//-----------------------------------------------------------------------------

HRESULT WindowsFirewallPortIsEnabled(
            IN INetFwProfile* fwProfile,
            IN LONG portNumber,
            IN NET_FW_IP_PROTOCOL ipProtocol,
            OUT BOOL* fwPortEnabled,
			unsigned short *nReturn )
{
    HRESULT hr = S_OK;
    VARIANT_BOOL fwEnabled;
    INetFwOpenPort* fwOpenPort = NULL;
    INetFwOpenPorts* fwOpenPorts = NULL;

    _ASSERT(fwProfile != NULL);
    _ASSERT(fwPortEnabled != NULL);

    *fwPortEnabled = FALSE;

    // Retrieve the globally open ports collection.
    hr = fwProfile->get_GloballyOpenPorts(&fwOpenPorts);
    if (FAILED(hr))
    {
		*nReturn|=ICF::eErrGetGlobalPorts;
        //printf("get_GloballyOpenPorts failed: 0x%08lx\n", hr);
        goto error;
    }

    // Attempt to retrieve the globally open port.
    hr = fwOpenPorts->Item(portNumber, ipProtocol, &fwOpenPort);
    if (SUCCEEDED(hr))
    {
        // Find out if the globally open port is enabled.
        hr = fwOpenPort->get_Enabled(&fwEnabled);
        if (FAILED(hr))
        {
			*nReturn|=ICF::eErrGetEnabled;
            //printf("get_Enabled failed: 0x%08lx\n", hr);
            goto error;
        }

        if (fwEnabled != VARIANT_FALSE)
        {
            // The globally open port is enabled.
            *fwPortEnabled = TRUE;
			*nReturn|= ICF::ePortEnabled;

            //printf("Port %ld is open in the firewall.\n", portNumber);
        }
        else
        {
			*nReturn&=~ICF::ePortEnabled;
            //printf("Port %ld is not open in the firewall.\n", portNumber);
        }
    }
    else
    {
        // The globally open port was not in the collection.
        hr = S_OK;
		*nReturn&=~ICF::ePortEnabled;

        printf("Port %ld is not open in the firewall.\n", portNumber);
    }

error:

    // Release the globally open port.
    if (fwOpenPort != NULL)
    {
        fwOpenPort->Release();
    }

    // Release the globally open ports collection.
    if (fwOpenPorts != NULL)
    {
        fwOpenPorts->Release();
    }

    return hr;
}

//-----------------------------------------------------------------------------

HRESULT WindowsFirewallPortAdd(
            IN INetFwProfile* fwProfile,
            IN LONG portNumber,
            IN NET_FW_IP_PROTOCOL ipProtocol,
            IN const wchar_t* name,
			unsigned short *nReturn )
{
    HRESULT hr = S_OK;
    BOOL fwPortEnabled;
    BSTR fwBstrName = NULL;
    INetFwOpenPort* fwOpenPort = NULL;
    INetFwOpenPorts* fwOpenPorts = NULL;

    _ASSERT(fwProfile != NULL);
    _ASSERT(name != NULL);

    // First check to see if the port is already added.
    hr = WindowsFirewallPortIsEnabled(
         fwProfile,
         portNumber,
         ipProtocol,
        &fwPortEnabled,
		 nReturn );
    if (FAILED(hr))
    {
#pragma message( __FILE__ ":DAVE should we add an error code here?" )
        //printf("WindowsFirewallPortIsEnabled failed: 0x%08lx\n", hr);
        goto error;
    }

    // Only add the port if it isn't already added.
    if (!fwPortEnabled)
    {
        // Retrieve the collection of globally open ports.
        hr = fwProfile->get_GloballyOpenPorts(&fwOpenPorts);
        if (FAILED(hr))
        {
			*nReturn|=ICF::eErrGetGlobalPorts;
            //printf("get_GloballyOpenPorts failed: 0x%08lx\n", hr);
            goto error;
        }

        // Create an instance of an open port.
        hr = CoCreateInstance(
                __uuidof(NetFwOpenPort),
                NULL,
                CLSCTX_INPROC_SERVER,
                __uuidof(INetFwOpenPort),
                (void**)&fwOpenPort
                );
        if (FAILED(hr))
        {
			*nReturn|=ICF::eErrCoCreateInstance;
            //printf("CoCreateInstance failed: 0x%08lx\n", hr);
            goto error;
        }

        // Set the port number.
        hr = fwOpenPort->put_Port(portNumber);
        if (FAILED(hr))
        {
			*nReturn|=ICF::eErrPut;
            //printf("put_Port failed: 0x%08lx\n", hr);
            goto error;
        }

        // Set the IP protocol.
        hr = fwOpenPort->put_Protocol(ipProtocol);
        if (FAILED(hr))
        {
			*nReturn|=ICF::eErrPut;
            //printf("put_Protocol failed: 0x%08lx\n", hr);
            goto error;
        }

        // Allocate a BSTR for the friendly name of the port.
        fwBstrName = SysAllocString(name);
        if (SysStringLen(fwBstrName) == 0)
        {
            hr = E_OUTOFMEMORY;
			*nReturn|=ICF::eErrAllocation;

            //printf("SysAllocString failed: 0x%08lx\n", hr);
            goto error;
        }

        // Set the friendly name of the port.
        hr = fwOpenPort->put_Name(fwBstrName);
        if (FAILED(hr))
        {
			*nReturn|=ICF::eErrPut;
            //printf("put_Name failed: 0x%08lx\n", hr);
            goto error;
        }

        // Opens the port and adds it to the collection.
        hr = fwOpenPorts->Add(fwOpenPort);
        if (FAILED(hr))
        {
			*nReturn|=ICF::eErrAdd;
            //printf("Add failed: 0x%08lx\n", hr);
            goto error;
        }

		*nReturn|=ICF::ePortEnabled;
        //printf("Port %ld is now open in the firewall.\n", portNumber);
    }

error:

    // Free the BSTR.
    SysFreeString(fwBstrName);

    // Release the open port instance.
    if (fwOpenPort != NULL)
    {
        fwOpenPort->Release();
    }

    // Release the globally open ports collection.
    if (fwOpenPorts != NULL)
    {
        fwOpenPorts->Release();
    }

    return hr;
}

//*****************************************************************************

/**
 * Retrieves the current state (block=true / nonblock=false) of the Windows ICF.  Available only on
 *		WinXP SP2 and above
 */

unsigned short CheckFirewallWarningImminent( const wchar_t * szProcessFileName )
{
	unsigned short nReturn=ICF::eNone;


	//	Here is the native WinXP SP2 COM code to open an INetFwPolicy interface
	//		(some code from above URL)
	{
		HRESULT hr = S_OK;
		HRESULT comInit = E_FAIL;
		INetFwProfile* fwProfile = NULL;

		// Initialize COM.
		comInit = CoInitializeEx(
					0,
					COINIT_APARTMENTTHREADED | COINIT_DISABLE_OLE1DDE
					);

		// Ignore RPC_E_CHANGED_MODE; this just means that COM has already been
		// initialized with a different mode. Since we don't care what the mode is,
		// we'll just use the existing mode.
		if (comInit != RPC_E_CHANGED_MODE)
		{
				hr = comInit;
				if (FAILED(hr))
				{
					nReturn|=ICF::eErrComInit;
					//printf("CoInitializeEx failed: 0x%08lx\n", hr);
					goto error;
				}
		}

		// Retrieve the firewall profile currently in effect.
		hr = WindowsFirewallInitialize(&fwProfile,&nReturn);
		if (FAILED(hr))
		{
#pragma message( __FILE__ ":DAVE should we add an error code here?" )
			//printf("WindowsFirewallInitialize failed: 0x%08lx\n", hr);
			goto error;
		}

		//---------------------------------------------------------------------
		//	Check for szProcessFileName to be included in the enabled 
		//		applications list for WinXP SP2 
		//---------------------------------------------------------------------
		if( fwProfile )
		{
			BOOL bTurnedOn=FALSE;
			hr=WindowsFirewallIsOn(
				 fwProfile,
				&bTurnedOn,
				&nReturn );

			if( SUCCEEDED(hr) && bTurnedOn==TRUE )
			{
				BOOL bEnabled=FALSE;

				hr=WindowsFirewallAppIsEnabled(
					 fwProfile,
					 szProcessFileName,
					&bEnabled,
					&nReturn );

				if( SUCCEEDED(hr) )
				{
					if( bEnabled==FALSE )
						nReturn&=~ICF::eProcessEnabled;
					else
						nReturn|= ICF::eProcessEnabled;
				}
				else
				{
					goto error;
				}
			}
			else
			{
#pragma message( __FILE__ ":DAVE should we really set ePortEnabled and eProcessEnabled here?" )
				//	Win XP SP2 ICF is disabled
				nReturn&=~ICF::eFirewallEnabled;
				nReturn|= ICF::ePortEnabled;
				nReturn|= ICF::eProcessEnabled;
			}
		}

		//---------------------------------------------------------------------
error:
		// Release the firewall profile.
		WindowsFirewallCleanup(fwProfile);

		// Uninitialize COM.
		if (SUCCEEDED(comInit))
		{
			CoUninitialize();
		}		
	}

	return nReturn;
}

//*****************************************************************************

extern "C"
JNIEXPORT jshort JNICALL 
Java_com_limegroup_gnutella_util_SystemUtils_isFirewallWarningImminent(
	JNIEnv		*	env, 
	jclass			clazz,
	jstring         appPath ) 
{
	jshort nRet=ICF::eNone;

	bool bWinXpSP2=false;

	{
		OSVERSIONINFOEX osv;
		osv.dwOSVersionInfoSize=sizeof(osv);

		if( GetVersionEx( (LPOSVERSIONINFO)&osv ) )
		{
			if(  osv.dwMajorVersion		>=(MIN_ICF_VER&0x00FF0000)>>0x10 
			  && osv.dwMinorVersion		>=(MIN_ICF_VER&0x0000FF00)>>0x08
			  && osv.wServicePackMajor	>=(MIN_ICF_VER&0x000000FF)>>0x00 )
				bWinXpSP2=true;
		}
	}

	if( bWinXpSP2 )
	{
		wchar_t wcBuf[MAX_PATH];
		const char * szAppPath = env->GetStringUTFChars( appPath, NULL );
		assert( szAppPath );

		size_t nChars = mbstowcs( &wcBuf[0], szAppPath, strlen(szAppPath)+1 );
	    
		nRet=(jshort)(CheckFirewallWarningImminent(APP_NAME));
		env->ReleaseStringUTFChars(appPath,szAppPath);
	}
	else
		nRet|=ICF::eErrUnsupportedOperation;

	if(  (nRet&ICF::eFirewallEnabled)
	  &&!(nRet&ICF::eProcessEnabled) )
		nRet |=ICF::ePopupImminent;

	return nRet;
}

//-----------------------------------------------------------------------------

extern "C" __declspec(dllexport) unsigned short IsFirewallWarningImminent( 
	const char	*	szAppPath )
{
	wchar_t wcBuf[MAX_PATH];
	jshort nRet=ICF::eNone;

	assert( szAppPath );

	bool bWinXpSP2=false;

	{
		OSVERSIONINFOEX osv;
		osv.dwOSVersionInfoSize=sizeof(osv);

		if( GetVersionEx( (LPOSVERSIONINFO)&osv ) )
		{
			if(  osv.dwMajorVersion		>=(MIN_ICF_VER&0x00FF0000)>>0x10 
			  && osv.dwMinorVersion		>=(MIN_ICF_VER&0x0000FF00)>>0x08
			  && osv.wServicePackMajor	>=(MIN_ICF_VER&0x000000FF)>>0x00 )
				bWinXpSP2=true;
		}
	}

	if( bWinXpSP2 )
	{
		size_t nChars = mbstowcs( &wcBuf[0], szAppPath, strlen(szAppPath)+1 );
		nRet=CheckFirewallWarningImminent(&wcBuf[0]);
	}
	else
		nRet|=ICF::eErrUnsupportedOperation;

	if(  (nRet&ICF::eFirewallEnabled)
	  &&!(nRet&ICF::eProcessEnabled) )
		nRet |=ICF::ePopupImminent;

	return nRet;
}

//*****************************************************************************

#endif

