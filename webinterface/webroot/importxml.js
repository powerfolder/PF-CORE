/*
var browserSupported = importXML( string: locationOfXMLFile, string: nameOfFunction[, optional boolean: allowCache] );
var browserSupported = importXML( 'myXML.xml', 'runThis' );

When the xml file has loaded, the named function will be run, and will be passed a reference to the document
object of the XML file. You can then manipulate the data in the file using W3C DOM scripting.

Browsers may cache the XML files (with Safari, the import fails if the file is already cached by the current page).
To prevent this, the script adds a timestamp to the end of each request URL (changes every millisecond).
If you do not want this timestamp to be added, pass the value 'true' as a third parameter.
var browserSupported = importXML( 'myXML.xml', 'runThis', true );
This is not recommended.
*/

var xmlHttpObjectsArray = [];

function importXML(url, handlerFunction, doNotPreventCache) {
	//note: in XML importing event handlers, 'this' refers to window
	if (!doNotPreventCache) { //prevent cache
		if (url.indexOf('?') == -1) {
			url += '?';
		} else {
			url += '&';
		}
		url += ( new Date() ).getTime(); 
	} 
	if (window.XMLHttpRequest) { // XMLHTTP request by standard - Gecko, Safari 1.2+ and Opera 7.6+
		xmlHttpObjectsArray[xmlHttpObjectsArray.length] = new XMLHttpRequest(); //creates a new object at a new array index
		xmlHttpObjectsArray[xmlHttpObjectsArray.length-1].onreadystatechange = new Function( 'if( xmlHttpObjectsArray['+(xmlHttpObjectsArray.length-1)+'].readyState == 4 && xmlHttpObjectsArray['+(xmlHttpObjectsArray.length-1)+'].status < 300 ) { '+handlerFunction+'(xmlHttpObjectsArray['+(xmlHttpObjectsArray.length-1)+'].responseXML); }' );
		xmlHttpObjectsArray[xmlHttpObjectsArray.length-1].open("GET", url, true);
		xmlHttpObjectsArray[xmlHttpObjectsArray.length-1].send(null);
		return true;
	}
	if (window.ActiveXObject) {	// IE
		try { //IE Mac may throw some errors
			try { 
				var activeXObj = new ActiveXObject( 'Microsoft.XMLDOM' ); //newer
			} catch(e) { 
				var activeXObj = new ActiveXObject( 'Msxml2.XMLHTTP' ); //older
			} 
			xmlHttpObjectsArray[xmlHttpObjectsArray.length] = activeXObj;
			xmlHttpObjectsArray[xmlHttpObjectsArray.length-1].onreadystatechange = new Function( 'if( xmlHttpObjectsArray['+(xmlHttpObjectsArray.length-1)+'].readyState == 4 ) { '+handlerFunction+'(xmlHttpObjectsArray['+(xmlHttpObjectsArray.length-1)+']); }' );
			xmlHttpObjectsArray[xmlHttpObjectsArray.length-1].load(url);
			return true;
		} catch(e) {}
	}
	return false; //no XMLHTTPRequest object found, use other browser
}