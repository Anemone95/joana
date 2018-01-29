package edu.kit.joana.wala.eval;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.config.FileOfClasses;
import com.ibm.wala.util.config.SetOfClasses;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.wala.core.ExternalCallCheck;
import edu.kit.joana.wala.core.Main;
import edu.kit.joana.wala.core.NullProgressMonitor;
import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;
import edu.kit.joana.wala.core.SDGBuilder.StaticInitializationTreatment;
import edu.kit.joana.wala.core.accesspath.AccessPath;
import edu.kit.joana.wala.flowless.MoJo;
import edu.kit.joana.wala.flowless.MoJo.CallGraphResult;
import edu.kit.joana.wala.flowless.pointsto.GraphAnnotater.Aliasing;
import edu.kit.joana.wala.flowless.pointsto.PointsToSetBuilder.PointsTo;
import edu.kit.joana.wala.util.PrettyWalaNames;

public final class RunEvalModular {

	private static final PrintStream out = System.out;
	private static final String STD_OUT_DIR = "./out/eval-modular/";
	private static final String EXCLUSION_REG_EXP = "java\\/awt\\/.*\n" + "javax\\/swing\\/.*\n" + "sun\\/awt\\/.*\n"
			+ "sun\\/swing\\/.*\n" + "com\\/sun\\/.*\n" + "sun\\/.*\n";
	private static final String AGGRESSIVE_EXCLUSION_REG_EXP = EXCLUSION_REG_EXP + "java\\/nio\\/.*\n" + "javax\\/.*\n"
			+ "java\\/util\\/.*\n" + "java\\/security\\/.*\n" + "java\\/beans\\/.*\n" + "org\\/omg\\/.*\n"
			+ "apple\\/awt\\/.*\n" + "com\\/apple\\/.*\n";
	
	private static class Run {
		public String name;
		public String entryMethod;
		public String classpath;
		public String outputDir = STD_OUT_DIR;
		public String exclusions = EXCLUSION_REG_EXP;
		
		public Run(String name, String entryMethod, String classpath) {
			this.name = name;
			this.entryMethod = entryMethod;
			this.classpath = classpath;
		}
		
		public Run(String name, String entryMethod, String classpath, String exclusions) {
			this(name, entryMethod, classpath);
			this.exclusions = exclusions;
		}
		
		public String toString() {
			return name + "(" + entryMethod + ")(" + classpath + ")";
		}
		
	}
	
	private static String RS3_GIT_PATH = "/Users/jgf/Documents/Produktiv/rs3/";

/*
de.uni.trier.infsec.protocols.smt_voting.Server.onSendResult(java.lang.String,int):
accesspathException in thread "main" java.util.ConcurrentModificationException
	at java.util.HashMap$HashIterator.nextEntry(HashMap.java:806)
	at java.util.HashMap$KeyIterator.next(HashMap.java:841)
	at edu.kit.joana.wala.core.accesspath.nodes.APNode$1.next(APNode.java:106)
	at edu.kit.joana.wala.core.accesspath.nodes.APNode$1.next(APNode.java:1)
	at edu.kit.joana.wala.core.accesspath.nodes.APNode.propagateTo(APNode.java:66)
	at edu.kit.joana.wala.core.accesspath.APIntraProc.propagateFrom(APIntraProc.java:144)
	at edu.kit.joana.wala.core.accesspath.AccessPath.propagateCalleeToSite(AccessPath.java:169)
	at edu.kit.joana.wala.core.accesspath.AccessPath.run(AccessPath.java:130)
	at edu.kit.joana.wala.core.accesspath.AccessPath.compute(AccessPath.java:78)
	at edu.kit.joana.wala.core.SDGBuilder.run(SDGBuilder.java:685)
	at edu.kit.joana.wala.core.SDGBuilder.run(SDGBuilder.java:514)
	at edu.kit.joana.wala.core.SDGBuilder.create(SDGBuilder.java:335)
	at edu.kit.joana.wala.eval.RunEvalModular.create(RunEvalModular.java:246)
	at edu.kit.joana.wala.eval.RunEvalModular.computeAccessPaths(RunEvalModular.java:204)
	at edu.kit.joana.wala.eval.RunEvalModular.exec(RunEvalModular.java:152)
	at edu.kit.joana.wala.eval.RunEvalModular.main(RunEvalModular.java:127)
 */
	
