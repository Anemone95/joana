/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.eval;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.ibm.wala.util.io.FileUtil;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;
import edu.kit.joana.wala.eval.CollectData.SDGData.SumStatus;

/**
 * @author Juergen Graf <juergen.graf@gmail.com>
 */
public class CollectData {
	
	private static final String SDG_SUFFIX = ".pdg";
	private static final String SDG_REGEX = ".*\\" + SDG_SUFFIX;
	private static final String SLICING_SUFFIX = "-heavyslicing.log";
	private static final String SDG_STATS_SUFFIX = ".log";
	private static final String SDG_STATS_SIZE_SUFFIX = ".size";
	private static final String SDG_STATS_NEWSUM_SUFFIX = "-sumnew.log";
	private static final String SDG_STATS_OLDSUM_SUFFIX = "-sumold.log";
	private static final boolean USE_OLD_SUMMARY = true;
	private static final boolean CREATE_SIZE_STATS_ON_DEMAND = true;

	private static final FixedComp fixComp = new FixedComp();
	
	static {
		fixComp.add("corporatecard", 1);
		fixComp.add("purse", 2);
		fixComp.add("safeapplet", 3);
		fixComp.add("wallet", 4);
		fixComp.add("battleship", 5);
		fixComp.add("cloudstorage", 6);
		fixComp.add("clientserver", 7);
		fixComp.add("hybrid", 8);
		fixComp.add("barrier", 9);
		fixComp.add("crypt", 10);
		fixComp.add("forkjoin", 11);
		fixComp.add("lufact", 12);
		fixComp.add("moldyn", 13);
		fixComp.add("montecarlo", 14);
		fixComp.add("raytracer", 15);
		fixComp.add("series", 16);
		fixComp.add("sor", 17);
		fixComp.add("sparsematmult", 18);
		fixComp.add("sync", 19);
		fixComp.add("barcode", 20);
		fixComp.add("bexplore", 21);
		fixComp.add("j2mesafe", 22);
		fixComp.add("keepass", 23);
		fixComp.add("onetimepass", 24);
		fixComp.add("freecs", 25);
		fixComp.add("hsqldb", 26);
	}
	
	public static void main(String[] args) throws IOException {
		final CollectData cd = new CollectData();
		cd.scanDir("../../example/output");
		cd.scanDir("../../deprecated/jSDG/out");
		for (final ProgramData pd : cd.name2data.values()) {
			System.out.println(pd.name + ": " + pd.runs.size());
			pd.readData();
		}
		
		System.out.println();
		
		final StatPrinter totalTime = new StatPrinter() {
			@Override
			public String info() {
				return "total time";
			}
			
			@Override
			public String extractValue(final SDGData data) {
				return data.totalTime() + "";
			}
		};
		printStats(totalTime, cd);

		System.out.println();
		
		final StatPrinter sumTime = new StatPrinter() {
			@Override
			public String info() {
				return "summary time";
			}
			
			@Override
			public String extractValue(final SDGData data) {
				return data.summaryTime + "";
			}
		};
		printStats(sumTime, cd);

		System.out.println();
		
		final StatPrinter computationTime = new StatPrinter() {
			@Override
			public String info() {
				return "computation time";
			}
			
			@Override
			public String extractValue(final SDGData data) {
				return data.computationTime + "";
			}
		};
		printStats(computationTime, cd);

		System.out.println();
		
		final StatPrinter precision = new StatPrinter() {
			@Override
			public String info() {
				return "slice precision";
			}
			
			@Override
			public String extractValue(final SDGData data) {
				return data.slicePrecision + "%";
			}
		};
		printStats(precision, cd);

		System.out.println();

		final StatPrinter methods = new StatPrinter() {
			@Override
			public String info() {
				return "method count";
			}
			
			@Override
			public String extractValue(final SDGData data) {
				return data.numberOfMethods + "";
			}
		};
		printStats(methods, cd);

		System.out.println();

		final StatPrinter totalnodes = new StatPrinter() {
			@Override
			public String info() {
				return "total nodes";
			}
			
			@Override
			public String extractValue(final SDGData data) {
				return data.totalNumberOfNodes() + "";
			}
		};
		printStats(totalnodes, cd);

		System.out.println();

		final StatPrinter paramnodes = new StatPrinter() {
			@Override
			public String info() {
				return "param nodes";
			}
			
			@Override
			public String extractValue(final SDGData data) {
				return data.numberOfParameterNodes + "";
			}
		};
		printStats(paramnodes, cd);

		System.out.println();

		final StatPrinter edges = new StatPrinter() {
			@Override
			public String info() {
				return "edge count";
			}
			
			@Override
			public String extractValue(final SDGData data) {
				return data.totalNumberOfEdges() + "";
			}
		};
		printStats(edges, cd);

		System.out.println();

		final StatPrinter sumedge = new StatPrinter() {
			@Override
			public String info() {
				return "sum edge count";
			}
			
			@Override
			public String extractValue(final SDGData data) {
				return data.numberOfSummaryEdges + "";
			}
		};
		printStats(sumedge, cd);

		System.out.println();

		final StatPrinter linessliced = new StatPrinter() {
			@Override
			public String info() {
				return "sliced lines";
			}
			
			@Override
			public String extractValue(final SDGData data) {
				return data.sliceLinesAvg + "";
			}
		};
		printStats(linessliced, cd);

		System.out.println();

		final StatPrinter linestotal = new StatPrinter() {
			@Override
			public String info() {
				return "total lines";
			}
			
			@Override
			public String extractValue(final SDGData data) {
				return data.sliceLinesRelevant + "";
			}
		};
		printStats(linestotal, cd);
	}

