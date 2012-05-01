/*
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2007-2011 Sun Microsystems, Inc. All rights reserved.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 * 
 * Contributor(s):
 * 
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
package org.jvnet.hk2.config.provider.internal;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jvnet.hk2.component.ComponentException;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.Inhabitant;
import org.jvnet.hk2.component.InhabitantProviderInterceptor;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.ConfiguredBy;

import com.sun.hk2.component.AbstractInhabitantImpl;
import com.sun.hk2.component.AbstractInhabitantProvider;
import com.sun.hk2.component.InhabitantStore;

public class ConfigInhabitantProvider extends AbstractInhabitantProvider {

  private final static Logger logger = Logger.getLogger(ConfigInhabitantProvider.class.getName());

  private final Habitat habitat;
  
  public ConfigInhabitantProvider(Habitat h) {
    this.habitat = h;
  }
  
  @Override
  public AbstractInhabitantImpl<?> visit(AbstractInhabitantImpl<?> i,
      String typeName, Set<String> indicies,
      Iterator<InhabitantProviderInterceptor> remainingInterceptors,
      InhabitantStore store) {
    boolean shouldProcess = false;
    
    final Inhabitant<?> originalInh = i;
    if (contains(indicies, ConfiguredBy.class.getName())) {
      logger.log(Level.FINE, "Found an @ConfiguredBy inhabitant: {0} with indicies {1}", new Object [] {i, indicies});
      
      ConfiguredBy cb = i.getAnnotation(ConfiguredBy.class);
      Class<?> configuredClass = cb.value();
      if (null == configuredClass) {
        throw new ComponentException("ConfiguredBy.value() is required");
      }
      Configured c = configuredClass.getAnnotation(Configured.class);
      if (null == c) {
        throw new ComponentException(i + " service implementation needs to be @Configured");
      }
      
      Set<String> newIndicies = new HashSet<String>(indicies);
      boolean removed = newIndicies.remove(ConfiguredBy.class.getName());
      assert(removed);
      
      i = new ConfigByMetaInhabitant(habitat, i, cb, newIndicies);
      
      shouldProcess = true;
    }
    
    InhabitantProviderInterceptor next = 
        remainingInterceptors.hasNext() ? remainingInterceptors.next() : null;
    i = (null == next) ? 
        i : next.visit(i, typeName, indicies, remainingInterceptors, store);

    if (shouldProcess) {
      if (null == i || null == store) {
        logger.log(Level.FINE, "expected to store inhabitant but couldn't - {0}", originalInh);
      } else {
        // add it ourself, ensuring that only the ConfiguredBy index is used.
        store.add(i);
        store.addIndex(i, ConfiguredBy.class.getName(), null);
        i = null;
      }
    }

    return i;
  }

}