	private static String[] SKIP_METHODS = {
		"raytracer.RayTracer.shade(int,double,raytracer.Vec,raytracer.Vec,raytracer.Vec,raytracer.Isect)",
		/*
Exception in thread "main" java.lang.IllegalArgumentException: Arguments should not be null: PARAM_IN(-1_2589)[<p1@32>.P.z], null
	at edu.kit.joana.wala.core.accesspath.nodes.APGraph$APEdge.<init>(APGraph.java:48)
	at edu.kit.joana.wala.core.accesspath.nodes.APGraph$APEdge.<init>(APGraph.java:46)
	at edu.kit.joana.wala.core.accesspath.nodes.APGraph.findAliasEdges(APGraph.java:91)
	at edu.kit.joana.wala.core.accesspath.APIntraProc.compute(APIntraProc.java:53)
	at edu.kit.joana.wala.core.accesspath.AccessPath.run(AccessPath.java:117)
	at edu.kit.joana.wala.core.accesspath.AccessPath.compute(AccessPath.java:78)
	at edu.kit.joana.wala.core.SDGBuilder.run(SDGBuilder.java:685)
	at edu.kit.joana.wala.core.SDGBuilder.run(SDGBuilder.java:514)
	at edu.kit.joana.wala.core.SDGBuilder.create(SDGBuilder.java:335)
	at edu.kit.joana.wala.eval.RunEvalModular.create(RunEvalModular.java:218)
	at edu.kit.joana.wala.eval.RunEvalModular.computeAccessPaths(RunEvalModular.java:176)
	at edu.kit.joana.wala.eval.RunEvalModular.exec(RunEvalModular.java:128)
	at edu.kit.joana.wala.eval.RunEvalModular.main(RunEvalModular.java:103)
		 */
	};
	
	private static Run[] RUNS = new Run[] {
		new Run("eVoting SMT Voting", "de.uni.trier.infsec.protocols.smt_voting.HonestVotersSetup.main([Ljava/lang/String;)V", RS3_GIT_PATH + "joana-apps/eVoting-HybridApproach2013/app-code/bin",	AGGRESSIVE_EXCLUSION_REG_EXP),
//		new Run("JGF Barrier", "def.JGFBarrierBench.main([Ljava/lang/String;)V", "../../example/joana.example.jars/javagrande/benchmarks.jar",	AGGRESSIVE_EXCLUSION_REG_EXP),
//		new Run("JGF Crypt", "def.JGFCryptBenchSizeA.main([Ljava/lang/String;)V", "../../example/joana.example.jars/javagrande/benchmarks.jar",	AGGRESSIVE_EXCLUSION_REG_EXP),
//		new Run("JGF ForkJoin", "def.JGFForkJoinBench.main([Ljava/lang/String;)V", "../../example/joana.example.jars/javagrande/benchmarks.jar", AGGRESSIVE_EXCLUSION_REG_EXP),
//		new Run("JGF LUFact", "def.JGFLUFactBenchSizeA.main([Ljava/lang/String;)V",	"../../example/joana.example.jars/javagrande/benchmarks.jar", AGGRESSIVE_EXCLUSION_REG_EXP),
//		new Run("JGF MolDyn", "def.JGFMolDynBenchSizeA.main([Ljava/lang/String;)V",	"../../example/joana.example.jars/javagrande/benchmarks.jar", AGGRESSIVE_EXCLUSION_REG_EXP),
//		new Run("JGF MonteCarlo", "def.JGFMonteCarloBenchSizeA.main([Ljava/lang/String;)V",	"../../example/joana.example.jars/javagrande/benchmarks.jar", AGGRESSIVE_EXCLUSION_REG_EXP),
//		new Run("JGF RayTracer", "def.JGFRayTracerBenchSizeA.main([Ljava/lang/String;)V", "../../example/joana.example.jars/javagrande/benchmarks.jar",	AGGRESSIVE_EXCLUSION_REG_EXP),
//		new Run("JGF Series", "def.JGFSeriesBenchSizeA.main([Ljava/lang/String;)V", "../../example/joana.example.jars/javagrande/benchmarks.jar", AGGRESSIVE_EXCLUSION_REG_EXP),
//		new Run("JGF SOR", "def.JGFSORBenchSizeA.main([Ljava/lang/String;)V", "../../example/joana.example.jars/javagrande/benchmarks.jar",	AGGRESSIVE_EXCLUSION_REG_EXP),
//		new Run("JGF SparseMatmult", "def.JGFSparseMatmultBenchSizeA.main([Ljava/lang/String;)V", "../../example/joana.example.jars/javagrande/benchmarks.jar",	AGGRESSIVE_EXCLUSION_REG_EXP),
//		new Run("JGF Sync", "def.JGFSyncBench.main([Ljava/lang/String;)V", "../../example/joana.example.jars/javagrande/benchmarks.jar", AGGRESSIVE_EXCLUSION_REG_EXP),
//		new Run("HSQLDB", "org.hsqldb.Server.main([Ljava/lang/String;)V", "../../example/joana.example.jars/hsqldb/HSQLDB.jar",	AGGRESSIVE_EXCLUSION_REG_EXP),
//		new Run("jEdit", "org.gjt.sp.jedit.jEdit.main([Ljava/lang/String;)V", "../../example/joana.example.jars/jedit/jedit.jar",
//				EXCLUSION_REG_EXP + "java\\/nio\\/.*\n" + "javax\\/.*\n" + "java\\/util\\/.*\n"
//						+ "java\\/security\\/.*\n" + "java\\/beans\\/.*\n" + "org\\/omg\\/.*\n"
//						+ "apple\\/awt\\/.*\n" + "com\\/apple\\/.*\n"),
	};
	
