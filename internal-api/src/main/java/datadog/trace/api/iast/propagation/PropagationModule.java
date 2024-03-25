package datadog.trace.api.iast.propagation;

import static datadog.trace.api.iast.VulnerabilityMarks.NOT_MARKED;

import datadog.trace.api.iast.IastContext;
import datadog.trace.api.iast.IastModule;
import datadog.trace.api.iast.Taintable.Source;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Main API for propagation of tainted values, */
@SuppressWarnings("unused")
public interface PropagationModule extends IastModule {

  /**
   * Taints the object with a source with the selected origin and no name, if target is a char
   * sequence it will be used as value
   */
  default void taintObject(@Nonnull IastContext ctx, @Nullable Object target, byte origin) {
    taintObject(ctx, target, origin, null);
  }

  /** @see #taintObject(IastContext, Object, byte) */
  default void taintString(@Nonnull IastContext ctx, @Nullable String target, byte origin) {
    taintString(ctx, target, origin, null);
  }

  /**
   * Taints the object with a source with the selected origin and name, if target is a char sequence
   * it will be used as value
   */
  default void taintObject(
      @Nonnull IastContext ctx, @Nullable Object target, byte origin, @Nullable CharSequence name) {
    taintObject(ctx, target, origin, name, target);
  }

  /** @see #taintObject(IastContext, Object, byte, CharSequence) */
  default void taintString(
      @Nonnull IastContext ctx, @Nullable String target, byte origin, @Nullable CharSequence name) {
    taintString(ctx, target, origin, name, target);
  }

  /** Taints the object with a source with the selected origin, name and value */
  void taintObject(
      @Nonnull IastContext ctx,
      @Nullable Object target,
      byte origin,
      @Nullable CharSequence name,
      @Nullable Object value);

  /** @see #taintObject(IastContext, Object, byte, CharSequence, Object) */
  void taintString(
      @Nonnull IastContext ctx,
      @Nullable String target,
      byte origin,
      @Nullable CharSequence name,
      @Nullable CharSequence value);

  /**
   * Taints the object with a source with the selected origin, range and no name. If target is a
   * char sequence it will be used as value.
   *
   * <p>If the value is already tainted this method will append a new range.
   */
  void taintObjectRange(
      @Nonnull IastContext ctx, @Nullable Object target, byte origin, int start, int length);

  /** @see #taintObjectRange(IastContext, Object, byte, int, int) */
  void taintStringRange(
      @Nonnull IastContext ctx, @Nullable String target, byte origin, int start, int length);

  /**
   * Taints the object only if the input value is tainted. If tainted, it will use the highest
   * priority source of the input to taint the object.
   */
  default void taintObjectIfTainted(
      @Nonnull IastContext ctx, @Nullable Object target, @Nullable Object input) {
    taintObjectIfTainted(ctx, target, input, false, NOT_MARKED);
  }

  /** @see #taintObjectIfTainted(IastContext, Object, Object) */
  default void taintStringIfTainted(
      @Nonnull IastContext ctx, @Nullable String target, @Nullable Object input) {
    taintStringIfTainted(ctx, target, input, false, NOT_MARKED);
  }

  /**
   * Taints the object only if the input value is tainted. It will try to reuse sources from the
   * input value according to:
   *
   * <ul>
   *   <li>keepRanges=true will reuse the ranges from the input tainted value and mark them
   *   <li>keepRanges=false will use the highest priority source from the input ranges and mark it
   * </ul>
   */
  void taintObjectIfTainted(
      @Nonnull IastContext ctx,
      @Nullable Object target,
      @Nullable Object input,
      boolean keepRanges,
      int mark);

  /** @see #taintObjectIfTainted(IastContext, Object, Object, boolean, int) */
  void taintStringIfTainted(
      @Nonnull IastContext ctx,
      @Nullable String target,
      @Nullable Object input,
      boolean keepRanges,
      int mark);

  /**
   * Taints the object only if the input value has a tainted range that intersects with the
   * specified range. It will try to reuse sources from the input value according to:
   *
   * <ul>
   *   <li>keepRanges=true will reuse the ranges from the intersection and mark them
   *   <li>keepRanges=false will use the highest priority source from the intersection and mark it
   * </ul>
   */
  void taintObjectIfRangeTainted(
      @Nonnull IastContext ctx,
      @Nullable Object target,
      @Nullable Object input,
      int start,
      int length,
      boolean keepRanges,
      int mark);

