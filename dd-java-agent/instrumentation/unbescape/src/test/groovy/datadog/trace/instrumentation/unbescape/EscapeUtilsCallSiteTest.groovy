package datadog.trace.instrumentation.unbescape

import com.datadog.iast.test.IastAgentTestRunner
import datadog.trace.api.iast.IastContext
import datadog.trace.api.iast.InstrumentationBridge
import datadog.trace.api.iast.VulnerabilityMarks
import datadog.trace.api.iast.propagation.PropagationModule
import foo.bar.TestEscapeUtilsSuite

class EscapeUtilsCallSiteTest extends IastAgentTestRunner {

  void 'test #method'() {
    given:
    final module = Mock(PropagationModule)
    InstrumentationBridge.registerIastModule(module)

    when:
    final result = computeUnderIastTrace { TestEscapeUtilsSuite."$method"(string) }

    then:
    result == expected
    1 * module.taintStringIfTainted(_ as IastContext, _ as String, string, false, VulnerabilityMarks.XSS_MARK)
    0 * _

    where:
    method             | string                                                            | expected
    'escapeHtml4Xml'   | '<htmlTag>"escape this < </htmlTag>'                          | '&lt;htmlTag&gt;&quot;escape this &lt; &lt;/htmlTag&gt;'
    'escapeHtml4'      | '<htmlTag>"escape this < </htmlTag>'                          | '&lt;htmlTag&gt;&quot;escape this &lt; &lt;/htmlTag&gt;'
    'escapeHtml5'      | '<htmlTag>"escape this < </htmlTag>'                          | '&lt;htmlTag&gt;&quot;escape this &lt; &lt;/htmlTag&gt;'
    'escapeHtml5Xml'   | '<htmlTag>"escape this < </htmlTag>'                          | '&lt;htmlTag&gt;&quot;escape this &lt; &lt;/htmlTag&gt;'
    'escapeJavaScript' | '<script>function a(){console.log("escape this < ")}<script>' | '<script>function a(){console.log(\\"escape this < \\")}<script>'
  }

  void 'test #method with null args not thrown exception'() {

    given:
    final module = Mock(PropagationModule)
    InstrumentationBridge.registerIastModule(module)

    when:
    runUnderIastTrace { TestEscapeUtilsSuite."$method"(null) }

    then:
    notThrown(Exception)
    0 * _

    where:
    method             | _
    'escapeHtml4Xml'   | _
    'escapeHtml4'      | _
    'escapeHtml5'      | _
    'escapeHtml5Xml'   | _
    'escapeJavaScript' | _
  }
}
