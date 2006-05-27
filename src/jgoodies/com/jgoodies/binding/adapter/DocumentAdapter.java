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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.*;

import com.jgoodies.binding.BindingUtils;
import com.jgoodies.binding.value.ValueModel;

/**
 * Converts the ValueModel interface into the Document interface, 
 * which is the model interface for Swing text components. 
 * It is used to bind String values to text components, 
 * for example a JTextField. At construction time the 
 * document is updated with the subject's contents.<p>
 * 
 * Instead of extending AbstractDocument or the specialized
 * PlainDocument this class holds a reference to a Document
 * instance and forwards all Document messages to the corresponding
 * method in the reference. By default the delegate is initialized as
 * an instance of PlainDocument; the two parameter constructor allows
 * to provide any other Document implementation. The latter can be useful
 * if the text component uses a custom Document, for example a custom
 * <code>IntegerDocument</code> or <code>MaskedDocument</code>.<p>
 * 
 * This DocumentAdapter provides limited support for handling
 * subject value modifications while updating the subject.
 * If a Document change initiates a subject value update, the subject
 * will be observed and a property change fired by the subject will be
 * handled - if any. In most cases, the subject will notify about a
 * change to the text that was just set by this DocumentAdapter.
 * However, in some cases the subject may decide to modify this text,
 * for example to ensure upper case characters. 
 * Since at this moment, this adapter's Document is still write-locked,
 * the Document update is performed later using 
 * <code>SwingUtilities#invokeLater</code>. 
 * <strong>Note:</strong> 
 * Such an update will typically change the Caret position in 
 * JTextField's and other JTextComponent's that use this DocumentAdapter 
 * as model. Hence, the subject value modifications can be used with
 * commit-on-focus-lost text components, but typically not with a
 * commit-on-key-typed component. For the latter case, you may consider
 * using a custom <code>DocumentFilter</code>.<p> 
 * 
 * <strong>Constraints:</strong> 
 * The ValueModel must be of type <code>String</code>.
 * <p>
 * <strong>Examples:</strong><pre>
 * ValueModel lastNameModel = new PropertyAdapter(customer, "lastName", true);
 * JTextField lastNameField = new JTextField();
 * lastNameField.setDocument(new DocumentAdapter(lastNameModel));
 * 
 * ValueModel codeModel = new PropertyAdapter(shipment, "code", true);
 * JTextField codeField = new JTextField();
 * codeField.setDocument(new DocumentAdapter(codeModel), 
 *                       new MaskedDocument(...));
 * </pre><p>
 * 
 * TODO: Consider changing the event source for DocumentEvents
 * from the delegate to this DocumentAdapter. DocumentListeners
 * may expect that the event source is the one they are registered with.
 * 
 * @author  Karsten Lentzsch
 * @version $Revision: 1.2 $
 * 
 * @see     ValueModel
 * @see     Document
 * @see     PlainDocument
 */
public final class DocumentAdapter implements Document, DocumentListener {

    /**
     * Holds the underlying ValueModel that is used to read values,
     * to update the document and to write values if the document changes.
     */
    private final ValueModel subject;
    
    /**
     * Holds a Document instance delegate that is used to forward
     * all Document messages of the outer adapter. By default it
     * is initialized as an instance of <code>PlainDocument</code>.
     */
    private final Document delegate;
    
    private final SubjectValueChangeHandler subjectValueChangeHandler;
    
    
    // Instance Creation ******************************************************
        
