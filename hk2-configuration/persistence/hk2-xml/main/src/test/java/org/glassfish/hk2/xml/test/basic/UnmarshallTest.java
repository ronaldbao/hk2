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
package org.glassfish.hk2.xml.test.basic;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.configuration.hub.api.Hub;
import org.glassfish.hk2.configuration.hub.api.Instance;
import org.glassfish.hk2.xml.api.XmlHk2ConfigurationBean;
import org.glassfish.hk2.xml.api.XmlRootHandle;
import org.glassfish.hk2.xml.api.XmlService;
import org.glassfish.hk2.xml.lifecycle.config.Association;
import org.glassfish.hk2.xml.lifecycle.config.Associations;
import org.glassfish.hk2.xml.lifecycle.config.Environment;
import org.glassfish.hk2.xml.lifecycle.config.LifecycleConfig;
import org.glassfish.hk2.xml.lifecycle.config.Service;
import org.glassfish.hk2.xml.lifecycle.config.Tenant;
import org.glassfish.hk2.xml.test.utilities.Utilities;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for unmarshalling xml into the hk2 hub
 * 
 * @author jwells
 */
public class UnmarshallTest {
    public final static String MUSEUM1_FILE = "museum1.xml";
    public final static String ACME1_FILE = "Acme1.xml";
    public final static String ACME2_FILE = "Acme2.xml";
    private final static String SAMPLE_CONFIG_FILE = "sample-config.xml";
    private final static String CYCLE = "cycle.xml";
    private final static String TYPE1_FILE = "type1.xml";
    
    public final static String BEN_FRANKLIN = "Ben Franklin";
    public final static String ACME = "Acme";
    public final static String ALICE = "Alice";
    public final static String BOB = "Bob";
    public final static String CAROL = "Carol";
    public final static String DAVE = "Dave";
    public final static String ENGLEBERT = "Englebert";
    public final static String FRANK = "Frank";
    private final static String ACME_SYMBOL = "acme";
    private final static String NYSE = "NYSE";
    private final static String COKE_TENANT = "coke";
    private final static String HRPROD_SERVICE = "HRProd";
    
    public final static String FINANCIALS_TYPE = "/employees/financials";
    public final static String FINANCIALS_INSTANCE = "employees.financials";
    
    public final static int HUNDRED_INT = 100;
    public final static int HUNDRED_TEN_INT = 110;
    
    public final static long HUNDRED_LONG = 100L;
    public final static long HUNDRED_ONE_LONG = 101L;
    
    public final static String COMPANY_NAME_TAG = "company-name";
    public final static String EMPLOYEE_TAG = "employee";
    public final static String NAME_TAG = "name";
    public final static String ID_TAG = "id";
    private final static String COKE_ENV = "cokeenv";
    
    /**
     * Tests the most basic of xml files can be unmarshalled with an interface
     * annotated with jaxb annotations
     * 
     * @throws Exception
     */
    @Test // @org.junit.Ignore
    public void testInterfaceJaxbUnmarshalling() throws Exception {
        ServiceLocator locator = Utilities.createLocator();
        XmlService xmlService = locator.getService(XmlService.class);
        
        URL url = getClass().getClassLoader().getResource(MUSEUM1_FILE);
        
        XmlRootHandle<Museum> rootHandle = xmlService.unmarshall(url.toURI(), Museum.class);
        Museum museum = rootHandle.getRoot();
        
        Assert.assertEquals(HUNDRED_INT, museum.getId());
        Assert.assertEquals(BEN_FRANKLIN, museum.getName());
        Assert.assertEquals(HUNDRED_TEN_INT, museum.getAge());
        
        Museum asService = locator.getService(Museum.class);
        Assert.assertNotNull(asService);
        
        Assert.assertEquals(museum, asService);
    }
    
