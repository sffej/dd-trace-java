package datadog.trace.instrumentation.iastinstrumenter;

import com.google.auto.service.AutoService;
import datadog.trace.agent.tooling.InstrumenterModule;
import datadog.trace.agent.tooling.bytebuddy.csi.Advices;
import datadog.trace.agent.tooling.bytebuddy.csi.CallSiteInstrumentation;
import datadog.trace.agent.tooling.bytebuddy.csi.CallSiteSupplier;
import datadog.trace.agent.tooling.csi.CallSites;
import datadog.trace.api.Config;
import datadog.trace.api.InstrumenterConfig;
import datadog.trace.api.ProductActivation;
import datadog.trace.api.iast.IastCallSites;
import datadog.trace.api.iast.telemetry.Verbosity;
import datadog.trace.instrumentation.iastinstrumenter.telemetry.TelemetryCallSiteSupplier;
import java.util.ServiceLoader;
import java.util.Set;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

@AutoService(InstrumenterModule.class)
public class IastInstrumentation extends CallSiteInstrumentation {

  public IastInstrumentation() {
    super("IastInstrumentation");
  }

  @Override
  public ElementMatcher<TypeDescription> callerType() {
    return IastMatchers.TYPE_INSTANCE;
  }

  @Override
  public ElementMatcher<Class<?>> loadedTypeMatcher() {
    return IastMatchers.LOADED_TYPE_INSTANCE;
  }

  @Override
  public boolean isApplicable(final Set<TargetSystem> enabledSystems) {
    return enabledSystems.contains(TargetSystem.IAST)
        || (isOptOutEnabled()
            && InstrumenterConfig.get().getAppSecActivation() == ProductActivation.FULLY_ENABLED);
  }

  @Override
  protected CallSiteSupplier callSites() {
    return IastCallSiteSupplier.INSTANCE;
  }

  @Override
  protected Advices buildAdvices(final Iterable<CallSites> callSites) {
    if (Config.get().isIastHardcodedSecretEnabled()) {
      return Advices.fromCallSites(callSites, IastHardcodedSecretListener.INSTANCE);
    } else {
      return Advices.fromCallSites(callSites);
    }
  }

  protected boolean isOptOutEnabled() {
    return false;
  }

  public static final class IastMatchers {
    private static final ElementMatcher.Junction<TypeDescription> TRIE_BASED_MATCHER =
        new ElementMatcher.Junction.ForNonNullValues<TypeDescription>() {
          @Override
          protected boolean doMatch(TypeDescription target) {
            return IastExclusionTrie.apply(target.getName()) != 1;
          }
        };

    private static final ElementMatcher.Junction<TypeDescription> IGNORE_ENCLOSED =
        new ElementMatcher.Junction.ForNonNullValues<TypeDescription>() {
          @Override
          protected boolean doMatch(TypeDescription target) {
            return target.getEnclosingType() == target.getDeclaringType();
          }
        };

    public static final ElementMatcher<TypeDescription> TYPE_INSTANCE =
        Config.get().isIastEnclosedClassEnabled()
            ? TRIE_BASED_MATCHER
            : IGNORE_ENCLOSED.and(TRIE_BASED_MATCHER);

    public static final ElementMatcher<Class<?>> LOADED_TYPE_INSTANCE =
        new ElementMatcher.Junction.AbstractBase<Class<?>>() {
          @Override
          public boolean matches(final Class<?> target) {
            if (target == null) {
              return true;
            }
            return target.getEnclosingClass() == target.getDeclaringClass();
          }
        };
  }

  public static class IastCallSiteSupplier implements CallSiteSupplier {

    public static final CallSiteSupplier INSTANCE;

    static {
      CallSiteSupplier supplier = new IastCallSiteSupplier(IastCallSites.class);
      final Config config = Config.get();
      final Verbosity verbosity = config.getIastTelemetryVerbosity();
      if (verbosity != Verbosity.OFF) {
        supplier = new TelemetryCallSiteSupplier(verbosity, supplier);
      }
      INSTANCE = supplier;
    }

    private final Class<?> spiInterface;

    public IastCallSiteSupplier(final Class<?> spiInterface) {
      this.spiInterface = spiInterface;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Iterable<CallSites> get() {
      final ClassLoader targetClassLoader = CallSiteInstrumentation.class.getClassLoader();
      return (ServiceLoader<CallSites>) ServiceLoader.load(spiInterface, targetClassLoader);
    }
  }
}
