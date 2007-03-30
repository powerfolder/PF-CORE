#include "stdafx.h"
#include "de_dal33t_powerfolder_util_os_Win32_WinUtils.h"

jclass shellLinkClass;
jfieldID argID, descID, pathID, workdirID;


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
	CoInitializeEx(NULL, COINIT_APARTMENTTHREADED);
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
		env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "ShellLink value or parameter missing!");
		return;
	}

	jstring argument = (jstring) env->GetObjectField(shellLink, argID);
	jstring desc = (jstring) env->GetObjectField(shellLink, descID);
	jstring path = (jstring) env->GetObjectField(shellLink, pathID);
	jstring workdir = (jstring) env->GetObjectField(shellLink, workdirID);


	if (!argument || !path)
		return;

    HRESULT hres; 
    IShellLink* psl; 
 
    // Get a pointer to the IShellLink interface. 
    hres = CoCreateInstance(CLSID_ShellLink, NULL, CLSCTX_INPROC_SERVER, 
                            IID_IShellLink, (LPVOID*)&psl); 
    if (SUCCEEDED(hres)) 
    { 
        IPersistFile* ppf; 
 
        // Set the path to the shortcut target and add the description. 
		const jchar* t = env->GetStringChars(argument, NULL);
		psl->SetArguments((LPCWSTR) t);
		env->ReleaseStringChars(argument, t);

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
			const jchar* t = env->GetStringChars(target, NULL);
			hres = ppf->Save((LPCWSTR) t, TRUE); 
			env->ReleaseStringChars(target, t);
            ppf->Release(); 
        } 
        psl->Release(); 
    } 
}
