package de.dal33t.powerfolder.clientserver;

/*
 * Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
 *
 * This file is part of PowerFolder.
 *
 * PowerFolder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation.
 *
 * PowerFolder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
 *
 * $Id$
 */

import java.awt.EventQueue;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.message.clientserver.RemoteMethodCallRequest;
import de.dal33t.powerfolder.message.clientserver.RemoteMethodCallResponse;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.ui.UIUtil;

public class RemoteServiceStubFactory {
    private static final Logger LOG = Logger
        .getLogger(RemoteServiceStubFactory.class.getName());

    private RemoteServiceStubFactory() {
        // No instance allowed
    }

    /**
     * Constructs a stub implementing the given service interface. All calls are
     * executed against the remote service repository of the remote site.
     * 
     * @param <T>
     *            The interface class of the service
     * @param controller
     *            the controller
     * @param serviceInterface
     * @param remoteSide
     * @return the remote stub implementing the service interface
     */
    @SuppressWarnings("unchecked")
    static <T> T createRemoteStub(Controller controller,
        Class<? extends T> serviceInterface, Member remoteSide)
    {
        Reject.ifNull(controller, "Controller is null");
        Reject.ifNull(remoteSide, "Remote site is null");
        Reject.ifFalse(serviceInterface.isInterface(),
            "Service interface class is not a interface! " + serviceInterface);
        InvocationHandler handler = new RemoteInvocationHandler(controller,
            serviceInterface.getName(), remoteSide);
        return (T) Proxy.newProxyInstance(RemoteServiceStubFactory.class
            .getClassLoader(), new Class[]{serviceInterface}, handler);
    }

    private static class RemoteInvocationHandler implements InvocationHandler {
        private Controller controller;
        private Member remoteSide;
        private String serviceId;

        private RemoteInvocationHandler(Controller controller,
            String serviceId, Member remoteSide)
        {
            super();
            this.controller = controller;
            this.remoteSide = remoteSide;
            this.serviceId = serviceId;
        }

        public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable
        {
            RuntimeException rte = new RuntimeException("Call source");
            StackTraceElement[] te = rte.getStackTrace();
            for (StackTraceElement stackTraceElement : te) {
                if (stackTraceElement.getMethodName().contains("handleMessage"))
                {
                    throw new RemoteCallException(
                        "Illegal to call remote service method (" + serviceId
                            + " " + method + ") in message handling code ("
                            + stackTraceElement + ").", rte);
                }
            }
            if (UIUtil.isAWTAvailable() && EventQueue.isDispatchThread()) {
                LOG.log(Level.WARNING, "Call to remote service method ("
                    + method + ") executed in EDT thread. Args: "
                    + (args != null ? Arrays.asList(args) : "n/a"), rte);
            }
            RequestExecutor executor = new RequestExecutor(controller,
                remoteSide);
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
