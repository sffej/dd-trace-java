package core

import datadog.trace.agent.test.AgentTestRunner
import datadog.trace.api.iast.IastContext
import datadog.trace.api.iast.InstrumentationBridge
import datadog.trace.api.iast.SourceTypes
import datadog.trace.api.iast.propagation.PropagationModule
import datadog.trace.bootstrap.instrumentation.api.AgentTracer
import datadog.trace.bootstrap.instrumentation.api.TagContext
import groovy.transform.CompileDynamic
import io.vertx.core.MultiMap
import io.vertx.core.http.CaseInsensitiveHeaders
import org.junit.Assume

@CompileDynamic
class CaseInsensitiveHeadersInstrumentationTest extends AgentTestRunner {

  private Object iastCtx

  @Override
  protected void configurePreAgent() {
    injectSysConfig('dd.iast.enabled', 'true')
  }

  void setup() {
    iastCtx = Stub(IastContext)
  }

  void 'test that get() is instrumented'() {
    given:
    final headers = new CaseInsensitiveHeaders()
    final module = Mock(PropagationModule)
    InstrumentationBridge.registerIastModule(module)
    addAll([key: 'value'], headers)

    when:
    runUnderIastTrace { headers.get('key') }

    then:
    1 * module.taintIfTainted(iastCtx, 'value', headers, SourceTypes.REQUEST_PARAMETER_VALUE, 'key')
  }

  void 'test that getAll() is instrumented'() {
    given:
    final headers = new CaseInsensitiveHeaders()
    final module = Mock(PropagationModule)
    InstrumentationBridge.registerIastModule(module)
    addAll([[key: 'value1'], [key: 'value2']], headers)

    when:
    runUnderIastTrace { headers.getAll('key') }

    then:
    1 * module.isTainted(iastCtx, headers) >> { false }
    0 * _

    when:
    runUnderIastTrace { headers.getAll('key') }

    then:
    1 * module.isTainted(iastCtx, headers) >> { true }
    1 * module.taint(iastCtx, 'value1', SourceTypes.REQUEST_PARAMETER_VALUE, 'key')
    1 * module.taint(iastCtx, 'value2', SourceTypes.REQUEST_PARAMETER_VALUE, 'key')
  }

  void 'test that names() is instrumented'() {
    given:
    final headers = new CaseInsensitiveHeaders()
    final module = Mock(PropagationModule)
    InstrumentationBridge.registerIastModule(module)
    addAll([[key: 'value1'], [key: 'value2']], headers)

    when:
    runUnderIastTrace { headers.names() }

    then:
    1 * module.isTainted(iastCtx, headers) >> { false }
    0 * _

    when:
    runUnderIastTrace { headers.names() }

    then:
    1 * module.isTainted(iastCtx, headers) >> { true }
    1 * module.taint(iastCtx, 'key', SourceTypes.REQUEST_PARAMETER_NAME, 'key')
  }

  void 'test that entries() is instrumented'() {
    given:
    // latest versions of vertx 3.x define the entries in the MultiMap interface, so we will lose propagation
    Assume.assumeTrue(hasMethod(CaseInsensitiveHeaders, 'entries'))
    final headers = new CaseInsensitiveHeaders()
    final module = Mock(PropagationModule)
    InstrumentationBridge.registerIastModule(module)
    addAll([[key: 'value1'], [key: 'value2']], headers)

    when:
    final result = runUnderIastTrace { headers.entries() }

    then:
    1 * module.isTainted(iastCtx, headers) >> { false }
    0 * _

    when:
    runUnderIastTrace { headers.entries() }

    then:
    1 * module.isTainted(iastCtx, headers) >> { true }
    result.collect { it.key }.unique().each {
      1 * module.taint(iastCtx, it, SourceTypes.REQUEST_PARAMETER_NAME, it)
    }
    result.each {
      1 * module.taint(iastCtx, it.value, SourceTypes.REQUEST_PARAMETER_VALUE, it.key)
    }
  }

  protected <E> E runUnderIastTrace(Closure<E> cl) {
    final ddctx = new TagContext().withRequestContextDataIast(iastCtx)
    final span = TEST_TRACER.startSpan("test", "test-iast-span", ddctx)
    try {
      return AgentTracer.activateSpan(span).withCloseable(cl)
    } finally {
      span.finish()
    }
  }

  private static boolean hasMethod(final Class<?> target, final String name, final Class<?>...types) {
    try {
      target.getDeclaredMethod(name, types) != null
    } catch (Throwable e) {
      return false
    }
  }

  private static void addAll(final Map<String, String> map, final MultiMap headers) {
    map.each { key, value -> headers.add(key, value) }
  }

  private static void addAll(final List<Map<String, String>> list, final MultiMap headers) {
    list.each { addAll(it, headers)  }
  }
}
