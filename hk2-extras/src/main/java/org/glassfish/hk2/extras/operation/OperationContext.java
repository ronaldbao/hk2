/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package org.glassfish.hk2.extras.operation;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.Context;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.extras.operation.internal.OperationHandleImpl;
import org.glassfish.hk2.extras.operation.internal.SingleOperationManager;
import org.jvnet.hk2.annotations.Contract;

/**
 * The implementation of {@link Context} for an Operation.
 * <p>
 * An operation is defined as a unit of work that can
 * be associated with one or more java threads, but where
 * two operations of the same type may not be associated
 * with the same thread at the same time.  Examples of such
 * an operation might be a RequestScope or a TenantRequesteOperation.
 * An operation is a more general concept than the normal
 * Java EE request scope, since it does not require a Java EE
 * container
 * <p>
 * Users of this API generally create a {@link Scope} annotation
 * and extend this class, implementing the {@link Context#getScope()}
 * and making sure the parameterized type is the Scope annotation.
 * The {@link Scope} annotation for an Operation is usually
 * {@link Proxiable} but does not have to be. As with all implementations
 * of {@link Context} the subclass of this class must be in the {@link Singleton}
 * scope.  The user code then uses the {@link OperationManager} and {@link OperationHandle}
 * to start and stop Operations and to associate and dis-associate
 * threads with Operations
 * <p>
 * Classes extending this class may also choose to override the method
 * {@link Context#supportsNullCreation()} which returns false by default
 * 
 * @author jwells
 */
@Contract
public abstract class OperationContext<T extends Annotation> implements Context<T> {
    private SingleOperationManager<T> manager;
    private final HashMap<OperationIdentifier<T>, HashMap<ActiveDescriptor<?>, Object>> operationMap =
            new HashMap<OperationIdentifier<T>, HashMap<ActiveDescriptor<?>, Object>>();
    private final HashSet<ActiveDescriptor<?>> creating = new HashSet<ActiveDescriptor<?>>();

    /* (non-Javadoc)
     * @see org.glassfish.hk2.api.Context#findOrCreate(org.glassfish.hk2.api.ActiveDescriptor, org.glassfish.hk2.api.ServiceHandle)
     */
    @SuppressWarnings("unchecked")
    @Override
    public <U> U findOrCreate(ActiveDescriptor<U> activeDescriptor,
            ServiceHandle<?> root) {
        SingleOperationManager<T> localManager;
        synchronized (this) {
            localManager = manager;
        }
        
        if (localManager == null) {
            throw new IllegalStateException("There is no operation of type " +
                getScope().getName() + " on thread " + Thread.currentThread().getId());
        }
        
        OperationHandleImpl<T> operation = localManager.getCurrentOperationOnThisThread();
        if (operation == null) {
            throw new IllegalStateException("There is no current operation of type " +
                getScope().getName() + " on thread " + Thread.currentThread().getId());
        }
        
        HashMap<ActiveDescriptor<?>, Object> serviceMap;
        synchronized (this) {
            serviceMap = operationMap.get(operation.getIdentifier());
            if (serviceMap == null) {
                serviceMap = new HashMap<ActiveDescriptor<?>, Object>();
                operationMap.put(operation.getIdentifier(), serviceMap);
            }
            
            Object retVal = serviceMap.get(activeDescriptor);
            if (retVal != null) return (U) retVal;
            
            if (supportsNullCreation() && serviceMap.containsKey(activeDescriptor)) {
                return null;
            }
            
            // retVal is null, and this is not an explicit null, so must actually do the creation
            while (creating.contains(activeDescriptor)) {
                try {
                    this.wait();
                }
                catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            
            retVal = serviceMap.get(activeDescriptor);
            if (retVal != null) return (U) retVal;
            
            if (supportsNullCreation() && serviceMap.containsKey(activeDescriptor)) {
                return null;
            }
            
            // Not in creating, and not created.  Create it ourselves
            creating.add(activeDescriptor);
        }
        
        Object retVal = null;
        boolean success = false;
        try {
            retVal = activeDescriptor.create(root);
            if (retVal == null && !supportsNullCreation()) {
                throw new IllegalArgumentException("The operation for context " + getScope().getName() +
                        " does not support null creation, but descriptor " + activeDescriptor + " returned null");
            }
            
            success = true;
        }
        finally {
            synchronized (this) {
                if (success) {
                    serviceMap.put(activeDescriptor, retVal);
                }
                
                creating.remove(activeDescriptor);
                this.notifyAll();
            }
        }
        
        return (U) retVal;
    }

    /* (non-Javadoc)
     * @see org.glassfish.hk2.api.Context#containsKey(org.glassfish.hk2.api.ActiveDescriptor)
     */
    @Override
    public boolean containsKey(ActiveDescriptor<?> descriptor) {
        SingleOperationManager<T> localManager;
        synchronized (this) {
            localManager = manager;
        }
        if (localManager == null) return false;
        
        OperationHandleImpl<T> operation = localManager.getCurrentOperationOnThisThread();
        if (operation == null) return false;
        
        synchronized (this) {
            HashMap<ActiveDescriptor<?>, Object> serviceMap;
            
            serviceMap = operationMap.get(operation.getIdentifier());
            if (serviceMap == null) return false;
            
            return serviceMap.containsKey(descriptor);
        }
        
    }

    /* (non-Javadoc)
     * @see org.glassfish.hk2.api.Context#destroyOne(org.glassfish.hk2.api.ActiveDescriptor)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void destroyOne(ActiveDescriptor<?> descriptor) {
        synchronized (this) {
            for (HashMap<ActiveDescriptor<?>, Object> serviceMap : operationMap.values()) {
                Object killMe = serviceMap.remove(descriptor);
                if (killMe == null) continue;
                
                ((ActiveDescriptor<Object>) descriptor).dispose(killMe);
            }
        }
    }
    
    /* (non-Javadoc)
     * @see org.glassfish.hk2.api.Context#shutdown()
     */
    @SuppressWarnings("unchecked")
    @Override
    public void shutdown() {
        synchronized (this) {
            for (HashMap<ActiveDescriptor<?>, Object> serviceMap : operationMap.values()) {
                for (Map.Entry<ActiveDescriptor<?>, Object> entry : serviceMap.entrySet()) {
                    ActiveDescriptor<?> descriptor = entry.getKey();
                    Object killMe = entry.getValue();
                    if (killMe == null) continue;
                    
                    ((ActiveDescriptor<Object>) descriptor).dispose(killMe);
                }
            }
            
            operationMap.clear();
        }
        
    }

    /* (non-Javadoc)
     * @see org.glassfish.hk2.api.Context#supportsNullCreation()
     */
    @Override
    public boolean supportsNullCreation() {
        return false;
    }

    /* (non-Javadoc)
     * @see org.glassfish.hk2.api.Context#isActive()
     */
    @Override
    public boolean isActive() {
        return true;
    }

    public synchronized void setOperationManager(SingleOperationManager<T> manager) {
        this.manager = manager;
    }
    
    @Override
    public String toString() {
        return "OperationContext(" + getScope().getName() + "," + System.identityHashCode(this) + ")";
    }
}
