/*
 *  Copyright (c) 2013,
 *      Tobias Blaschke <code@tobiasblaschke.de>
 *  All rights reserved.

 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *
 *  3. The names of the contributors may not be used to endorse or promote
 *     products derived from this software without specific prior written
 *     permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 */
package edu.kit.joana.wala.jodroid;

import java.io.PrintStream;
import java.util.EnumSet;
import java.util.Set;

import com.ibm.wala.analysis.reflection.ReflectionContextInterpreter;
import com.ibm.wala.analysis.reflection.ReflectionContextSelector;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.dalvik.classLoader.DexIRFactory;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.parameters.DefaultInstantiationBehavior;
import com.ibm.wala.dalvik.ipa.callgraph.propagation.cfa.IntentContextInterpreter;
import com.ibm.wala.dalvik.ipa.callgraph.propagation.cfa.IntentContextSelector;
import com.ibm.wala.dalvik.util.AndroidEntryPointLocator.LocatorFlags;
import com.ibm.wala.dalvik.util.AndroidEntryPointManager;
import com.ibm.wala.dalvik.util.AndroidPreFlightChecks;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisOptions.ReflectionOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.impl.DefaultContextSelector;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKeyFactory;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.cfa.DefaultSSAInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXInstanceKeys;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IRFactory;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.util.NullProgressMonitor;

import edu.kit.joana.wala.core.CliProgressMonitor;
import edu.kit.joana.wala.core.ExternalCallCheck;
import edu.kit.joana.wala.core.Main;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;
import edu.kit.joana.wala.core.SDGBuilder.SDGBuilderConfig;
import edu.kit.joana.wala.core.SDGBuilder.StaticInitializationTreatment;
import edu.kit.joana.wala.flowless.pointsto.AliasGraph;
import edu.kit.joana.wala.flowless.spec.java.ast.MethodInfo;
import edu.kit.joana.wala.jodroid.AnalysisPresets.Preset;
// Basic representations
// Overall settings
// Building the class hierarchy..
// Controll the beahaviour of the android-model
// Controll the generaton of the call-graph and pointsto anaysis
// Building the SDG
// Intermediate Reperesentation stuff
// Additional helpers
// Prepare build

/**
 * This class provides presets for the various aspects of the SDG-generation.
 *
 *  The apply-methods only overwrite certain aspects of the presets. The 
 *  make-methods are equal to apply...(makeDefault()).
 *
 * If you changed certain aspects of the presets after retrieval using the
 * make- or apply-functions you may want to use the regenerate-function
 * to reinstantiate aspects dependent on these settings.
 *
 * @author  Tobias Blaschke <code@tobiasblaschke.de>
 * @since   2013-09-11
 */
public class AnalysisPresets {
   
   /**
     *  Encapsulates all settings made by AnalysisisPresets.
     */
    public static final class Preset {
        //
        // Main settings
        //
        /** Controls the behavior of WALA; These settings will be replaced by Joana in
         *  the later analysis. */
        public AnalysisOptions options;
        /** The settings of Joana also contain ubiquitously needed structures. */
        public SDGBuilderConfig scfg;
        /** Control how entrypoints of Android-Apps are identified. */ 
        public Set<LocatorFlags> entrypointLocatorFlags;
        //
        // Additional settings
        //
        /** helps control context-sensitivity */
        public ContextSelector ctxSel;
        /** interface to local method information needed for CFA */
        public SSAContextInterpreter ctxIpr;
        /** model instances in the heap */
        public InstanceKeyFactory instKeys;
    
        Preset(final AnalysisScope scope) {
            this.scfg = new SDGBuilderConfig();
            this.scfg.scope = scope;
            this.options = new AnalysisOptions(scope, null);
        }

        private Preset() {
            this.scfg = null;
        }

        public void setScope(final AnalysisScope scope) {
            this.scfg.scope = scope;
            this.options = new AnalysisOptions(scope, null);
        }
    }

    /**
     *  May be used by an UI or CLI; describes presets available.
     */
    public static enum PresetDescription {
        DEFAULT("Generates a ContextSensitive analysis requireing a moderate ammount of resources " +
                "(about 3GB of RAM for a small Android-App). Results produced using this setting " +
                "should be pretty usbale for an IFC-Analysis while not being as accurate as possible."),
        FAST("Having trimmed anything to fast computation this setting is useful for testing stuff " +
                "during development. No actual analysis should be made using this setting as actual " +
                "Entypoints will be missed and the result is rather inaccurate."),
        FULL("This variant enables all of the optional computations. Thus the ammount of required " +
                "reqsources is pretty high (about 10GB of RAM for a small Android-App). Needles to " +
                "say that the results will be as accurate as possible");