    /**
     * Constructs a DocumentAdapter on the specified String-typed 
     * subject ValueModel. Doesn't filter newline characters.
     * 
     * @param subject   the underlying String typed ValueModel 
     * 
     * @throws NullPointerException  if the subject is <code>null</code>
     */
    public DocumentAdapter(ValueModel subject) {
        this(subject, new PlainDocument(), false);
    }
    
    
    /**
     * Constructs a DocumentAdapter on the specified String-typed 
     * subject ValueModel. The boolean parameter specifies if
     * newline characters shall be filtered or retained.
     * 
     * @param subject         the underlying String typed ValueModel 
     * @param filterNewlines  <code>true</code> to filter newline characters, 
     *     <code>false</code> to retain them
     *     
     * @throws NullPointerException  if the subject is <code>null</code>
     */
    public DocumentAdapter(ValueModel subject, boolean filterNewlines) {
        this(subject, new PlainDocument(), filterNewlines);
    }
    
    
    /**
     * Constructs a DocumentAdapter on the specified String-typed 
     * subject ValueModel. Doesn't filter newline characters.
     * 
     * @param subject   the underlying String-typed ValueModel
     * @param document  the underlying Document implementation
     * 
     * @throws NullPointerException  
     *     if the subject or document is <code>null</code>
     */
    public DocumentAdapter(ValueModel subject, Document document) {
        this(subject, document, false);
    }
    
    
    /**
     * Constructs a DocumentAdapter on the specified subject.
     * The subject must return values of type String. 
     * The boolean parameter specifies if newline characters 
     * shall be filtered or retained.
     * 
     * @param subject   the underlying String typed ValueModel
     * @param document  the underlying Document implementation
     * @param filterNewlines  <code>true</code> to filter newline characters, 
     *     <code>false</code> to retain them
     *     
     * @throws NullPointerException  
     *     if the subject or document is <code>null</code>
     */
    public DocumentAdapter(ValueModel subject, Document document, boolean filterNewlines) {
        if (subject == null)
            throw new NullPointerException("The subject must not be null.");
        if (document == null)
            throw new NullPointerException("The document must not be null.");
        this.subject  = subject;
        this.delegate = document;
        document.putProperty("filterNewlines", Boolean.valueOf(filterNewlines));
        this.subjectValueChangeHandler = new SubjectValueChangeHandler();
        document.addDocumentListener(this);
        subject.addValueChangeListener(subjectValueChangeHandler);
        setDocumentTextSilently(getSubjectText());
    }
    

    // Synchronization ********************************************************
    
    /**
     * Reads the current text from the document and sets it as new
     * value of the subject.
     */
    private void updateSubject() {
        setSubjectText(getDocumentText());
    }
    
    /**
     * Returns the text contained in the document.
     *
     * @return the text contained in the document
     */
    private String getDocumentText() {
        int length = delegate.getLength();
        try {
            return delegate.getText(0, length);
        } catch (BadLocationException e) {
            e.printStackTrace();
            return "";
        }
    }
        
    /**
     * Sets the document contents without notifying the subject of changes.
     * Invoked by the subject change listener. Removes the existing text first,
     * then inserts the new text; therefore a BadLocationException should not
     * happen. In case the delegate is an <code>AbstractDocument</code> 
     * the text is replaced instead of a combined remove plus insert.
     * 
     * @param newText  the text to be set in the document
     */
    private void setDocumentTextSilently(String newText) {
        delegate.removeDocumentListener(this);
        try {
            if (delegate instanceof AbstractDocument) {
                ((AbstractDocument) delegate).
                        replace(0, delegate.getLength(), newText, null);
            } else {
                delegate.remove(0, delegate.getLength());
                delegate.insertString(0, newText, null);
            }
        } catch (BadLocationException e) {
            // Should not happen in the way we invoke #remove and #insertString
        }
        delegate.addDocumentListener(this);
    }
        
    /**
     * Returns the subject's text value.
     * 
     * @return the subject's text value
     * @throws ClassCastException   if the subject value is not a String
     */
    private String getSubjectText() {
        String str = (String) subject.getValue();
        return str == null ? "" : str;
    }
        
    /**
     * Sets the given text as new subject value. Since the subject may modify 
     * this text, we cannot update silently, i.e. we cannot remove and add 
     * the subjectValueChangeHandler before/after the update. Since this
     * change is invoked during a Document write operation, the document
     * is write-locked and so, we cannot modify the document before all 
     * document listeners have been notified about the change.<p>
     * 
     * Therefore we listen to subject changes and defer any document changes
     * using <code>SwingUtilities.invokeLater</code>. This mode is activated
     * by setting the subject change handler's <code>updateLater</code> to true. 
     * 
     * @param newText   the text to be set in the subject
     */
    private void setSubjectText(String newText) {
        subjectValueChangeHandler.setUpdateLater(true);
        subject.setValue(newText);
        subjectValueChangeHandler.setUpdateLater(false);
    }
    
        
    // DocumentListener Implementation ****************************************
        
