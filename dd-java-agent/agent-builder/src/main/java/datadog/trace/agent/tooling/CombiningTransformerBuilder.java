package datadog.trace.agent.tooling;

import static datadog.trace.agent.tooling.bytebuddy.DDTransformers.defaultTransformers;
import static net.bytebuddy.matcher.ElementMatchers.not;

import datadog.trace.agent.tooling.Instrumenter.WithPostProcessor;
import datadog.trace.agent.tooling.bytebuddy.ExceptionHandlers;
import datadog.trace.agent.tooling.context.FieldBackedContextInjector;
import datadog.trace.agent.tooling.context.FieldBackedContextMatcher;
import datadog.trace.api.InstrumenterConfig;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;

/** Builds multiple instrumentations into a single combining-matcher and splitting-transformer. */
public final class CombiningTransformerBuilder extends AbstractTransformerBuilder {
  private final AgentBuilder agentBuilder;

  private final List<MatchRecorder> matchers = new ArrayList<>();
  private final BitSet knownTypesMask;
  private AdviceStack[] transformers;
  private int nextSupplementaryId;

  // temporary buffer for collecting advice; reset for each instrumenter
  private final List<AgentBuilder.Transformer> advice = new ArrayList<>();
  private ElementMatcher<? super MethodDescription> ignoredMethods;

  /**
   * Post processor to be applied to instrumenter advices if they implement {@link
   * WithPostProcessor}
   */
  private Advice.PostProcessor.Factory postProcessor;

  public CombiningTransformerBuilder(
      AgentBuilder agentBuilder, InstrumenterIndex instrumenterIndex) {
    super(instrumenterIndex);
    this.agentBuilder = agentBuilder;
    int transformationCount = instrumenterIndex.transformationCount();
    this.knownTypesMask = new BitSet(transformationCount);
    this.transformers = new AdviceStack[transformationCount];
    this.nextSupplementaryId = transformationCount;
  }

  @Override
  protected void prepareInstrumentation(InstrumenterModule module, int instrumentationId) {}

  @Override
  protected void buildTypeInstrumentation(Instrumenter member, int transformationId) {

    if (transformationId < 0) {
      // this is an additional "dd.trace.methods" instrumenter configured at runtime
      // (a separate instance is created for each class listed in "dd.trace.methods")
      // allocate a distinct id for matching purposes to avoid mixing trace methods
      transformationId = nextSupplementaryId++;
      if (transformers.length <= transformationId) {
        transformers = Arrays.copyOf(transformers, transformationId + 1);
      }
    }

    buildInstrumentationMatcher(member, transformationId);
    buildInstrumentationAdvice(member, transformationId);
  }

  private void buildInstrumentationMatcher(Instrumenter member, int transformationId) {

    //    if (member instanceof Instrumenter.ForSingleType
    //        || member instanceof Instrumenter.ForKnownTypes) {
    //      knownTypesMask.set(id);
    //    } else if (member instanceof Instrumenter.ForTypeHierarchy) {
    //      matchers.add(new MatchRecorder.ForHierarchy(id, (Instrumenter.ForTypeHierarchy)
    // member));
    //    } else if (member instanceof Instrumenter.ForCallSite) {
    //      matchers.add(new MatchRecorder.ForType(id, ((Instrumenter.ForCallSite)
    // member).callerType()));
    //    }
    //
    //    if (member instanceof Instrumenter.ForConfiguredTypes) {
    //      Collection<String> names =
    //          ((Instrumenter.ForConfiguredTypes) member).configuredMatchingTypes();
    //      if (null != names && !names.isEmpty()) {
    //        matchers.add(new MatchRecorder.ForType(id, namedOneOf(names)));
    //      }
    //    }
    //
    //    if (member instanceof Instrumenter.CanShortcutTypeMatching
    //        && !((Instrumenter.CanShortcutTypeMatching) member).onlyMatchKnownTypes()) {
    //      matchers.add(new MatchRecorder.ForHierarchy(id, (Instrumenter.ForTypeHierarchy)
    // member));
    //    }
    //
    //    ElementMatcher<ClassLoader> classLoaderMatcher = module.classLoaderMatcher();
    //    if (classLoaderMatcher != ANY_CLASS_LOADER) {
    //      matchers.add(new MatchRecorder.NarrowLocation(id, classLoaderMatcher));
    //    }
    //
    //    if (member instanceof Instrumenter.WithTypeStructure) {
    //      matchers.add(
    //          new MatchRecorder.NarrowType(
    //              id, ((Instrumenter.WithTypeStructure) member).structureMatcher()));
    //    }
    //
    //    MuzzleCheck muzzle = new MuzzleCheck(module, instrumenterIndex.instrumenterId(module));
    //    matchers.add(new MatchRecorder.NarrowLocation(id, muzzle));
  }