        public final String description;
        PresetDescription(String description) {
            this.description = description;
        }

        public static String dumpOptions() {
            String ret = "";
            for (final PresetDescription d : PresetDescription.values()) {
                ret = ret + d.toString() + ": " + d.description + "\n";
            }
            return ret;
        }
    }

    /**
     *  Dispatches to other make-functions based on a PresetDescription.
     *
     *  @param  which   The description to generate settings based on
     *  @param  scope   Defines the input-program and used librarys as well as exclusions for
     *      the analysis
     *  @param  cha     The ClassHierarchy of the anayzed program
     *  @return Preset containing Settings for "all situations"
     */
    public static Preset make(final AndroidEntryPointManager manager, final PresetDescription which, final AnalysisScope scope, final IClassHierarchy cha) {
        switch(which) {
            case DEFAULT:
                return makeDefault(manager, scope, cha);
            case FAST:
                return makeFast(manager, scope, cha);
            case FULL:
                return makeFull(manager, scope, cha);
            default:
                throw new IllegalArgumentException("The requested preset (" + which.toString() + ") is not known");
        }
    }

    /**
     *  A Preset without cha, scope and AnalysisOptions.
     */
    public static Preset makeEmpty(AndroidEntryPointManager manager) {
        final Preset p = new Preset(null);

        // The following settings are used by Joana. Based on them new settings of
        // WALAs AnalysisOptions will be generated by Joana.
        applyPrettyCLIOutput(manager, p); 
        p.scfg.cache = new AnalysisCacheImpl((IRFactory<IMethod>) new DexIRFactory());

        // On external stuff. These settings have little effect on the result.
        p.scfg.ext = makeStandardExternalCallCheck();
        p.scfg.accessPath = false;  // Setting this to true overrides other settings

        // On debugging
        p.scfg.showTypeNameInValue = false; // on true: Second type-inference often breaks
        p.scfg.debugManyGraphsDotOutput = false;

        p.scfg.immutableNoOut = Main.IMMUTABLE_NO_OUT;
        p.scfg.immutableStubs = Main.IMMUTABLE_STUBS;
        p.scfg.ignoreStaticFields = Main.IGNORE_STATIC_FIELDS;

        // What to considder
        //p.scfg.exceptions = ExceptionAnalysis.ALL_NO_ANALYSIS;
        p.scfg.exceptions = ExceptionAnalysis.IGNORE_ALL;
        p.scfg.staticInitializers = StaticInitializationTreatment.SIMPLE;   // Do not use ACCURATE: Is defunct

        // Precision
        p.scfg.pts = PointsToPrecision.INSTANCE_BASED;

        p.scfg.prunecg = Main.DEFAULT_PRUNE_CG;
        //p.scfg.fieldPropagation = FieldPropagation.OBJ_GRAPH;   // Do not use OBJ_TREE: Is outdated
        //p.scfg.fieldPropagation = FieldPropagation.OBJ_GRAPH_SIMPLE_PROPAGATION;
        p.scfg.fieldPropagation = FieldPropagation.OBJ_GRAPH_NO_FIELD_MERGE;
        p.scfg.computeInterference = false;

        // No changes to the settings of AndroidEntypointManager...
        // p.aem
        //p.entrypointLocatorFlags = EnumSet.of(LocatorFlags.EP_HEURISTIC, LocatorFlags.WITH_SUPER,  
        //        LocatorFlags.INCLUDE_CALLBACKS, LocatorFlags.CB_HEURISTIC);
        p.entrypointLocatorFlags = EnumSet.of(LocatorFlags.EP_HEURISTIC, LocatorFlags.CB_HEURISTIC,
                LocatorFlags.INCLUDE_CALLBACKS);

        manager.setDoBootSequence(false);

        return p;
    }

