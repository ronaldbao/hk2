package org.jvnet.hk2.osgiadapter;

import static org.ops4j.pax.exam.CoreOptions.cleanCaches;
import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.systemPackage;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.workingDirectory;

import java.io.File;
import java.util.List;

import javax.inject.Inject;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.Configuration;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import test.TestModuleStartup;

import com.sun.enterprise.module.ModulesRegistry;
import com.sun.enterprise.module.bootstrap.Main;
import com.sun.enterprise.module.bootstrap.ModuleStartup;
import com.sun.enterprise.module.bootstrap.StartupContext;

/**
 * Tests to be run under OSGi
 * 
 * @author jwells
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ServiceLocatorHk2MainTest {

	/* package */ static final String GROUP_ID = "org.glassfish.hk2";
	/* package */ static final String EXT_GROUP_ID = "org.glassfish.hk2.external";

	@Inject
	private BundleContext bundleContext;

	static File cacheDir;
	static File testFile;

	static final String text = "# generated on 2 Apr 2012 18:04:09 GMT\n"
			+ "class=com.sun.enterprise.admin.cli.optional.RestoreDomainCommand,index=com.sun.enterprise.admin.cli.CLICommand:restore-domain\n"
			+ "class=com.sun.enterprise.admin.cli.optional.ListBackupsCommand,index=com.sun.enterprise.admin.cli.CLICommand:list-backups\n";

	@Configuration
	public Option[] configuration() {
		String projectVersion = System.getProperty("project.version");
		return options(
		        workingDirectory(System.getProperty("basedir") + "/target/wd"),
		        systemProperty("java.io.tmpdir").value(System.getProperty("basedir") + "/target"),
		        frameworkProperty("org.osgi.framework.storage").value(System.getProperty("basedir") + "/target/felix"),
				systemPackage("sun.misc"),
				systemPackage("javax.net.ssl"),
				systemPackage("javax.xml.bind"),
				systemPackage("javax.xml.bind.annotation"),
				systemPackage("javax.xml.bind.annotation.adapters"),
				systemPackage("javax.xml.namespace"),
				systemPackage("javax.xml.parsers"),
				systemPackage("javax.xml.stream"),
				systemPackage("javax.xml.stream.events"),
				systemPackage("javax.xml.transform"),
				systemPackage("javax.xml.transform.stream"),
				systemPackage("javax.xml.validation"),
				systemPackage("javax.annotation"),
				systemPackage("javax.script"),
				systemPackage("javax.management"),
				systemPackage("org.w3c.dom"),
				systemPackage("org.xml.sax"),
				junitBundles(),
				provision(mavenBundle().groupId(GROUP_ID).artifactId("hk2-utils").version(projectVersion).startLevel(4)),
				provision(mavenBundle().groupId(GROUP_ID).artifactId("hk2-api").version(projectVersion).startLevel(4)),
                provision(mavenBundle().groupId(GROUP_ID).artifactId("hk2-runlevel").version(projectVersion).startLevel(4)),
                provision(mavenBundle().groupId(GROUP_ID).artifactId("hk2-core").version(projectVersion).startLevel(4)),
                provision(mavenBundle().groupId(GROUP_ID).artifactId("hk2-config").version(projectVersion).startLevel(4)),
                provision(mavenBundle().groupId(GROUP_ID).artifactId(
                        "hk2-locator").version(projectVersion).startLevel(4)),
                provision(mavenBundle().groupId(EXT_GROUP_ID).artifactId(
                        "javax.inject").version(projectVersion).startLevel(4)),
 				provision(mavenBundle().groupId(EXT_GROUP_ID).artifactId(
                                                "bean-validator").version(projectVersion).startLevel(4)),
				provision(mavenBundle().groupId("org.javassist").artifactId(
						"javassist").version("3.18.1-GA").startLevel(4)),
				provision(mavenBundle().groupId(EXT_GROUP_ID).artifactId(
						"asm-all-repackaged").version(projectVersion).startLevel(4)),
				provision(mavenBundle().groupId(EXT_GROUP_ID).artifactId(
						"aopalliance-repackaged").version(projectVersion).startLevel(4)),
				provision(mavenBundle().groupId(GROUP_ID)
						.artifactId("osgi-resource-locator").version("1.0.1").startLevel(4)),
				provision(mavenBundle().groupId(GROUP_ID).artifactId(
						"class-model").version(projectVersion).startLevel(4)),
				provision(mavenBundle().groupId(GROUP_ID).artifactId(
						"osgi-adapter").version(projectVersion).startLevel(1)),
				provision(mavenBundle().groupId(GROUP_ID).artifactId(
						"test-module-startup").version(projectVersion).startLevel(4)),
				provision(mavenBundle().groupId(GROUP_ID).artifactId(
		                        "contract-bundle").version(projectVersion).startLevel(4)),
		        provision(mavenBundle().groupId(GROUP_ID).artifactId(
		                        "no-hk2-bundle").version(projectVersion).startLevel(4)),
		        provision(mavenBundle().groupId(GROUP_ID).artifactId(
		                        "sdp-management-bundle").version(projectVersion).startLevel(4)),
                                provision(mavenBundle().groupId("javax.el").artifactId("javax.el-api").version("2.2.5")),
				// systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level")
				//		.value("DEBUG"),
				cleanCaches()
		// systemProperty("com.sun.enterprise.hk2.repositories").value(cacheDir.toURI().toString()),
		// vmOption(
		// "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005" )
		);
	}

	@Test
	public <d> void testHK2Main() throws Throwable {

		try {
			Assert.assertNotNull("OSGi did not properly boot", this.bundleContext);

			final StartupContext startupContext = new StartupContext();
			final ServiceTracker hk2Tracker = new ServiceTracker(
					this.bundleContext, Main.class.getName(), null);
			hk2Tracker.open();
			final Main main = (Main) hk2Tracker.waitForService(0);

			// Expect correct subclass of Main to be registered as OSGi service
			Assert.assertEquals("org.jvnet.hk2.osgiadapter.HK2Main", main.getClass()
					.getCanonicalName());
			hk2Tracker.close();
			final ModulesRegistry mr = ModulesRegistry.class.cast(bundleContext
					.getService(bundleContext
							.getServiceReference(ModulesRegistry.class
									.getName())));

			Assert.assertEquals("org.jvnet.hk2.osgiadapter.OSGiModulesRegistryImpl",
					mr.getClass().getCanonicalName());

			final ServiceLocator serviceLocator = main.createServiceLocator(
                    mr, startupContext,null,null);

			ModulesRegistry mrFromServiceLocator = serviceLocator
					.getService(ModulesRegistry.class);
			Assert.assertEquals(mr, mrFromServiceLocator);

			// serviceLocator should have been registered as an OSGi service
			checkServiceLocatorOSGiRegistration(serviceLocator);

			// check osgi services got registered
			List<?> startLevelServices = serviceLocator
					.getAllServices(BuilderHelper
							.createContractFilter("org.osgi.service.startlevel.StartLevel"));

    		Assert.assertEquals(1, startLevelServices.size());

    		Assert.assertFalse("TestModuleStartup already called", TestModuleStartup.wasCalled);
			
    		ModuleStartup moduleStartup = main.launch(mr, null, startupContext);
			
			Assert.assertNotNull(
					"Expected a ModuleStartup that was provisioned as part of this test",
					moduleStartup);

			Assert.assertTrue("TestModuleStartup not called", TestModuleStartup.wasCalled);
					
		} catch (Exception ex) {
			if (ex.getCause() != null)
				throw ex.getCause();

			throw ex;
		} finally {
			TestModuleStartup.wasCalled=false;
		}
	}
	
	private ServiceLocator getMainServiceLocator() throws Throwable {
	    StartupContext startupContext = new StartupContext();
        ServiceTracker hk2Tracker = new ServiceTracker(
                this.bundleContext, Main.class.getName(), null);
        hk2Tracker.open();
        Main main = (Main) hk2Tracker.waitForService(0);
        
        hk2Tracker.close();
        
        ModulesRegistry mr = (ModulesRegistry) bundleContext
                .getService(bundleContext
                        .getServiceReference(ModulesRegistry.class
                                .getName()));

        ServiceLocator serviceLocator = main.createServiceLocator(
                mr, startupContext,null,null);
        
        ServiceLocatorUtilities.enableLookupExceptions(serviceLocator);
        
        return serviceLocator;
	    
	}
	
	@Test
	public <d> void testRemovalOfBundle() throws Throwable {

		try {
				
			
			final ServiceLocator serviceLocator = getMainServiceLocator();

			ModuleStartup m = serviceLocator.getService(ModuleStartup.class);
			
			Assert.assertNotNull("ModuleStartup expected", m);
			
				
			for ( Bundle b: bundleContext.getBundles() ) {
				
				if ("org.glassfish.hk2.test-module-startup".equals(b.getSymbolicName())) {
					b.stop();
					b.uninstall();
				
					break;
				}
			}
			
			Thread.sleep(2000l);
			
		    m = serviceLocator.getService(ModuleStartup.class);
		    
		    Assert.assertNull("ModuleStartup should have been removed from hk2 registry when bundle was uninstalled", m);
			
		} catch (Exception ex) {
			if (ex.getCause() != null)
				throw ex.getCause();

			throw ex;
		} finally {
			TestModuleStartup.wasCalled=false;
		}
	}
	
	private void checkServiceLocatorOSGiRegistration(
			final ServiceLocator serviceLocator) {
		ServiceReference serviceLocatorRef = bundleContext
				.getServiceReference(ServiceLocator.class.getName());

		ServiceLocator serviceLocatorFromOSGi = (ServiceLocator) bundleContext
				.getService(serviceLocatorRef);

		Assert.assertNotNull("Expected ServiceLocator to be registed in OSGi",
				serviceLocatorFromOSGi);
		Assert.assertEquals(
				"Expected same ServiceLocator in OSGi as the one passed in",
				serviceLocator, serviceLocatorFromOSGi);
	}
}
