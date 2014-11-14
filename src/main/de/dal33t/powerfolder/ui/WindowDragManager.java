package de.dal33t.powerfolder.ui;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.Timer;

import de.dal33t.powerfolder.util.Reject;

/**
 * Handles relocating of components via drag events.
 *
 * On some platforms (e.g. Linux) sending a lot of location updates
 * is a problem, because the underlying window system cannot process
 * the location updates fast enough. This leads to a window that
 * lags behind the mouse motion.
 *
 * {@link WindowDragManager} solves this problem by accepting an arbitrary
 * number of {@link MouseEvent}s, but updating the location of the
 * window in constant intervals, and only if the position actually differs.
 *
 * @author <a href="mailto:radig@powerfolder.com">Matthias Radig</a>
 *
 */
public class WindowDragManager {

    private Timer timer;
    private Component window;
    private int x;
    private int y;
    private int startX;
    private int startY;
    private int origX;
    private int origY;

    public WindowDragManager(Component window, int interval) {
        Reject.ifNull(window, "Component must not be null");
        Reject.ifTrue(interval <= 0, "timeout must be > 0");
        this.timer = new Timer(interval, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateComponentLocation();
            }
        });
        this.window = window;
        this.x = window.getX();
        this.y = window.getY();
    }

    public void start(MouseEvent e) {
        timer.start();
        Point p = e.getLocationOnScreen();
        Point l = window.getLocation();
        startX = p.x;
        startY = p.y;
        origX = l.x;
        origY = l.y;
    }

    public void stop(MouseEvent e) {
        timer.stop();
        update(e);
        updateComponentLocation();
    }

    public void update(MouseEvent e) {
        Point p = e.getLocationOnScreen();
        int dx = p.x - startX;
        int dy = p.y - startY;
        this.x = origX + dx;
        this.y = origY + dy;
    }

    public void updateComponentLocation() {
        Point l = window.getLocation();
        if (l.x != x || l.y != y) {
            window.setLocation(x, y);
        }
    }
}