  /** @see #taintObjectIfTainted(IastContext, Object, Object, boolean, int) */
  void taintStringIfRangeTainted(
      @Nonnull IastContext ctx,
      @Nullable String target,
      @Nullable Object input,
      int start,
      int length,
      boolean keepRanges,
      int mark);

  /**
   * Taints the object only if the input value is tainted, the resulting value will be tainted using
   * a source with the specified origin and no name, if target is a char sequence it will be used as
   * value
   */
  default void taintObjectIfTainted(
      @Nonnull IastContext ctx, @Nullable Object target, @Nullable Object input, byte origin) {
    taintObjectIfTainted(ctx, target, input, origin, null, target);
  }

  /** @see #taintObjectIfTainted(IastContext, Object, Object, byte) */
  default void taintStringIfTainted(
      @Nonnull IastContext ctx, @Nullable String target, @Nullable Object input, byte origin) {
    taintStringIfTainted(ctx, target, input, origin, null, target);
  }

  /**
   * Taints the object only if the input value is tainted, the resulting value will be tainted using
   * a source with the specified origin and name, if target is a char sequence it will be used as
   * value
   */
  default void taintObjectIfTainted(
      @Nonnull IastContext ctx,
      @Nullable Object target,
      @Nullable Object input,
      byte origin,
      @Nullable CharSequence name) {
    taintObjectIfTainted(ctx, target, input, origin, name, target);
  }

  /** @see #taintObjectIfTainted(IastContext, Object, Object, byte, CharSequence) */
  default void taintStringIfTainted(
      @Nonnull IastContext ctx,
      @Nullable String target,
      @Nullable Object input,
      byte origin,
      @Nullable CharSequence name) {
    taintStringIfTainted(ctx, target, input, origin, name, target);
  }

  /**
   * Taints the object only if the input value is tainted, the resulting value will be tainted using
   * a source with the specified origin, name and value.
   */
  void taintObjectIfTainted(
      @Nonnull IastContext ctx,
      @Nullable Object target,
      @Nullable Object input,
      byte origin,
      @Nullable CharSequence name,
      @Nullable Object value);

  /** @see #taintObjectIfTainted(IastContext, Object, Object, byte, CharSequence, Object) */
  void taintStringIfTainted(
      @Nonnull IastContext ctx,
      @Nullable String target,
      @Nullable Object input,
      byte origin,
      @Nullable CharSequence name,
      @Nullable Object value);

  /**
   * Taints the object if any of the inputs is tainted. When a tainted input is found the logic is
   * the same as in {@link #taintObjectIfTainted(IastContext, Object, Object)}
   *
   * @see #taintObjectIfTainted(IastContext, Object, Object)
   */
  default void taintObjectIfAnyTainted(
      @Nonnull IastContext ctx, @Nullable Object target, @Nullable Object[] inputs) {
    taintObjectIfAnyTainted(ctx, target, inputs, false, NOT_MARKED);
  }

  /** @see #taintObjectIfAnyTainted(IastContext, Object, Object[]) */
  default void taintStringIfAnyTainted(
      @Nonnull IastContext ctx, @Nullable String target, @Nullable Object[] inputs) {
    taintStringIfAnyTainted(ctx, target, inputs, false, NOT_MARKED);
  }

  /**
   * Taints the object if any of the inputs is tainted. When a tainted input is found the logic is
   * the same as in {@link #taintObjectIfTainted(IastContext, Object, Object, boolean, int)}
   *
   * @see #taintObjectIfTainted(IastContext, Object, Object, boolean, int)
   */
  void taintObjectIfAnyTainted(
      @Nonnull IastContext ctx,
      @Nullable Object target,
      @Nullable Object[] inputs,
      boolean keepRanges,
      int mark);

  /** @see #taintObjectIfAnyTainted(IastContext, Object, Object[], boolean, int) */
  void taintStringIfAnyTainted(
      @Nonnull IastContext ctx,
      @Nullable String target,
      @Nullable Object[] inputs,
      boolean keepRanges,
      int mark);

  /**
   * Visit the graph of the object and taints all the string properties found using a source with
   * the selected origin and no name.
   *
   * @param classFilter filter for types that should be included in the visiting process
   * @return number of tainted elements
   */
  int taintDeeply(
      @Nonnull IastContext ctx,
      @Nullable Object target,
      byte origin,
      Predicate<Class<?>> classFilter);

  /** Checks if an arbitrary object is tainted */
  boolean isTainted(@Nonnull IastContext ctx, @Nullable Object target);

  /**
   * Returns the source with the highest priority if the object is tainted, {@code null} otherwise
   */
  @Nullable
  Source findSource(@Nonnull IastContext ctx, @Nullable Object target);
}
