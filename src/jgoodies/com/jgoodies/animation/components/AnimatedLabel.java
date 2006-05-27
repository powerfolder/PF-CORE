/*
 * Copyright (c) 2001-2004 JGoodies Karsten Lentzsch. All Rights Reserved.
 *
 * This software is the proprietary information of Karsten Lentzsch.  
 * Use is subject to license terms.
 *
 */

package com.jgoodies.animation.components;

import java.awt.*;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.plaf.FontUIResource;

import com.jgoodies.animation.AbstractAnimation;
import com.jgoodies.animation.Animation;
import com.jgoodies.animation.AnimationAdapter;
import com.jgoodies.animation.AnimationEvent;
import com.jgoodies.animation.Animator;

/**
 * An anti-aliased text label that can animate text changes
 * using a blend over effect.<p>
 * 
 * <strong>Note: This is preview code that is not supported.
 * It is more raw than other classes that you have downloaded
 * from JGoodies.com in the past and contains known bugs.</strong>
 *
 * @author Karsten Lentzsch
 * @version $Revision: 1.4 $
 */

public final class AnimatedLabel extends JPanel {

    public static final int RIGHT = SwingConstants.RIGHT;
    public static final int CENTER = SwingConstants.CENTER;
    public static final int LEFT = SwingConstants.LEFT;

    public static final Color DEFAULT_BASE_COLOR = new Color(64, 64, 64);
    public static final int DEFAULT_FONT_EXTRA_SIZE = 8;

    private static final int DEFAULT_DURATION = 300;
    private static final int DEFAULT_ANIMATION_FPS = 30;

    private JLabel[] labels;
    private int foreground = 0;
    private int background = 1;

    private Color baseColor;
    private boolean animationEnabled;
    private int orientation;
    private long duration;
    private int fps;
    private Animation animation;

    // Instance Creation ****************************************************

    /**
     * Constructs an <code>AnimatedLabel</code> with the given properties.
     * 
     * @param baseColor      the color used as a basis for the text color
     * @param fontExtraSize  pixels that are added to the dialog font size
     * @param text           the initial text to be displayed
     * @param orientation    the label's orientation
     * @param duration       the duration of the blend over animation
     * @param frames_per_second  the blend over animation's frame rate
     */
    public AnimatedLabel(
        Color baseColor,
        int fontExtraSize,
        String text,
        int orientation,
        int duration,
        int frames_per_second) {
        this.baseColor = baseColor;
        this.orientation = orientation;
        this.duration = duration;
        this.fps = frames_per_second;
        this.animationEnabled = true;
        initComponents(fontExtraSize);
        build();
        setTextImmediately(text);
    }

    /**
     * Constructs an <code>AnimatedLabel</code> with the given initial text
     * and orientation.
     * 
     * @param baseColor      the color used as a basis for the text color
     * @param fontExtraSize  pixels that are added to the dialog font size
     * @param text           the initial text to be displayed
     * @param orientation    the label's orientation
     */
    public AnimatedLabel(
        Color baseColor,
        int fontExtraSize,
        String text,
        int orientation) {
        this(
            baseColor,
            fontExtraSize,
            text,
            orientation,
            DEFAULT_DURATION,
            DEFAULT_ANIMATION_FPS);
    }

    /**
     * Constructs an <code>AnimatedLabel</code> with the given initial text
     * using a left oriented label.
     * 
     * @param baseColor      the color used as a basis for the text color
     * @param fontExtraSize  pixels that are added to the dialog font size
     * @param text           the initial text to be displayed
     */
    public AnimatedLabel(Color baseColor, int fontExtraSize, String text) {
        this(baseColor, fontExtraSize, text, LEFT);
    }

    /**
     * Constructs an <code>AnimatedLabel</code> with default base color,
     * default font extra size, and an empty text.
     */
    public AnimatedLabel() {
        this(DEFAULT_BASE_COLOR, DEFAULT_FONT_EXTRA_SIZE, "");
    }

    // Public API ***********************************************************

