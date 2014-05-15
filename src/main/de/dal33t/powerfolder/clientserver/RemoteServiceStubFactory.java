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

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.message.Identity;
import de.dal33t.powerfolder.message.clientserver.RemoteMethodCallRequest;
import de.dal33t.powerfolder.message.clientserver.RemoteMethodCallResponse;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StackDump;
import de.dal33t.powerfolder.util.Util;

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
    public static <T> T createRemoteStub(Controller controller,
        Class<? extends T> serviceInterface, Member remoteSide)
    {
        return createRemoteStub(controller, serviceInterface, remoteSide, null);
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
     * @param throwableHandler
     * @return the remote stub implementing the service interface
     */
    @SuppressWarnings("unchecked")
    public static <T> T createRemoteStub(Controller controller,
        Class<? extends T> serviceInterface, Member remoteSide,
        ThrowableHandler throwableHandler)
    {
        Reject.ifNull(controller, "Controller is null");
        Reject.ifNull(remoteSide, "Remote site is null");
        Reject.ifFalse(serviceInterface.isInterface(),
            "Service interface class is not a interface! " + serviceInterface);
        InvocationHandler handler = new RemoteInvocationHandler(controller,
            serviceInterface.getName(), remoteSide, throwableHandler);
        return (T) Proxy.newProxyInstance(RemoteServiceStubFactory.class
            .getClassLoader(), new Class[]{serviceInterface}, handler);
    }

    private static class RemoteInvocationHandler implements InvocationHandler {
        private Controller controller;
        private Member remoteSide;
        private String serviceId;
        private ThrowableHandler throwableHandler;

        private RemoteInvocationHandler(Controller controller,
            String serviceId, Member remoteSide,
            ThrowableHandler throwableHandler)
        {
            this.controller = controller;
            this.remoteSide = remoteSide;
            this.serviceId = serviceId;
            this.throwableHandler = throwableHandler;
        }

        public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable
        {
            if (Util.isAwtAvailable() && EventQueue.isDispatchThread()) {
                LOG.log(Level.WARNING, "Call to remote service method ("
                    + method + ") executed in EDT thread. Args: "
                    + (args != null ? Arrays.asList(args) : "n/a"),
                    new StackDump());
            }
            if (ServerClient.SERVER_HANDLE_MESSAGE_THREAD.get()) {
                LOG.log(Level.WARNING, "Call to remote service method ("
                    + method + ") executed Server/Member.handleMessage. Args: "
                    + (args != null ? Arrays.asList(args) : "n/a"),
                    new StackDump());
            }
            Identity id = remoteSide.getIdentity();
            RequestExecutor executor = new RequestExecutor(controller,
                remoteSide);
            RemoteMethodCallRequest request = new RemoteMethodCallRequest(
                serviceId, method, args);
            if (id == null || id.isSupportsSerializedRequest()) {
                request = request.toSerzializedForm();
            } else {
                if (ConfigurationEntry.SECURITY_PERMISSIONS_STRICT
                    .getValueBoolean(controller))
                {
                    LOG
                        .severe("Using strict permission security setting while executing legacy type request."
                            + "Please check program version of "
                            + remoteSide
                            + ": "
                            + (id == null ? "" : id.getProgramVersion())
                            + ". Request: " + request);
                }

            }
            RemoteMethodCallResponse response;
            try {
                response = (RemoteMethodCallResponse) executor.execute(request);
            } catch (ConnectionException e) {
                throw new RemoteCallException(e);
            }
            if (response.isException()) {
                StackTraceElement[] serverSte = response.getException()
                    .getStackTrace();
                StackTraceElement[] clientSte = new RuntimeException()
                    .getStackTrace();
                StackTraceElement[] fullSte = new StackTraceElement[serverSte.length
                    + clientSte.length];
                System.arraycopy(serverSte, 0, fullSte, 0, serverSte.length);
                System.arraycopy(clientSte, 0, fullSte, serverSte.length,
                    clientSte.length);
                response.getException().setStackTrace(fullSte);
                boolean exceptionDeclared = Arrays.asList(
                    method.getExceptionTypes()).contains(
                    response.getException().getClass());
                if (exceptionDeclared) {
                    throw response.getException();
                }
                if (throwableHandler != null) {
                    try {
                        throwableHandler.handle(response.getException());
                    } catch (Exception e) {
                        LOG
                            .warning("ThrowableHandler threw exception! What a pity! "
                                + e);
                    }
                }
                if (response.getException() instanceof RuntimeException) {
                    throw (RuntimeException) response.getException();
                }
                throw new RemoteCallException(response.getException());
            }
            return response.getResult();
        }
    }

}