    public static Preset finalize(final AndroidEntryPointManager manager, Preset empty, AnalysisScope scope, IClassHierarchy cha) {
        Preset p = empty;
        p.scfg.cha = cha;

        // The preliminary settings of WALAs AnalysisOptions
        p.options.setReflectionOptions(ReflectionOptions.FULL);
        manager.setInstantiationBehavior(new DefaultInstantiationBehavior(cha));
        Util.addDefaultSelectors(p.options, cha);
        Util.addDefaultBypassLogic(p.options, scope, Util.class.getClassLoader(), cha); // ??
     
        // No changes to the settings of AndroidEntypointManager...
        // p.aem
        //p.entrypointLocatorFlags = EnumSet.of(LocatorFlags.EP_HEURISTIC, LocatorFlags.WITH_SUPER,  
        //        LocatorFlags.INCLUDE_CALLBACKS, LocatorFlags.CB_HEURISTIC);
        p.entrypointLocatorFlags = EnumSet.of(LocatorFlags.EP_HEURISTIC, LocatorFlags.CB_HEURISTIC,
                LocatorFlags.INCLUDE_CALLBACKS);

        // The following selectors will be injected when Joana generates its new 
        // AnalysisOptions
        // TODO: Add ReflectionContext ?
        p.ctxSel = new DefaultContextSelector(p.options, p.scfg.cha);
        p.ctxIpr = new DefaultSSAInterpreter(p.options, p.scfg.cache);
        p.instKeys = new ZeroXInstanceKeys(p.options, p.scfg.cha, p.ctxIpr, ZeroXInstanceKeys.NONE);

        return p;
    }

    /**
     *  Default settings as described below.
     *
     *  These settings contain:
     *      * a relatively basic setting for the context-sensitivity
     *      * no entrypoints have been set(!)
     *
     *  @return a bunch of settings
     */
    public static Preset makeDefault(final AndroidEntryPointManager manager, AnalysisScope scope, IClassHierarchy cha) {
        final Preset p = new Preset(scope);

        // The following settings are used by Joana. Based on them new settings of
        // WALAs AnalysisOptions will be generated by Joana.
        applyPrettyCLIOutput(manager, p); 
        p.scfg.cache = new AnalysisCacheImpl((IRFactory<IMethod>) new DexIRFactory());
        p.scfg.cha = cha;

        // On external stuff. These settings have little effect on the result.
        p.scfg.ext = makeStandardExternalCallCheck();
        p.scfg.accessPath = false;  // Setting this to true overrides other settings

        // On debugging
        p.scfg.showTypeNameInValue = false; // on true: Second type-inference often breaks
        p.scfg.debugManyGraphsDotOutput = false;

        p.scfg.immutableNoOut = Main.IMMUTABLE_NO_OUT;
        p.scfg.immutableStubs = Main.IMMUTABLE_STUBS;
        p.scfg.ignoreStaticFields = Main.IGNORE_STATIC_FIELDS;

        // What to considder
        //p.scfg.exceptions = ExceptionAnalysis.ALL_NO_ANALYSIS;
        p.scfg.exceptions = ExceptionAnalysis.IGNORE_ALL;
        p.scfg.staticInitializers = StaticInitializationTreatment.SIMPLE;   // Do not use ACCURATE: Is defunct

        // Precision
        p.scfg.pts = PointsToPrecision.INSTANCE_BASED;

        p.scfg.prunecg = Main.DEFAULT_PRUNE_CG;
        //p.scfg.fieldPropagation = FieldPropagation.OBJ_GRAPH;   // Do not use OBJ_TREE: Is outdated
        //p.scfg.fieldPropagation = FieldPropagation.OBJ_GRAPH_SIMPLE_PROPAGATION;
        p.scfg.fieldPropagation = FieldPropagation.OBJ_GRAPH;
        p.scfg.computeInterference = false;


        // The preliminary settings of WALAs AnalysisOptions
        p.options.setReflectionOptions(ReflectionOptions.FULL);
        manager.setInstantiationBehavior(new DefaultInstantiationBehavior(cha));
        Util.addDefaultSelectors(p.options, cha);
        Util.addDefaultBypassLogic(p.options, scope, Util.class.getClassLoader(), cha); // ??
     
        // No changes to the settings of AndroidEntypointManager...
        // p.aem
        //p.entrypointLocatorFlags = EnumSet.of(LocatorFlags.EP_HEURISTIC, LocatorFlags.WITH_SUPER,  
        //        LocatorFlags.INCLUDE_CALLBACKS, LocatorFlags.CB_HEURISTIC);
        p.entrypointLocatorFlags = EnumSet.of(LocatorFlags.EP_HEURISTIC, LocatorFlags.CB_HEURISTIC,
                LocatorFlags.INCLUDE_CALLBACKS);

        // The following selectors will be injected when Joana generates its new 
        // AnalysisOptions
        // TODO: Add ReflectionContext ?
        p.ctxSel = new DefaultContextSelector(p.options, p.scfg.cha);
        p.ctxIpr = new DefaultSSAInterpreter(p.options, p.scfg.cache);
        p.instKeys = new ZeroXInstanceKeys(p.options, p.scfg.cha, p.ctxIpr, ZeroXInstanceKeys.NONE);



        // XXX TEMP:
        manager.setDoBootSequence(false);

        return p;
    }

