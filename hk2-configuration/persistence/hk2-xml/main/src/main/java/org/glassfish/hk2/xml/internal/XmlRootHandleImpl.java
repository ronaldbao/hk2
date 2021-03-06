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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.configuration.hub.api.Hub;
import org.glassfish.hk2.configuration.hub.api.WriteableBeanDatabase;
import org.glassfish.hk2.xml.api.XmlHubCommitMessage;
import org.glassfish.hk2.xml.api.XmlRootCopy;
import org.glassfish.hk2.xml.api.XmlRootHandle;
import org.glassfish.hk2.xml.jaxb.internal.BaseHK2JAXBBean;

/**
 * @author jwells
 *
 */
public class XmlRootHandleImpl<T> implements XmlRootHandle<T> {
    private final XmlServiceImpl parent;
    private final Hub hub;
    private T root;
    private final UnparentedNode rootNode;
    private URI rootURI;
    private final boolean advertised;
    private final boolean advertisedInHub;
    private final DynamicChangeInfo changeControl;
    
    /* package */ XmlRootHandleImpl(
            XmlServiceImpl parent,
            Hub hub,
            T root,
            UnparentedNode rootNode,
            URI rootURI,
            boolean advertised,
            boolean inHub,
            DynamicChangeInfo changes) {
        this.parent = parent;
        this.hub = hub;
        this.root = root;
        this.rootNode = rootNode;
        this.rootURI = rootURI;
        this.advertised = advertised;
        this.advertisedInHub = inHub;
        this.changeControl = changes;
    }

    /* (non-Javadoc)
     * @see org.glassfish.hk2.xml.api.XmlRootHandle#getRoot()
     */
    @Override
    public T getRoot() {
        return root;
    }

    /* (non-Javadoc)
     * @see org.glassfish.hk2.xml.api.XmlRootHandle#getRootClass()
     */
    @SuppressWarnings("unchecked")
    @Override
    public Class<T> getRootClass() {
        return (Class<T>) rootNode.getOriginalInterface();
    }

    /* (non-Javadoc)
     * @see org.glassfish.hk2.xml.api.XmlRootHandle#getURI()
     */
    @Override
    public URI getURI() {
        return rootURI;
    }
    
    /* (non-Javadoc)
     * @see org.glassfish.hk2.xml.api.XmlRootHandle#isAdvertisedInLocator()
     */
    @Override
    public boolean isAdvertisedInLocator() {
        return advertised;
    }

    /* (non-Javadoc)
     * @see org.glassfish.hk2.xml.api.XmlRootHandle#isAdvertisedInHub()
     */
    @Override
    public boolean isAdvertisedInHub() {
        return advertisedInHub;
    }

    /* (non-Javadoc)
     * @see org.glassfish.hk2.xml.api.XmlRootHandle#overlay(org.glassfish.hk2.xml.api.XmlRootHandle)
     */
    @Override
    public void overlay(XmlRootHandle<T> newRoot) {
        throw new AssertionError("overlay not yet implemented");
        
    }
    
    /* (non-Javadoc)
     * @see org.glassfish.hk2.xml.api.XmlRootHandle#getXmlRootCopy()
     */
    @SuppressWarnings("unchecked")
    @Override
    public XmlRootCopy<T> getXmlRootCopy() {
        Hub useHub = (advertisedInHub) ? hub : null;
        
        // In any case, the child should not be directly given the hub, as
        // it is not reflected in the hub
        DynamicChangeInfo copyController =
                new DynamicChangeInfo(changeControl.getJAUtilities(),
                        null,
                        changeControl.getIdGenerator(),
                        null,
                        changeControl.getServiceLocator());
        
        changeControl.getReadLock().lock();
        try {
            BaseHK2JAXBBean bean = (BaseHK2JAXBBean) root;
            if (bean == null) {
                return new XmlRootCopyImpl<T>(useHub, this, changeControl.getChangeNumber(), null);
            }
        
            BaseHK2JAXBBean copy;
            try {
                copy = doCopy(bean, copyController);
            }
            catch (RuntimeException re) {
                throw re;
            }
            catch (Throwable th) {
                throw new RuntimeException(th);
            }
        
            return new XmlRootCopyImpl<T>(useHub, this, changeControl.getChangeNumber(), (T) copy);
        }
        finally {
            changeControl.getReadLock().unlock();
        }
    }
    
