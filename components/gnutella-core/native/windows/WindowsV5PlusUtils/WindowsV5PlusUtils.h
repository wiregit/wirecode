// WindowsV5PlusUtils.h : Defines the enumerated return values for Windows ICF
//

#ifndef __WINDOWSV5PLUSUTILS__
#define __WINDOWSV5PLUSUTILS__

namespace ICF
{
	enum
	{
		eNone					=	0x0000,

		//	FW STATUS
		eFirewallEnabled		=	0x0001,
		eProcessEnabled			=	0x0002,
		ePortEnabled			=	0x0004,

		//	FW POPUP STATUS
		ePopupImminent			=	0x0008,

		//	ERRORS
		eErrUnsupportedOperation=	0x0010,		//	Attempted to call under WinVer < WinXP SP2
		eErrGetGlobalPorts		=	0x0020,
		eErrAdd					=	0x0040,		//	Used for adding Process and Port
		eErrPut					=	0x0080,		//	Used for Filename, Friendly name, Port, & Protocol
		eErrGetEnabled			=	0x0100,		//	Used for both "Process GetEnabled" and "Port GetEnabled"
		eErrAllocation			=	0x0200,		//	Out of memory
		eErrGetAuthApps			=	0x0400,
		eErrGetFwEnabled		=	0x0800,
		eErrGetCurProfile		=	0x1000,
		eErrGetLocalPolicy		=	0x2000,
		eErrCoCreateInstance	=	0x4000,		//	Used for all COM creation calls
		eErrComInit				=	0x8000,
	};
}


#endif

