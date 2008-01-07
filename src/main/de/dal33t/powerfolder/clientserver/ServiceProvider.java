package de.dal33t.powerfolder.clientserver;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.clientserver.RequestExecutor;
import de.dal33t.powerfolder.message.clientserver.RemoteMethodCallRequest;
import de.dal33t.powerfolder.message.clientserver.RemoteMethodCallResponse;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.util.Reject;

public class ServiceProvider {

    private ServiceProvider() {
        // Not allowed
    }

    /**
     * Constructs a stub implementing the given service interface. All calls are
     * executed against the remote service repository of the remote site.
     * 
     * @param serviceId
     * @param serviceInterface
     * @param remoteSide
     * @return
     */
    @SuppressWarnings("unchecked")
    public static Object createRemoteStub(Controller controller,
        String serviceId, Class serviceInterface, Member remoteSide)
    {
        Reject.ifNull(controller, "Controller is null");
        Reject.ifBlank(serviceId, "Service Id is null");
        Reject.ifNull(remoteSide, "Remote site is null");
        Reject.ifFalse(serviceInterface.isInterface(),
            "Service interface class is not a interface! " + serviceInterface);
        InvocationHandler handler = new RemoteInvocationHandler(controller,
            serviceId, remoteSide);
        return Proxy.newProxyInstance(Thread.currentThread()
            .getContextClassLoader(), new Class[]{serviceInterface}, handler);
    }

    private static class RemoteInvocationHandler implements InvocationHandler {
        private String serviceId;
        private RequestExecutor executor;

        private RemoteInvocationHandler(Controller controller,
            String serviceId, Member remoteSide)
        {
            super();
            this.executor = new RequestExecutor(controller, remoteSide);
            this.serviceId = serviceId;
        }

        public synchronized Object invoke(Object proxy, Method method,
            Object[] args) throws Throwable
        {
            RemoteMethodCallRequest request = new RemoteMethodCallRequest(
                serviceId, method, args);
            RemoteMethodCallResponse response;
            try {
                response = (RemoteMethodCallResponse) executor.execute(request);
            } catch (ConnectionException e) {
                throw new RemoteCallException(e);
            }
            if (response.isException()) {
                boolean exceptionDeclared = Arrays.asList(
                    method.getExceptionTypes()).contains(
                    response.getException().getClass());
                if (exceptionDeclared) {
                    throw response.getException();
                }
                throw new RemoteCallException(response.getException());
            }
            return response.getResult();
        }
    }
}