    public static void prepareBuild(final AndroidEntryPointManager manager, Preset p) {
        // Set overrides
        /* { // Add overrides necessary for context-free analysis (context sensitive fall back)
            // XXX CAUTION!
            // Activating Context-Free overrides will yield the cartesian product of Sources and Sinks
            // as the result of the later IFC-Analysis. This is most definetly conservative!
                final AndroidModel modeller = new AndroidModel(p.scfg.cha, p.options, p.scfg.cache);
                final MethodTargetSelector overrideStartComponent;

                try {
                    final Overrides overrides = new Overrides(modeller, p.scfg.cha, p.options, p.scfg.cache);
                    overrideStartComponent = overrides.overrideAll();
                } catch (CancelException e) {
                    throw new SDGConstructionException(e);
                }

                p.scfg.methodTargetSelector = overrideStartComponent;
        } // */        

        { // Add IntentContextSelector & -Interpreter
            // These are needed to detect the targets of Intents and replace their starts with a wrapper-function
            p.scfg.additionalContextSelector = new IntentContextSelector(manager, p.scfg.cha);
            p.scfg.additionalContextInterpreter = new IntentContextInterpreter(manager, p.scfg.cha, p.options, p.scfg.cache);
        } // */

        { // Some optional checks...
            final AndroidPreFlightChecks pfc = new AndroidPreFlightChecks(manager, p.scfg.cha);
            final boolean pass = pfc.all();
            //if (! pass) {
            //    logger.warn("Not all preFlightChecks passed");
            //}
        } // */
    }

    /**
     *  Enable optional computations in a preset.
     *
     *  @param  p   Preset to alter
     *  @return altered Preset
     *  @todo TODO: Make the full-Preset object-sensitive.
     */
    public static Preset applyFull(final AndroidEntryPointManager manager, final Preset p) {
        p.scfg.exceptions = ExceptionAnalysis.IGNORE_ALL;
        //p.scfg.exceptions = ExceptionAnalysis.INTERPROC;
        p.scfg.staticInitializers = StaticInitializationTreatment.SIMPLE;   // Do not use ACCURATE: Is defunct
        p.scfg.pts = PointsToPrecision.OBJECT_SENSITIVE;  // TODO: Enable in "full"

        // Some of the following settings will match the dafults of AndroidEntypointManager.
        // However they are repeated here to be shure.
        manager.setDoBootSequence(false);
        p.entrypointLocatorFlags = EnumSet.allOf(LocatorFlags.class);

        return p;
    }

    /**
     *  Generate a new set of settings which enable all optional coputations.
     *
     *  @param  scope   Defines the input-program and used librarys as well as exclusions for
     *      the analysis
     *  @param  cha     The ClassHierarchy of the anayzed program
     *  @return Preset containing Settings for "all situations"
     */
    public static Preset makeFull(final AndroidEntryPointManager manager, AnalysisScope scope, IClassHierarchy cha) {
        return applyFull(manager, makeDefault(manager, scope, cha));
    }

    /**
     *  Make the analysis as cheap as possible.
     *
     *  These settings barely respect context. They are intended for testing purposes.
     *  Remember to call {@link #regenerate(Preset)} when you alter these settings outside.
     *
     *  @param  p preset to alter
     *  @return altered preset
     */
    public static Preset applyFast(final AndroidEntryPointManager manager, final Preset p) {
        // Let the model use a single global instance for each type..
        p.scfg.fieldPropagation = FieldPropagation.FLAT;
        p.scfg.pts = PointsToPrecision.TYPE_BASED;

        manager.setDoBootSequence(false);
        // The setting of entrypointLocatorFlags causes missed entrypoints!
        p.entrypointLocatorFlags = EnumSet.noneOf(LocatorFlags.class);

        // Create a dummy Context Selector and interpreter...
        p.options.setReflectionOptions(ReflectionOptions.NONE);
        p.ctxSel = ReflectionContextSelector.createReflectionContextSelector(p.options);
        p.ctxIpr = ReflectionContextInterpreter.createReflectionContextInterpreter(p.scfg.cha, p.options, p.scfg.cache);

        return p;
    }