    /**
     * Tests the most basic of xml files can be unmarshalled with an interface
     * annotated with jaxb annotations
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test // @org.junit.Ignore
    public void testBeanLikeMapOfInterface() throws Exception {
        ServiceLocator locator = Utilities.createLocator();
        XmlService xmlService = locator.getService(XmlService.class);
        
        URL url = getClass().getClassLoader().getResource(ACME1_FILE);
        
        XmlRootHandle<Employees> rootHandle = xmlService.unmarshall(url.toURI(), Employees.class);
        Employees employees = rootHandle.getRoot();
        
        Assert.assertTrue(employees instanceof XmlHk2ConfigurationBean);
        XmlHk2ConfigurationBean hk2Configuration = (XmlHk2ConfigurationBean) employees;
        
        Map<String, Object> beanLikeMap = hk2Configuration._getBeanLikeMap();
        Assert.assertEquals(ACME, beanLikeMap.get(COMPANY_NAME_TAG));
        
        List<Employee> employeeChildList = (List<Employee>) beanLikeMap.get(EMPLOYEE_TAG);
        Assert.assertNotNull(employeeChildList);
        Assert.assertEquals(2, employeeChildList.size());
        
        boolean first = true;
        for (Employee employee : employeeChildList) {
            Assert.assertTrue(employee instanceof XmlHk2ConfigurationBean);
            XmlHk2ConfigurationBean employeeConfiguration = (XmlHk2ConfigurationBean) employee;
            
            Map<String, Object> employeeBeanLikeMap = employeeConfiguration._getBeanLikeMap();
            
            if (first) {
                first = false;
                
                Assert.assertEquals(HUNDRED_LONG, employeeBeanLikeMap.get(ID_TAG));
                Assert.assertEquals(BOB, employeeBeanLikeMap.get(NAME_TAG));
            }
            else {
                Assert.assertEquals(HUNDRED_ONE_LONG, employeeBeanLikeMap.get(ID_TAG));
                Assert.assertEquals(CAROL, employeeBeanLikeMap.get(NAME_TAG));
            }
        }
        
        Assert.assertNotNull(locator.getService(Employees.class));
        
        Assert.assertNotNull(locator.getService(Employee.class, BOB));
        Assert.assertNotNull(locator.getService(Employee.class, CAROL));
    }
    
    
    
    /**
     * Tests the most basic of xml files can be unmarshalled with an interface
     * annotated with jaxb annotations
     * 
     * @throws Exception
     */
    @Test // @org.junit.Ignore
    public void testInterfaceJaxbUnmarshallingWithChildren() throws Exception {
        ServiceLocator locator = Utilities.createLocator();
        XmlService xmlService = locator.getService(XmlService.class);
        Hub hub = locator.getService(Hub.class);
        
        URL url = getClass().getClassLoader().getResource(ACME1_FILE);
        
        XmlRootHandle<Employees> rootHandle = xmlService.unmarshall(url.toURI(), Employees.class);
        Employees employees = rootHandle.getRoot();
        
        Assert.assertEquals(ACME, employees.getCompanyName());
        
        Assert.assertEquals(2, employees.getEmployees().size());
        
        boolean first = true;
        for (Employee employee : employees.getEmployees()) {
            if (first) {
                first = false;
                Assert.assertEquals(HUNDRED_LONG, employee.getId());
                Assert.assertEquals(BOB, employee.getName());
            }
            else {
                Assert.assertEquals(HUNDRED_ONE_LONG, employee.getId());
                Assert.assertEquals(CAROL, employee.getName());
            }
        }
        
        Financials financials = employees.getFinancials();
        Assert.assertNotNull(financials);
        
        Assert.assertEquals(ACME_SYMBOL, financials.getSymbol());
        Assert.assertEquals(NYSE, financials.getExchange());
        
        Assert.assertEquals(Employees.class, rootHandle.getRootClass());
        Assert.assertEquals(url, rootHandle.getURI().toURL());
        
        Assert.assertNotNull(hub.getCurrentDatabase().getInstance(FINANCIALS_TYPE, FINANCIALS_INSTANCE));
    }
    
    private final static String LIFECYCLE_ROOT_TYPE = "/lifecycle-config";
    private final static String LIFECYCLE_ROOT_INSTANCE = "lifecycle-config";
    private final static String LIFECYCLE_RUNTIMES_TYPE = "/lifecycle-config/runtimes";
    private final static String LIFECYCLE_RUNTIMES_INSTANCE = "lifecycle-config.runtimes";
    private final static String LIFECYCLE_TENANTS_TYPE = "/lifecycle-config/tenants";
    private final static String LIFECYCLE_TENANTS_INSTANCE = "lifecycle-config.tenants";
    private final static String LIFECYCLE_ENVIRONMENTS_TYPE = "/lifecycle-config/environments";
    private final static String LIFECYCLE_ENVIRONMENTS_INSTANCE = "lifecycle-config.environments";
    
