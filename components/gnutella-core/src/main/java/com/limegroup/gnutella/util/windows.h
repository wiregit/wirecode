/*++ BUILD Version: 0001    Increment this if a change has global effects

Copyright (c) 1985-1999, Microsoft Corporation

Module Name:

    windows.h

Abstract:

    Master include file for Windows applications.

--*/

#ifndef _WINDOWS_
#define _WINDOWS_

#ifdef __BORLANDC__
#pragma defineonoption __BOROPT_A -A
#pragma defineonoption __BOROPT_H -H

#if (__BORLANDC__ == 0x550) && defined(__BOROPT_A) && defined(__BOROPT_H)
#error The use of both -A (ANSI mode) and -H (pre-compiled headers) at the same time is unsupported.
#endif

#endif /* __BORLANDC__ */

#pragma option push -b -a8 -pc -A- /*P_O_Push*/

#ifndef WINVER
#define WINVER 0x0500
#else
#if defined(_WIN32_WINNT) && (WINVER < 0x0400) && (_WIN32_WINNT > 0x0400)
#error WINVER setting conflicts with _WIN32_WINNT setting
#endif
#endif

#if 0 // (WINVER >= 0x0500)
#pragma message ("")
#pragma message ("NOTE: WINVER has been defined as 0x0500 or greater which enables")
#pragma message ("Windows 2000 features.")
#pragma message ("")
#pragma message ("Caution: When building applications with WINVER set to 0x0500")
#pragma message ("or greater, the resulting binary may not run as expected")
#pragma message ("on earlier platforms, such Windows NT 4.0 or Windows 95 & Windows 98")
#pragma message ("")
#pragma message ("See the Platform SDK release notes for more information.")
#pragma message ("")
#endif

#ifndef _INC_WINDOWS
#define _INC_WINDOWS

#if defined (_MSC_VER) && (_MSC_VER >= 1020)
#pragma once
#endif

/*  If defined, the following flags inhibit definition
 *     of the indicated items.
 *
 *  NOGDICAPMASKS     - CC_*, LC_*, PC_*, CP_*, TC_*, RC_
 *  NOVIRTUALKEYCODES - VK_*
 *  NOWINMESSAGES     - WM_*, EM_*, LB_*, CB_*
 *  NOWINSTYLES       - WS_*, CS_*, ES_*, LBS_*, SBS_*, CBS_*
 *  NOSYSMETRICS      - SM_*
 *  NOMENUS           - MF_*
 *  NOICONS           - IDI_*
 *  NOKEYSTATES       - MK_*
 *  NOSYSCOMMANDS     - SC_*
 *  NORASTEROPS       - Binary and Tertiary raster ops
 *  NOSHOWWINDOW      - SW_*
 *  OEMRESOURCE       - OEM Resource values
 *  NOATOM            - Atom Manager routines
 *  NOCLIPBOARD       - Clipboard routines
 *  NOCOLOR           - Screen colors
 *  NOCTLMGR          - Control and Dialog routines
 *  NODRAWTEXT        - DrawText() and DT_*
 *  NOGDI             - All GDI defines and routines
 *  NOKERNEL          - All KERNEL defines and routines
 *  NOUSER            - All USER defines and routines
 *  NONLS             - All NLS defines and routines
 *  NOMB              - MB_* and MessageBox()
 *  NOMEMMGR          - GMEM_*, LMEM_*, GHND, LHND, associated routines
 *  NOMETAFILE        - typedef METAFILEPICT
 *  NOMINMAX          - Macros min(a,b) and max(a,b)
 *  NOMSG             - typedef MSG and associated routines
 *  NOOPENFILE        - OpenFile(), OemToAnsi, AnsiToOem, and OF_*
 *  NOSCROLL          - SB_* and scrolling routines
 *  NOSERVICE         - All Service Controller routines, SERVICE_ equates, etc.
 *  NOSOUND           - Sound driver routines
 *  NOTEXTMETRIC      - typedef TEXTMETRIC and associated routines
 *  NOWH              - SetWindowsHook and WH_*
 *  NOWINOFFSETS      - GWL_*, GCL_*, associated routines
 *  NOCOMM            - COMM driver routines
 *  NOKANJI           - Kanji support stuff.
 *  NOHELP            - Help engine interface.
 *  NOPROFILER        - Profiler interface.
 *  NODEFERWINDOWPOS  - DeferWindowPos routines
 *  NOMCX             - Modem Configuration Extensions
 */

#if defined(RC_INVOKED) && !defined(NOWINRES)

#include <winresrc.h>

#else

#if defined(RC_INVOKED)
/* Turn off a bunch of stuff to ensure that RC files compile OK. */
#define NOATOM
#define NOGDI
#define NOGDICAPMASKS
#define NOMETAFILE
#define NOMINMAX
#define NOMSG
#define NOOPENFILE
#define NORASTEROPS
#define NOSCROLL
#define NOSOUND
#define NOSYSMETRICS
#define NOTEXTMETRIC
#define NOWH
#define NOCOMM
#define NOKANJI
#define NOCRYPT
#define NOMCX
#endif

#if defined(__BORLANDC__) && defined(__cplusplus)
#    include <mem.h> // Needed for protos of memxxx() routines
#endif

#if defined(__BORLANDC__)
#    ifndef _WIN32_WINNT
#        define _WIN32_WINNT 0x0500  /* If not set, assume NT 5.00  */
#    endif
#    if defined(__cplusplus)
#       define NOMINMAX              /* for WINDEF.H */
#    else
#        define NONAMELESSUNION      /* for OAIDL.H, OBJIDL.H,...  */
#    endif
#    if defined(_UNICODE)
#        define UNICODE
#    endif
#endif