    /**
     * There was an insert into the document; update the subject.
     *
     * @param e the document event
     */
    public void insertUpdate(DocumentEvent e) {
        updateSubject();
    }
    
    /**
     * A portion of the document has been removed; update the subject.
     *
     * @param e the document event
     */
    public void removeUpdate(DocumentEvent e) {
        updateSubject();
    }
    
    /**
     * An attribute or set of attributes has changed; do nothing.
     *
     * @param e the document event
     */
    public void changedUpdate(DocumentEvent e) {
        // Do nothing on attribute changes.
    }
        
    
    // Document Implementation / Forwarding Methods ***************************
    
    /**
     * Returns number of characters of content currently 
     * in the document.
     *
     * @return number of characters >= 0
     */
    public int getLength() {
        return delegate.getLength();
    }
    
    /**
     * Registers the given observer to begin receiving notifications
     * when changes are made to the document.
     *
     * @param listener the observer to register
     * @see Document#removeDocumentListener(javax.swing.event.DocumentListener)
     */
    public void addDocumentListener(DocumentListener listener) {
        delegate.addDocumentListener(listener);
    }
    
    /**
     * Unregisters the given observer from the notification list
     * so it will no longer receive change updates.  
     *
     * @param listener the observer to register
     * @see Document#addDocumentListener(javax.swing.event.DocumentListener)
     */
    public void removeDocumentListener(DocumentListener listener) {
        delegate.removeDocumentListener(listener);
    }
    
    /**
     * Registers the given observer to begin receiving notifications
     * when undoable edits are made to the document.
     *
     * @param listener the observer to register
     * @see javax.swing.event.UndoableEditEvent
     */
    public void addUndoableEditListener(UndoableEditListener listener) {
        delegate.addUndoableEditListener(listener);
    }
    
    /**
     * Unregisters the given observer from the notification list
     * so it will no longer receive updates.
     *
     * @param listener the observer to register
     * @see javax.swing.event.UndoableEditEvent
     */
    public void removeUndoableEditListener(UndoableEditListener listener) {
        delegate.removeUndoableEditListener(listener);
    }
    
    /**
     * Gets the properties associated with the document.
     *
     * @param key a non-<code>null</code> property key
     * @return the properties
     * @see #putProperty(Object, Object)
     */
    public Object getProperty(Object key) {
        return delegate.getProperty(key);
    }
    
    /**
     * Associates a property with the document.  Two standard 
     * property keys provided are: <a href="#StreamDescriptionProperty">
     * <code>StreamDescriptionProperty</code></a> and
     * <a href="#TitleProperty"><code>TitleProperty</code></a>.
     * Other properties, such as author, may also be defined.
     *
     * @param key the non-<code>null</code> property key
     * @param value the property value
     * @see #getProperty(Object)
     */
    public void putProperty(Object key, Object value) {
        delegate.putProperty(key, value);
    }
    
    /**
     * Removes a portion of the content of the document.  
     * This will cause a DocumentEvent of type 
     * DocumentEvent.EventType.REMOVE to be sent to the 
     * registered DocumentListeners, unless an exception
     * is thrown.  The notification will be sent to the
     * listeners by calling the removeUpdate method on the
     * DocumentListeners.
     * <p>
     * To ensure reasonable behavior in the face 
     * of concurrency, the event is dispatched after the 
     * mutation has occurred. This means that by the time a 
     * notification of removal is dispatched, the document
     * has already been updated and any marks created by
     * <code>createPosition</code> have already changed.
     * For a removal, the end of the removal range is collapsed 
     * down to the start of the range, and any marks in the removal 
     * range are collapsed down to the start of the range.
     * <p align=center><img src="doc-files/Document-remove.gif"
     *  alt="Diagram shows removal of 'quick' from 'The quick brown fox.'">
     * <p>
     * If the Document structure changed as result of the removal,
     * the details of what Elements were inserted and removed in
     * response to the change will also be contained in the generated
     * DocumentEvent. It is up to the implementation of a Document
     * to decide how the structure should change in response to a
     * remove.
     * <p>
     * If the Document supports undo/redo, an UndoableEditEvent will
     * also be generated.  
     *
     * @param offs  the offset from the beginning >= 0
     * @param len   the number of characters to remove >= 0
     * @exception BadLocationException  some portion of the removal range
     *   was not a valid part of the document.  The location in the exception
     *   is the first bad position encountered.
     * @see javax.swing.event.DocumentEvent
     * @see javax.swing.event.DocumentListener
     * @see javax.swing.event.UndoableEditEvent
     * @see javax.swing.event.UndoableEditListener
     */
    public void remove(int offs, int len) throws BadLocationException {
        delegate.remove(offs, len);
    }
    
