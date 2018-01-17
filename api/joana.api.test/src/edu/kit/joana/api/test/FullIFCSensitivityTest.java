/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collection;

import org.junit.Test;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.api.lattice.BuiltinLattices;
import edu.kit.joana.api.sdg.SDGConfig;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.api.sdg.SDGProgramPart;
import edu.kit.joana.api.test.util.ApiTestException;
import edu.kit.joana.api.test.util.JoanaPath;
import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.violations.IViolation;
import edu.kit.joana.ifc.sdg.mhpoptimization.MHPType;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.util.Stubs;
import edu.kit.joana.wala.core.SDGBuilder.DynamicDispatchHandling;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;

/**
 * @author Juergen Graf <graf@kit.edu>
 */
public class FullIFCSensitivityTest {


	public static IFCAnalysis buildAndAnnotate(final String className) throws ApiTestException {
		return buildAndAnnotate(className, PointsToPrecision.INSTANCE_BASED);
	}
	
	public static IFCAnalysis buildWithThreadsAndAnnotate(final String className, MHPType mhpType) throws ApiTestException {
		return buildAndAnnotate(className, PointsToPrecision.INSTANCE_BASED, DynamicDispatchHandling.SIMPLE, true, mhpType);
	}
	
	public static IFCAnalysis buildAndAnnotate(final String className, PointsToPrecision pts) throws ApiTestException {
		return buildAndAnnotate(className, pts, DynamicDispatchHandling.SIMPLE, false, MHPType.NONE);
	}
	
	public static IFCAnalysis buildAndAnnotate(final String className, final PointsToPrecision pts, final DynamicDispatchHandling ddisp,
			final boolean computeInterference, MHPType mhpType) throws ApiTestException {
		JavaMethodSignature mainMethod = JavaMethodSignature.mainMethodOfClass(className);
		SDGConfig config = new SDGConfig(JoanaPath.JOANA_MANY_SMALL_PROGRAMS_CLASSPATH, mainMethod.toBCString(), Stubs.JRE_15);
		config.setComputeInterferences(computeInterference);
		config.setExceptionAnalysis(ExceptionAnalysis.INTRAPROC);
		config.setFieldPropagation(FieldPropagation.OBJ_GRAPH);
		config.setPointsToPrecision(pts);
		config.setDynamicDispatchHandling(ddisp);
		config.setMhpType(mhpType);
		SDGProgram prog = null;
		
		try {
			prog = SDGProgram.createSDGProgram(config);
		} catch (ClassHierarchyException e) {
			throw new ApiTestException(e);
		} catch (IOException e) {
			throw new ApiTestException(e);
		} catch (UnsoundGraphException e) {
			throw new ApiTestException(e);
		} catch (CancelException e) {
			throw new ApiTestException(e);
		}
		
		IFCAnalysis ana = new IFCAnalysis(prog);
		SDGProgramPart secret = ana.getProgramPart("sensitivity.Security.SECRET");
		assertNotNull(secret);
		ana.addSourceAnnotation(secret, BuiltinLattices.STD_SECLEVEL_HIGH);
		SDGProgramPart output = ana.getProgramPart("sensitivity.Security.leak(I)V");
		assertNotNull(output);
		ana.addSinkAnnotation(output, BuiltinLattices.STD_SECLEVEL_LOW);
		
		return ana;
	}
	
	private static void testLeaksFound(IFCAnalysis ana, int leaks) {
		Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
		assertFalse(illegal.isEmpty());
		assertEquals(leaks, illegal.size());
	}
	
	private static void testPrecision(IFCAnalysis ana) {
		Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
		assertTrue(illegal.isEmpty());
		assertEquals(0, illegal.size());
	}
	