    private static BaseHK2JAXBBean doCopy(BaseHK2JAXBBean copyMe, DynamicChangeInfo copyController) throws Throwable {
        if (copyMe == null) return null;
        
        BaseHK2JAXBBean retVal = Utilities.createBean(copyMe.getClass());
        retVal._shallowCopyFrom(copyMe);
        
        Set<String> childrenProps = copyMe._getChildrenXmlTags();
        for (String childProp : childrenProps) {
            Object child = copyMe._getProperty(childProp);
            if (child == null) continue;
            
            if (child instanceof List) {
                List<?> childList = (List<?>) child;
                
                ArrayList<Object> toSetChildList = new ArrayList<Object>(childList.size());
                
                for (Object subChild : childList) {
                    BaseHK2JAXBBean copiedChild = doCopy((BaseHK2JAXBBean) subChild, copyController);
                    copiedChild._setParent(retVal);
                    
                    toSetChildList.add(copiedChild);
                }
                
                // Sets the list property into the parent
                retVal._setProperty(childProp, toSetChildList);
            }
            else {
                // A direct child
                BaseHK2JAXBBean copiedChild = doCopy((BaseHK2JAXBBean) child, copyController);
                copiedChild._setParent(retVal);
                
                retVal._setProperty(childProp, copiedChild);
            }
        }
        
        retVal._setDynamicChangeInfo(copyController);
        return retVal;
    }
    
    /* package */ long getRevision() {
        return changeControl.getChangeNumber();
    }

    /* (non-Javadoc)
     * @see org.glassfish.hk2.xml.api.XmlRootHandle#addRoot(java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void addRoot(T newRoot) {
        changeControl.getWriteLock().lock();
        try {
            if (root != null) {
                throw new IllegalStateException("An attempt was made to add a root to a handle that already has a root " + this);
            }
            if (!(newRoot instanceof BaseHK2JAXBBean)) {
                throw new IllegalArgumentException("The added bean must be from XmlService.createBean");
            }
            
            WriteableBeanDatabase wbd = null;
            if (advertisedInHub) {
                wbd = hub.getWriteableDatabaseCopy();
            }
            
            DynamicConfiguration config = null;
            if (advertised) {
                config = parent.getDynamicConfigurationService().createDynamicConfiguration();
            }
            
            BaseHK2JAXBBean copiedRoot = Utilities._addRoot(rootNode,
                    newRoot,
                    changeControl,
                    parent.getClassReflectionHelper(),
                    wbd,
                    config);
            
            if (config != null) {
                config.commit();
            }
            
            if (wbd != null) {
                wbd.commit(new XmlHubCommitMessage() {});
            }
            
            root = (T) copiedRoot;
        }
        finally {
            changeControl.getWriteLock().unlock();
        }
    }

    /* (non-Javadoc)
     * @see org.glassfish.hk2.xml.api.XmlRootHandle#createAndAddRoot()
     */
    @SuppressWarnings("unchecked")
    @Override
    public void addRoot() {
        addRoot(parent.createBean((Class<T>) rootNode.getOriginalInterface()));
    }

    /* (non-Javadoc)
     * @see org.glassfish.hk2.xml.api.XmlRootHandle#deleteRoot()
     */
    @Override
    public T removeRoot() {
        throw new AssertionError("removeRoot not implemented");
    }
    
    /* (non-Javadoc)
     * @see org.glassfish.hk2.xml.api.XmlRootHandle#getReadOnlyRoot(boolean)
     */
    @Override
    public T getReadOnlyRoot(boolean representDefaults) {
        throw new AssertionError("getReadOnlyRoot not implemented");
    }
    
    /* package */ DynamicChangeInfo getChangeInfo() {
        return changeControl;
    }
    
    @Override
    public String toString() {
        return "XmlRootHandleImpl(" + root + "," + rootNode + "," + rootURI + "," + System.identityHashCode(this) + ")";
    }

    
}
