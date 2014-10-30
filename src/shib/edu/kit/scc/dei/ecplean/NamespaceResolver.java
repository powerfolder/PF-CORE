package edu.kit.scc.dei.ecplean;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;

public class NamespaceResolver implements NamespaceContext {

	private Map<String, String> prefixMap;
	private Map<String, String> uriMap;

	public NamespaceResolver() {
		prefixMap = new HashMap<String, String>();
		uriMap = new HashMap<String, String>();
	}

	public void addNamespace(String prefix, String uri) {
		prefixMap.put(prefix, uri);
		uriMap.put(uri, prefix);
	}

	@Override
	public String getNamespaceURI(String prefix) {
		return prefixMap.get(prefix);
	}

	@Override
	public String getPrefix(String namespaceURI) {
		return uriMap.get(namespaceURI);
	}

	@Override
	public Iterator<String> getPrefixes(String namespaceURI) {
		return prefixMap.keySet().iterator();
	}

}