  private void buildInstrumentationAdvice(Instrumenter member, int transformationId) {

    //    postProcessor =
    //        member instanceof WithPostProcessor ? ((WithPostProcessor) member).postProcessor() :
    // null;
    //
    //    String[] helperClassNames = module.helperClassNames();
    //    if (module.injectHelperDependencies()) {
    //      helperClassNames = HelperScanner.withClassDependencies(helperClassNames);
    //    }
    //    if (helperClassNames.length > 0) {
    //      advice.add(new HelperTransformer(module.getClass().getSimpleName(), helperClassNames));
    //    }
    //
    //    Map<String, String> contextStore = module.contextStore();
    //    if (!contextStore.isEmpty()) {
    //      // rewrite context store access to call FieldBackedContextStores with assigned store-id
    //      advice.add(
    //          new VisitingTransformer(
    //              new FieldBackedContextRequestRewriter(contextStore, module.name())));
    //
    //      registerContextStoreInjection(module, member, contextStore);
    //    }
    //
    //    ignoredMethods = module.methodIgnoreMatcher();
    //    if (member instanceof Instrumenter.HasTypeAdvice) {
    //      ((Instrumenter.HasTypeAdvice) member).typeAdvice(this);
    //    }
    //    if (member instanceof Instrumenter.HasMethodAdvice) {
    //      ((Instrumenter.HasMethodAdvice) member).methodAdvice(this);
    //    }
    //    transformers[id] = new AdviceStack(advice);
    //
    //    advice.clear();
  }

  @Override
  public void applyAdvice(Instrumenter.TransformingAdvice typeAdvice) {
    advice.add(typeAdvice::transform);
  }

  @Override
  public void applyAdvice(ElementMatcher<? super MethodDescription> matcher, String adviceClass) {
    Advice.WithCustomMapping customMapping = Advice.withCustomMapping();
    if (postProcessor != null) {
      customMapping = customMapping.with(postProcessor);
    }
    advice.add(
        new AgentBuilder.Transformer.ForAdvice(customMapping)
            .include(Utils.getBootstrapProxy(), Utils.getAgentClassLoader())
            .withExceptionHandler(ExceptionHandlers.defaultExceptionHandler())
            .advice(not(ignoredMethods).and(matcher), adviceClass));
  }

  @Override
  protected void applyContextStoreInjection(
      Map.Entry<String, String> contextStore, ElementMatcher<ClassLoader> activation) {
    String keyClassName = contextStore.getKey();
    String contextClassName = contextStore.getValue();

    FieldBackedContextMatcher contextMatcher =
        new FieldBackedContextMatcher(keyClassName, contextClassName);
    FieldBackedContextInjector contextAdvice =
        new FieldBackedContextInjector(keyClassName, contextClassName);

    int id = nextSupplementaryId++;

    matchers.add(new MatchRecorder.ForContextStore(id, activation, contextMatcher));
    transformers[id] = new AdviceStack(new VisitingTransformer(contextAdvice));
  }

  @Override
  public ClassFileTransformer installOn(Instrumentation instrumentation) {
    if (InstrumenterConfig.get().isRuntimeContextFieldInjection()) {
      // expand so we have enough space for a context injecting transformer for each store
      transformers = Arrays.copyOf(transformers, transformers.length + contextStoreCount());
      applyContextStoreInjection();
    }

    return agentBuilder
        .type(new CombiningMatcher(knownTypesMask, matchers))
        .and(NOT_DECORATOR_MATCHER)
        .transform(defaultTransformers())
        .transform(new SplittingTransformer(transformers))
        .installOn(instrumentation);
  }
}
