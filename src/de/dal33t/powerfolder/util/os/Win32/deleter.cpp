#include <jni.h>
#include "de_dal33t_powerfolder_util_os_Win32_RecycleDeleteImpl.h"

#include <stdio.h>
#include "stdafx.h"

bool convert_jstringToChars(JNIEnv *jni_enviroment, jstring	source_jstr,
                            WCHAR *target_chars, int chars_length) {
	if (source_jstr == NULL) {
		target_chars[0] = 0;
	} else {
		int	jstr_length = jni_enviroment->GetStringLength(source_jstr);
		if ( jstr_length >= chars_length ){
            printf( "convert_jstringToChars: source_jstr length invalid");
			return( false );
		}
		const jchar *jStringData = jni_enviroment->GetStringChars(source_jstr, NULL);
		for (int i=0;i<jstr_length;i++){
			target_chars[i] = (WCHAR)jStringData[i];
		}
		target_chars[jstr_length]=0;
		jni_enviroment->ReleaseStringChars( source_jstr, jStringData );
	}
	return( true );
}

JNIEXPORT void JNICALL Java_de_dal33t_powerfolder_util_os_Win32_RecycleDeleteImpl_delete__Ljava_lang_String_2
(JNIEnv *jni_enviroment, jclass _class, jstring _fileName) {
    WCHAR file_name[16000];
    SHFILEOPSTRUCTW opFile;
    
	if (!convert_jstringToChars(jni_enviroment, _fileName, file_name, sizeof(file_name)-1)) {
		return;
	}
    HANDLE fileToDelete = CreateFileW(file_name, GENERIC_READ, FILE_SHARE_READ, NULL, 
                            OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);
    //does the file exists?
	if (fileToDelete == INVALID_HANDLE_VALUE) {   
		printf("Java_de_dal33t_powerfolder_util_Win32Deleter_delete file not found");
        return;
    }
    
    CloseHandle(fileToDelete);
    file_name[ wcslen(file_name)+1 ] = 0;
    ZeroMemory(&opFile, sizeof(opFile));
    opFile.wFunc = FO_DELETE; //Delete the file(s) specified in pFrom.
    opFile.pFrom = file_name; 
    int flags = 0; 
 
    flags = FOF_NOCONFIRMATION;
 
    flags = flags | FOF_SILENT;

    opFile.fFlags = FOF_ALLOWUNDO | flags;
    
    if (SHFileOperationW(&opFile)){
        printf("Java_de_dal33t_powerfolder_util_Win32Deleter_delete SHFileOperation failed");
    }
}
JNIEXPORT void JNICALL Java_de_dal33t_powerfolder_util_os_Win32_RecycleDeleteImpl_delete__Ljava_lang_String_2Z
(JNIEnv *jni_enviroment, jclass _class, jstring _fileName, 
                            jboolean confirm) {
    WCHAR file_name[16000];
    SHFILEOPSTRUCTW opFile;
    
	if (!convert_jstringToChars(jni_enviroment, _fileName, file_name, sizeof(file_name)-1)) {
		return;
	}
    HANDLE fileToDelete = CreateFileW(file_name, GENERIC_READ, FILE_SHARE_READ, NULL, 
                            OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);
    //does the file exists?
	if (fileToDelete == INVALID_HANDLE_VALUE) {   
		printf("Java_de_dal33t_powerfolder_util_Win32Deleter_delete file not found");
        return;
    }
    
    CloseHandle(fileToDelete);
    file_name[ wcslen(file_name)+1 ] = 0;
    ZeroMemory(&opFile, sizeof(opFile));
    opFile.wFunc = FO_DELETE; //Delete the file(s) specified in pFrom.
    opFile.pFrom = file_name; 
    int flags = 0; 
    if (!confirm) {
        flags = FOF_NOCONFIRMATION;
    }
    flags = flags | FOF_SILENT;

    opFile.fFlags = FOF_ALLOWUNDO | flags;
    
    if (SHFileOperationW(&opFile)){
        printf("Java_de_dal33t_powerfolder_util_Win32Deleter_delete SHFileOperation failed");
    }
}

JNIEXPORT void JNICALL Java_de_dal33t_powerfolder_util_os_Win32_RecycleDeleteImpl_delete__Ljava_lang_String_2ZZ
(JNIEnv *jni_enviroment, jclass _class, jstring _fileName, 
                            jboolean confirm, jboolean showProgress) {
    WCHAR file_name[16000];
    SHFILEOPSTRUCTW opFile;
    
	if (!convert_jstringToChars(jni_enviroment, _fileName, file_name, sizeof(file_name)-1)) {
		return;
	}
    HANDLE fileToDelete = CreateFileW(file_name, GENERIC_READ, FILE_SHARE_READ, NULL, 
                            OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);
    //does the file exists?
	if (fileToDelete == INVALID_HANDLE_VALUE) {   
		printf("Java_de_dal33t_powerfolder_util_Win32Deleter_delete file not found");
        return;
    }
    
    CloseHandle(fileToDelete);
    file_name[ wcslen(file_name)+1 ] = 0;
    ZeroMemory(&opFile, sizeof(opFile));
    opFile.wFunc = FO_DELETE; //Delete the file(s) specified in pFrom.
    opFile.pFrom = file_name; 
    int flags = 0; 
    if (!confirm) {
        flags = FOF_NOCONFIRMATION;
    }
    if (!showProgress) {
        flags = flags | FOF_SILENT;
    }
    opFile.fFlags = FOF_ALLOWUNDO | flags;
    
    if (SHFileOperationW(&opFile)){
        printf("Java_de_dal33t_powerfolder_util_Win32Deleter_delete SHFileOperation failed");
    }
}
