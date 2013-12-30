package edu.kit.scc.dei.ecplean;

public class ECPAuthenticationException extends Exception {

	private static final long serialVersionUID = 1L;

	public ECPAuthenticationException() {
	}

	public ECPAuthenticationException(String message) {
		super(message);
	}

	public ECPAuthenticationException(Throwable cause) {
		super(cause);
	}

	public ECPAuthenticationException(String message, Throwable cause) {
		super(message, cause);
	}

}
