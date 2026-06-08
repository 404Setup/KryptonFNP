package one.pkg.kreno.jmh.network;

import org.openjdk.jmh.annotations.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
public class PriorityQueueDrainBenchmark {

    private PriorityQueue<Long> pq;

    @Setup(Level.Invocation)
    public void setup() {
        pq = new PriorityQueue<>();
        pq.offer(10L);
        pq.offer(20L);
        pq.offer(5L);
        pq.offer(15L);
        pq.offer(30L);
    }

    @Benchmark
    public List<Long> testArrayListSort() {
        List<Long> top5 = new ArrayList<>(pq);
        top5.sort((a, b) -> Long.compare(b, a));
        return top5;
    }

    @Benchmark
    public List<Long> testArrayPoll() {
        int size = pq.size();
        Long[] arr = new Long[size];
        for (int i = size - 1; i >= 0; i--) {
            arr[i] = pq.poll();
        }
        return Arrays.asList(arr);
    }
}