	@Test
	public void testFlowSensLeak() {
		try {
			IFCAnalysis ana = buildAndAnnotate("sensitivity.FlowSensLeak");
			testLeaksFound(ana, 2);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testFlowSensValid() {
		try {
			IFCAnalysis ana = buildAndAnnotate("sensitivity.FlowSensValid");
			testPrecision(ana);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testFieldSensLeak() {
		try {
			IFCAnalysis ana = buildAndAnnotate("sensitivity.FieldSensLeak");
			testLeaksFound(ana, 2);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testFieldSensValid() {
		try {
			IFCAnalysis ana = buildAndAnnotate("sensitivity.FieldSensValid");
			testPrecision(ana);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testContextSensLeak() {
		try {
			IFCAnalysis ana = buildAndAnnotate("sensitivity.ContextSensLeak");
			testLeaksFound(ana, 2);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testContextSensValid() {
		try {
			IFCAnalysis ana = buildAndAnnotate("sensitivity.ContextSensValid");
			testPrecision(ana);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testObjectSensLeak() {
		try {
			IFCAnalysis ana = buildAndAnnotate("sensitivity.ObjectSensLeak", PointsToPrecision.OBJECT_SENSITIVE);
			testLeaksFound(ana, 2);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testObjectSensValid() {
		try {
			IFCAnalysis ana = buildAndAnnotate("sensitivity.ObjectSensValid", PointsToPrecision.OBJECT_SENSITIVE);
			testPrecision(ana);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testObjectSensValidFailOnContextSens() {
		try {
			IFCAnalysis ana = buildAndAnnotate("sensitivity.ObjectSensValid", PointsToPrecision.INSTANCE_BASED);
			testLeaksFound(ana, 2);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testTimeSensValid() {
		try {
			IFCAnalysis ana = buildWithThreadsAndAnnotate("sensitivity.TimeSensValid", MHPType.PRECISE);
			ana.setTimesensitivity(true);
			testPrecision(ana);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testTimeSensLeak() {
		try {
			IFCAnalysis ana = buildWithThreadsAndAnnotate("sensitivity.TimeSensLeak", MHPType.PRECISE);
			ana.setTimesensitivity(true);
			testLeaksFound(ana, 2);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testLockSensValid() {
		try {
			IFCAnalysis ana = buildWithThreadsAndAnnotate("sensitivity.LockSensValid", MHPType.PRECISE);
			ana.setTimesensitivity(true);
			testLeaksFound(ana, 2); // with lock-sensitive IFC, this test will hopefully fail some day
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testKillingDefValid() {
		try {
			IFCAnalysis ana = buildAndAnnotate("sensitivity.KillingDefValid");
			testPrecision(ana);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testKillingDefLeak() {
		try {
			IFCAnalysis ana = buildAndAnnotate("sensitivity.KillingDefLeak");
			testLeaksFound(ana, 2);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testDynamicDispatchLeak() {
		try {
			IFCAnalysis ana = buildAndAnnotate("sensitivity.DynDispLeak", PointsToPrecision.INSTANCE_BASED, DynamicDispatchHandling.PRECISE, false, MHPType.NONE);
			testLeaksFound(ana, 6);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testDynamicDispatchLeak2() {
		try {
			IFCAnalysis ana = buildAndAnnotate("sensitivity.DynDispLeak2", PointsToPrecision.INSTANCE_BASED, DynamicDispatchHandling.PRECISE, false, MHPType.NONE);
			testLeaksFound(ana, 2);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testDynamicDispatchValid() {
		try {
			IFCAnalysis ana = buildAndAnnotate("sensitivity.DynDispValid", PointsToPrecision.INSTANCE_BASED, DynamicDispatchHandling.PRECISE, false, MHPType.NONE);
			testPrecision(ana);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testDynamicDispatchValidImprecision() {
		try {
			IFCAnalysis ana = buildAndAnnotate("sensitivity.DynDispValid", PointsToPrecision.INSTANCE_BASED, DynamicDispatchHandling.SIMPLE, false, MHPType.NONE);
			testLeaksFound(ana, 6);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}