    private final static String LIFECYCLE_RUNTIME_TYPE = "/lifecycle-config/runtimes/runtime";
    private final static String LIFECYCLE_RUNTIME_wlsRuntime_INSTANCE = "lifecycle-config.runtimes.wlsRuntime";
    private final static String LIFECYCLE_RUNTIME_DatabaseTestRuntime_INSTANCE = "lifecycle-config.runtimes.DatabaseTestRuntime";
    
    /**
     * Tests a more complex XML format.  This test will ensure
     * all elements are in the Hub with expected names
     * 
     * @throws Exception
     */
    @Test // @org.junit.Ignore
    public void testComplexUnmarshalling() throws Exception {
        ServiceLocator locator = Utilities.createLocator();
        XmlService xmlService = locator.getService(XmlService.class);
        Hub hub = locator.getService(Hub.class);
        
        URL url = getClass().getClassLoader().getResource(SAMPLE_CONFIG_FILE);
        
        XmlRootHandle<LifecycleConfig> rootHandle = xmlService.unmarshall(url.toURI(), LifecycleConfig.class);
        LifecycleConfig lifecycleConfig = rootHandle.getRoot();
        Assert.assertNotNull(lifecycleConfig);
        
        Assert.assertNotNull(hub.getCurrentDatabase().getInstance(LIFECYCLE_ROOT_TYPE, LIFECYCLE_ROOT_INSTANCE));
        Assert.assertNotNull(hub.getCurrentDatabase().getInstance(LIFECYCLE_RUNTIMES_TYPE, LIFECYCLE_RUNTIMES_INSTANCE));
        Assert.assertNotNull(hub.getCurrentDatabase().getInstance(LIFECYCLE_TENANTS_TYPE, LIFECYCLE_TENANTS_INSTANCE));
        Assert.assertNotNull(hub.getCurrentDatabase().getInstance(LIFECYCLE_ENVIRONMENTS_TYPE, LIFECYCLE_ENVIRONMENTS_INSTANCE));
        
        // Runtime
        Assert.assertNotNull(hub.getCurrentDatabase().getInstance(LIFECYCLE_RUNTIME_TYPE, LIFECYCLE_RUNTIME_wlsRuntime_INSTANCE));
        Assert.assertNotNull(hub.getCurrentDatabase().getInstance(LIFECYCLE_RUNTIME_TYPE, LIFECYCLE_RUNTIME_DatabaseTestRuntime_INSTANCE));
        
        Tenant tenant = locator.getService(Tenant.class, COKE_TENANT);
        Assert.assertNotNull(tenant);
        
        Service hrProdService = tenant.lookupServices(HRPROD_SERVICE);
        Assert.assertNotNull(hrProdService);
        Assert.assertEquals(HRPROD_SERVICE, hrProdService.getName());
    }
    
    private final static String ASSOCIATION_PARTITION1_TYPE = "/lifecycle-config/environments/environment/associations/association/partition1";
    private final static String ASSOCIATION_PARTITION2_TYPE = "/lifecycle-config/environments/environment/associations/association/partition2";
    private final static String ASSOCIATION_PARTITION_INSTANCE_PREFIX = "lifecycle-config.environments.cokeenv.associations.";
    private final static String ASSOCIATION_PARTITION1_0_INSTANCE_APPENDIX = ".part1-0";
    private final static String ASSOCIATION_PARTITION2_0_INSTANCE_APPENDIX = ".part2-0";
    private final static String ASSOCIATION_PARTITION1_1_INSTANCE_APPENDIX = ".part1-1";
    private final static String ASSOCIATION_PARTITION2_1_INSTANCE_APPENDIX = ".part2-1";
    
    private final static String PART1_0_NAME = "part1-0";
    private final static String PART2_0_NAME = "part2-0";
    private final static String PART1_1_NAME = "part1-1";
    private final static String PART2_1_NAME = "part2-1";
    
