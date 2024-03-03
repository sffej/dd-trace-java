package datadog.trace.agent.tooling;

import java.util.Collections;
import java.util.List;

public final class InstrumenterIndex {

  public List<InstrumenterModule> modules() {
    return Collections.emptyList();
  }

  public int instrumentationCount() {
    return 0;
  }

  public int transformationCount() {
    return 0;
  }

  public int instrumentationId(InstrumenterModule module) {
    return -1;
  }

  public int transformationId(Instrumenter instrumenter) {
    return -1;
  }

  public static InstrumenterIndex readIndex() {
    return new InstrumenterIndex();
  }
}
