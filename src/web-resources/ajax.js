function getHttpObject(handler, useXml) { 
	var localObjHttp=null

	if (navigator.userAgent.indexOf("MSIE")>=0) { 
		var strName="Msxml2.XMLHTTP"
		if (navigator.appVersion.indexOf("MSIE 5.5")>=0) {
			strName="Microsoft.XMLHTTP"
		} 
		try { 
			localObjHttp=new ActiveXObject(strName)
			localObjHttp.onreadystatechange=handler 
			return localObjHttp
		} catch(e) { 
			alert("Error. Scripting for ActiveX might be disabled") 
			return 
		} 
	} 
	
	if (navigator.userAgent.indexOf("Mozilla")>=0) {
		localObjHttp=new XMLHttpRequest()
		if (useXml) {
			localObjHttp.overrideMimeType('text/xml');
		}
		localObjHttp.onload=handler
		localObjHttp.onerror=handler 
		return localObjHttp
	}
} 

function getPage(url, handler, useXml) {
	objHttp=getHttpObject(handler, useXml)
	objHttp.open("GET", url , true)
	objHttp.send(null)
	return objHttp;
}