#if !defined(_68K_) && !defined(_MPPC_) && !defined(_PPC_) && !defined(_ALPHA_) && !defined(_MIPS_) && !defined(_X86_) && !defined(_IA64_) && defined(_M_IX86)
#define _X86_
#endif

#if !defined(_68K_) && !defined(_MPPC_) && !defined(_PPC_) && !defined(_ALPHA_) && !defined(_X86_) && !defined(_IA64_) && !defined(_MIPS_) && defined(_M_MRX000)
#define _MIPS_
#endif

#if !defined(_68K_) && !defined(_MPPC_) && !defined(_PPC_) && !defined(_ALPHA_) && !defined(_X86_) && !defined(_IA64_) && !defined(_MIPS_) && defined(_M_ALPHA)
#define _ALPHA_
#endif

#if !defined(_68K_) && !defined(_MPPC_) && !defined(_PPC_) && !defined(_ALPHA_) && !defined(_X86_) && !defined(_IA64_) && !defined(_MIPS_) && defined(_M_PPC)
#define _PPC_
#endif

#if !defined(_68K_) && !defined(_MPPC_) && !defined(_PPC_) && !defined(_ALPHA_) && !defined(_X86_) && !defined(_IA64_) && !defined(_MIPS_) && defined(_M_M68K)
#define _68K_
#endif

#if !defined(_68K_) && !defined(_MPPC_) && !defined(_PPC_) && !defined(_ALPHA_) && !defined(_X86_) && !defined(_IA64_) && !defined(_MIPS_) && defined(_M_MPPC)
#define _MPPC_
#endif

#if !defined(_68K_) && !defined(_MPPC_) && !defined(_PPC_) && !defined(_ALPHA_) && !defined(_MIPS_) && !defined(_X86_) && !defined(_M_IX86) && defined(_M_IA64)
#if !defined(_IA64_)
#define _IA64_
#endif // !_IA64_
#endif

#ifndef _MAC
#if defined(_68K_) || defined(_MPPC_)
#define _MAC
#endif
#endif

#ifndef RC_INVOKED
#if     ( _MSC_VER >= 800 ) || defined(__BORLANDC__)
#pragma warning(disable:4514)
#ifndef __WINDOWS_DONT_DISABLE_PRAGMA_PACK_WARNING__
#pragma warning(disable:4103)
#endif
#if _MSC_VER >= 1200
#pragma warning(push)
#endif
#pragma warning(disable:4001)
#pragma warning(disable:4201)
#pragma warning(disable:4214)
#endif
#include <excpt.h>
#include <stdarg.h>
#endif /* RC_INVOKED */

#include <windef.h>
#include <winbase.h>
#include <wingdi.h>
#include <winuser.h>
#if !defined(_MAC) || defined(_WIN32NLS)
#include <winnls.h>
#endif
#ifndef _MAC
#include <wincon.h>
#include <winver.h>
#endif
#if !defined(_MAC) || defined(_WIN32REG)
#include <winreg.h>
#endif
#ifndef _MAC
#include <winnetwk.h>
#endif

#ifndef WIN32_LEAN_AND_MEAN
#include <cderr.h>
#include <dde.h>
#include <ddeml.h>
#include <dlgs.h>
#ifndef _MAC
#include <lzexpand.h>
#include <mmsystem.h>
#include <nb30.h>
#include <rpc.h>
#endif
#include <shellapi.h>
#ifndef _MAC
#include <winperf.h>

#if(_WIN32_WINNT >= 0x0400)
#if defined(__BORLANDC__) && !defined(_WINSOCKAPI_) && !defined(_WINSOCK2API_)
#include <winsock2.h>
#endif /* __BORLANDC__ */
#if defined(__BORLANDC__) && !defined(_WINSOCKAPI_) && !defined(_MSWSOCK_)
#include <mswsock.h>
#endif /* __BORLANDC__ */
#else
#if defined(__BORLANDC__) && !defined(_WINSOCKAPI_) && !defined(_WINSOCK2API_)
#include <winsock.h>
#endif /* __BORLANDC__ */
#endif /* _WIN32_WINNT >=  0x0400 */

#endif
#ifndef NOCRYPT
#include <wincrypt.h>
#include <winefs.h>
#include <winscard.h>
#endif

#ifndef NOGDI
#ifndef _MAC
#include <winspool.h>
#ifdef INC_OLE1
#include <ole.h>
#else
#include <ole2.h>
#endif /* !INC_OLE1 */
#endif /* !MAC */
#include <commdlg.h>
#endif /* !NOGDI */
#endif /* WIN32_LEAN_AND_MEAN */

#ifdef _MAC
#include <winwlm.h>
#endif


#ifdef INC_OLE2
#include <ole2.h>
#endif /* INC_OLE2 */

#ifndef _MAC
#ifndef NOSERVICE
#include <winsvc.h>
#endif

#if(WINVER >= 0x0400)
#ifndef NOMCX
#include <mcx.h>
#endif /* NOMCX */

#ifndef NOIME
#include <imm.h>
#endif
#endif /* WINVER >= 0x0400 */
#endif

#ifndef RC_INVOKED
#if     ( _MSC_VER >= 800 ) || defined(__BORLANDC__)
#if _MSC_VER >= 1200
#pragma warning(pop)
#else
#pragma warning(default:4001)
#pragma warning(default:4201)
#pragma warning(default:4214)
/* Leave 4514 disabled.  It's an unneeded warning anyway. */
#endif
#endif
#endif /* RC_INVOKED */

#endif /* RC_INVOKED */

#endif /* _INC_WINDOWS */
#pragma option pop /*P_O_Pop*/
#endif /* _WINDOWS_ */
