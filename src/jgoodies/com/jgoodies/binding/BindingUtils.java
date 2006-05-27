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

package com.jgoodies.binding;


/**
 * Consists exclusively of static methods that provide
 * convenience behavior used by the Binding classes.
 *
 * @author  Karsten Lentzsch
 * @version $Revision: 1.2 $
 */

public final class BindingUtils {
    
    
    private BindingUtils() {
        // Override default constructor; prevents instantiation.
    }
    
    
    /**
     * Checks and answers if the two objects are 
     * both <code>null</code> or equal.
     * 
     * <pre>
     * #equals(null, null)  == true
     * #equals("Hi", "Hi")  == true
     * #equals("Hi", null)  == false
     * #equals(null, "Hi")  == false
     * #equals("Hi", "Ho")  == false
     * </pre>
     * 
     * @param o1        the first object to compare
     * @param o2        the second object to compare
     * @return boolean  <code>true</code> if and only if 
     *    both objects are <code>null</code> or equal
     */
    public static boolean equals(Object o1, Object o2) {
        return    ((o1 != null) && (o2 != null) && (o1.equals(o2)))
               || ((o1 == null) && (o2 == null));
    }
    

    /**
     * Checks and answers if the given string is whitespace, 
     * empty (<code>""</code>) or <code>null</code>.
     * 
     * <pre>
     * #isBlank(null)  == true
     * #isBlank("")    == true
     * #isBlank(" ")   == true
     * #isBlank("Hi ") == false
     * </pre>
     * 
     * @param str   the string to check, may be <code>null</code>
     * @return <code>true</code> if the string is whitespace, empty 
     *    or <code>null</code>
     * @see #isEmpty(String)
     */
    public static boolean isBlank(String str) {
        return str == null || str.trim().length() == 0;
    }
    
    
    /**
     * Checks and answers if the given string is empty (<code>""</code>) 
     * or <code>null</code>.<p>
     * 
     * <pre>
     * #isEmpty(null)  == true
     * #isEmpty("")    == true
     * #isEmpty(" ")   == false
     * #isEmpty("Hi ") == false
     * </pre>
     * 
     * @param str   the string to check, may be <code>null</code>
     * @return <code>true</code> if the string is empty or <code>null</code>
     * @see #isBlank(String)
     */
    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }
    
}
