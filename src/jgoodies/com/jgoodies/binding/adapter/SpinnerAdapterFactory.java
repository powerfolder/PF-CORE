/*
 * Copyright (c) 2002-2006 JGoodies Karsten Lentzsch. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *  o Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimer. 
 *     
 *  o Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution. 
 *     
 *  o Neither the name of JGoodies Karsten Lentzsch nor the names of 
 *    its contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission. 
 *     
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE 
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */

package com.jgoodies.binding.adapter;

import java.util.Calendar;
import java.util.Date;

import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

import com.jgoodies.binding.value.ValueModel;

/**
 * A factory that vends {@link SpinnerModel} implementations that are bound to 
 * a ValueModel. Can be used to bind a ValueModel to instances of JSpinner.<p>
 *  
 * To keep the ValueModel and SpinnerModel synchronized, this class listens
 * to changes in both sides and updates the other silently, i.e. without
 * firing a duplicate change event.<p>
 * 
 * <strong>Constraints:</strong> 
 * The ValueModel's type must be compatible with the type required by the 
 * referenced SpinnerModel. For example a {@link SpinnerNumberModel} requires
 * <code>Number</code> values.
 * 
 * <strong>Example:</strong><pre>
 * // General Connection
 * ValueModel   levelModel   = new PropertyAdapter(settings, "level", true);
 * SpinnerModel spinnerModel = new SpinnerNumberModel(9, 5, 10, 1);
 * SpinnerAdapterFactory.connect(levelModel, spinnerModel);
 * JSpinner levelSpinner = new JSpinner(spinnerModel);
 * 
 * // Short Form
 * ValueModel levelModel = new PropertyAdapter(settings, "level", true);
 * SpinnerNumberModel spinnerModel = 
 *     SpinnerAdapterFactory.createNumberAdapter(levelModel, 5, 10, 1);
 * JSpinner levelSpinner = new JSpinner(spinnerModel);
 * </pre>
 * 
 * @author  Karsten Lentzsch
 * @version $Revision: 1.5 $
 * 
 * @see     ValueModel
 * @see     SpinnerModel
 * @see     javax.swing.JSpinner
 * 
 * @since 1.1
 */
public final class SpinnerAdapterFactory {

    private SpinnerAdapterFactory() {
    // Override default constructor; prevents instantiation.
    }


    // Factory Methods ********************************************************

    /**
     * Creates and returns a <code>SpinnerDateModel</code> bound to the given
     * <code>valueModel</code>. The <code>calendarField</code>
     * is equal to <code>Calendar.DAY_OF_MONTH</code>; there are no 
     * <code>start</code>/<code>end</code> limits.
     * 
     * @param valueModel   a <code>Date</code> typed model that holds the spinner value
     * @param defaultDate  the date used if the valueModel's value is <code>null</code>
     * @return a <code>SpinnerDateModel</code> bound to the given 
     *     <code>valueModel</code> without start and end limits using
     *     <code>Calendar.DAY_OF_MONTH</code> as calendar field
     *     
     * @throws NullPointerException       if the valueModel or defaultDate is <code>null</code>
     */
    public static SpinnerDateModel createDateAdapter(ValueModel valueModel, Date defaultDate) {
        return createDateAdapter(valueModel, defaultDate, null, null, Calendar.DAY_OF_MONTH);
    }


