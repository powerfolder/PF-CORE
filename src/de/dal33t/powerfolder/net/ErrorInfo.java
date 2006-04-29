package de.dal33t.powerfolder.net;

/*
 * creates a class of DynDns server errors
 * 
 * @author albena roshelova
 *
 */
public class ErrorInfo {

	private String 	code;
	private int    	intCode;
	private int    	errType;
	private String 	errTxt;
	
	public ErrorInfo(String code, int intCode, int errType, String errTxt)
	{
		this.code = code;
		this.intCode = intCode;
		this.errType = errType;
		this.errTxt = errTxt;
	}

	/*
	 * Returns the error type. It might be one of the following:
	 * NO_ERROR, ERROR, WARN, GOOD.
	 */
	public int getType() { return errType; }
	
	/*
	 * Returns the type of the code error 
	 * (e.g. nochg, badsys, badagent, badauth, and etc).
	 */
	public int getCode() { return intCode; }
	
	/*
	 * Returns the specific code error text
	 */
	public String getText() { return errTxt; }
	
	public String getShortText() { return code; }
}