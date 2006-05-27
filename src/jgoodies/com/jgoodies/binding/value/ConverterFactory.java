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

package com.jgoodies.binding.value;

import java.text.Format;
import java.text.ParseException;

/**
 * A factory that vends ValueModels that convert types, for example 
 * Dates to Strings. More formally, a converting ValueModel <i>VM1</i> 
 * converts the type <i>T2</i> of an object being held as a value in 
 * one ValueModel <i>VM2</i> into another type <i>T1</i>. 
 * When reading a value from VM1, instances of T2 are read from VM2 
 * and are converted to T1. When storing a new value to VM1, 
 * the type converter will perform the inverse conversion and 
 * will convert an instance of T1 to T2.<p>
 * 
 * Type converters should be used judiciously and only to bridge two 
 * ValueModels. To bind non-Strings to a text UI component 
 * you should better use a {@link javax.swing.JFormattedTextField}. 
 * They provide a more powerful means to convert strings to objects 
 * and handle many cases that arise around invalid input. See also the classes
 * {@link com.jgoodies.binding.adapter.Bindings} and 
 * {@link com.jgoodies.binding.adapter.BasicComponentFactory} on how to 
 * bind ValueModels to formatted text fields.<p>
 * 
 * The inner converter implementations have a 'public' visibility
 * to enable reflection access.
 * 
 * @author  Karsten Lentzsch
 * @version $Revision: 1.4 $
 * 
 * @see     ValueModel
 * @see     Format
 * @see     javax.swing.JFormattedTextField
 */
public final class ConverterFactory {
    
    private ConverterFactory() {
        // Overrides default constructor; prevents instantiation.
    }
    
    // Factory Methods ********************************************************
    