    /**
     * Associations has unkeyed children of type Association.  We
     * get them and make sure they have unique keys generated
     * by the system
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test // @org.junit.Ignore
    public void testUnkeyedChildren() throws Exception {
        ServiceLocator locator = Utilities.createLocator();
        XmlService xmlService = locator.getService(XmlService.class);
        Hub hub = locator.getService(Hub.class);
        
        URL url = getClass().getClassLoader().getResource(SAMPLE_CONFIG_FILE);
        
        XmlRootHandle<LifecycleConfig> rootHandle = xmlService.unmarshall(url.toURI(), LifecycleConfig.class);
        LifecycleConfig lifecycleConfig = rootHandle.getRoot();
        Assert.assertNotNull(lifecycleConfig);
        
        // Lets look at an unkeyed child
        Environment cokeEnv = locator.getService(Environment.class, COKE_ENV);
        Assert.assertNotNull(cokeEnv);
        
        // Lets get the generated unique IDs for the unkeyed children
        Associations associations = cokeEnv.getAssociations();
        Assert.assertNotNull(associations);
        
        String generatedKey1 = null;
        String generatedKey2 = null;
        for (Association association : associations.getAssociations()) {
            XmlHk2ConfigurationBean bean = (XmlHk2ConfigurationBean) association;
            Assert.assertNull(bean._getKeyPropertyName());
            
            if (generatedKey1 == null) {
                generatedKey1 = bean._getKeyValue();
            }
            else if (generatedKey2 == null) {
                generatedKey2 = bean._getKeyValue();
            }
            else {
                Assert.fail("Should only have been two associations, but we found at least three");
            }
        }
        
        Assert.assertNotNull(generatedKey1);
        Assert.assertNotNull(generatedKey2);
        Assert.assertNotEquals(generatedKey1, generatedKey2);
        
        // Given the generated key we can now construct the paths to the children
        // Lets get them from the hub
        String part1_0_instance_name = ASSOCIATION_PARTITION_INSTANCE_PREFIX + generatedKey1 + ASSOCIATION_PARTITION1_0_INSTANCE_APPENDIX;
        String part2_0_instance_name = ASSOCIATION_PARTITION_INSTANCE_PREFIX + generatedKey1 + ASSOCIATION_PARTITION2_0_INSTANCE_APPENDIX;
        String part1_1_instance_name = ASSOCIATION_PARTITION_INSTANCE_PREFIX + generatedKey2 + ASSOCIATION_PARTITION1_1_INSTANCE_APPENDIX;
        String part2_1_instance_name = ASSOCIATION_PARTITION_INSTANCE_PREFIX + generatedKey2 + ASSOCIATION_PARTITION2_1_INSTANCE_APPENDIX;
        
        Instance p1_0 = hub.getCurrentDatabase().getInstance(ASSOCIATION_PARTITION1_TYPE, part1_0_instance_name);
        Instance p2_0 = hub.getCurrentDatabase().getInstance(ASSOCIATION_PARTITION2_TYPE, part2_0_instance_name);
        Instance p1_1 = hub.getCurrentDatabase().getInstance(ASSOCIATION_PARTITION1_TYPE, part1_1_instance_name);
        Instance p2_1 = hub.getCurrentDatabase().getInstance(ASSOCIATION_PARTITION2_TYPE, part2_1_instance_name);
        
        Assert.assertNotNull(p1_0);
        Assert.assertNotNull(p2_0);
        Assert.assertNotNull(p1_1);
        Assert.assertNotNull(p2_1);
        
        String p1_0_name = ((Map<String, String>) p1_0.getBean()).get(NAME_TAG);
        String p2_0_name = ((Map<String, String>) p2_0.getBean()).get(NAME_TAG);
        String p1_1_name = ((Map<String, String>) p1_1.getBean()).get(NAME_TAG);
        String p2_1_name = ((Map<String, String>) p2_1.getBean()).get(NAME_TAG);
        
        Assert.assertEquals(PART1_0_NAME, p1_0_name);
        Assert.assertEquals(PART2_0_NAME, p2_0_name);
        Assert.assertEquals(PART1_1_NAME, p1_1_name);
        Assert.assertEquals(PART2_1_NAME, p2_1_name);
        
    }
    
    private final static String FOOBAR_ROOT_TYPE = "/foobar";
    private final static String FOOBAR_ROOT_INSTANCE = "foobar";
    
    private final static String FOOBAR_FOO_TYPE = "/foobar/foo";
    private final static String FOOBAR_FOO1_INSTANCE = "foobar.foo1";
    private final static String FOOBAR_FOO2_INSTANCE = "foobar.foo2";
    
    private final static String FOOBAR_BAR_TYPE = "/foobar/bar";
    private final static String FOOBAR_BAR1_INSTANCE = "foobar.bar1";
    private final static String FOOBAR_BAR2_INSTANCE = "foobar.bar2";
    
    
    /**
     * Foobar has two children, foo and bar, both of which are of type DataBean
     * 
     * @throws Exception
     */
    @Test // @org.junit.Ignore
    public void testSameClassTwoChildren() throws Exception {
        ServiceLocator locator = Utilities.createLocator();
        XmlService xmlService = locator.getService(XmlService.class);
        Hub hub = locator.getService(Hub.class);
        
        URL url = getClass().getClassLoader().getResource("foobar.xml");
        
        XmlRootHandle<FooBarBean> rootHandle = xmlService.unmarshall(url.toURI(), FooBarBean.class);
        FooBarBean foobar = rootHandle.getRoot();
        Assert.assertNotNull(foobar);
        
        Assert.assertNotNull(hub.getCurrentDatabase().getInstance(FOOBAR_ROOT_TYPE, FOOBAR_ROOT_INSTANCE));
        Assert.assertNotNull(hub.getCurrentDatabase().getInstance(FOOBAR_FOO_TYPE, FOOBAR_FOO1_INSTANCE));
        Assert.assertNotNull(hub.getCurrentDatabase().getInstance(FOOBAR_FOO_TYPE, FOOBAR_FOO2_INSTANCE));
        Assert.assertNotNull(hub.getCurrentDatabase().getInstance(FOOBAR_BAR_TYPE, FOOBAR_BAR1_INSTANCE));
        Assert.assertNotNull(hub.getCurrentDatabase().getInstance(FOOBAR_BAR_TYPE, FOOBAR_BAR2_INSTANCE));
    }
    
