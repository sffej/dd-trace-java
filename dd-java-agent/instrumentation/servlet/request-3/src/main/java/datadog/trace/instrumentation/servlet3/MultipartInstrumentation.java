package datadog.trace.instrumentation.servlet3;

import static datadog.trace.agent.tooling.bytebuddy.matcher.HierarchyMatchers.implementsInterface;
import static datadog.trace.agent.tooling.bytebuddy.matcher.NameMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import com.google.auto.service.AutoService;
import datadog.trace.advice.ActiveRequestContext;
import datadog.trace.advice.RequiresRequestContext;
import datadog.trace.agent.tooling.Instrumenter;
import datadog.trace.agent.tooling.InstrumenterModule;
import datadog.trace.api.gateway.RequestContext;
import datadog.trace.api.gateway.RequestContextSlot;
import datadog.trace.api.iast.IastContext;
import datadog.trace.api.iast.InstrumentationBridge;
import datadog.trace.api.iast.Source;
import datadog.trace.api.iast.SourceTypes;
import datadog.trace.api.iast.propagation.PropagationModule;
import java.util.Collection;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

@AutoService(InstrumenterModule.class)
public class MultipartInstrumentation extends InstrumenterModule.Iast
    implements Instrumenter.ForTypeHierarchy {

  public MultipartInstrumentation() {
    super("servlet", "multipart");
  }

  public String muzzleDirective() {
    return "servlet-3.x";
  }

  @Override
  public String hierarchyMarkerType() {
    return "javax.servlet.http.Part";
  }

  @Override
  public ElementMatcher<TypeDescription> hierarchyMatcher() {
    return implementsInterface(named(hierarchyMarkerType()));
  }

  @Override
  public void methodAdvice(MethodTransformer transformer) {
    transformer.applyAdvice(
        named("getName").and(isPublic()).and(takesArguments(0)),
        getClass().getName() + "$GetNameAdvice");
    transformer.applyAdvice(
        named("getHeader").and(isPublic()).and(takesArguments(String.class)),
        getClass().getName() + "$GetHeaderAdvice");
    transformer.applyAdvice(
        named("getHeaders").and(isPublic()).and(takesArguments(String.class)),
        getClass().getName() + "$GetHeadersAdvice");
    transformer.applyAdvice(
        named("getHeaderNames").and(isPublic()).and(takesArguments(0)),
        getClass().getName() + "$GetHeaderNamesAdvice");
  }

  @RequiresRequestContext(RequestContextSlot.IAST)
  public static class GetNameAdvice {
    @Advice.OnMethodExit(suppress = Throwable.class)
    @Source(SourceTypes.REQUEST_MULTIPART_PARAMETER)
    public static String onExit(
        @Advice.Return final String name, @ActiveRequestContext RequestContext reqCtx) {
      final PropagationModule module = InstrumentationBridge.PROPAGATION;
      if (module != null) {
        IastContext ctx = reqCtx.getData(RequestContextSlot.IAST);
        module.taint(ctx, name, SourceTypes.REQUEST_MULTIPART_PARAMETER, "Content-Disposition");
      }
      return name;
    }
  }

  @RequiresRequestContext(RequestContextSlot.IAST)
  public static class GetHeaderAdvice {
    @Advice.OnMethodExit(suppress = Throwable.class)
    @Source(SourceTypes.REQUEST_MULTIPART_PARAMETER)
    public static String onExit(
        @Advice.Return final String value,
        @Advice.Argument(0) final String name,
        @ActiveRequestContext RequestContext reqCtx) {
      final PropagationModule module = InstrumentationBridge.PROPAGATION;
      if (module != null) {
        IastContext ctx = reqCtx.getData(RequestContextSlot.IAST);
        module.taint(ctx, value, SourceTypes.REQUEST_MULTIPART_PARAMETER, name);
      }
      return value;
    }
  }

  @RequiresRequestContext(RequestContextSlot.IAST)
  public static class GetHeadersAdvice {
    @Advice.OnMethodExit(suppress = Throwable.class)
    @Source(SourceTypes.REQUEST_MULTIPART_PARAMETER)
    public static void onExit(
        @Advice.Argument(0) final String headerName,
        @Advice.Return Collection<String> headerValues,
        @ActiveRequestContext RequestContext reqCtx) {
      if (null == headerValues || headerValues.isEmpty()) {
        return;
      }
      final PropagationModule module = InstrumentationBridge.PROPAGATION;
      if (module != null) {
        final IastContext ctx = reqCtx.getData(RequestContextSlot.IAST);
        for (final String value : headerValues) {
          module.taint(ctx, value, SourceTypes.REQUEST_MULTIPART_PARAMETER, headerName);
        }
      }
    }
  }

  @RequiresRequestContext(RequestContextSlot.IAST)
  public static class GetHeaderNamesAdvice {
    @Advice.OnMethodExit(suppress = Throwable.class)
    @Source(SourceTypes.REQUEST_MULTIPART_PARAMETER)
    public static void onExit(
        @Advice.Return final Collection<String> headerNames,
        @ActiveRequestContext RequestContext reqCtx) {
      if (null == headerNames || headerNames.isEmpty()) {
        return;
      }
      final PropagationModule module = InstrumentationBridge.PROPAGATION;
      if (module != null) {
        final IastContext ctx = reqCtx.getData(RequestContextSlot.IAST);
        for (final String name : headerNames) {
          module.taint(ctx, name, SourceTypes.REQUEST_MULTIPART_PARAMETER);
        }
      }
    }
  }
}
