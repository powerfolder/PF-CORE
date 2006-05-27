/*
 * Copyright (c) 2001-2004 JGoodies Karsten Lentzsch. All Rights Reserved.
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

package com.jgoodies.animation;

import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.SwingUtilities;

/**
 * Provides some behavior useful in the animation framework, 
 * or to implement custom animation functions and animations.
 *
 * @author  Karsten Lentzsch
 * @version $Revision: 1.3 $
 */
public final class AnimationUtils {


    private AnimationUtils() {
        // Override the default constructor; prevents instantiation.
    }

    
    /**
     * Invokes the given runnable when the specified animation stopped.
     * 
     * @param animation  the animation that is observed
     * @param runnable   the runnable that will be executed on animation stop
     */
    public static void invokeOnStop(Animation animation, Runnable runnable) {
        animation.addAnimationListener(new StopListener(runnable));
    }

    
    /**
     * Returns an array of strings by splitting a given text 
     * into tokens, that are separated by the  '|' character.
     * 
     * @param separatedTexts   a string that encodes a bunch of texts
     * separated by a | character
     * @return an array of the separated texts
     */
    public static String[] splitTexts(String separatedTexts) {
        StringTokenizer tokenizer = new StringTokenizer(separatedTexts, "|");
        List texts = new LinkedList();
        while (tokenizer.hasMoreTokens()) {
            texts.add(tokenizer.nextToken());
        }
        return (String[]) texts.toArray(new String[texts.size()]);
    }
    
    
    // Helper Code ***********************************************************

    // Performs a runnable at animation stop.
    private static class StopListener extends AnimationAdapter {

        private final Runnable runnable;

        private StopListener(Runnable runnable) {
            this.runnable = runnable;
        }

        public void animationStopped(AnimationEvent e) {
            SwingUtilities.invokeLater(runnable);
        }
    }

}