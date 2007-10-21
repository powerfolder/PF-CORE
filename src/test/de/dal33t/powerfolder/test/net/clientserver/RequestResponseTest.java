package de.dal33t.powerfolder.test.net.clientserver;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.clientserver.EchoHandler;
import de.dal33t.powerfolder.clientserver.RequestExecutor;
import de.dal33t.powerfolder.message.clientserver.EchoRequest;
import de.dal33t.powerfolder.message.clientserver.EchoResponse;
import de.dal33t.powerfolder.message.clientserver.Response;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

/**
 * Test for the basic request-response logic.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class RequestResponseTest extends TwoControllerTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        connectBartAndLisa();
    }

    public void testEchoRequest() throws ConnectionException {
        // Install echo handlder @ bart
        new EchoHandler(getContollerBart());

        Member bartAtLisa = getContollerLisa().getNodeManager()
            .getConnectedNodes().iterator().next();
        RequestExecutor executor = new RequestExecutor(getContollerLisa(),
            bartAtLisa);

        StringBuilder b = new StringBuilder("Hello!");
        for (int i = 0; i < 100; i++) {
            EchoRequest request = new EchoRequest();
            request.payload = b.toString();

            b.append("---" + IdGenerator.makeId() + " XXX");
            Response response = executor.execute(request);
            assertTrue(response instanceof EchoResponse);
            EchoResponse echoResp = (EchoResponse) response;
            assertEquals(request.payload, echoResp.payload);
        }
    }

    public void testMultipleEchoRequest() throws Exception {
        for (int i = 0; i < 10; i++) {
            testEchoRequest();
            tearDown();
            setUp();
        }
    }
}
