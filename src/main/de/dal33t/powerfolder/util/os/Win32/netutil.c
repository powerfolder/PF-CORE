#include "de_dal33t_powerfolder_util_os_Win32_NetworkHelperImpl.h"
#include <windows.h>
#include <Iphlpapi.h>


PIP_ADAPTER_INFO getAdapter() {
    ULONG bufferSize = sizeof(IP_ADAPTER_INFO);
    PIP_ADAPTER_INFO addresses = (PIP_ADAPTER_INFO) malloc(bufferSize);
	int i;
    
    for (i = 0; i < 3; i++) {
        int t = GetAdaptersInfo(addresses, &bufferSize);
        if (t != ERROR_BUFFER_OVERFLOW) {
            return addresses;
        }
        if (addresses != NULL) {
            free(addresses);
        }
        
        addresses = (PIP_ADAPTER_INFO) malloc(bufferSize);
    }
    return NULL;
}

JNIEXPORT jobject JNICALL Java_de_dal33t_powerfolder_util_os_Win32_NetworkHelperImpl_getInterfaceAddresses(JNIEnv *env, jobject t) {
	jclass juLinkedList = (*env)->FindClass(env, "Ljava/util/LinkedList;");
	jmethodID cAdd = (*env)->GetMethodID(env, juLinkedList, "add", "(Ljava/lang/Object;)Z");
	jmethodID cConst = (*env)->GetMethodID(env, juLinkedList, "<init>", "()V");
	jobject col = (*env)->NewObject(env, juLinkedList, cConst);
	jclass jlString = (*env)->FindClass(env, "Ljava/lang/String;");
	PIP_ADAPTER_INFO aStart = getAdapter(), i;
	PIP_ADDR_STRING j;


	for (i = aStart; i; i = i->Next) {
		for (j = &i->IpAddressList; j; j = j->Next) {
			jobjectArray sA = (*env)->NewObjectArray(env, 2, jlString, NULL);
			(*env)->SetObjectArrayElement(env, sA, 0, 
					(*env)->NewStringUTF(env, j->IpAddress.String));
			(*env)->SetObjectArrayElement(env, sA, 1, 
					(*env)->NewStringUTF(env, j->IpMask.String));
			(*env)->CallBooleanMethod(env, col, cAdd, sA);
		}
	}
	free(aStart);

	return col;  
}


