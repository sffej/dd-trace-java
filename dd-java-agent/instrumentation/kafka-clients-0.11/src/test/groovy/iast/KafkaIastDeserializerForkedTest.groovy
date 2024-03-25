package iast

import com.datadog.iast.test.IastAgentTestRunner
import com.fasterxml.jackson.core.JsonParser
import datadog.trace.api.iast.IastContext
import datadog.trace.api.iast.InstrumentationBridge
import datadog.trace.api.iast.SourceTypes
import datadog.trace.api.iast.Taintable.Source
import datadog.trace.api.iast.VulnerabilityMarks
import datadog.trace.api.iast.propagation.CodecModule
import datadog.trace.api.iast.propagation.PropagationModule
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.ByteBufferDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.kafka.support.serializer.JsonDeserializer

class KafkaIastDeserializerForkedTest extends IastAgentTestRunner {

  void 'test string deserializer'() {
    given:
    final propagationModule = Mock(PropagationModule)
    final codecModule = Mock(CodecModule)
    [propagationModule, codecModule].each { InstrumentationBridge.registerIastModule(it) }

    and:
    final payload = "Hello World!".bytes
    final deserializer = new StringDeserializer()

    when:
    runUnderIastTrace {
      deserializer.configure([:], source == SourceTypes.KAFKA_MESSAGE_KEY)
      deserializer.deserialize("test", payload)
    }

    then:
    1 * propagationModule.taintObject(_ as IastContext, payload, source)
    1 * codecModule.onStringFromBytes(payload, _, _, _, _)
    0 * _

    where:
    source                          | _
    SourceTypes.KAFKA_MESSAGE_KEY   | _
    SourceTypes.KAFKA_MESSAGE_VALUE | _
  }

  void 'test byte array deserializer'() {
    given:
    final propagationModule = Mock(PropagationModule)
    InstrumentationBridge.registerIastModule(propagationModule)

    and:
    final payload = "Hello World!".bytes
    final deserializer = new ByteArrayDeserializer()

    when:
    runUnderIastTrace {
      deserializer.configure([:], source == SourceTypes.KAFKA_MESSAGE_KEY)
      deserializer.deserialize("test", payload)
    }

    then:
    1 * propagationModule.taintObject(_ as IastContext, payload, source)
    0 * _

    where:
    source                          | _
    SourceTypes.KAFKA_MESSAGE_KEY   | _
    SourceTypes.KAFKA_MESSAGE_VALUE | _
  }

  void 'test byte buffer deserializer'() {
    given:
    final propagationModule = Mock(PropagationModule)
    InstrumentationBridge.registerIastModule(propagationModule)

    and:
    final payload = "Hello World!".bytes
    final deserializer = new ByteBufferDeserializer()

    when:
    runUnderIastTrace {
      deserializer.configure([:], source == SourceTypes.KAFKA_MESSAGE_KEY)
      deserializer.deserialize("test", payload)
    }

    then:
    1 * propagationModule.taintObject(_ as IastContext, payload, source)
    1 * propagationModule.taintObjectIfTainted(_ as IastContext, payload, true, VulnerabilityMarks.NOT_MARKED)
    0 * _

    where:
    source                          | _
    SourceTypes.KAFKA_MESSAGE_KEY   | _
    SourceTypes.KAFKA_MESSAGE_VALUE | _
  }

  void 'test json deserialization'() {
    given:
    final propagationModule = Mock(PropagationModule)
    InstrumentationBridge.registerIastModule(propagationModule)

    and:
    final json = '{ "name": "Mr Bean" }'
    final payload = json.bytes
    final deserializer = new JsonDeserializer(TestBean)

    when:
    runUnderIastTrace {
      deserializer.configure([:], source == SourceTypes.KAFKA_MESSAGE_KEY)
      deserializer.deserialize('test', payload)
    }

    then:
    1 * propagationModule.taintObject(_ as IastContext, payload, source)
    1 * propagationModule.taintObjectIfTainted(_ as IastContext, _ as JsonParser, payload)
    1 * propagationModule.findSource(_ as IastContext, _) >> Stub(Source) {
      getOrigin() >> source
      getValue() >> json
    }
    1 * propagationModule.taintString(_  as IastContext, 'name', source, 'name', json)
    1 * propagationModule.taintString(_ as IastContext, 'Mr Bean', source, 'name', json)
    0 * _

    where:
    source                          | _
    SourceTypes.KAFKA_MESSAGE_KEY   | _
    SourceTypes.KAFKA_MESSAGE_VALUE | _
  }

  static class TestBean {
    String name
  }
}