    /**
     * Inserts a string of content.  This will cause a DocumentEvent
     * of type DocumentEvent.EventType.INSERT to be sent to the
     * registered DocumentListers, unless an exception is thrown.
     * The DocumentEvent will be delivered by calling the
     * insertUpdate method on the DocumentListener.
     * The offset and length of the generated DocumentEvent
     * will indicate what change was actually made to the Document.
     * <p align=center><img src="doc-files/Document-insert.gif"
     *  alt="Diagram shows insertion of 'quick' in 'The quick brown fox'">
     * <p>
     * If the Document structure changed as result of the insertion,
     * the details of what Elements were inserted and removed in
     * response to the change will also be contained in the generated
     * DocumentEvent.  It is up to the implementation of a Document
     * to decide how the structure should change in response to an
     * insertion.
     * <p>
     * If the Document supports undo/redo, an UndoableEditEvent will
     * also be generated.  
     *
     * @param offset  the offset into the document to insert the content >= 0.
     *    All positions that track change at or after the given location 
     *    will move.  
     * @param str    the string to insert
     * @param a      the attributes to associate with the inserted
     *   content.  This may be null if there are no attributes.
     * @exception BadLocationException  the given insert position is not a valid 
     * position within the document
     * @see javax.swing.event.DocumentEvent
     * @see javax.swing.event.DocumentListener
     * @see javax.swing.event.UndoableEditEvent
     * @see javax.swing.event.UndoableEditListener
     */
    public void insertString(int offset, String str, AttributeSet a) throws BadLocationException {
        delegate.insertString(offset, str, a);
    }
    
    /**
     * Fetches the text contained within the given portion 
     * of the document.
     *
     * @param offset  the offset into the document representing the desired 
     *   start of the text >= 0
     * @param length  the length of the desired string >= 0
     * @return the text, in a String of length >= 0
     * @exception BadLocationException  some portion of the given range
     *   was not a valid part of the document.  The location in the exception
     *   is the first bad position encountered.
     */
    public String getText(int offset, int length) throws BadLocationException {
        return delegate.getText(offset, length);
    }
    
    /**
     * Fetches the text contained within the given portion 
     * of the document.
     * <p>
     * If the partialReturn property on the txt parameter is false, the
     * data returned in the Segment will be the entire length requested and
     * may or may not be a copy depending upon how the data was stored.
     * If the partialReturn property is true, only the amount of text that
     * can be returned without creating a copy is returned.  Using partial
     * returns will give better performance for situations where large 
     * parts of the document are being scanned.  The following is an example
     * of using the partial return to access the entire document:
     * <p>
     * <pre><code>
     *
     * &nbsp; int nleft = doc.getDocumentLength();
     * &nbsp; Segment text = new Segment();
     * &nbsp; int offs = 0;
     * &nbsp; text.setPartialReturn(true);   
     * &nbsp; while (nleft > 0) {
     * &nbsp;     doc.getText(offs, nleft, text);
     * &nbsp;     // do someting with text
     * &nbsp;     nleft -= text.count;
     * &nbsp;     offs += text.count;
     * &nbsp; }
     *
     * </code></pre>
     *
     * @param offset  the offset into the document representing the desired 
     *   start of the text >= 0
     * @param length  the length of the desired string >= 0
     * @param txt the Segment object to return the text in
     *
     * @exception BadLocationException  Some portion of the given range
     *   was not a valid part of the document.  The location in the exception
     *   is the first bad position encountered.
     */
    public void getText(int offset, int length, Segment txt) throws BadLocationException {
        delegate.getText(offset, length, txt);
    }
    
