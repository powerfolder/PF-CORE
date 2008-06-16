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

import java.lang.reflect.Method;

/**
 * Request to execute a method on a service registred in the
 * <code>ServiceRegistry</code>
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class RemoteMethodCallRequest extends Request {
    private static final long serialVersionUID = 100L;

    private String serviceId;
    private String methodName;
    private Class<?>[] methodParamTypes;
    private Object[] args;

    public RemoteMethodCallRequest(String serviceId, Method method,
        Object... args)
    {
        super();
        this.serviceId = serviceId;
        this.methodName = method.getName();
        this.methodParamTypes = method.getParameterTypes();
        this.args = args;
    }

    public String getServiceId() {
        return serviceId;
    }

    public Method getMethod(Object target) throws NoSuchMethodException {
        return target.getClass().getMethod(methodName, methodParamTypes);
    }

    public Object[] getArgs() {
        return args;
    }

    public String toString() {
        return "RemoteCall on '" + serviceId + "', Method " + methodName
            + ", args " + (args != null ? args.length : "n/a");
    }
}
