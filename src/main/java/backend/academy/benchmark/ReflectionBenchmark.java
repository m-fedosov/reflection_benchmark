package backend.academy.benchmark;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

/**
 * This benchmark compares various ways to access a simple method:
 * 1. Direct method access.
 * 2. Reflection.
 * 3. MethodHandles.
 * 4. LambdaMetafactory.
 *
 * The goal is to measure performance differences and assess their suitability for different use cases.
 */
@State(Scope.Thread) // isolation between benchmark iterations
@SuppressWarnings({"UncommentedMain", "MagicNumber"})
public class ReflectionBenchmark {
    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
            .include(ReflectionBenchmark.class.getSimpleName())
            .shouldFailOnError(true)
            .shouldDoGC(true)
            .mode(Mode.AverageTime)
            .timeUnit(TimeUnit.NANOSECONDS)
            .forks(1)
            .warmupForks(1)
            .warmupIterations(1)
            .warmupTime(TimeValue.seconds(5))
            .measurementIterations(1)
            .measurementTime(TimeValue.seconds(5))
            .build();

        new Runner(options).run();
    }

    @Setup // Initialization before benchmarks run
    public void setup() throws Throwable {
        mamkinProgrammist = new MamkinProgrammist("m-fedosov");

        // reflection-based method access
        method            = mamkinProgrammist.getClass().getMethod("name");

        // MethodHandle-based access
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        methodHandle                = lookup.unreflect(method);

        // LambdaMetafactory-based access
        CallSite site = LambdaMetafactory.metafactory(
            lookup,
            "call",
            MethodType.methodType(Callable.class, MamkinProgrammist.class),
            MethodType.methodType(Object.class),
            methodHandle,
            MethodType.methodType(String.class)
        );
        lambda = (Callable<String>) site.getTarget().bindTo(mamkinProgrammist).invokeExact();
    }

    @Benchmark
    public void directAccess(Blackhole bh) {
        String name = mamkinProgrammist.name();
        bh.consume(name);
    }

    @Benchmark
    public void reflectionAccess(Blackhole bh) throws InvocationTargetException, IllegalAccessException {
        String name = (String) method.invoke(mamkinProgrammist);
        bh.consume(name);
    }

    @Benchmark
    public void methodHandlesAccess(Blackhole bh) throws Throwable {
        String name = (String) methodHandle.invoke(mamkinProgrammist);
        bh.consume(name);
    }

    @Benchmark
    public void lambdaMetafactoryAccess(Blackhole bh) throws Exception {
        String name = lambda.call();
        bh.consume(name);
    }

    private MamkinProgrammist mamkinProgrammist; // Object under test
    private Method method;                       // Reflection-based method access
    private MethodHandle methodHandle;           // MethodHandles-based access
    private Callable<String> lambda;             // LambdaMetafactory-based callable

    // Record used for testing simple method access scenarios
    record MamkinProgrammist(String name) {

    }
}