	static class FixedComp implements Comparator<ProgramData> {
		private final Map<String, Integer> name2num = new HashMap<String, Integer>();
		
		public void add(final String str, final int num) {
			name2num.put(str, num);
		}

		/* (non-Javadoc)
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@Override
		public int compare(final ProgramData p1, final ProgramData p2) {
			final String o1 = p1.name;
			final String o2 = p2.name;
			
			if (name2num.containsKey(o1)) {
				if (name2num.containsKey(o2)) {
					final int i1 = name2num.get(o1);
					final int i2 = name2num.get(o2);
					return i1 - i2;
				} else {
					return -1;
				}
			} else {
				if (name2num.containsKey(o2)) {
					return 1;
				} else {
					return o1.compareTo(o2);
				}
			}
		}
		
		
	}

	public interface StatPrinter {
		String extractValue(SDGData data);
		String info();
	}

	public static void printStats(final StatPrinter sp, final CollectData cd) {
		System.out.println(sp.info() + ";unstruct-type;unstruct-inst;unstruct-obj;tree-type;tree-inst;tree-obj;"
			+ "graph-type;graph-inst;graph-obj;");
//		"graph-fp-type;graph-fp-inst;graph-fp-obj;graph-opt-type;graph-opt-inst;"
//			+ "graph-opt-obj;graph-fp-opt-type;graph-fp-opt-inst;graph-fp-opt-obj;");
		
		final LinkedList<ProgramData> pdata = new LinkedList<ProgramData>(cd.name2data.values());
		Collections.sort(pdata, fixComp);
		
		for (final ProgramData pd : pdata) {
			List<SDGData> noexc = pd.runs; //filterRuns(pd.runs, FILTER_INTRAEXC);
			//noexc = filterRuns(noexc, FILTER_SUMMARY);
			List<SDGData> tree = filterRuns(noexc, FILTER_TREE);
			List<SDGData> unstruct = filterRuns(noexc, FILTER_UNSTRUCTURED);
//			List<SDGData> graph = filterRuns(noexc, new DataFilterList(new DataFilter[] {FILTER_GRAPH_SIMPLE, FILTER_ESCAPE, FILTER_NOOPT}));
			List<SDGData> graphfp = filterRuns(noexc, new DataFilterList(new DataFilter[] { FILTER_GRAPH_FIXP /*, FILTER_ESCAPE /*, FILTER_NOOPT */}));
//			List<SDGData> graphopt = filterRuns(noexc, new DataFilterList(new DataFilter[] {FILTER_GRAPH_SIMPLE, FILTER_ESCAPE, FILTER_OPT}));
//			List<SDGData> graphfpopt = filterRuns(noexc, new DataFilterList(new DataFilter[] {FILTER_GRAPH_FIXP, FILTER_ESCAPE, FILTER_OPT}));
//			for (final SDGData d : tree) {
//				printComputationTime(d, pd.name);
//			}
//			for (final SDGData d : unstruct) {
//				printComputationTime(d, pd.name);
//			}
//			for (final SDGData d : graph) {
//				printComputationTime(d, pd.name);
//			}
//			for (final SDGData d : graphfp) {
//				printComputationTime(d, pd.name);
//			}
			System.out.print(pd.name + ";");
			printSeparated(sp, unstruct, ";", new DataFilter[] {FILTER_PTS_TYPE, FILTER_PTS_INST, FILTER_PTS_OBJ});
			printSeparated(sp, tree, ";", new DataFilter[] {FILTER_PTS_TYPE, FILTER_PTS_INST, FILTER_PTS_OBJ});
