package io.github.coderodde.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.Spliterator;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Tests for the deliberately restricted SubList API and the modifiable root list. */
public class AvlTreeListTest {

    private static <E> void same(List<E> expected, AvlTreeList<E> actual) {
        assertTrue(actual.invariantHolds(), "AVL height/size/balance invariant");
        assertEquals(expected.size(), actual.size());
        assertEquals(expected.isEmpty(), actual.isEmpty());
        assertEquals(expected, actual);
        assertEquals(actual, expected);
        assertArrayEquals(expected.toArray(), actual.toArray());
        assertEquals(expected.hashCode(), actual.hashCode());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), actual.get(i), "index=" + i);
        }
    }

    @Test void constructorsAndBasicAccess() {
        AvlTreeList<Integer> empty = new AvlTreeList<>();
        same(List.of(), empty);
        assertThrows(NullPointerException.class, () -> new AvlTreeList<Integer>(null));
        AvlTreeList<Integer> list = new AvlTreeList<>(Arrays.asList(3, null, 1, 3));
        same(Arrays.asList(3, null, 1, 3), list);
        assertTrue(list.contains(null));
        assertTrue(list.contains(1));
        assertFalse(list.contains(99));
        assertTrue(list.containsAll(Arrays.asList(3, null)));
        assertFalse(list.containsAll(List.of(99)));
        assertThrows(NullPointerException.class, () -> list.containsAll(null));
        assertEquals(0, list.indexOf(3));
        assertEquals(3, list.lastIndexOf(3));
        assertEquals(1, list.indexOf(null));
        assertEquals(-1, list.lastIndexOf(99));
        assertEquals(-1, empty.indexOf(1));
        assertEquals(-1, empty.lastIndexOf(1));
    }

    @Test void addSetRemoveAndBounds() {
        AvlTreeList<String> list = new AvlTreeList<>();
        assertTrue(list.add("b"));
        list.add(0, "a");
        list.add(2, "d");
        list.add(2, "c");
        same(List.of("a", "b", "c", "d"), list);
        assertEquals("b", list.set(1, "B"));
        assertEquals("a", list.remove(0));
        assertEquals("d", list.remove(2));
        assertTrue(list.remove("B"));
        assertFalse(list.remove("missing"));
        same(List.of("c"), list);
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(list.size()));
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(-1, "x"));
        assertThrows(IndexOutOfBoundsException.class, () -> list.remove(1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(-1, "x"));
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(2, "x"));
    }

    @Test void bulkInsertionIncludingSelfAndEmpty() {
        AvlTreeList<Integer> list = new AvlTreeList<>(List.of(1, 4));
        assertTrue(list.addAll(1, List.of(2, 3)));
        assertTrue(list.addAll(List.of(5, 6)));
        assertFalse(list.addAll(2, List.of()));
        assertFalse(list.addAll(List.of()));
        same(List.of(1, 2, 3, 4, 5, 6), list);
        assertTrue(list.addAll(2, list));
        same(List.of(1, 2, 1, 2, 3, 4, 5, 6, 3, 4, 5, 6), list);
        assertThrows(NullPointerException.class, () -> list.addAll(null));
        assertThrows(NullPointerException.class, () -> list.addAll(0, null));
        assertThrows(IndexOutOfBoundsException.class, () -> list.addAll(-1, List.of(1)));
        assertThrows(IndexOutOfBoundsException.class, () -> list.addAll(list.size() + 1, List.of(1)));
    }

    @Test void removeAllRetainAllAndRemoveIf() {
        AvlTreeList<Integer> list = new AvlTreeList<>(List.of(1, 2, 2, 3, 4, 5));
        assertTrue(list.removeAll(Set.of(2, 5)));
        same(List.of(1, 3, 4), list);
        assertFalse(list.removeAll(Set.of(9)));
        assertTrue(list.retainAll(Set.of(1, 4)));
        same(List.of(1, 4), list);
        assertFalse(list.retainAll(Set.of(1, 4)));
        assertTrue(list.removeIf(x -> x % 2 == 0));
        same(List.of(1), list);
        assertFalse(list.removeIf(x -> false));
        assertThrows(NullPointerException.class, () -> list.removeAll(null));
        assertThrows(NullPointerException.class, () -> list.retainAll(null));
        assertThrows(NullPointerException.class, () -> list.removeIf(null));
    }

    @Test void removeAllAndRetainAllSelf() {
        AvlTreeList<Integer> list = new AvlTreeList<>(List.of(1, 2, 3));
        assertFalse(list.retainAll(list));
        same(List.of(1, 2, 3), list);
        assertTrue(list.removeAll(list));
        same(List.of(), list);
        assertFalse(list.removeAll(list));
    }

    @Test void arraysAndGenerator() {
        AvlTreeList<String> list = new AvlTreeList<>(Arrays.asList("a", null, "c"));
        assertArrayEquals(new Object[]{"a", null, "c"}, list.toArray());
        assertArrayEquals(new String[]{"a", null, "c"}, list.toArray(new String[0]));
        String[] oversized = {"x", "x", "x", "x", "sentinel"};
        assertSame(oversized, list.toArray(oversized));
        assertArrayEquals(new String[]{"a", null, "c", null, "sentinel"}, oversized);
        assertArrayEquals(new String[]{"a", null, "c"}, list.toArray(String[]::new));
        assertThrows(NullPointerException.class, () -> list.toArray((String[]) null));
        assertThrows(NullPointerException.class, () -> list.toArray((java.util.function.IntFunction<String[]>) null));
        assertThrows(ArrayStoreException.class, () -> list.toArray(new Integer[0]));
    }

    @Test void replaceAllSortAndOrdering() {
        AvlTreeList<Integer> list = new AvlTreeList<>(List.of(3, 1, 4, 2));
        list.replaceAll(x -> x * 2);
        same(List.of(6, 2, 8, 4), list);
        list.sort(null);
        same(List.of(2, 4, 6, 8), list);
        list.sort(Comparator.reverseOrder());
        same(List.of(8, 6, 4, 2), list);
        assertThrows(NullPointerException.class, () -> list.replaceAll(null));
    }

    @Test void equalsHashCodeAndNulls() {
        AvlTreeList<Integer> list = new AvlTreeList<>(Arrays.asList(null, 1, null));
        assertEquals(Arrays.asList(null, 1, null), list);
        assertEquals(list, new AvlTreeList<>(Arrays.asList(null, 1, null)));
        assertNotEquals(list, Arrays.asList(null, 1));
        assertNotEquals(list, Arrays.asList(1, null, null));
        assertNotEquals(list, "not a list");
        assertNotEquals(list, null);
        assertEquals(Arrays.asList(null, 1, null).hashCode(), list.hashCode());
    }

    @Test void iteratorForwardBackwardAndIndices() {
        AvlTreeList<Integer> list = new AvlTreeList<>(List.of(10, 20, 30));
        ListIterator<Integer> it = list.listIterator();
        assertFalse(it.hasPrevious());
        assertEquals(0, it.nextIndex());
        assertEquals(-1, it.previousIndex());
        assertEquals(10, it.next());
        assertEquals(10, it.previous());
        assertEquals(10, it.next());
        assertEquals(20, it.next());
        assertEquals(30, it.next());
        assertFalse(it.hasNext());
        assertEquals(3, it.nextIndex());
        assertThrows(NoSuchElementException.class, it::next);
        assertEquals(30, it.previous());
        assertEquals(20, it.previous());
        assertEquals(10, it.previous());
        assertThrows(NoSuchElementException.class, it::previous);
        assertThrows(IndexOutOfBoundsException.class, () -> list.listIterator(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.listIterator(4));
        assertEquals(30, list.listIterator(3).previous());
        assertEquals(20, list.listIterator(1).next());
        Iterator<Integer> simple = list.iterator();
        assertEquals(10, simple.next());
    }

    @Test void iteratorMutationsAndIllegalStates() {
        AvlTreeList<Integer> list = new AvlTreeList<>(List.of(1, 2, 3));
        ListIterator<Integer> it = list.listIterator();
        assertThrows(IllegalStateException.class, it::remove);
        assertThrows(IllegalStateException.class, () -> it.set(5));
        assertEquals(1, it.next());
        it.set(10);
        it.add(11);
        assertThrows(IllegalStateException.class, it::remove);
        assertThrows(IllegalStateException.class, () -> it.set(12));
        assertEquals(2, it.next());
        it.remove();
        assertThrows(IllegalStateException.class, it::remove);
        same(List.of(10, 11, 3), list);
        assertEquals(11, it.previous());
        it.remove();
        same(List.of(10, 3), list);
        assertEquals(3, it.next());
        it.set(30);
        same(List.of(10, 30), list);
    }

    @Test void iteratorFailFast() {
        AvlTreeList<Integer> list = new AvlTreeList<>(List.of(1, 2, 3));
        ListIterator<Integer> it = list.listIterator();
        list.add(4);
        assertThrows(ConcurrentModificationException.class, it::next);
        assertThrows(ConcurrentModificationException.class, () -> it.add(7));
        assertThrows(ConcurrentModificationException.class, it::remove);
        assertThrows(ConcurrentModificationException.class, () -> it.set(8));
    }

    @Test void clearAndEmptyList() {
        AvlTreeList<Integer> list = new AvlTreeList<>();
        list.clear();
        assertTrue(list.invariantHolds());
        list.addAll(List.of(1, 2, 3));
        list.clear();
        same(List.of(), list);
        list.clear();
        assertThrows(NoSuchElementException.class, list.iterator()::next);
        assertThrows(NoSuchElementException.class, list.listIterator()::previous);
    }

    @Test void subListReadClearAndBounds() {
        AvlTreeList<Integer> list = new AvlTreeList<>(List.of(0, 1, 2, 3, 4, 5));
        List<Integer> sub = list.subList(1, 5);
        assertEquals(List.of(1, 2, 3, 4), sub);
        assertEquals(4, sub.size());
        assertEquals(2, sub.get(1));
        assertThrows(IndexOutOfBoundsException.class, () -> sub.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> sub.get(4));
        assertThrows(IndexOutOfBoundsException.class, () -> list.subList(-1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.subList(0, 7));
        assertThrows(IllegalArgumentException.class, () -> list.subList(4, 2));
        sub.clear();
        same(List.of(0, 5), list);
        assertTrue(sub.isEmpty());
        sub.clear();
        assertEquals(0, sub.size());
    }

    @Test void subListUnsupportedMutatorsByDesign() {
        AvlTreeList<Integer> list = new AvlTreeList<>(List.of(1, 2, 3));
        List<Integer> sub = list.subList(0, 2);
        assertThrows(UnsupportedOperationException.class, () -> sub.set(0, 9));
        assertThrows(UnsupportedOperationException.class, () -> sub.add(0, 9));
        assertThrows(UnsupportedOperationException.class, () -> sub.remove(0));
        assertThrows(UnsupportedOperationException.class, () -> sub.addAll(List.of(9)));
        same(List.of(1, 2, 3), list);
    }

    @Test void subListFailFastAndNestedClear() {
        AvlTreeList<Integer> list = new AvlTreeList<>(List.of(0, 1, 2, 3, 4, 5));
        List<Integer> outer = list.subList(1, 5);
        List<Integer> inner = outer.subList(1, 3);
        assertEquals(List.of(2, 3), inner);
        // AbstractList's nested sublist delegates range removal to its parent.
        // The parent SubList implements removeRange(), so this is supported.
        inner.clear();
        assertEquals(List.of(), inner);
        assertEquals(List.of(1, 4), outer);
        same(List.of(0, 1, 4, 5), list);
        assertTrue(list.invariantHolds());
        List<Integer> stale = list.subList(0, 2);
        list.add(99);
        assertThrows(ConcurrentModificationException.class, stale::size);
        assertThrows(ConcurrentModificationException.class, () -> stale.get(0));
        assertThrows(ConcurrentModificationException.class, stale::clear);
    }

    @Test void subListRangeClearStress() {
        Random random = new Random(20261117L);
        for (int trial = 0; trial < 60; trial++) {
            ArrayList<Integer> expected = new ArrayList<>();
            for (int i = 0; i < 100; i++) expected.add(i);
            AvlTreeList<Integer> actual = new AvlTreeList<>(expected);
            for (int step = 0; step < 70; step++) {
                int a = random.nextInt(expected.size() + 1);
                int b = a + random.nextInt(expected.size() - a + 1);
                expected.subList(a, b).clear();
                actual.subList(a, b).clear();
                same(expected, actual);
            }
        }
    }

    @Test void forEachStreamsAndSpliterator() {
        AvlTreeList<Integer> list = new AvlTreeList<>(List.of(1, 2, 3, 4));
        List<Integer> visited = new ArrayList<>();
        list.forEach(visited::add);
        assertEquals(List.of(1, 2, 3, 4), visited);
        assertEquals(10, list.stream().mapToInt(Integer::intValue).sum());
        assertEquals(10, list.parallelStream().mapToInt(Integer::intValue).sum());
        Spliterator<Integer> spliterator = list.spliterator();
        List<Integer> splitVisited = new ArrayList<>();
        spliterator.forEachRemaining(splitVisited::add);
        assertEquals(List.of(1, 2, 3, 4), splitVisited);
        assertThrows(NullPointerException.class, () -> list.forEach(null));
    }

    @Test void randomizedRootOperationsAgainstArrayList() {
        Random random = new Random(20261117L);
        for (int trial = 0; trial < 40; trial++) {
            ArrayList<Integer> expected = new ArrayList<>();
            AvlTreeList<Integer> actual = new AvlTreeList<>();
            for (int step = 0; step < 700; step++) {
                int op = random.nextInt(8);
                int n = expected.size();
                int value = random.nextInt(201) - 100;
                switch (op) {
                    case 0, 1 -> {
                        int i = random.nextInt(n + 1);
                        expected.add(i, value);
                        actual.add(i, value);
                    }
                    case 2 -> {
                        if (n > 0) {
                            int i = random.nextInt(n);
                            assertEquals(expected.remove(i), actual.remove(i));
                        }
                    }
                    case 3 -> {
                        if (n > 0) {
                            int i = random.nextInt(n);
                            assertEquals(expected.set(i, value), actual.set(i, value));
                        }
                    }
                    case 4 -> {
                        int i = random.nextInt(n + 1);
                        List<Integer> batch = new ArrayList<>();
                        int count = random.nextInt(9);
                        for (int j = 0; j < count; j++) batch.add(random.nextInt(50));
                        assertEquals(expected.addAll(i, batch), actual.addAll(i, batch));
                    }
                    case 5 -> {
                        int a = random.nextInt(n + 1);
                        int b = a + random.nextInt(n - a + 1);
                        expected.subList(a, b).clear();
                        actual.subList(a, b).clear();
                    }
                    case 6 -> {
                        assertEquals(expected.remove((Object) value), actual.remove((Object) value));
                    }
                    case 7 -> {
                        if (n > 0) {
                            int i = random.nextInt(n);
                            assertEquals(expected.get(i), actual.get(i));
                        }
                    }
                    default -> throw new AssertionError(op);
                }
                same(expected, actual);
            }
        }
    }

    @Test void randomizedListIteratorAgainstArrayList() {
        Random random = new Random(919191L);
        for (int trial = 0; trial < 35; trial++) {
            ArrayList<Integer> expected = new ArrayList<>();
            for (int i = 0; i < 30; i++) expected.add(i);
            AvlTreeList<Integer> actual = new AvlTreeList<>(expected);
            int start = random.nextInt(expected.size() + 1);
            ListIterator<Integer> e = expected.listIterator(start);
            ListIterator<Integer> a = actual.listIterator(start);
            for (int step = 0; step < 500; step++) {
                int op = random.nextInt(5);
                assertEquals(e.hasNext(), a.hasNext());
                assertEquals(e.hasPrevious(), a.hasPrevious());
                assertEquals(e.nextIndex(), a.nextIndex());
                assertEquals(e.previousIndex(), a.previousIndex());
                switch (op) {
                    case 0 -> { if (e.hasNext()) assertEquals(e.next(), a.next()); }
                    case 1 -> { if (e.hasPrevious()) assertEquals(e.previous(), a.previous()); }
                    case 2 -> {
                        int v = random.nextInt(100);
                        e.add(v); a.add(v);
                    }
                    case 3 -> {
                        try {
                            e.remove(); a.remove();
                        } catch (IllegalStateException ex) {
                            assertThrows(IllegalStateException.class, a::remove);
                        }
                    }
                    case 4 -> {
                        int v = random.nextInt(100);
                        try {
                            e.set(v); a.set(v);
                        } catch (IllegalStateException ex) {
                            assertThrows(IllegalStateException.class, () -> a.set(v));
                        }
                    }
                    default -> throw new AssertionError(op);
                }
                same(expected, actual);
            }
        }
    }
}
