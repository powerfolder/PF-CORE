package de.dal33t.powerfolder.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class EstimatedTimeTest {
    private static final int DELTA_TIME_MILLIS = 100;
    private static final boolean ACTIVE = true;
    private static final boolean INACTIVE = false;
    private static final int NEGATIVE_DELTA = -1;
    private EstimatedTime estimatedTime;

    @Test
    public void shouldReturnValues() {
        estimatedTime = new EstimatedTime(DELTA_TIME_MILLIS, ACTIVE);
        assertEquals(DELTA_TIME_MILLIS, estimatedTime.getDeltaTimeMillis());
        assertEquals(ACTIVE,estimatedTime.isActive());
    }

    @Test
    public void testToString(){
        estimatedTime =new EstimatedTime(DELTA_TIME_MILLIS, INACTIVE);
        assertEquals("",estimatedTime.toString());

        estimatedTime =new EstimatedTime(NEGATIVE_DELTA, ACTIVE);
        assertEquals(Translation.get("estimation.unknown"),estimatedTime.toString());

        estimatedTime =new EstimatedTime(DELTA_TIME_MILLIS, ACTIVE);
        assertEquals(Format.formatDeltaTime(DELTA_TIME_MILLIS),estimatedTime.toString());
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowNullPointerException(){
        estimatedTime.isActive();
    }
}