    /**
     * Creates and returns a ValueModel that negates Booleans and leaves
     * null unchanged.<p>
     * 
     * <strong>Constraints:</strong> The subject is of type <code>Boolean</code>.
     * 
     * @param booleanSubject  a Boolean ValueModel
     * @return a ValueModel that inverts Booleans
     *
     * @throws NullPointerException if the subject is <code>null</code>
     */
    public static ValueModel createBooleanNegator(
            ValueModel booleanSubject) {
        return new BooleanNegator(booleanSubject);
    }
    
    
    /**
     * Creates and returns a ValueModel that converts Booleans
     * to the associated of the two specified strings, and vice versa.
     * Null values are mapped to an empty string.
     * Ignores cases when setting a text.<p>
     * 
     * <strong>Constraints:</strong> The subject is of type <code>Boolean</code>.
     * 
     * @param booleanSubject  a Boolean ValueModel
     * @param trueText      the text associated with <code>Boolean.TRUE</code>
     * @param falseText     the text associated with <code>Boolean.FALSE</code>
     *
     * @return a ValueModel that converts boolean to the associated text
     *
     * @throws NullPointerException if the subject, trueText or falseText
     *         is <code>null</code>
     * @throws IllegalArgumentException if the trueText equals the falseText
     */
    public static ValueModel createBooleanToStringConverter(
            ValueModel booleanSubject,
            String trueText, 
            String falseText) {
        return createBooleanToStringConverter(
                booleanSubject, 
                trueText, 
                falseText,
                "");
    }
    
    
    /**
     * Creates and returns a ValueModel that converts Booleans
     * to the associated of the two specified strings, and vice versa.
     * Null values are mapped to the specified text.
     * Ignores cases when setting a text.<p>
     * 
     * <strong>Constraints:</strong> The subject is of type <code>Boolean</code>.
     * 
     * @param booleanSubject  a Boolean ValueModel
     * @param trueText      the text associated with <code>Boolean.TRUE</code>
     * @param falseText     the text associated with <code>Boolean.FALSE</code>
     * @param nullText      the text associated with <code>null</code>
     *
     * @return a ValueModel that converts boolean to the associated text
     *
     * @throws NullPointerException if the subject, trueText, falseText
     *     or nullText is <code>null</code>
     * @throws IllegalArgumentException if the trueText equals the falseText
     */
    public static ValueModel createBooleanToStringConverter(
            ValueModel booleanSubject,
            String trueText, 
            String falseText,
            String nullText) {
        return new BooleanToStringConverter(booleanSubject, trueText, falseText, nullText);
    }
    
    
    /**
     * Creates and returns a ValueModel that converts Doubles using the
     * specified multiplier.<p>
     * 
     * Examples: multiplier=100, Double(1.23) -> Double(123), 
     * multiplier=1000, Double(1.23) -> Double(1230)<p>
     * 
     * <strong>Constraints:</strong> The subject is of type <code>Double</code>.
     * 
     * @param doubleSubject  a Double ValueModel
     * @param multiplier the multiplier used for the conversion
     * 
     * @return a ValueModel that converts Doubles using the specified multiplier
     *
     * @throws NullPointerException if the subject is <code>null</code>
     * 
     * @since 1.0.2
     */
    public static ValueModel createDoubleConverter(
            ValueModel doubleSubject, double multiplier) {
        return new DoubleConverter(doubleSubject, multiplier);
    }
       
    
    /**
     * Creates and returns a ValueModel that converts Doubles to Integer,
     * and vice versa.<p>
     * 
     * <strong>Constraints:</strong> The subject is of type <code>Double</code>.
     * 
     * @param doubleSubject  a Double ValueModel
     * 
     * @return a ValueModel that converts Doubles to Integer
     *
     * @throws NullPointerException if the subject is <code>null</code>
     */
    public static ValueModel createDoubleToIntegerConverter(
            ValueModel doubleSubject) {
        return createDoubleToIntegerConverter(doubleSubject, 1);
    }
       
    
    /**
     * Creates and returns a ValueModel that converts Doubles to Integer,
     * and vice versa. The multiplier can be used to convert Doubles 
     * to percent, permill, etc. For a percentage, set the multiplier to be 100, 
     * for a permill, set the multiplier to be 1000.<p>
     * 
     * Examples: multiplier=100, Double(1.23) -> Integer(123), 
     * multiplier=1000, Double(1.23) -> Integer(1230)<p>
     * 
     * <strong>Constraints:</strong> The subject is of type <code>Double</code>.
     * 
     * @param doubleSubject  a Double ValueModel
     * @param multiplier the multiplier used to convert the Double to Integer
     * 
     * @return a ValueModel that converts Doubles to Integer
     *
     * @throws NullPointerException if the subject is <code>null</code>
     */
    public static ValueModel createDoubleToIntegerConverter(
            ValueModel doubleSubject, int multiplier) {
        return new DoubleToIntegerConverter(doubleSubject, multiplier);
    }
       
    
    /**
     * Creates and returns a ValueModel that converts Floats using the
     * specified multiplier.<p>
     * 
     * Examples: multiplier=100, Float(1.23) -> Float(123), 
     * multiplier=1000, Float(1.23) -> Float(1230)<p>
     * 
     * <strong>Constraints:</strong> The subject is of type <code>Float</code>.
     * 
     * @param floatSubject  a Float ValueModel
     * @param multiplier the multiplier used for the conversion
     * 
     * @return a ValueModel that converts Float using the specified multiplier
     *
     * @throws NullPointerException if the subject is <code>null</code>
     * 
     * @since 1.0.2
     */
    public static ValueModel createFloatConverter(
            ValueModel floatSubject, float multiplier) {
        return new FloatConverter(floatSubject, multiplier);
    }
       
    
    /**
     * Creates and returns a ValueModel that converts Floats to Integer,
     * and vice versa.<p>
     * 
     * <strong>Constraints:</strong> The subject is of type <code>Float</code>.
     * s
     * @param floatSubject  a Float ValueModel
     * 
     * @return a ValueModel that converts Floats to Integer
     *
     * @throws NullPointerException if the subject is <code>null</code>
     */
    public static ValueModel createFloatToIntegerConverter(ValueModel floatSubject) {
        return createFloatToIntegerConverter(floatSubject, 1);
    }
       
    
    /**
     * Creates and returns a ValueModel that converts Floats to Integer,
     * and vice versa. The multiplier can be used to convert Floats 
     * to percent, permill, etc. For a percentage, set the multiplier to be 100, 
     * for a permill, set the multiplier to be 1000.<p>
     * 
     * <strong>Constraints:</strong> The subject is of type <code>Float</code>.
     * 
     * @param floatSubject  a Float ValueModel
     * @param multiplier the multiplier used to convert the Float to Integer
     * 
     * @return a ValueModel that converts Floats to Integer
     *
     * @throws NullPointerException if the subject is <code>null</code>
     */
    public static ValueModel createFloatToIntegerConverter(
            ValueModel floatSubject, int multiplier) {
        return new FloatToIntegerConverter(floatSubject, multiplier);
    }
       
    
    /**
     * Creates and returns a ValueModel that converts Integers using the
     * specified multiplier.<p>
     * 
     * Examples: multiplier=100, Integer(3) -> Integer(300), 
     * multiplier=1000, Integer(3) -> Integer(3000)<p>
     * 
     * <strong>Constraints:</strong> The subject is of type <code>Integer</code>.
     * 
     * @param integerSubject  a Integer ValueModel
     * @param multiplier the multiplier used for the conversion
     * 
     * @return a ValueModel that converts Integers using the specified multiplier
     *
     * @throws NullPointerException if the subject is <code>null</code>
     * 
     * @since 1.0.2
     */
    public static ValueModel createIntegerConverter(
            ValueModel integerSubject, double multiplier) {
        return new IntegerConverter(integerSubject, multiplier);
    }
       
    
    /**
     * Creates and returns a ValueModel that converts Long using the
     * specified multiplier.<p>
     * 
     * Examples: multiplier=100, Long(3) -> Long(300), 
     * multiplier=1000, Long(3) -> Long(3000)<p>
     * 
     * <strong>Constraints:</strong> The subject is of type <code>Long</code>.
     * 
     * @param longSubject  a Long ValueModel
     * @param multiplier the multiplier used for the conversion
     * 
     * @return a ValueModel that converts Longs using the specified multiplier
     *
     * @throws NullPointerException if the subject is <code>null</code>
     * 
     * @since 1.0.2
     */
    public static ValueModel createLongConverter(
            ValueModel longSubject, double multiplier) {
        return new LongConverter(longSubject, multiplier);
    }
       
    
    /**
     * Creates and returns a ValueModel that converts Longs to Integer 
     * and vice versa.<p>
     * 
     * <strong>Constraints:</strong> The subject is of type <code>Long</code>,
     * values written to the converter are of type <code>Integer</code>.
     * 
     * @param longSubject  a Long ValueModel
     * @return a ValueModel that converts Longs to Integer
     *
     * @throws NullPointerException if the subject is <code>null</code>
     */
    public static ValueModel createLongToIntegerConverter(ValueModel longSubject) {
        return createLongToIntegerConverter(longSubject, 1);
    }
       
    
    /**
     * Creates and returns a ValueModel that converts Longs to Integer 
     * and vice versa.<p>
     * 
     * <strong>Constraints:</strong> The subject is of type <code>Long</code>,
     * values written to the converter are of type <code>Integer</code>.
     * 
     * @param longSubject  a Long ValueModel
     * @param multiplier   used to multiply the Long when converting to Integer
     * @return a ValueModel that converts Longs to Integer
     *
     * @throws NullPointerException if the subject is <code>null</code>
     */
    public static ValueModel createLongToIntegerConverter(
            ValueModel longSubject, int multiplier) {
        return new LongToIntegerConverter(longSubject, multiplier);
    }
       
    
    /**
     * Creates and returns a ValueModel that converts objects to Strings
     * and vice versa. The conversion is performed by a <code>Format</code>.<p>
     * 
     * <strong>Constraints:</strong> The subject is of type <code>Object</code>; 
     * it must be formattable and parsable via the given <code>Format</code>.
     *
     * @param subject  the underlying ValueModel.
     * @param format   the <code>Format</code> used to format and parse
     *
     * @return a ValueModel that converts objects to Strings and vice versa
     *
     * @throws NullPointerException if the subject or the format
     *         is <code>null</code>
     */
    public static ValueModel createStringConverter(ValueModel subject, Format format) {
        return new StringConverter(subject, format);
    }
      
    
    // Converter Implementations **********************************************
    
