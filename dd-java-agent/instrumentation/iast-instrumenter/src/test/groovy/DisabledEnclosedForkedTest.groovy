import datadog.trace.api.config.IastConfig
import net.bytebuddy.description.type.TypeDescription

/**
 * When enclosed classes support is disabled only top level classes should be instrumented by IAST
 */
class DisabledEnclosedForkedTest extends EnclosedForkedTest {

  void setup() {
    injectSysConfig(IastConfig.IAST_ENCLOSED_CLASS_ENABLED, 'false')
  }

  @Override
  protected boolean expectedMatch(TypeDescription type) {
    return type.enclosingType == type.declaringType
  }

  @Override
  protected boolean expectedMatch(Class<?> type) {
    return type.enclosingClass == type.declaringClass
  }
}
