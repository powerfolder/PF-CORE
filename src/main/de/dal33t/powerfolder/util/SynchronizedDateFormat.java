/*
* Copyright 2004 - 2008 Christian Sprajc, Dennis Waldherr. All rights reserved.
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
package de.dal33t.powerfolder.util;

import java.text.AttributedCharacterIterator;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Helper class which wraps a DateFormat and synchronizes all its methods.
 * 
 * 
 * @author Dennis "Bytekeeper" Waldherr
 *
 */
public class SynchronizedDateFormat extends DateFormat {
	private static final long serialVersionUID = -4583523279891619161L;
	private DateFormat format;

	public SynchronizedDateFormat(DateFormat wrap) {
		format = wrap;
	}
	
	@Override
	public synchronized boolean equals(Object obj) {
		return format.equals(obj);
	}

	@Override
	public synchronized StringBuffer format(Date date, StringBuffer toAppendTo,
			FieldPosition fieldPosition) {
		return format.format(date, toAppendTo, fieldPosition);
	}

	@Override
	public synchronized Calendar getCalendar() {
		return format.getCalendar();
	}

	@Override
	public synchronized NumberFormat getNumberFormat() {
		return format.getNumberFormat();
	}

	@Override
	public synchronized TimeZone getTimeZone() {
		return format.getTimeZone();
	}

	@Override
	public synchronized int hashCode() {
		return format.hashCode();
	}

	@Override
	public synchronized boolean isLenient() {
		return format.isLenient();
	}

	@Override
	public synchronized Date parse(String source, ParsePosition pos) {
		return format.parse(source, pos);
	}

	@Override
	public synchronized Date parse(String source) throws ParseException {
		return format.parse(source);
	}

	@Override
	public synchronized Object parseObject(String source, ParsePosition pos) {
		return format.parseObject(source, pos);
	}

	@Override
	public synchronized void setCalendar(Calendar newCalendar) {
		format.setCalendar(newCalendar);
	}

	@Override
	public synchronized void setLenient(boolean lenient) {
		format.setLenient(lenient);
	}

	@Override
	public synchronized void setNumberFormat(NumberFormat newNumberFormat) {
		format.setNumberFormat(newNumberFormat);
	}

	@Override
	public synchronized void setTimeZone(TimeZone zone) {
		format.setTimeZone(zone);
	}

	@Override
	public synchronized AttributedCharacterIterator formatToCharacterIterator(Object obj) {
		return format.formatToCharacterIterator(obj);
	}

	@Override
	public synchronized Object parseObject(String source) throws ParseException {
		return format.parseObject(source);
	}
}