    /**
     * Negates Booleans leaving null unchanged. Maps Boolean.TRUE 
     * to Boolean.FALSE, Boolean.FALSE to Boolean.TRUE, and null to null.
     */
    public static final class BooleanNegator extends AbstractConverter {
        
        BooleanNegator(ValueModel booleanSubject) {
            super(booleanSubject);
        }
        
        
        /**
         * Negates Booleans leaving null unchanged. 
         * Maps Boolean.TRUE to Boolean.FALSE,
         * Boolean.FALSE to Boolean.TRUE, and null to null.
         * 
         * @param subjectValue   the subject value to invert
         * @return the text that represents the subject value
         * 
         * @throws ClassCastException if the subject's value is not a Boolean
         */
        public Object convertFromSubject(Object subjectValue) {
            return negate(subjectValue);
        }
        
        
        /** 
         * Inverts the given Boolean and sets it as the subject's new value.
         * 
         * @param newValue the value to be inverted and set as new subject value
         * @throws ClassCastException if the new value is not a Boolean
         * @throws IllegalArgumentException if the new value does neither match
         *     the trueText nor the falseText
         */
        public void setValue(Object newValue) {
            subject.setValue(negate(newValue));
        }
        
        
        /**
         * Negates Booleans leaving null unchanged. 
         * Maps Boolean.TRUE to Boolean.FALSE ,
         * Boolean.FALSE to Boolean.TRUE, and null to null.
         * 
         * @param value   the value to invert
         * @return the inverted Boolean value, or null if value is null
         * 
         * @throws ClassCastException if the value is not a Boolean
         */
        private Boolean negate(Object value) {
            if (value == null)
                return null;
            else if (Boolean.TRUE.equals(value))
                return Boolean.FALSE;
            else if (Boolean.FALSE.equals(value))
                return Boolean.TRUE;
            else
                throw new ClassCastException("The value must be a Boolean.");
        }
        
    }
    
    
    /**
     * Converts Booleans to Strings and vice-versa using given texts for 
     * true, false, and null. Throws a ClassCastException if the value 
     * to convert is not a Boolean, or not a String for the reverse conversion. 
     */
    public static final class BooleanToStringConverter extends AbstractConverter {
        
