import com.datadog.iast.test.IastAgentTestRunner
import datadog.trace.api.iast.IastContext
import datadog.trace.api.iast.InstrumentationBridge
import datadog.trace.api.iast.propagation.PropagationModule
import datadog.trace.api.iast.sink.SsrfModule
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.HttpMethod
import org.apache.commons.httpclient.methods.GetMethod
import spock.lang.AutoCleanup
import spock.lang.Shared

import static datadog.trace.agent.test.server.http.TestHttpServer.httpServer

class IastCommonsHttpClientInstrumentationTest extends IastAgentTestRunner {

  @AutoCleanup
  @Shared
  def server = httpServer {
    handlers {
      prefix('/') {
        String msg = "Hello."
        response.status(200).send(msg)
      }
    }
  }

  @Shared
  Map<?, ?> tainteds = new IdentityHashMap<>()

  void setup() {
    tainteds.clear()
    mockPropagation()
  }

  void 'test ssrf'() {
    given:
    final url = server.address.toString()
    tainteds.put(url, null)
    final ssrf = Mock(SsrfModule)
    InstrumentationBridge.registerIastModule(ssrf)

    when:
    runUnderIastTrace { new HttpClient().executeMethod(new GetMethod(url)) }

    then:
    1 * ssrf.onURLConnection({ value -> tainteds.containsKey(value) })
  }

  private void mockPropagation() {
    final propagation = Mock(PropagationModule) {
      taintObjectIfTainted(_ as IastContext, _ as HttpMethod, _ as String) >> {
        final method = it[1] as HttpMethod
        final url = it[2] as String
        if (tainteds.containsKey(url)) {
          tainteds.put(method, null)
        }
      }
    }
    InstrumentationBridge.registerIastModule(propagation)
  }
}
