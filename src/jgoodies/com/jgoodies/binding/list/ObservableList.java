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

package com.jgoodies.binding.list;
 
import java.util.List;

import javax.swing.ListModel;
 
/**
 * Combines the {@link List} and {@link ListModel} interfaces.
 * Implementations can be used to bind lists to list-based
 * user interface components like <code>JList</code>, <code>JTable</code>
 * and <code>JComboBox</code>.<p>
 * 
 * The JGoodies Data Binding ships with two predefined implementations:
 * <code>ArrayListModel</code> and <code>LinkedListModel</code>.<p>
 * 
 * See also the class comment in {@link SelectionInList} that discusses
 * the advantages you gain if you add <code>ListModel</code> capabilities
 * to a <code>List</code>.<p>
 * 
 * TODO: Check if this type is really necessary; remove it if obsolete.
 * Without doubt the ObservableList implementations are really useful,
 * among others the predefined ArrayListModel and LinkedListModel.
 * It's just that these implementations are typically used as List
 * and exposed as ListModel, and so there may be no need for the ObservableList
 * interface.
 *
 * @author Karsten Lentzsch
 * @version $Revision: 1.2 $
 * 
 * @see ArrayListModel
 * @see LinkedListModel
 * @see SelectionInList
 */
public interface ObservableList extends List, ListModel {
        
    // This interface just combines List and ListModel and 
    // doesn't add anything new.

}