        private final String trueText;
        private final String falseText;
        private final String nullText;
        
        BooleanToStringConverter(
                ValueModel booleanSubject, 
                String trueText, 
                String falseText,
                String nullText) {
            super(booleanSubject);
            if ((trueText == null) || (falseText == null) || (nullText == null)) {
                throw new NullPointerException("The trueText, falseText and nullText must not be null.");
            }
            if (trueText.equals(falseText)) {
                throw new IllegalArgumentException("The trueText and falseText must be different.");
            }
            this.trueText  = trueText;
            this.falseText = falseText;
            this.nullText  = nullText;
        }
    
        
        /**
         * Converts the subject value to associated text representation.
         * Rejects non-Boolean values.
         * 
         * @param subjectValue the subject's new value
         * @return the text that represents the subject value
         * 
         * @throws ClassCastException if the subject's value is not a Boolean
         */
        public Object convertFromSubject(Object subjectValue) {
            if (Boolean.TRUE.equals(subjectValue))
                return trueText;
            else if (Boolean.FALSE.equals(subjectValue))
                return falseText;
            else if (subjectValue == null)
                return nullText;
            else 
                throw new ClassCastException(
                "The subject value must be of type Boolean.");
        }
        
        
        /** 
         * Converts the given String and sets the associated Boolean as 
         * the subject's new value. In case the new value equals neither
         * this class' trueText, nor the falseText, nor the nullText,
         * an IllegalArgumentException is thrown.
         * 
         * @param newValue  the value to be converted and set as new subject value
         * @throws ClassCastException if the new value is not a String
         * @throws IllegalArgumentException if the new value does neither match
         *     the trueText nor the falseText nor the nullText
         */
        public void setValue(Object newValue) {
            if (!(newValue instanceof String))
                throw new ClassCastException(
                        "The new value must be a string.");
            
            String newString = (String) newValue;
            if (trueText.equalsIgnoreCase(newString)) {
                subject.setValue(Boolean.TRUE);
            } else if (falseText.equalsIgnoreCase(newString)) {
                subject.setValue(Boolean.FALSE);
            } else if (nullText.equalsIgnoreCase(newString)) {
                subject.setValue(null);
            } else 
                throw new IllegalArgumentException(
                        "The new value must be one of: "
                      + trueText + '/'
                      + falseText + '/'
                      + nullText);
        }
        
    }
    
    
    /**
     * Converts Doubles using a given multiplier.
     */
    public static final class DoubleConverter extends AbstractConverter {