    /**
     * Tests that an xml hierarchy with a cycle can be unmarshalled
     * 
     * @throws Exception
     */
    @Test
    public void testBeanCycle() throws Exception {
        ServiceLocator locator = Utilities.createLocator();
        XmlService xmlService = locator.getService(XmlService.class);
        
        URL url = getClass().getClassLoader().getResource(CYCLE);
        
        XmlRootHandle<RootWithCycle> rootHandle = xmlService.unmarshall(url.toURI(), RootWithCycle.class);
        RootWithCycle cycle = rootHandle.getRoot();
        
        Assert.assertNotNull(cycle);
        Assert.assertNotNull(cycle.getLeafWithCycle());
        Assert.assertNotNull(cycle.getLeafWithCycle().getRootWithCycle());
        Assert.assertNull(cycle.getLeafWithCycle().getRootWithCycle().getLeafWithCycle());
    }
    
    /**
     * Tests every scalar type that can be read
     * 
     * @throws Exception
     */
    @Test
    public void testEveryType() throws Exception {
        ServiceLocator locator = Utilities.createLocator();
        XmlService xmlService = locator.getService(XmlService.class);
        
        URL url = getClass().getClassLoader().getResource(TYPE1_FILE);
        
        XmlRootHandle<TypeBean> rootHandle = xmlService.unmarshall(url.toURI(), TypeBean.class);
        TypeBean types = rootHandle.getRoot();
        
        Assert.assertNotNull(types);
        Assert.assertEquals(13, types.getIType());
        Assert.assertEquals(-13L, types.getJType());
        Assert.assertEquals(true, types.getZType());
        Assert.assertEquals((byte) 120, types.getBType());
        Assert.assertEquals((short) 161, types.getSType());
        Assert.assertEquals(0, Float.compare((float) 3.14, types.getFType()));
        Assert.assertEquals(0, Double.compare(2.71828, types.getDType()));
    }
    