    /**
     *  Generate a new set of as cheap as possible settings.
     *
     *  @param  scope   Defines the input-program and used librarys as well as exclusions for
     *      the analysis
     *  @param  cha     The ClassHierarchy of the anayzed program
     *  @return Preset containing Settings for "all situations"
     */
    public static Preset makeFast(final AndroidEntryPointManager manager, AnalysisScope scope, IClassHierarchy cha) {
        return applyFast(manager, makeDefault(manager, scope, cha));
    }


    //
    // Output options
    //
    
    /**
     *  Describes available output-settings
     */
    public static enum OutputDescription {
        BASIC("Writes Joana-Output to the console and uses a progress-monitor with dots."),
        PRETTY("Writes less output to the console and uses a pretty progress-monitor."),
        QUIET("Does not produce any output."),
        VERBOSE("Produces a lot of output."),
        DEBUG("Set the log-level to debug and disables progress-monitoring.");

        public final String description;
        OutputDescription(final String description) {
            this.description = description;
        }
    }

    /**
     *  Dispatches to output-application-methods.
     */
    public static Preset applyOutput(final AndroidEntryPointManager manager, final OutputDescription output, final Preset p) {
        switch(output) {
            case BASIC:
                return applyBasicCLIOutput(manager, p);
            case PRETTY:
                return applyPrettyCLIOutput(manager, p);
            case QUIET:
                return applyQuietOutput(manager, p);
            case VERBOSE:
                return applyVerboseOutput(manager, p);
            case DEBUG:
                return applyDebugOutput(manager, p);
            default:
                throw new IllegalArgumentException("Unknown output variant: " + output.toString());
        }
    }

    /**
     *  Pretty progress-bar, reduced logging, less Joana-Output.
     */
    public static Preset applyPrettyCLIOutput(final AndroidEntryPointManager manager, final Preset p) {
        PrintStream nullPrinter;
        try {
            nullPrinter = new PrintStream("joana.log"); // TODO: fails!
        } catch (Exception e) {
            nullPrinter = null;
        }

        p.scfg.out = nullPrinter;
        manager.setProgressMonitor(new CliProgressMonitor(System.out));

        return p;
    }

    /**
     *  All Joana-Output, reduced logging, progress-meter with dots.
     */
    public static Preset applyBasicCLIOutput(final AndroidEntryPointManager manager, final Preset p) {
        p.scfg.out = System.out;
        manager.setProgressMonitor(new edu.kit.joana.wala.util.VerboseProgressMonitor(System.out));

        return p;
    }

    /**
     *  All Joana-Output, more logging, progress-meter with dots.
     */
    public static Preset applyVerboseOutput(final AndroidEntryPointManager manager, final Preset p) {
        applyBasicCLIOutput(manager, p);
        // TODO: Set log-level

        return p;
    }

    /**
     *  Do not generate any console-output.
     */
    public static Preset applyQuietOutput(final AndroidEntryPointManager manager, final Preset p) {
        PrintStream nullPrinter;
        try {
            nullPrinter = new PrintStream("joana.log"); // TODO: fails!
        } catch (Exception e) {
            nullPrinter = null;
        }

        p.scfg.out = nullPrinter;
        manager.setProgressMonitor(new NullProgressMonitor());
        // TODO: Set log-level

        return p;
    }

    /**
     *  All Joana-Output, full debug logging, progress-meter with dots.
     */
    public static Preset applyDebugOutput(final AndroidEntryPointManager manager, final Preset p) {
        applyVerboseOutput(manager, p);
        // TODO: Set log-level

        return p;
    }

    //
    //  Only private helper-constructions follow below...
    //

    private static ExternalCallCheck makeStandardExternalCallCheck() {
        return new ExternalCallCheck() {
            @Override
            public boolean isCallToModule(SSAInvokeInstruction invk) {
                return false;
            }

            @Override
            public void registerAliasContext(SSAInvokeInstruction invk, int callNodeId, AliasGraph.MayAliasGraph context) {
            }

            @Override
            public void setClassHierarchy(IClassHierarchy cha) {
            }

            @Override
            public MethodInfo checkForModuleMethod(IMethod im) {
                return null;
            }

            @Override
            public boolean resolveReflection() {
                return false;
            }
        };
    }
}