        private final double multiplier;
        
        DoubleConverter(
                ValueModel doubleSubject, double multiplier) {
            super(doubleSubject);
            this.multiplier = multiplier;
        }
        
        /**
         * Converts the subject's value and returns a 
         * corresponding <code>Double</code> using the multiplier.
         *  
         * @param subjectValue  the subject's value
         * @return the converted subjectValue
         * @throws ClassCastException if the subject value is not of type
         *     <code>Double</code>
         */
        public Object convertFromSubject(Object subjectValue) {
            double doubleValue = ((Double) subjectValue).doubleValue();
            return new Double(doubleValue * multiplier);
        }
        
        /** 
         * Converts a <code>Double</code> using the multiplier 
         * and sets it as new value.
         * 
         * @param newValue  the <code>Double</code> object that shall be converted
         * @throws ClassCastException if the new value is not of type
         *     <code>Double</code>
         */
        public void setValue(Object newValue) {
            double doubleValue = ((Double) newValue).doubleValue();
            subject.setValue(new Double(doubleValue / multiplier));
        }
        
    }
    
    /**
     * Converts Doubles to Integers and vice-versa.
     */
    public static final class DoubleToIntegerConverter extends AbstractConverter {

        private final int multiplier;
        
        DoubleToIntegerConverter(
                ValueModel doubleSubject, int multiplier) {
            super(doubleSubject);
            this.multiplier = multiplier;
        }
        
        /**
         * Converts the subject's value and returns a 
         * corresponding <code>Integer</code> value using the multiplier.
         *  
         * @param subjectValue  the subject's value
         * @return the converted subjectValue
         * @throws ClassCastException if the subject value is not of type
         *     <code>Double</code>
         */
        public Object convertFromSubject(Object subjectValue) {
            double doubleValue = ((Double) subjectValue).doubleValue();
            if (multiplier != 1)
                doubleValue *= multiplier;
            return new Integer((int) Math.round(doubleValue));
        }
        
        /** 
         * Converts a <code>Double</code> using the multiplier 
         * and sets it as new value.
         * 
         * @param newValue  the <code>Integer</code> object that shall
         *     be converted
         * @throws ClassCastException if the new value is not of type
         *     <code>Integer</code>
         */
        public void setValue(Object newValue) {
            double doubleValue = ((Integer) newValue).doubleValue();
            if (multiplier != 1)
                doubleValue /= multiplier;
            subject.setValue(new Double(doubleValue));
        }
        
    }
    