	private RunEvalModular() {}

	public static void main(String[] args) throws ClassHierarchyException, IOException, IllegalArgumentException, CancelException, UnsoundGraphException {
		for (final Run run : RUNS) {
			exec(run);
		}
	}

	private static void exec(final Run run) throws IOException, ClassHierarchyException, IllegalArgumentException, CancelException, UnsoundGraphException {
		out.println("analyzing " + run.name);
		out.print("\tcreating class hierarchy... ");

		final AnalysisScope scope = AnalysisScopeReader.makePrimordialScope(null);
		final SetOfClasses exclusions = new FileOfClasses(new ByteArrayInputStream(run.exclusions.getBytes()));
		scope.setExclusions(exclusions);
		final ClassLoaderReference loader = scope.getLoader(AnalysisScope.APPLICATION);
		AnalysisScopeReader.addClassPathToScope(run.classpath, scope, loader, true);
		final ClassHierarchy cha = ClassHierarchyFactory.make(scope);

		out.println("done.");
		
		out.println("\tcomputing access paths... ");
		final MoJo mojo = MoJo.create(cha);
		
		int numComputed = 0;
		for (final IClass cls : cha) {
			if (cls.getClassLoader().getName() == AnalysisScope.APPLICATION) {
				for (final IMethod im : cls.getDeclaredMethods()) {
					if (isValidForAP(im)) {
						computeAccessPaths(run, mojo, im);
						numComputed++;
					}
				}
			}
		}

		out.println("(" + numComputed + " variants) done.");
	}
	
	private static boolean isInSkipMethods(final IMethod im) {
		final String name = PrettyWalaNames.methodName(im);
		for (final String m : SKIP_METHODS) {
			if (name.equals(m)) {
				return true;
			}
		}
		
		return false;
	}
	
	private static boolean isValidForAP(final IMethod im) {
		if (im.isAbstract() || im.isInit() || im.isNative() || im.isSynthetic() || im.isClinit()
				|| isInSkipMethods(im)) {
			return false;
		}
		
		int objParams = 0;
		for (int num = 0; num < im.getNumberOfParameters(); num++) {
			final TypeReference tr = im.getParameterType(num);
			if (tr.isClassType() && tr.getClassLoader().getName() == AnalysisScope.APPLICATION) {
				objParams++;
			}
		}
		
//		if (objParams <= 1) {
//			System.out.println("\nnon-valid method: " + PrettyWalaNames.methodName(im));
//		}
				
		return objParams > 1;
	}
	