    /**
     * Creates and returns a <code>SpinnerDateModel</code> that represents a sequence 
     * of dates and is bound to the given <code>valueModel</code>.
     * The dates are between <code>start</code> and <code>end</code>.  The 
     * <code>nextValue</code> and <code>previousValue</code> methods 
     * compute elements of the sequence by advancing or reversing
     * the current date <code>value</code> by the 
     * <code>calendarField</code> time unit.  For a precise description
     * of what it means to increment or decrement a <code>Calendar</code>
     * <code>field</code>, see the <code>add</code> method in 
     * <code>java.util.Calendar</code>.<p>
     * 
     * The <code>start</code> and <code>end</code> parameters can be
     * <code>null</code> to indicate that the range doesn't have an
     * upper or lower bound.  If <code>value</code> or
     * <code>calendarField</code> is <code>null</code>, or if both 
     * <code>start</code> and <code>end</code> are specified and 
     * <code>mininum &gt; maximum</code> then an
     * <code>IllegalArgumentException</code> is thrown.
     * Similarly if <code>(minimum &lt;= value &lt;= maximum)</code> is false,
     * an IllegalArgumentException is thrown.<p>
     * 
     * <strong>This method has not been tested.</strong>
     * 
     * @param valueModel   a <code>Date</code> typed model that holds the spinner value
     * @param defaultDate  the date used if the valueModel's value is <code>null</code>
     * @param start the first date in the sequence or <code>null</code>
     * @param end the last date in the sequence or <code>null</code>
     * @param calendarField one of 
     *   <ul>
     *    <li><code>Calendar.ERA</code>
     *    <li><code>Calendar.YEAR</code>
     *    <li><code>Calendar.MONTH</code>
     *    <li><code>Calendar.WEEK_OF_YEAR</code>
     *    <li><code>Calendar.WEEK_OF_MONTH</code>
     *    <li><code>Calendar.DAY_OF_MONTH</code>
     *    <li><code>Calendar.DAY_OF_YEAR</code>
     *    <li><code>Calendar.DAY_OF_WEEK</code>
     *    <li><code>Calendar.DAY_OF_WEEK_IN_MONTH</code>
     *    <li><code>Calendar.AM_PM</code>
     *    <li><code>Calendar.HOUR</code>
     *    <li><code>Calendar.HOUR_OF_DAY</code>
     *    <li><code>Calendar.MINUTE</code>
     *    <li><code>Calendar.SECOND</code>
     *    <li><code>Calendar.MILLISECOND</code>
     *   </ul>
     * @return a <code>SpinnerDateModel</code> bound to the given 
     *     <code>valueModel</code> using the specified start and end dates
     *     and calendar field.
     * 
     * @throws NullPointerException       if the valueModel or defaultDate is <code>null</code>
     * @throws IllegalArgumentException   if <code>calendarField</code> isn't valid,
     *    or if the following expression is 
     *    false: <code>(start &lt;= value &lt;= end)</code>.
     * 
     * @see java.util.Calendar
     * @see Date
     */
    public static SpinnerDateModel createDateAdapter(
            ValueModel valueModel,
            Date defaultDate,
            Comparable start, Comparable end, int calendarField) {
        if (valueModel == null)
            throw new NullPointerException("The valueModel must not be null.");
        if (defaultDate == null)
            throw new NullPointerException("The default date must not be null.");
        
        Date valueModelDate = (Date) valueModel.getValue();
        Date initialDate = valueModelDate != null
            ? valueModelDate
            : defaultDate;
        SpinnerDateModel spinnerModel = new SpinnerDateModel(initialDate,
                start, end, calendarField);
        connect(spinnerModel, valueModel, defaultDate);
        return spinnerModel;
    }


    /**
     * Creates and returns a {@link SpinnerNumberModel} that is connected to 
     * the given {@link ValueModel} and that honors the specified minimum,
     * maximum and step values.
     * 
     * @param valueModel   an <code>Integer</code> typed model that holds the spinner value
     * @param defaultValue the number used if the valueModel's value is <code>null</code>
     * @param minValue     the lower bound of the spinner number
     * @param maxValue     the upper bound of the spinner number
     * @param stepSize     used to increment and decrement the current value
     * @return a <code>SpinnerNumberModel</code> that is connected to the given
     *     <code>ValueModel</code> 
     *     
     * @throws NullPointerException   if the valueModel is <code>null</code>
     */
    public static SpinnerNumberModel createNumberAdapter(
            ValueModel valueModel,
            int defaultValue,
            int minValue, int maxValue, int stepSize) {
        return createNumberAdapter(valueModel, new Integer(defaultValue), 
                new Integer(minValue), new Integer(maxValue), new Integer(stepSize));
    }


    /**
     * Creates and returns a {@link SpinnerNumberModel} that is connected to 
     * the given {@link ValueModel} and that honors the specified minimum,
     * maximum and step values.
     * 
     * @param valueModel   a <code>Number</code> typed model that holds the spinner value
     * @param defaultValue the number used if the valueModel's value is <code>null</code>
     * @param minValue     the lower bound of the spinner number
     * @param maxValue     the upper bound of the spinner number
     * @param stepSize     used to increment and decrement the current value
     * @return a <code>SpinnerNumberModel</code> that is connected to the given
     *     <code>ValueModel</code> 
     *     
     * @throws NullPointerException  if the valueModel or defaultValue is <code>null</code>
     */
    public static SpinnerNumberModel createNumberAdapter(
            ValueModel valueModel,
            Number defaultValue,
            Comparable minValue, Comparable maxValue, Number stepSize) {
        Number valueModelNumber = (Number) valueModel.getValue();
        Number initialValue = valueModelNumber != null
            ? valueModelNumber
            : defaultValue;
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(
                initialValue,
                minValue, maxValue, stepSize);
        connect(spinnerModel, valueModel, defaultValue);
        return spinnerModel;
    }


    // Connecting a ValueModel with a General SpinnerModel*********************

    /**
     * Connects the given ValueModel and SpinnerModel 
     * by synchronizing their values. 
     * 
     * @param spinnerModel  the underlying SpinnerModel implementation
     * @param valueModel    provides a value
     * @param defaultValue  the value used if the valueModel's value is <code>null</code>
     * @throws NullPointerException  
     *     if the spinnerModel, valueModel or defaultValue is <code>null</code>
     */
    public static void connect(SpinnerModel spinnerModel, ValueModel valueModel, Object defaultValue) {
        new SpinnerToValueModelConnector(spinnerModel, valueModel, defaultValue);
    }
}