    /**
     * Converts Floats using a given multiplier.
     */
    public static final class FloatConverter extends AbstractConverter {

        private final float multiplier;
        
        FloatConverter(
                ValueModel floatSubject, float multiplier) {
            super(floatSubject);
            this.multiplier = multiplier;
        }
        
        /**
         * Converts the subject's value and returns a 
         * corresponding <code>Float</code> using the multiplier.
         *  
         * @param subjectValue  the subject's value
         * @return the converted subjectValue
         * @throws ClassCastException if the subject value is not of type
         *     <code>Float</code>
         */
        public Object convertFromSubject(Object subjectValue) {
            float floatValue = ((Float) subjectValue).floatValue();
            return new Float(floatValue * multiplier);
        }
        
        /** 
         * Converts a <code>Float</code> using the multiplier 
         * and sets it as new value.
         * 
         * @param newValue  the <code>Float</code> object that shall be converted
         * @throws ClassCastException if the new value is not of type
         *     <code>Float</code>
         */
        public void setValue(Object newValue) {
            float floatValue = ((Float) newValue).floatValue();
            subject.setValue(new Float(floatValue / multiplier));
        }
        
    }
    
    /**
     * Converts Floats to Integers and vice-versa.
     */
    public static final class FloatToIntegerConverter extends AbstractConverter {

        private final int multiplier;
        
        FloatToIntegerConverter(
                ValueModel floatSubject, int multiplier) {
            super(floatSubject);
            this.multiplier = multiplier;
        }

        
        /**
         * Converts the subject's value and returns a 
         * corresponding <code>Integer</code> using the multiplier.
         *  
         * @param subjectValue  the subject's value
         * @return the converted subjectValue
         * @throws ClassCastException if the subject value is not of type
         *     <code>Float</code>
         */
        public Object convertFromSubject(Object subjectValue) {
            float floatValue = ((Float) subjectValue).floatValue();
            if (multiplier != 1)
                floatValue *= multiplier;
            return new Integer(Math.round(floatValue));
        }

        
        /** 
         * Converts a <code>Float</code> using the multiplier and 
         * sets it as new value.
         * 
         * @param newValue  the <code>Integer</code> object that shall be converted
         * @throws ClassCastException if the new value is not of type
         *     <code>Integer</code>
         */
        public void setValue(Object newValue) {
            float floatValue = ((Integer) newValue).floatValue();
            if (multiplier != 1)
                floatValue /= multiplier;
            subject.setValue(new Float(floatValue));
        }
        
    }

    
    /**
     * Converts Longs using a given multiplier.
     */
    public static final class LongConverter extends AbstractConverter {

        private final double multiplier;
        
        LongConverter(
                ValueModel longSubject, double multiplier) {
            super(longSubject);
            this.multiplier = multiplier;
        }
        
        /**
         * Converts the subject's value and returns a 
         * corresponding <code>Long</code> using the multiplier.
         *  
         * @param subjectValue  the subject's value
         * @return the converted subjectValue
         * @throws ClassCastException if the subject value is not of type
         *     <code>Long</code>
         */
        public Object convertFromSubject(Object subjectValue) {
            double doubleValue = ((Long) subjectValue).doubleValue();
            return new Long((long) (doubleValue * multiplier));
        }
        
        /** 
         * Converts a <code>Long</code> using the multiplier 
         * and sets it as new value.
         * 
         * @param newValue  the <code>Long</code> object that shall be converted
         * @throws ClassCastException if the new value is not of type
         *     <code>Long</code>
         */
        public void setValue(Object newValue) {
            double doubleValue = ((Long) newValue).doubleValue();
            subject.setValue(new Long((long) (doubleValue / multiplier)));
        }
        
    }
    
    /**
     * Converts Integers using a given multiplier.
     */
    public static final class IntegerConverter extends AbstractConverter {

