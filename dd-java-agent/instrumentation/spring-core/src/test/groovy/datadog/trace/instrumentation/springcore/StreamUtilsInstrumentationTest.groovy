package datadog.trace.instrumentation.springcore

import com.datadog.iast.test.IastAgentTestRunner
import datadog.trace.api.iast.InstrumentationBridge
import datadog.trace.api.iast.propagation.PropagationModule
import org.springframework.util.StreamUtils

import java.nio.charset.StandardCharsets

class StreamUtilsInstrumentationTest extends IastAgentTestRunner {

  void 'test'(){
    setup:
    InstrumentationBridge.clearIastModules()
    final module= Mock(PropagationModule)
    InstrumentationBridge.registerIastModule(module)

    when:
    runUnderIastTrace { StreamUtils.copyToString(new ByteArrayInputStream("test".getBytes()), StandardCharsets.ISO_8859_1) }

    then:
    1 * module.taintObjectIfTainted(_ as String, _ as InputStream)
    0 * _
  }
}
