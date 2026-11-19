package io.github.coderodde.util.benchmark.avltree;

import io.github.coderodde.util.AvlTreeList;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class AvlTreeBenchmark {
    
    private static final int N = 100_000;
    private static final int N_PRELOAD = 150_000;

    private static void preload(List<Integer> list) {
        for (int i = 0; i < N_PRELOAD; ++i) {
            list.add(i);
        }
    }
    
    private AvlTreeBenchmark() {
        
    }
    
    public static void main(String[] args) {
        System.out.println("Warming up...");
        
        warmup();
        
        System.out.println();
        System.out.println("Warming up done!");
        System.out.println("Benchmarking...");
        System.out.println();
        
        benchmark();
        
        System.out.println("Benchmarking done!");
    }
    
    private static void warmup() {
        benchmarkImpl(false);
    }
    
    private static void benchmark() {
        benchmarkImpl(true);
    }
    
    private static void benchmarkImpl(boolean doPrint) {
        List<Integer> avlTreeList = new AvlTreeList<>();
        List<Integer> arrayList   = new ArrayList<>();
        
        benchmarkImpl(avlTreeList, doPrint);
        benchmarkImpl(arrayList,   doPrint);
    }
    
    private static void benchmarkImpl(List<Integer> list, boolean doPrint) {
        if (doPrint) {
            System.out.println(
                "== List: %s".formatted(list.getClass().getSimpleName()));
        }
        
        int[] addPayload = createRandomAddPayloadIntegers();
        
        int[] getPayload = createRandomGetPayloadIntegers(addPayload.length);
        
        int[] removePayload = 
            createRandomRemovePayloadIntegers(addPayload.length);
        
        preload(list);
                
        long totalDuration = 0L;
        long startTime = System.currentTimeMillis();
        
        for (int i : addPayload) {
            list.add(i, null);
        }
        
        long duration = System.currentTimeMillis() - startTime;
        totalDuration += duration;
        
        if (doPrint) {
            System.out.println("add(int) in %d ms.".formatted(duration));
        }
        
        startTime = System.currentTimeMillis();
        
        for (int i : getPayload) {
            list.get(i);
        }
        
        duration = System.currentTimeMillis() - startTime;
        totalDuration += duration;
        
        if (doPrint) {
            System.out.println("get(int) in %d ms.".formatted(duration));
        }
        
        startTime = System.currentTimeMillis();
        
        for (int i : removePayload) {
            list.remove(i);
        }
        
        duration = System.currentTimeMillis() - startTime;
        totalDuration += duration;
        
        if (doPrint) {
            System.out.println("remove(int) in %d ms.".formatted(duration));
            System.out.println("Total time: %d ms".formatted(totalDuration));
            System.out.println();
        }
    }
    
    private static final int[] createRandomAddPayloadIntegers() {
        Random r = new Random(13L);
        int[] array = new int[N];
        
        for (int i = 0; i < array.length; ++i) {
            array[i] = r.nextInt(3 * N / 2);
        }
        
        return array;
    }
    
    private static final int[] 
        createRandomGetPayloadIntegers(int indexUpperBound) {
            
        Random r = new Random(29L);
        int[] array = new int[N];
        
        for (int i = 0; i < array.length; ++i) {
            array[i] = r.nextInt(indexUpperBound);
        }
        
        return array;
    }
    
    private static final int[] createRandomRemovePayloadIntegers(int size) {
        Random r = new Random(666L);
        int[] array = new int[N];
        
        for (int i = 0; i < array.length; ++i) {
            array[i] = r.nextInt(size--);
        }
        
        return array;
    }
}