	@SuppressWarnings("unused")
	private static void computeAccessPaths(final Run run, final MoJo mojo, final IMethod im) throws IllegalArgumentException, CancelException, ClassHierarchyException, IOException, UnsoundGraphException {
		out.print("\t" + PrettyWalaNames.methodName(im) + " ");
		final Aliasing alias = mojo.computeMinMaxAliasing(im);
		out.print(".");
		final PointsTo ptsMax = MoJo.computePointsTo(alias.upperBound);
		out.print(".");
		final AnalysisOptions opt = mojo.createAnalysisOptionsWithPTS(ptsMax, im);
		out.print(".");
		final CallGraphResult cgr = mojo.computeContextSensitiveCallGraph(opt);
		out.print(".");
		final SDG sdg = create(run, opt.getAnalysisScope(), mojo, cgr, im);
		out.println();
	}
	
	private static SDG create(final Run run, AnalysisScope scope, MoJo mojo, CallGraphResult cg, IMethod im) throws IOException, ClassHierarchyException, UnsoundGraphException, CancelException {
		if (!Main.checkOrCreateOutputDir(run.outputDir)) {
			out.println("Could not access/create diretory '" + run.outputDir +"'");
			return null;
		}

		out.print("Building system dependence graph... ");

		final ExternalCallCheck chk = ExternalCallCheck.EMPTY;
//		if (cfg.extern == null) {
//			chk = ExternalCallCheck.EMPTY;
//		} else {
//			chk = cfg.extern;
//		}

		final SDGBuilder.SDGBuilderConfig scfg = new SDGBuilder.SDGBuilderConfig();
		scfg.out = out;
		scfg.scope = scope;
		scfg.cache = cg.cache;
		scfg.cha = mojo.getHierarchy();
		scfg.entry = im;
		scfg.ext = chk;
		scfg.immutableNoOut = Main.IMMUTABLE_NO_OUT;
		scfg.immutableStubs = Main.IMMUTABLE_STUBS;
		scfg.ignoreStaticFields = Main.IGNORE_STATIC_FIELDS;
		scfg.exceptions = ExceptionAnalysis.IGNORE_ALL;
		scfg.accessPath = true;
		scfg.prunecg = Main.DEFAULT_PRUNE_CG;
		scfg.pts = PointsToPrecision.INSTANCE_BASED;
		scfg.debugAccessPath = false;
		scfg.debugAccessPathOutputDir = "out/";
		scfg.computeInterference = false;
		scfg.staticInitializers = StaticInitializationTreatment.NONE;
		scfg.debugStaticInitializers = false;
		scfg.fieldPropagation = FieldPropagation.OBJ_TREE_AP;
		scfg.mergeFieldsOfPrunedCalls = false;
		scfg.debugManyGraphsDotOutput = false;

		final SDGBuilder sdg = SDGBuilder.create(scfg, cg.cg, cg.pts);


		final SDG joanaSDG = SDGBuilder.convertToJoana(out, sdg, NullProgressMonitor.INSTANCE);

		AccessPath.computeMinMaxAliasSummaryEdges(out, sdg, sdg.getMainPDG(), joanaSDG, NullProgressMonitor.INSTANCE);

		out.println("\ndone.");

		out.print("Writing SDG to disk... ");
		joanaSDG.setFileName(run.name != null ? sdg.getMainMethodName() + "-" + run.name : sdg.getMainMethodName());
		final String fileName =
				(run.outputDir.endsWith(File.separator) ? run.outputDir : run.outputDir + File.separator) + joanaSDG.getFileName() + ".pdg";
		final File file = new File(fileName);
		out.print("(" + file.getAbsolutePath() + ") ");
		PrintWriter pw = new PrintWriter(file);
		SDGSerializer.toPDGFormat(joanaSDG, pw);
		out.println("done.");

		return joanaSDG;
	}

}