    /**
     * Returns a position that represents the start of the document.  The 
     * position returned can be counted on to track change and stay 
     * located at the beginning of the document.
     *
     * @return the position
     */
    public Position getStartPosition() {
        return delegate.getStartPosition();
    }
        
    /**
     * Returns a position that represents the end of the document.  The
     * position returned can be counted on to track change and stay 
     * located at the end of the document.
     *
     * @return the position
     */
    public Position getEndPosition() {
        return delegate.getEndPosition();
    }
    
    /**
     * This method allows an application to mark a place in
     * a sequence of character content. This mark can then be 
     * used to tracks change as insertions and removals are made 
     * in the content. The policy is that insertions always
     * occur prior to the current position (the most common case) 
     * unless the insertion location is zero, in which case the 
     * insertion is forced to a position that follows the
     * original position. 
     *
     * @param offs  the offset from the start of the document >= 0
     * @return the position
     * @exception BadLocationException  if the given position does not
     *   represent a valid location in the associated document
     */
    public Position createPosition(int offs) throws BadLocationException {
        return delegate.createPosition(offs);
    }
    
    /**
     * Returns all of the root elements that are defined.
     * <p>
     * Typically there will be only one document structure, but the interface
     * supports building an arbitrary number of structural projections over the 
     * text data. The document can have multiple root elements to support 
     * multiple document structures.  Some examples might be:
     * </p>
     * <ul>
     * <li>Text direction.
     * <li>Lexical token streams.
     * <li>Parse trees.
     * <li>Conversions to formats other than the native format.
     * <li>Modification specifications.
     * <li>Annotations.
     * </ul>
     *
     * @return the root element
     */
    public Element[] getRootElements() {
        return delegate.getRootElements();
    }
    
    /**
     * Returns the root element that views should be based upon,
     * unless some other mechanism for assigning views to element
     * structures is provided.
     *
     * @return the root element
     */
    public Element getDefaultRootElement() {
        return delegate.getDefaultRootElement();
    }
    
    /**
     * This allows the model to be safely rendered in the presence
     * of currency, if the model supports being updated asynchronously.
     * The given runnable will be executed in a way that allows it
     * to safely read the model with no changes while the runnable
     * is being executed.  The runnable itself may <em>not</em>
     * make any mutations.  
     *
     * @param r a Runnable used to render the model
     */
    public void render(Runnable r) {
        delegate.render(r);
    }

    
    // Event Handling *********************************************************
    
    /**
     * Handles changes in the subject value and updates this document 
     * - if necessary.<p>
     * 
     * Document changes update the subject text and result in a subject
     * property change. Most of these changes will just reflect the
     * former subject change. However, in some cases the subject may
     * modify the text set, for example to ensure upper case characters.
     * This method reduces the number of document updates by checking
     * the old and new text. If the old and new text are equal or 
     * both null, this method does nothing.<p>
     * 
     * Since subject changes as a result of a document change may not
     * modify the write-locked document immediately, we defer the update
     * if necessary using <code>SwingUtilities.invokeLater</code>.<p>
     * 
     * See the DocumentAdapter's JavaDoc class comment for the limitations
     * of the deferred document change.
     */
    private class SubjectValueChangeHandler implements PropertyChangeListener {
        
        private boolean updateLater;
    
        void setUpdateLater(boolean updateLater) {
            this.updateLater = updateLater;
        }
        
        /**
         * The subject value has changed; updates the document immediately
         * or later - depending on the <code>updateLater</code> state.
         * 
         * @param evt   the event to handle
         */
        public void propertyChange(PropertyChangeEvent evt) {
            final String oldText = getDocumentText();
            final Object newValue = evt.getNewValue();
            final String newText = newValue == null
                ? getSubjectText()
                : (String) newValue;
            if (BindingUtils.equals(oldText, newText)) 
                return;
            
            if (updateLater) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        setDocumentTextSilently(newText);
                    }
                });
            } else {
                setDocumentTextSilently(newText);
            }
        }
        
    }
        
}