    /**
     * Returns whether the animation is currently enabled.
     * 
     * @return true if the animation is enabled, false if disabled
     */
    public boolean isAnimationEnabled() {
        return animationEnabled;
    }

    /**
     * Enables or disables the blend over effect. This can be useful in 
     * environments with a poor rendering performance or if the user disables
     * all kinds of animations. You can still use this class but enable and
     * disable the animations.
     * 
     * @param enable true to enable the blend over effect, false to disable it
     */
    public void setAnimationEnabled(boolean enable) {
        boolean oldValue = animationEnabled;
        animationEnabled = enable;
        firePropertyChange("animationEnabled", oldValue, enable);
    }

    /**
     * Returns the label's foreground base color.
     * 
     * @return this label's foreground base color
     */
    public Color getForeground() {
        return baseColor;
    }

    /**
     * Sets a new foreground base color.
     * 
     * @param newForeground   the color to be set as new foreground base color
     */
    public void setForeground(Color newForeground) {
        Color oldForeground = getForeground();
        baseColor = newForeground;
        firePropertyChange("foreground", oldForeground, newForeground);
    }

    /**
     * Returns the duration of the blend over animation.
     * 
     * @return the duration of the blend over animaton
     */
    public long getDuration() {
        return duration;
    }

    /**
     * Sets the animation's duration and invalidates the animation cache.
     * 
     * @param newDuration   the duration to be set
     */
    public void setDuration(int newDuration) {
        long oldDuration = duration;
        duration = newDuration;
        animation = null;
        firePropertyChange("duration", oldDuration, newDuration);
    }

    // Animation Creation ***************************************************

    /**
     * Returns the text of the foreground label.
     * 
     * @return the text of the foreground label
     */
    public synchronized String getText() {
        return labels[foreground].getText();
    }

    /**
     * Sets a new text. If the animation is disabled the text will
     * be set immediately otherwise a blend over animation is used.
     * 
     * @param newText    the new text to be displayed
     */
    public synchronized void setText(String newText) {
        if (!isAnimationEnabled()) {
            setTextImmediately(newText);
            return;
        } 
        
        String oldText = getText();
        labels[background].setText(newText);
        foreground = 1 - foreground;
        background = 1 - background;
        new Animator(animation(), fps).start();
        firePropertyChange("text", oldText, newText);
    }

    /**
     * Sets a new text without using the blend over animation.
     * 
     * @param newText  the text to be set
     */
    public void setTextImmediately(String newText) {
        String oldText = getText();
        labels[background].setText(newText);
        foreground = 1 - foreground;
        background = 1 - background;
        setAlpha(255, 0);
        firePropertyChange("text", oldText, newText);
    }

    /**
     * Lazily creates and returns the blend over animation.
     * 
     * @return the lazily created blend over animation
     */
    private Animation animation() {
        if (animation == null) {
            animation = new BlendOverAnimation(duration);
            animation.addAnimationListener(new AnimationAdapter() {
                public void animationStopped(AnimationEvent e) {
                    setAlpha(255, 0);
                }
            });
        }
        return animation;
    }

    // Building *************************************************************

    /**
     * Creates and configures the UI components. The label's size is specified
     * using an <code>fontExtraSize</code> that is a delta in pixel to the 
     * dialog font size. 
     * 
     * @param fontExtraSize   the pixel size delta for the label sizes 
     */
    private void initComponents(int fontExtraSize) {
        labels = new JLabel[2];
        labels[foreground] =
            createBoldLabel(fontExtraSize, getTranslucentColor(255));
        labels[background] =
            createBoldLabel(fontExtraSize, getTranslucentColor(255));
    }

    private void build() {
        setOpaque(false);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = anchor();
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(labels[foreground], gbc);
        add(labels[background], gbc);
    }

    private int anchor() {
        if (orientation == RIGHT) {
            return GridBagConstraints.EAST;
        } else if (orientation == CENTER) {
            return GridBagConstraints.CENTER;
        } else {
            return GridBagConstraints.WEST;
        }
    }