//			printSeparated(sp, graph, ";", new DataFilter[] {FILTER_PTS_TYPE, FILTER_PTS_INST, FILTER_PTS_OBJ});
			printSeparated(sp, graphfp, ";", new DataFilter[] {FILTER_PTS_TYPE, FILTER_PTS_INST, FILTER_PTS_OBJ});
//			printSeparated(sp, graphopt, ";", new DataFilter[] {FILTER_PTS_TYPE, FILTER_PTS_INST, FILTER_PTS_OBJ});
//			printSeparated(sp, graphfpopt, ";", new DataFilter[] {FILTER_PTS_TYPE, FILTER_PTS_INST, FILTER_PTS_OBJ});
			System.out.println();
		}
	}
	
	private static void printSeparated(final StatPrinter sp, final List<SDGData> data, final String separator,
			final DataFilter[] filters) {
		for (final DataFilter df : filters) {
			final List<SDGData> d = filterRuns(data, df);
			if (d.isEmpty()) {
				System.out.print("-");
			} else if (d.size() == 1) {
				final SDGData sd = d.get(0);
				System.out.print(sp.extractValue(sd));
			} else {
				System.out.print("?");
				System.err.println("abigous: multiple entrires:");
				for (final SDGData sd : d) {
					System.err.println(sd);
				}
			}
			System.out.print(separator);
		}
		//System.out.println();
	}
	
	@SuppressWarnings("unused")
	private static void printComputationTime(final SDGData d, final String name) {
		System.out.println(name + "(" + d.model  + "): " + d.computationTime + " - " + (d.summaryStat == SumStatus.OK ? d.summaryTime : d.summaryStat)  + "   " + d.totalTime());
	}
	
	@SuppressWarnings("unused")
	private static final DataFilter FILTER_ESCAPE = new DataFilter() {
		@Override
		public boolean accept(SDGData data) {
			return (data.model == ParamModel.OBJ_GRAPH_FIXP || data.model == ParamModel.OBJ_GRAPH_SIMPLE)
				&& !data.noEscape;
		}
	};
	
	private static final DataFilter FILTER_PTS_TYPE = new DataFilter() {
		@Override
		public boolean accept(SDGData data) {
			return data.pts == PointsToPrecision.TYPE_BASED;
		}
	};
	
	private static final DataFilter FILTER_PTS_INST = new DataFilter() {
		@Override
		public boolean accept(SDGData data) {
			return data.pts == PointsToPrecision.INSTANCE_BASED;
		}
	};
	
	private static final DataFilter FILTER_PTS_OBJ = new DataFilter() {
		@Override
		public boolean accept(SDGData data) {
			return data.pts == PointsToPrecision.OBJECT_SENSITIVE;
		}
	};
	
	@SuppressWarnings("unused")
	private static final DataFilter FILTER_ALLEXC = new DataFilter() {
		@Override
		public boolean accept(SDGData data) {
			return data.exc == ExceptionAnalysis.ALL_NO_ANALYSIS;
		}
	};
	
	@SuppressWarnings("unused")
	private static final DataFilter FILTER_IGNOREEXC = new DataFilter() {
		@Override
		public boolean accept(SDGData data) {
			return data.exc == ExceptionAnalysis.IGNORE_ALL;
		}
	};
	
	@SuppressWarnings("unused")
	private static final DataFilter FILTER_INTRAEXC = new DataFilter() {
		@Override
		public boolean accept(SDGData data) {
			return data.exc == ExceptionAnalysis.INTRAPROC;
		}
	};
	
	@SuppressWarnings("unused")
	private static final DataFilter FILTER_INTEREXC = new DataFilter() {
		@Override
		public boolean accept(SDGData data) {
			return data.exc == ExceptionAnalysis.INTERPROC;
		}
	};
	
	private static final DataFilter FILTER_TREE = new DataFilter() {
		@Override
		public boolean accept(SDGData data) {
			return data.model == ParamModel.OBJ_TREE;
		}
	};
	
	private static final DataFilter FILTER_GRAPH_FIXP = new DataFilter() {
		@Override
		public boolean accept(SDGData data) {
			return data.model == ParamModel.OBJ_GRAPH_FIXP;
		}
	};
	
	@SuppressWarnings("unused")
	private static final DataFilter FILTER_NOOPT = new DataFilter() {
		@Override
		public boolean accept(SDGData data) {
			return data.noOptimizations;
		}
	};
	
	@SuppressWarnings("unused")
	private static final DataFilter FILTER_OPT = new DataFilter() {
		@Override
		public boolean accept(SDGData data) {
			return !data.noOptimizations;
		}
	};
	
	@SuppressWarnings("unused")
	private static final DataFilter FILTER_GRAPH_SIMPLE = new DataFilter() {
		@Override
		public boolean accept(SDGData data) {
			return data.model == ParamModel.OBJ_GRAPH_SIMPLE;
		}
	};
	
	private static final DataFilter FILTER_UNSTRUCTURED = new DataFilter() {
		@Override
		public boolean accept(SDGData data) {
			return data.model == ParamModel.UNSTRUCTURED;
		}
	};
	
	@SuppressWarnings("unused")
	private static final DataFilter FILTER_SUMMARY = new DataFilter() {
		@Override
		public boolean accept(SDGData data) {
			return data.summaryStat == SumStatus.OK;
		}
	};
	
	private interface DataFilter {
		boolean accept(final SDGData data);
	}
	
	private static class DataFilterList implements DataFilter {
		private final DataFilter[] filters;
		
		public DataFilterList(final DataFilter[] filters) {
			this.filters = filters;
		}
		
		public boolean accept(final SDGData data) {
			for (final DataFilter df : filters) {
				if (!df.accept(data)) {
					return false;
				}
			}
			
			return true;
		}
	}
	
	private static List<SDGData> filterRuns(final List<SDGData> list, final DataFilter filter) {
		final List<SDGData> filtered = new LinkedList<SDGData>();
		for (final SDGData data : list) {
			if (filter.accept(data)) {
				filtered.add(data);
			}
		}
		
		return filtered;
	}

	public Map<String, ProgramData> name2data = new HashMap<String, CollectData.ProgramData>();
	
	public void scanDir(final String dirName) {
		final Collection<File> result = FileUtil.listFiles(dirName, SDG_REGEX, true);
		for (final File f : result) {
			final String name = extractName(f.getAbsolutePath());
			ProgramData pd = name2data.get(name);
			if (pd == null) {
				pd = new ProgramData(name);
				name2data.put(name, pd);
			}
			final SDGData sd = new SDGData(f.getAbsolutePath());
			pd.runs.add(sd);
		}
	}
	
	public static boolean checkExists(final String fileName) {
		final File f = new File(fileName);
		return f.exists() && f.isAbsolute() && f.canRead() && f.length() > 0;
	}
	
	public static String extractName(final String pdgFile) {
		String name  = null;
		if (pdgFile.contains("test_")) {
			// junit-graph file
			// typical name: C:\Users\Juergen\git\joana\example\output\test_JRE14_HSQLDB_PtsInst_Graph-noopt.pdg
			name = pdgFile.substring(pdgFile.indexOf("test_") + "test_".length());
			if (name.startsWith("J2ME_Safe")) {
				// special treatment for bogus name
				name = "J2MESafe";
			} else if (name.startsWith("JC_Safe")) {
				// special treatment for bogus name
				name = "SafeApplet"; 
			} else {
				name = name.substring(name.indexOf("_") + 1);
				if (name.startsWith("JavaGrande")) {
					name = name.substring("JavaGrande".length());
				} else if (name.startsWith("RS3")) {
					// special treatment for RS3 projects - cut off prefix
					name = name.substring("RS3".length()); 
				}
				name = name.substring(0, name.indexOf("_"));
			}
		} else {
			// tree or unstructured pdg file
			// typical name: C:\Users\Juergen\git\joana\deprecated\jSDG\out\tree-0-1-cfa\j2me-Barcode.pdg
			name = pdgFile.substring(pdgFile.lastIndexOf("-") + 1);
			name = name.substring(0, name.length() - SDG_SUFFIX.length());
			if (name.contains("KeePassJ2ME")) {
				// special treatment for bogus name
				name = "KeePass";
			}
		}
		
		return name.toLowerCase();
	}
	
	public static String statsFile(final String pdgFile) {
		return pdgFile.substring(0, pdgFile.length() - SDG_SUFFIX.length()) + SDG_STATS_SUFFIX;
	}
	
	public static String statsSizeFile(String pdgFile) {
		return pdgFile.substring(0, pdgFile.length() - SDG_SUFFIX.length()) + SDG_STATS_SIZE_SUFFIX;
	}
	
	public static String statsSumFile(final String pdgFile) {
		if (USE_OLD_SUMMARY) {
			return statsOldSumFile(pdgFile);
		} else {
			return statsNewSumFile(pdgFile);
		}
	}
	
	public static String statsNewSumFile(final String pdgFile) {
		return pdgFile + SDG_STATS_NEWSUM_SUFFIX;
	}
	
	public static String statsHeavySlicingFile(final String pdgFile) {
		return pdgFile + SLICING_SUFFIX;
	}
	
	public static String statsOldSumFile(final String pdgFile) {
		return pdgFile + SDG_STATS_OLDSUM_SUFFIX;
	}
	
	public static class ProgramData {
		final String name;
		final List<SDGData> runs = new LinkedList<SDGData>();
		
		public ProgramData(final String name) {
			this.name = name;
		}
		
		public void readData() throws IOException {
			for (final SDGData sd : runs) {
				sd.readData();
			}
		}
		
		public String toString() {
			return "program data of " + name;
		}
	}
	
	public static enum ParamModel {
		UNSTRUCTURED, OBJ_TREE, OBJ_GRAPH_FIXP, OBJ_GRAPH_SIMPLE
	}
	
	public static class SDGData {
		final String sdgFile;
		ExceptionAnalysis exc;
		PointsToPrecision pts;
		ParamModel model;
		boolean noOptimizations;
		boolean noEscape;
		int numberOfMethods;
		int numberOfNormalNodes;
		int numberOfParameterNodes;
		int numberOfNormalEdges;
		int numberOfSummaryEdges;		
		long computationTime;
		long summaryTime;
		SumStatus summaryStat = SumStatus.UNKNOWN;
		double slicePrecision;
		int sliceLinesAvg;
		int sliceLinesRelevant;
		
		public enum SumStatus { UNKNOWN, OK, TIMEOUT, OUT_OF_MEMORY }
		
		
		public SDGData(final String fileName) {
			this.sdgFile = fileName;
		}
		
		public void readData() throws IOException {
			extractParamModel();
			extractExceptionAnalysis();
			extractPointsToPrecision();
			extractNoOpt();
			extractNoEscape();
			
			final String statsFile = statsFile(sdgFile);
			if (checkExists(statsFile)) {
				final BufferedReader bIn = new BufferedReader(new FileReader(statsFile));
				final String line = bIn.readLine();
				// string for obj-tree and unstructured:
				// "114558 nodes and 1027491 edges in 12208 ms"
				// string for obj-graph stats:
				// "test_JC_Safe_PtsInst_Graph_StdNoOpt-noopt-noexc: 201 methods (201 unpruned) total of 10482 nodes and 57046 edges. Computation time was 366 ms"
				if (line.contains(" nodes")) { 
					String nodes = line.substring(0, line.indexOf(" nodes"));
					if (nodes.contains("total of ")) {
						nodes = nodes.substring(nodes.indexOf("total of ") + "total of ".length());
					}
					numberOfNormalNodes = Integer.parseInt(nodes);
				} else {
					System.err.println("illegal line found in " + statsFile);
					System.err.println(line);
				}
				
				if (line.contains(" edges")) {
					String edges = line.substring(0, line.indexOf(" edges"));
					edges = edges.substring(edges.indexOf("nodes and ") + "nodes and ".length());
					numberOfNormalEdges = Integer.parseInt(edges);
				} else {
					System.err.println("illegal line found in " + statsFile);
					System.err.println(line);
				}
				
				if (line.contains(" ms")) {
					String time = line.substring(0, line.indexOf(" ms"));
					if (time.contains("time was ")) {
						time = time.substring(time.indexOf("time was ") + "time was ".length());
					} else {
						time = time.substring(time.indexOf("edges in ") + "edges in ".length());
					}
					computationTime = Long.parseLong(time);
				} else {
					System.err.println("illegal line found in " + statsFile);
					System.err.println(line);
				}
				bIn.close();
			}
			
			final String statsNewSumFile = statsSumFile(sdgFile);
			if (checkExists(statsNewSumFile)) {
				// example line:
				// 75073 edges in 1462 ms for C:\Users\Juergen\git\joana\wala\joana.wala.eval\..\..\deprecated\jSDG\out\tree-0-1-cfa\j2me-Barcode.pdg
				final BufferedReader bIn = new BufferedReader(new FileReader(statsSumFile(sdgFile)));
				final String line = bIn.readLine();
				if (line.contains(" edges in ")) {
					final String edges = line.substring(0, line.indexOf(" edges in "));
					numberOfSummaryEdges = Integer.parseInt(edges);
					
					String sumTime = line.substring(line.indexOf(" edges in ") + " edges in ".length());
					sumTime = sumTime.substring(0, sumTime.indexOf(" ms for "));
					summaryTime = Long.parseLong(sumTime);
					summaryStat = SumStatus.OK;
				} else if (line.contains("TIMEOUT ERROR")) {
					summaryStat = SumStatus.TIMEOUT;
				} else if (line.contains("OUT OF MEMORY")) {
					summaryStat = SumStatus.OUT_OF_MEMORY;
				} else {
					System.err.println("illegal line found in " + statsNewSumFile);
					System.err.println(line);
				}
				bIn.close();
			}
			
			final String statsHeavySlicingFile = statsHeavySlicingFile(sdgFile);
			if (checkExists(statsHeavySlicingFile)) {
				final BufferedReader bIn = new BufferedReader(new FileReader(statsHeavySlicingFile(sdgFile)));
				while (bIn.ready()) {
					final String line = bIn.readLine();

					if (line.contains("lines   :")) {
						final String extract = line.substring(line.indexOf("lines   :") + "lines   :".length());
						final String percentStr = extract.substring(0, extract.indexOf("%")).replace(',','.');
						double parsedPercent = Double.parseDouble(percentStr);
						this.slicePrecision = parsedPercent;
						final String avgLines = extract.substring(extract.indexOf("(") + 1, extract.indexOf(" of"));
						this.sliceLinesAvg = Integer.parseInt(avgLines);
						final String totalLines = extract.substring(extract.indexOf("of ") + "of ".length(), extract.indexOf(")"));
						this.sliceLinesRelevant = Integer.parseInt(totalLines);
					}
				}
				
				bIn.close();
			}
			
			final String statsSizeFile = statsSizeFile(sdgFile);
			if (!checkExists(statsSizeFile) && CREATE_SIZE_STATS_ON_DEMAND) {
				createStatsSizeFile(sdgFile, statsSizeFile);
			}
			
			if (checkExists(statsSizeFile)) {
				int methodCount = 0;
				int nodeCount = 0;
				int edgeCount = 0;
				int paramCount = 0;
				int sumCount = 0;
				final BufferedReader bIn = new BufferedReader(new FileReader(statsSizeFile));
				while (bIn.ready()) {
					final String line = bIn.readLine();
					if (line.startsWith("methods ")) {
						final String mCount = line.substring("methods ".length());
						methodCount = Integer.parseInt(mCount);
					} else if (line.startsWith("nodes ")) {
						final String nCount = line.substring("nodes ".length());
						nodeCount = Integer.parseInt(nCount);
					} else if (line.startsWith("edges ")) {
						final String eCount = line.substring("edges ".length());
						edgeCount = Integer.parseInt(eCount);
					} else if (line.startsWith("param nodes ")) {
						final String pCount = line.substring("param nodes ".length(),  line.indexOf("(") - 1);
						paramCount = Integer.parseInt(pCount);
					} else if (line.startsWith("sum edges ")) {
						final String sCount = line.substring("sum edges ".length(), line.indexOf("(") - 1);
						sumCount = Integer.parseInt(sCount);
					}
				}
				bIn.close();
				this.numberOfMethods = methodCount;
				this.numberOfParameterNodes = paramCount;
				this.numberOfNormalNodes = nodeCount - paramCount;
				this.numberOfNormalEdges = edgeCount - sumCount;
				this.numberOfSummaryEdges = sumCount;
			}
		}
		
		private static void createStatsSizeFile(final String sdgFile, final String statsSizeFile) throws IOException {
			// build you own statsSizeFile
			final SDG sdg = SDG.readFromAndUseLessHeap(sdgFile);
			final int nodeCount = sdg.vertexSet().size();
			final int edgeCount = sdg.edgeSet().size();
			final int methodCount = sdg.lastProc() - 1;
			int sumEdge = 0;
			for (final SDGEdge e : sdg.edgeSet()) {
				switch (e.getKind()) {
				case SUMMARY:
				case SUMMARY_DATA:
				case SUMMARY_NO_ALIAS:
					sumEdge++;
				default:
					break;
				}
			}
			
			int paramNodes = 0;
			for (final SDGNode n : sdg.vertexSet()) {
				switch (n.kind) {
				case ACTUAL_IN:
				case ACTUAL_OUT:
				case FORMAL_IN:
				case FORMAL_OUT:
					paramNodes++;
				default:
					break;
				}
			}
			
			final PrintWriter pw = new PrintWriter(statsSizeFile);
			pw.println("methods " + methodCount);
			pw.println("nodes " + nodeCount);
			pw.println("edges " + edgeCount);
			pw.println("param nodes " + paramNodes + " (" + ((paramNodes * 100) / nodeCount) + " %)");
			pw.println("sum edges " + sumEdge + " (" + ((sumEdge * 100) / edgeCount) + " %)");
			pw.flush();
			pw.close();
		}
		
		private void extractNoOpt() {
			noOptimizations = true;
			if (model == ParamModel.OBJ_GRAPH_FIXP || model == ParamModel.OBJ_GRAPH_SIMPLE) {
				noOptimizations = false;
				if (sdgFile.contains("NoOpt") || sdgFile.contains("-noopt")) {
					noOptimizations = true;
				}
			}
		}
		
		private void extractNoEscape() {
			noEscape = false;
			if (model == ParamModel.OBJ_GRAPH_FIXP || model == ParamModel.OBJ_GRAPH_SIMPLE) {
				if (sdgFile.contains("NoEscape") || sdgFile.contains("-noesc")) {
					noEscape = true;
				}
			}
		}
		
		private void extractPointsToPrecision() {
			if (sdgFile.contains("PtsType") || sdgFile.contains("-0-cfa")) {
				pts = PointsToPrecision.TYPE_BASED;
			} else if (sdgFile.contains("PtsInst") || sdgFile.contains("-0-1-cfa")) {
				pts = PointsToPrecision.INSTANCE_BASED;
			} else if (sdgFile.contains("PtsObj") || sdgFile.contains("-objsens")) {
				pts = PointsToPrecision.OBJECT_SENSITIVE;
			} else {
				pts = PointsToPrecision.RTA;
				System.err.println("unknown points-to for filename: " + sdgFile);
			}
		}
		
		private void extractExceptionAnalysis() {
			exc = ExceptionAnalysis.INTRAPROC;
			if (sdgFile.contains("-allexc")) {
				exc = ExceptionAnalysis.ALL_NO_ANALYSIS;
			} else if (sdgFile.contains("-noexc")) {
				exc = ExceptionAnalysis.IGNORE_ALL;
			}
		}
		
		private void extractParamModel() {
			if (sdgFile.contains("Graph_Fast")) {
				model = ParamModel.OBJ_GRAPH_SIMPLE;
			} else if (sdgFile.contains("Graph_Std") || sdgFile.contains("Graph-") || sdgFile.contains("Graph.")) {
				model = ParamModel.OBJ_GRAPH_FIXP;
			} else if (sdgFile.contains("tree-")) {
				model = ParamModel.OBJ_TREE;
			} else if (sdgFile.contains("wala-")) {
				model = ParamModel.UNSTRUCTURED;
			} else {
				System.err.println("unknown parameter model for filename: " + sdgFile);
			}
		}
		
		public int totalNumberOfNodes() {
			return numberOfNormalNodes + numberOfParameterNodes;
		}
		
		public int totalNumberOfEdges() {
			return numberOfNormalEdges + numberOfSummaryEdges;
		}
		
		public long totalTime() {
			return computationTime + summaryTime;
		}
		
		public String toString() {
			return sdgFile + ": " + totalTime() + "ms";
		}
		
	}
	
	public String toString() {
		return "collected data: " + name2data.size() + " programs";
	}
}
