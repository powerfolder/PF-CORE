package edu.kit.scc.dei.ecplean;

import java.net.URI;

public class ECPAuthenticationInfo {

	private String username;
	private String password;
	private URI idpEcpEndpoint;
	private URI spUrl;
	private ECPAuthState authState;

	public ECPAuthenticationInfo(String username, String password,
			URI idpEcpEndpoint, URI spUrl) {
		super();
		this.username = username;
		this.password = password;
		this.idpEcpEndpoint = idpEcpEndpoint;
		this.spUrl = spUrl;
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

	public ECPAuthState getAuthState() {
		return authState;
	}

	public void setAuthState(ECPAuthState authState) {
		this.authState = authState;
	}


}
