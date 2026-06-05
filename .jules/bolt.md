## 2026-06-05 - [Optimize TrafficMonitor Top N Algorithm]
**Learning:** In top-K search algorithms using a PriorityQueue, unconditionally instantiating wrapper objects and adding/removing them for the entire collection creates massive allocation and GC overhead. Checking the item score against the PriorityQueue's minimum element (i.e. `peek()` before adding drastically eliminates these allocations.
**Action:** Use a fast-path score check `score > pq.peek().score` to drop non-top items entirely before allocating wrapper records or modifying the heap.
