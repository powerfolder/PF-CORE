package de.dal33t.powerfolder.ui;

import java.awt.Component;
import java.awt.event.MouseEvent;

import junit.framework.TestCase;

/**
 * Test for {@link WindowDragManager}.
 *
 * @author <a href="mailto:radig@powerfolder.com">Matthias Radig</a>
 *
 */
public class WindowDragManagerTest extends TestCase {


    private TestComponent testComponent = new TestComponent();

    public void testConstructor() {
        try {
            new WindowDragManager(null, 30);
            fail("No nullcheck performed in constructor");
        } catch (NullPointerException e) {
            // expected
        }
        try {
            new WindowDragManager(testComponent, 0);
            new WindowDragManager(testComponent, -1);
            fail("Only positive interval should be allowed");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testInvariance() {
        WindowDragManager m = new WindowDragManager(testComponent, Integer.MAX_VALUE);

        m.start(mouseEvent(0, 10));
        m.stop(mouseEvent(0, 10));
        assertEquals(0, testComponent.sets);

        testComponent.reset();
        m.start(mouseEvent(0, 10));
        m.updateComponentLocation();
        m.update(mouseEvent(0, 10));
        m.updateComponentLocation();
        m.stop(mouseEvent(0, 10));
        assertEquals(0, testComponent.sets);

        testComponent.reset();
        m.start(mouseEvent(103, 392));
        m.stop(mouseEvent(103, 392));
        assertEquals(0, testComponent.sets);

        m.start(mouseEvent(0, 10));
        // should not have an effect since the timer does not fire
        m.update(mouseEvent(1, 1));
        m.update(mouseEvent(103, 392));
        m.update(mouseEvent(0, 10));
        m.update(mouseEvent(500, 3999));
        m.stop(mouseEvent(0, 10));
        assertEquals(0, testComponent.sets);

        // updateComponentLocation should do nothing if component location
        // equals the location dragged to
        testComponent.reset();
        m.start(mouseEvent(103, 392));
        m.updateComponentLocation();
        m.update(mouseEvent(103, 392));
        m.updateComponentLocation();
        m.stop(mouseEvent(103, 392));
        assertEquals(0, testComponent.sets);
    }

    public void testUpdate() {
        WindowDragManager m = new WindowDragManager(testComponent, Integer.MAX_VALUE);

        testComponent.reset();
        m.start(mouseEvent(103, 392));
        m.update(mouseEvent(0, 10));
        m.update(mouseEvent(500, 3999));
        m.update(mouseEvent(500, 3999));
        m.stop(mouseEvent(10, 20));
        // updates once, on stop
        assertEquals(1, testComponent.sets);
        assertLocationEquals(-93, -372);

        testComponent.reset();
        m.start(mouseEvent(103, 392));
        m.update(mouseEvent(0, 10));

        assertEquals(0, testComponent.sets);
        assertLocationEquals(0,0);

        m.updateComponentLocation();
        assertEquals(1, testComponent.sets);
        assertLocationEquals(-103, -382);

        m.update(mouseEvent(0, 10));
        m.updateComponentLocation();
        assertEquals(1, testComponent.sets);
        assertLocationEquals(-103, -382);

        m.update(mouseEvent(500, 3999));
        assertEquals(1, testComponent.sets);
        assertLocationEquals(-103, -382);

        m.stop(mouseEvent(10, 20));
        // updates once, on stop
        assertEquals(2, testComponent.sets);
        assertLocationEquals(-93, -372);
    }

    public void testTimer() throws Exception {
        WindowDragManager m = new WindowDragManager(testComponent, 5);
        testComponent.setLocation(50, 50);
        m.start(mouseEvent(0, 0));
        m.update(mouseEvent(100, 100));
        Thread.sleep(30);
        assertEquals(2, testComponent.sets);
        m.stop(mouseEvent(100, 100));
        assertEquals(2, testComponent.sets);

        testComponent.reset();
        m = new WindowDragManager(testComponent, 5);
        testComponent.setLocation(50, 50);
        m.start(mouseEvent(0, 0));
        m.update(mouseEvent(100, 100));
        Thread.sleep(30);
        assertEquals(2, testComponent.sets);
        assertLocationEquals(150, 150);
        m.stop(mouseEvent(100, 100));
        assertEquals(2, testComponent.sets);
        assertLocationEquals(150, 150);
    }

    private MouseEvent mouseEvent(int xAbs, int yAbs) {
        // use bogus relative positions as calculation should be performed
        // with absolute positions
        return new MouseEvent(testComponent, 0, System.currentTimeMillis(),
            0, 0, 0, xAbs, yAbs, 1, false, MouseEvent.BUTTON1);
    }

    private void assertLocationEquals(int x, int y) {
        assertEquals(x, testComponent.getX());
        assertEquals(y, testComponent.getY());
    }

    @SuppressWarnings("serial")
    static final class TestComponent extends Component {
        volatile int sets = 0;

        @Override
        public void setLocation(int x, int y) {
            super.setLocation(x, y);
            sets++;
        }

        void reset() {
            setLocation(0, 0);
            sets = 0;
        }
    }
}
