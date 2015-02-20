package edu.kit.scc.dei.ecplean;

import java.net.URI;

public class ECPAuthenticationInfo {

	private String username;
	private String password;
	private URI idpEcpEndpoint;
	private URI spUrl;
	private ECPAuthState authState;
	private String proxyUsername;
	private String proxyPassword;
	
	public ECPAuthenticationInfo(String username, String password,
			URI idpEcpEndpoint, URI spUrl, String proxyUsername, String proxyPassword) {
		super();
		this.username = username;
		this.password = password;
		this.idpEcpEndpoint = idpEcpEndpoint;
		this.spUrl = spUrl;
		this.proxyUsername = proxyUsername;
		this.proxyPassword = proxyPassword;
	}

	public String getUsername() {
		return username;
	}
	
	public String getPassword() {
		return password;
	}
	
	public URI getIdpEcpEndpoint() {
		return idpEcpEndpoint;
	}
	
	public URI getSpUrl() {
		return spUrl;
	}

	public String getProxyUsername() {
	    return proxyUsername;
	}

    public String getProxyPassword() {
        return proxyPassword;
    }

	public ECPAuthState getAuthState() {
		return authState;
	}

	public void setAuthState(ECPAuthState authState) {
		this.authState = authState;
	}

	
}
