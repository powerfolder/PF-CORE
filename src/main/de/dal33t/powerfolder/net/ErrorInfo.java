/*
* Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
*
* This file is part of PowerFolder.
*
* PowerFolder is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation.
*
* PowerFolder is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
*
* $Id$
*/
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