    /**
     * Tests that the annotation is fully copied over on the method
     * 
     * @throws Exception
     */
    @Test // @org.junit.Ignore
    public void testAnnotationWithEverythingCopied() throws Exception {
        ServiceLocator locator = Utilities.createLocator();
        XmlService xmlService = locator.getService(XmlService.class);
        
        URL url = getClass().getClassLoader().getResource(ACME1_FILE);
        
        XmlRootHandle<Employees> rootHandle = xmlService.unmarshall(url.toURI(), Employees.class);
        Employees employees = rootHandle.getRoot();
        
        Method setBagelMethod = employees.getClass().getMethod("setBagelPreference", new Class<?>[] { int.class });
        EverythingBagel bagel = setBagelMethod.getAnnotation(EverythingBagel.class);
        
        Assert.assertEquals((byte) 13, bagel.byteValue());
        Assert.assertTrue(bagel.booleanValue());
        Assert.assertEquals('e', bagel.charValue());
        Assert.assertEquals((short) 13, bagel.shortValue());
        Assert.assertEquals(13, bagel.intValue());
        Assert.assertEquals(13L, bagel.longValue());
        Assert.assertEquals(0, Float.compare((float) 13.00, bagel.floatValue()));
        Assert.assertEquals(0, Double.compare(13.00, bagel.doubleValue()));
        Assert.assertEquals("13", bagel.stringValue());
        Assert.assertEquals(Employees.class, bagel.classValue());
        Assert.assertEquals(GreekEnum.BETA, bagel.enumValue());
        
        Assert.assertTrue(Arrays.equals(new byte[] { 13, 14 }, bagel.byteArrayValue()));
        Assert.assertTrue(Arrays.equals(new boolean[] { true, false }, bagel.booleanArrayValue()));
        Assert.assertTrue(Arrays.equals(new char[] { 'e', 'E' }, bagel.charArrayValue()));
        Assert.assertTrue(Arrays.equals(new short[] { 13, 14 }, bagel.shortArrayValue()));
        Assert.assertTrue(Arrays.equals(new int[] { 13, 14 }, bagel.intArrayValue()));
        Assert.assertTrue(Arrays.equals(new long[] { 13, 14 }, bagel.longArrayValue()));
        Assert.assertTrue(Arrays.equals(new String[] { "13", "14" }, bagel.stringArrayValue()));
        Assert.assertTrue(Arrays.equals(new Class[] { String.class, double.class }, bagel.classArrayValue()));
        Assert.assertTrue(Arrays.equals(new GreekEnum[] { GreekEnum.GAMMA, GreekEnum.ALPHA }, bagel.enumArrayValue()));
        
        // The remaining need to be compared manually (not with Arrays)
        Assert.assertEquals(0, Float.compare((float) 13.00, bagel.floatArrayValue()[0]));
        Assert.assertEquals(0, Float.compare((float) 14.00, bagel.floatArrayValue()[1]));
        
        Assert.assertEquals(0, Double.compare(13.00, bagel.doubleArrayValue()[0]));
        Assert.assertEquals(0, Double.compare(14.00, bagel.doubleArrayValue()[1]));
    }
    
    /**
     * Tests that a list child with no elements returns an empty list (not null)
     * 
     * @throws Exception
     */
    @Test // @org.junit.Ignore
    public void testEmptyListChildReturnsEmptyList() throws Exception {
        ServiceLocator locator = Utilities.createLocator();
        XmlService xmlService = locator.getService(XmlService.class);
        
        URL url = getClass().getClassLoader().getResource(ACME1_FILE);
        
        XmlRootHandle<Employees> rootHandle = xmlService.unmarshall(url.toURI(), Employees.class);
        Employees employees = rootHandle.getRoot();
        
        List<OtherData> noChildrenList = employees.getNoChildList();
        Assert.assertNotNull(noChildrenList);
        Assert.assertTrue(noChildrenList.isEmpty());
    }
    
    /**
     * Tests that a list child with no elements returns an empty array (not null)
     * 
     * @throws Exception
     */
    @Test // @org.junit.Ignore
    public void testEmptyArrayChildReturnsEmptyArray() throws Exception {
        ServiceLocator locator = Utilities.createLocator();
        XmlService xmlService = locator.getService(XmlService.class);
        
        URL url = getClass().getClassLoader().getResource(ACME1_FILE);
        
        XmlRootHandle<Employees> rootHandle = xmlService.unmarshall(url.toURI(), Employees.class);
        Employees employees = rootHandle.getRoot();
        
        OtherData[] noChildrenList = employees.getNoChildArray();
        Assert.assertNotNull(noChildrenList);
        Assert.assertEquals(0, noChildrenList.length);
    }
}