        private final double multiplier;
        
        IntegerConverter(
                ValueModel integerSubject, double multiplier) {
            super(integerSubject);
            this.multiplier = multiplier;
        }
        
        /**
         * Converts the subject's value and returns a 
         * corresponding <code>Integer</code> using the multiplier.
         *  
         * @param subjectValue  the subject's value
         * @return the converted subjectValue
         * @throws ClassCastException if the subject value is not of type
         *     <code>Integer</code>
         */
        public Object convertFromSubject(Object subjectValue) {
            double doubleValue = ((Integer) subjectValue).doubleValue();
            return new Integer((int) (doubleValue * multiplier));
        }
        
        /** 
         * Converts a <code>Integer</code> using the multiplier 
         * and sets it as new value.
         * 
         * @param newValue  the <code>Integer</code> object that shall be converted
         * @throws ClassCastException if the new value is not of type
         *     <code>Integer</code>
         */
        public void setValue(Object newValue) {
            double doubleValue = ((Integer) newValue).doubleValue();
            subject.setValue(new Integer((int) (doubleValue / multiplier)));
        }
        
    }
    
    /**
     * Converts Longs to Integers and vice-versa.
     */
    public static final class LongToIntegerConverter extends AbstractConverter {

        private final int multiplier;
        
        LongToIntegerConverter(
                ValueModel longSubject, int multiplier) {
            super(longSubject);
            this.multiplier = multiplier;
        }
        
        /**
         * Converts the subject's value and returns a 
         * corresponding <code>Integer</code>.
         *  
         * @param subjectValue  the subject's value
         * @return the converted subjectValue
         * @throws ClassCastException if the subject value is not of type
         *     <code>Float</code>
         */
        public Object convertFromSubject(Object subjectValue) {
            int intValue = ((Long) subjectValue).intValue();
            if (multiplier != 1)
                intValue *= multiplier;
            return new Integer(intValue);
        }

        /** 
         * Converts an Integer to Long and sets it as new value.
         * 
         * @param newValue  the <code>Integer</code> object that represents
         *     the percent value
         * @throws ClassCastException if the new value is not of type
         *     <code>Integer</code>
         */
        public void setValue(Object newValue) {
            long longValue = ((Integer) newValue).longValue();
            if (multiplier != 1)
                longValue /= multiplier;
            subject.setValue(new Long(longValue));
        }
        
    }
    
    
    /**
     * Converts Values to Strings and vice-versa using a given Format.
     */
    public static final class StringConverter extends AbstractConverter {
        
        /**
         * Holds the <code>Format</code> used to format and parse.
         */
        private final Format format;
        
    
        // Instance Creation **************************************************
    
        /**
         * Constructs a <code>StringConverter</code> on the given 
         * subject using the specified <code>Format</code>.
         * 
         * @param subject  the underlying ValueModel.
         * @param format   the <code>Format</code> used to format and parse
         * @throws NullPointerException if the subject or the format is null.
         */
        StringConverter(ValueModel subject, Format format) {
            super(subject);
            if (format == null) {
                throw new NullPointerException("The format must not be null.");
            }
            this.format = format;
        }
        
        
        // Implementing Abstract Behavior *************************************
    
        /**
         * Formats the subject value and returns a String representation.
         *  
         * @param subjectValue  the subject's value
         * @return the formatted subjectValue
         */
        public Object convertFromSubject(Object subjectValue) {
            return format.format(subjectValue);
        }
       
    
        // Implementing ValueModel ********************************************
        
        /** 
         * Parses the given String encoding and sets it as the subject's 
         * new value. Silently catches <code>ParseException</code>.
         * 
         * @param value  the value to be converted and set as new subject value
         */
        public void setValue(Object value) {
            try {
                subject.setValue(format.parseObject((String) value));
            } catch (ParseException e) {
                // Do not change the subject value
            }
        }
        
    }
            
    
}
