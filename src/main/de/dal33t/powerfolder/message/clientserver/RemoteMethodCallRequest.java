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
package de.dal33t.powerfolder.message.clientserver;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.Serializable;
import java.lang.reflect.Method;

import de.dal33t.powerfolder.util.ByteSerializer;

/**
 * Request to execute a method on a service registred in the
 * <code>ServiceRegistry</code>
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class RemoteMethodCallRequest extends Request {
    private static final long serialVersionUID = 100L;

    private String serviceId;
    private String methodName;
    private Class<?>[] methodParamTypes;
    private Object[] args;
    private byte[][] argsSerialized;

    public RemoteMethodCallRequest(String serviceId, Method method,
        Object... args)
    {
        super();
        this.serviceId = serviceId;
        this.methodName = method.getName();
        this.methodParamTypes = method.getParameterTypes();
        this.args = args;
    }

    /**
     * @return the
     * @throws IOException
     */
    public RemoteMethodCallRequest toSerzializedForm() throws IOException {
        if (args == null) {
            argsSerialized = null;
            return this;
        }
        if (args.length == 0) {
            argsSerialized = new byte[0][0];
            return this;
        }
        argsSerialized = new byte[args.length][0];
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg == null) {
                argsSerialized[i] = null;
                continue;
            }
            if (arg instanceof Serializable) {
                argsSerialized[i] = ByteSerializer.serializeStatic(
                    (Serializable) arg, false);
            } else {
                throw new NotSerializableException(
                    "Argument not serializable: " + arg);
            }
        }
        args = null;
        return this;
    }

    public String getServiceId() {
        return serviceId;
    }

    public Method getMethod(Object target) throws NoSuchMethodException {
        return target.getClass().getMethod(methodName, methodParamTypes);
    }

    public String getMethodName() {
        return methodName;
    }

    public Class<?>[] getArgsTypes() {
        return methodParamTypes;
    }

    public Object[] getArgs() throws IOException, ClassNotFoundException {
        if (args == null && argsSerialized != null) {
            args = new Object[argsSerialized.length];
            for (int i = 0; i < argsSerialized.length; i++) {
                byte[] buf = argsSerialized[i];
                if (buf == null) {
                    args[i] = null;
                    continue;
                }
                args[i] = ByteSerializer.deserializeStatic(buf, false);
            }
        }
        return args;
    }

    public String toString() {
        return "RemoteCall(" + getRequestId() + ") on '" + serviceId
            + "', Method " + methodName + ", args "
            + (methodParamTypes != null ? methodParamTypes.length : "n/a");
    }
}