    /**
     * Creates and returns an anti-aliased label with a bold font for the 
     * specified size increment and foreground color.
     * 
     * @param sizeIncrement    a size delta in pixels relative to 
     *     the dialog font size
     * @param aForeground      the label's foreground base color
     * @return a bold anti aliased label
     */
    private JLabel createBoldLabel(int sizeIncrement, Color aForeground) {
        JLabel label = new AntiAliasedLabel("", Font.BOLD, sizeIncrement);
        label.setForeground(aForeground);
        return label;
    }

    // Helper Methods *******************************************************

    /**
     * Creates and returns a translucent color with the label's base color.
     * 
     * @param alpha   the current alpha value
     * @return a translucent color with the given alpha based on this label's
     *     foreground base color.
     */
    private Color getTranslucentColor(int alpha) {
        return new Color(
            baseColor.getRed(),
            baseColor.getGreen(),
            baseColor.getBlue(),
            alpha);
    }

    /**
     * Sets the foreground and background colors using the given alpha values.
     * 
     * @param foregroundAlpha       alpha value for the foreground label
     * @param backgroundAlpha       alpha value for the background label
     */
    private void setAlpha0(int foregroundAlpha, int backgroundAlpha) {
        labels[foreground].setForeground(getTranslucentColor(foregroundAlpha));
        labels[background].setForeground(getTranslucentColor(backgroundAlpha));
    }

    /**
     * Sets the foreground and background colors in the event dispatch thread.
     *
     * @param foregroundAlpha       alpha value for the foreground label
     * @param backgroundAlpha       alpha value for the background label
     */
    private void setAlpha(
        final int foregroundAlpha,
        final int backgroundAlpha) {
        if (SwingUtilities.isEventDispatchThread()) {
            setAlpha0(foregroundAlpha, backgroundAlpha);
            return;
        }
        Runnable runnable = new Runnable() {
            public void run() {
                setAlpha0(foregroundAlpha, backgroundAlpha);
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    // Animation Class ******************************************************

    /**
     * An animation that changes the colors of overlapping labels
     * to implement a blend over effect.
     */
    private class BlendOverAnimation extends AbstractAnimation {

        /**
         * Constructs an animation that changes the colors of the
         * prefix and suffix labels over the duration.
         * 
         * @param duration   the animation's duration
         */
        public BlendOverAnimation(long duration) {
            super(duration, true);
        }

        /**
         * Applies the effect: sets the text and time. 
         * 
         * @param time   the time point used to apply the effect
         */
        protected void applyEffect(long time) {
            int foregroundAlpha = (int) (255 * time / duration());
            int backgroundAlpha = 255 - foregroundAlpha;
            setAlpha(foregroundAlpha, backgroundAlpha);
        }
    }

    // Helper Class ***********************************************************

    private static class AntiAliasedLabel extends JLabel {

        private final int fontExtraSize;
        private final int fontStyle;

        /**
         * Constructs an <code>AntiAliasedLabel</code> for the given text,
         * font style and font extra size.
         * 
         * @param text   the label's initial text
         * @param fontStyle   the font style attribute
         * @param fontExtraSize  a size delta in pixel relative to the dialog
         *     font size in pixels
         */
        private AntiAliasedLabel(
            String text,
            int fontStyle,
            int fontExtraSize) {
            super(text);
            this.fontStyle = fontStyle;
            this.fontExtraSize = fontExtraSize;
            updateUI();
        }

        /**
         * Switches anti-aliasing on, paints, and switches it off again.
         * 
         * @param g   the Graphics object to render on
         */
        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            Object oldHint =
                g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
            g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            super.paint(g2);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldHint);
        }

        /**
         * Restores the font after the UI has changed.
         */
        public void updateUI() {
            super.updateUI();
            Font font = getFont();
            if (0 == fontExtraSize) {
                if (font.getStyle() != fontStyle)
                    setFont(new FontUIResource(font.deriveFont(fontStyle)));
            } else
                setFont(
                    new FontUIResource(
                        new Font(
                            font.getName(),
                            fontStyle,
                            font.getSize() + fontExtraSize)));
        }
    }

}