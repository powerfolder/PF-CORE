#include "stdafx.h"
#include "de_dal33t_powerfolder_util_os_Win32_WinUtils.h"

jclass shellLinkClass;
jfieldID argID, descID, pathID, workdirID;

void throwHRES(JNIEnv* env, const char* classname, HRESULT err) {
	char buf[512] = "Error";
	WideCharToMultiByte(CP_ACP, WC_NO_BEST_FIT_CHARS, _com_error(err).ErrorMessage(), -1, buf, 512, NULL, NULL);
	env->ThrowNew(env->FindClass(classname), buf);
}

JNIEXPORT jstring JNICALL Java_de_dal33t_powerfolder_util_os_Win32_WinUtils_getSystemFolderPath
(JNIEnv *env, jobject m, jint id, jboolean type) {
	wchar_t path[MAX_PATH];
	if (SUCCEEDED(SHGetFolderPath(NULL, (int) id, NULL,
		(type ? SHGFP_TYPE_DEFAULT : SHGFP_TYPE_CURRENT),
		path))) {
			return env->NewString((jchar*) path, (jsize) wcslen(path));
	}
	return NULL;
}

JNIEXPORT void JNICALL Java_de_dal33t_powerfolder_util_os_Win32_WinUtils_init
(JNIEnv *env, jobject jobj) {
	HRESULT hres = CoInitializeEx(NULL, NULL);
	if (FAILED(hres)) {
		throwHRES(env, "java/lang/RuntimeException", hres);
		return;
	}
	shellLinkClass = env->FindClass("de/dal33t/powerfolder/util/os/Win32/ShellLink");
	if (shellLinkClass) {
		char* jls = "Ljava/lang/String;";
		argID = env->GetFieldID(shellLinkClass, "arguments", jls);
		descID = env->GetFieldID(shellLinkClass, "description", jls); 
		pathID = env->GetFieldID(shellLinkClass, "path", jls);
		workdirID = env->GetFieldID(shellLinkClass, "workdir", jls);
	}
}


JNIEXPORT void JNICALL Java_de_dal33t_powerfolder_util_os_Win32_WinUtils_createLink
(JNIEnv *env, jobject obj, jobject shellLink, jstring target) {

	if (!shellLinkClass || !obj || !argID || !descID || !pathID || !target) { 
		env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "ShellLink class error or parameter missing!");
		return;
	}

	jstring argument = (jstring) env->GetObjectField(shellLink, argID);
	jstring desc = (jstring) env->GetObjectField(shellLink, descID);
	jstring path = (jstring) env->GetObjectField(shellLink, pathID);
	jstring workdir = (jstring) env->GetObjectField(shellLink, workdirID);


	if (!path) {
		env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "ShellLink.path not set!");
		return;
	}

    HRESULT hres; 
    IShellLink* psl; 
 
    // Get a pointer to the IShellLink interface. 
    hres = CoCreateInstance(CLSID_ShellLink, NULL, CLSCTX_INPROC_SERVER, 
                            IID_IShellLink, (LPVOID*)&psl); 
    if (SUCCEEDED(hres)) 
    { 
        IPersistFile* ppf; 
		const jchar* t;

		if (argument) {
			// Set the path to the shortcut target and add the description. 
			t = env->GetStringChars(argument, NULL);
			psl->SetArguments((LPCWSTR) t);
			env->ReleaseStringChars(argument, t);
		}

		// Path is mandatory
		t = env->GetStringChars(path, NULL);
		psl->SetPath((LPCWSTR) t);
		env->ReleaseStringChars(path, t);

		if (desc != NULL) {
			t = env->GetStringChars(desc, NULL);
			psl->SetDescription((LPCWSTR) t);
			env->ReleaseStringChars(desc, t);
		}
		if (workdir != NULL) {
			t = env->GetStringChars(workdir, NULL);
			psl->SetWorkingDirectory((LPCWSTR) t);
			env->ReleaseStringChars(workdir, t);
		}
 
        // Query IShellLink for the IPersistFile interface for saving the 
        // shortcut in persistent storage. 
        hres = psl->QueryInterface(IID_IPersistFile, (LPVOID*)&ppf); 
 
        if (SUCCEEDED(hres)) 
        { 
            // Save the link by calling IPersistFile::Save. 
			t = env->GetStringChars(target, NULL);
			hres = ppf->Save((LPCWSTR) t, TRUE);
			if (FAILED(hres)) {
				throwHRES(env, "java/io/IOException", hres);
			}
			env->ReleaseStringChars(target, t);
            ppf->Release(); 
		}  else {
			throwHRES(env, "java/io/IOException", hres);
		}
        psl->Release(); 
	} else {
		throwHRES(env, "java/io/IOException", hres);
	}
}

/*
JNIEXPORT jstring JNICALL Java_de_dal33t_powerfolder_util_os_Win32_WinUtils_getCommandLine
(JNIEnv *env, jobject obj) {
	LPWSTR s = GetCommandLine();
	return env->NewString((jchar*) s, (jsize) wcslen(s));
	HMODULE exemod = GetModuleHandle(NULL);
	wchar_t* fname = NULL;
	DWORD size = MAX_PATH;
	DWORD result;
	do {
		fname = (wchar_t*) realloc(fname, sizeof(wchar_t) * size + 1);
		result = GetModuleFileName(exemod, fname, size);
		size *= 2;
	} while (GetLastError() == ERROR_INSUFFICIENT_BUFFER);
	if (!result) {
		env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "Error while retrieving executable path!");
		return NULL;
	}
	return env->NewString((jchar*) fname, (jsize) wcslen(fname));
}
*/