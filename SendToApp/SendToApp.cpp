// SendToApp.cpp : Defines the entry point for the console application.
//

#include "stdafx.h"

// -files <filepath> <filepath> <filepath>
// -body <body>
// -to <recipient>
// -subject <subject>
// enclose content or filepaths with quotes whenever required


const int MAXFILES = 256;

int main(int argc, char* argv[])
{
	
	if (argc <= 1)
	{
		printf ("sendto.exe usage : \r\n" \
			"sendto.exe -files <file1> <file2> ... -body <content> -to <email address> -subject <content>\r\n" \
			"example : sendto.exe -files \"c:\\my files\\file1.ppt\" c:\\document.doc\r\n");
		return 1;
	}

	MapiFileDesc arrfileDesc[MAXFILES];

	typedef ULONG (FAR PASCAL *MAPIFUNC)(LHANDLE lhSession, 
									ULONG ulUIParam,
									lpMapiMessage lpMessage, 
									FLAGS flFlags, 
									ULONG ulReserved);
	MapiMessage Msg;
	MAPIFUNC lpMAPISendMail;

	// charge la dll mapi
	HINSTANCE hMAPILib = ::LoadLibrary("MAPI32.DLL");
	if (hMAPILib == NULL)
		return 0;

	lpMAPISendMail = (MAPIFUNC)GetProcAddress(hMAPILib, "MAPISendMail");
	if (lpMAPISendMail == NULL)
	{
		::FreeLibrary(hMAPILib);
	}

	memset(&Msg, 0, sizeof(Msg));

	int n = 0;
	int i = 1;
	int ibody = -1;
	int isubject = -1;
	int ito = -1;

	while (i < argc)
	{
		char* param = argv[i++];
		if (_stricmp(param,"-files") == 0)
		{
			while (i < argc && argv[i] && argv[i][0]!='-')
			{
				arrfileDesc[n].ulReserved = 0;
				arrfileDesc[n].flFlags = 0;
				arrfileDesc[n].lpFileType = NULL;
				arrfileDesc[n].nPosition = 0xFFFFFFFF;
				arrfileDesc[n].lpszPathName = argv[i];
				arrfileDesc[n].lpszFileName = NULL;

				n++;
				i++;
			}
		}
		else if (_stricmp(param,"-body") == 0)
		{
			ibody = i++;
		}
		else if (_stricmp(param,"-subject") == 0)
		{
			isubject = i++;
		}
		else if (_stricmp(param,"-to") == 0)
		{
			ito = i++;
		}
	}

	if (n == 0)
	{
		::FreeLibrary(hMAPILib);
		return 0;
	}

	Msg.nFileCount = n;
	Msg.lpFiles = arrfileDesc;
	Msg.lpszNoteText = (ibody == -1) ? "" : argv[ibody]; // body
	Msg.lpszSubject = (isubject == -1) ? "" : argv[isubject]; // subject
	
	Msg.nRecipCount = 1;
	MapiRecipDesc recip;
	recip.ulReserved = 0;
	recip.ulRecipClass = MAPI_TO;
	recip.lpszName = (ito == -1) ? "someone@domain.com" : argv[ito]; // recipient (friendly name)
	recip.lpszAddress = (ito == -1) ? "someone@domain.com" : argv[ito]; // recipient (a@domain)
	recip.ulEIDSize = 0;
	recip.lpEntryID = NULL;
	Msg.lpRecips = &recip;


	DWORD res = 0;
	try
	{
		res = lpMAPISendMail(NULL, NULL, &Msg, MAPI_LOGON_UI|MAPI_DIALOG, 0);
	}
	catch (...)
	{
		return 2;
	}

	::FreeLibrary(hMAPILib);	


	return 0;
}

