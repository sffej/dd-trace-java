import datadog.trace.agent.test.AgentTestRunner
import datadog.trace.agent.tooling.bytebuddy.outline.OutlineTypeParser
import datadog.trace.instrumentation.iastinstrumenter.IastInstrumentation
import net.bytebuddy.description.type.TypeDescription

import java.nio.file.Files
import java.nio.file.Paths

/**
 * When enclosed classes support is enabled only non loaded classed classes will be taken into account
 */
class EnclosedForkedTest extends AgentTestRunner {

  void 'test Iast Instrumentation enablement'() {
    given:
    final instrumentation = new IastInstrumentation()

    when:
    final type = descriptionOf(clazz)
    final typeMatches = instrumentation.callerType().matches(type)

    then: 'non loaded classes should always match'
    typeMatches == expectedMatch(type)
    (type.enclosingType != null) == enclosed

    when:
    final loaded = loadClass(clazz)
    final loadedTypeMatches = instrumentation.loadedTypeMatcher().matches(loaded)

    then:
    loadedTypeMatches == expectedMatch(loaded)
    (loaded.getEnclosingClass() != null) == enclosed

    where:
    clazz                         | enclosed
    'OuterClass'                  | false
    'OuterClass$1'                | true
    'OuterClass$InnerClass'       | true
    'OuterClass$InnerStaticClass' | true
  }

  protected boolean expectedMatch(final TypeDescription type) {
    return true // by default non loaded types should always match
  }

  protected boolean expectedMatch(final Class<?> type) {
    return type.enclosingClass == type.declaringClass // by default exclude enclosed classes when retransformed
  }

  protected TypeDescription descriptionOf(final String name) {
    final data = fetchClass(name)
    return new OutlineTypeParser().parse(data)
  }

  protected Class<?> loadClass(final String name) {
    return getClass().getClassLoader().loadClass(name)
  }

  protected static byte[] fetchClass(final String name) {
    final folder = Paths.get(Thread.currentThread().contextClassLoader.getResource('.').toURI())
    final root = folder.parent.parent
    final fileSeparatorPattern = File.separator == "\\" ? "\\\\" : File.separator
    final classFile = name.replaceAll('\\.', fileSeparatorPattern) + '.class'
    final groovy = root.resolve('groovy/test').resolve(classFile)
    if (Files.exists(groovy)) {
      return groovy.toFile().bytes
    }
    return root.resolve('java/test').resolve(classFile).toFile().bytes
  }
}
