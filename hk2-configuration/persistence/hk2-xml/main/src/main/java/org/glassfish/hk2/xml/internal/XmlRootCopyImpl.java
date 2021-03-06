/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.hk2.xml.internal;

import org.glassfish.hk2.api.MultiException;
import org.glassfish.hk2.configuration.hub.api.Hub;
import org.glassfish.hk2.configuration.hub.api.PrepareFailedException;
import org.glassfish.hk2.configuration.hub.api.WriteableBeanDatabase;
import org.glassfish.hk2.xml.api.XmlHubCommitMessage;
import org.glassfish.hk2.xml.api.XmlRootCopy;
import org.glassfish.hk2.xml.api.XmlRootHandle;
import org.glassfish.hk2.xml.jaxb.internal.BaseHK2JAXBBean;

/**
 * @author jwells
 *
 */
public class XmlRootCopyImpl<T> implements XmlRootCopy<T> {
    private final Hub hub;
    private final XmlRootHandleImpl<T> parent;
    private final long basis;
    private final T copy;
    
    /* package */ XmlRootCopyImpl(Hub hub, XmlRootHandleImpl<T> parent, long basis, T copy) {
        if (copy == null) throw new IllegalStateException("Only a non-empty Handle can be copied");
        
        this.hub = hub;
        this.parent = parent;
        this.basis = basis;
        this.copy = copy;
        
    }

    /* (non-Javadoc)
     * @see org.glassfish.hk2.xml.api.XmlRootCopy#getParent()
     */
    @Override
    public XmlRootHandle<T> getParent() {
        return parent;
    }

    /* (non-Javadoc)
     * @see org.glassfish.hk2.xml.api.XmlRootCopy#getChildRoot()
     */
    @Override
    public T getChildRoot() {
        return copy;
    }

    /* (non-Javadoc)
     * @see org.glassfish.hk2.xml.api.XmlRootCopy#isMergeable()
     */
    @Override
    public boolean isMergeable() {
        return (parent.getRevision() == basis);
    }

    /* (non-Javadoc)
     * @see org.glassfish.hk2.xml.api.XmlRootCopy#merge()
     */
    @Override
    public void merge() {
        parent.getChangeInfo().getWriteLock().lock();
        try {
            if (!isMergeable()) {
                throw new AssertionError("Parent has changed since copy was made, no merge possible");
            }
            
            WriteableBeanDatabase writeableDatabase = null;
            if (hub != null) {
                writeableDatabase = hub.getWriteableDatabaseCopy();
            }
        
            BaseHK2JAXBBean copyBean = (BaseHK2JAXBBean) copy;
            BaseHK2JAXBBean original = (BaseHK2JAXBBean) parent.getRoot();
            
            original._merge(copyBean, writeableDatabase);
            
            boolean success = false;
            try {
                writeableDatabase.commit(new XmlHubCommitMessage(){});
                success = true;
            }
            catch (MultiException me) {
                boolean foundPrepareException = false;
                for (Throwable th : me.getErrors()) {
                    if (th instanceof PrepareFailedException) {
                        foundPrepareException = true;
                        break;
                    }
                }
                
                if (foundPrepareException) {
                    success = false;
                }
                else {
                    // If there was no prepare exception then the
                    // transaction went through but some joker
                    // failed in the commit phase.  But it still
                    // means that the values have to be accounted
                    // for in the beans, so it appears like success
                    success = true;
                }
            }
            finally {
                if (success) {
                    parent.getChangeInfo().incrementChangeNumber();
                }
                
                original._endMerge(success);
            }
        }
        finally {
            parent.getChangeInfo().getWriteLock().unlock();
        }
    }
    
    

}
