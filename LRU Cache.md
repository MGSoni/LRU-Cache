# LRU Cache — Study Notes

**Pattern:** HashMap + Doubly Linked List for O(1) get/put with recency eviction
**Asked at:** Amazon SDE2 (Round 2), extremely common at Meta, Google, Microsoft, most senior backend/system-design-adjacent rounds

---

## Why this problem is asked so often

It tests four things at once, which is why interviewers love it:
1. **Data structure design** — recognizing HashMap alone can't do O(1) eviction, and a plain DLL alone can't do O(1) lookup by key. You need both, working together.
2. **Pointer manipulation discipline** — DLL insert/delete has real edge cases (head, tail, single node, empty list) that are easy to get subtly wrong.
3. **Implementation from scratch** — many interviewers explicitly forbid `LinkedHashMap` (Java's built-in that does this natively) specifically to test #1 and #2.
4. **Complexity reasoning** — you must justify O(1) for both operations, not just get the right answer.

---

## Core Design

**Two data structures, working together:**
- `Map<Integer, Node>` — key → Node, gives O(1) lookup
- Doubly Linked List — tracks recency order: **head = most recently used, tail = least recently used**

**The node stores BOTH key and value** — this is the detail most candidates miss on a first attempt. You need the key on the node so that when you evict the tail, you know which entry to remove from the map (the tail node only "knows" its own key/value, not what map entry points to it).

---

## The DLL (built first, as its own reusable class)

```java
class Node {
    Node next;
    Node prev;
    int key;
    int value;

    Node(int value, int key) {
        this.next = null;
        this.prev = null;
        this.value = value;
        this.key = key;
    }
}

public class DLL {
    Node head;
    Node tail;

    public void insertAtHead(Node node) {
        if (head == null) {
            head = node;
            tail = node;
            node.prev = null;
            node.next = null;
            return;
        }
        head.prev = node;
        node.next = head;
        head = node;
        node.prev = null;
    }

    public void deleteNode(Node node) {
        if (node == null) return;

        if (node.prev == null && node.next == null) {
            head = null;
            tail = null;
        } else if (node.prev == null) {
            head = node.next;
            head.prev = null;
        } else if (node.next == null) {
            tail = node.prev;
            tail.next = null;
        } else {
            node.prev.next = node.next;
            node.next.prev = node.prev;
        }
        node.prev = null;
        node.next = null;
    }
}
```

### Key lessons learned building this (mistakes worth remembering)

1. **`deleteNode` takes the node directly — no search loop.** A first-draft version that walks the list comparing values is O(n) and defeats the whole purpose. Since the caller already has the `Node` reference (from the map), just use it directly. **This is the single most important optimization in the whole problem.**
2. **All 4 cases must update BOTH `head`/`tail` (as needed) AND clear stale pointers on the removed node.** Forgetting to null out `node.prev`/`node.next` after deletion causes a bug that only shows up later — e.g., re-inserting that node elsewhere leaves a stale backward pointer, creating a hidden two-node loop that corrupts backward traversal.
3. **Always delete before you insert when "moving" a node.** Calling `insertAtHead` on a node that's still linked into its old position causes two neighbors to both think they're connected to it — an inconsistent list. Detach fully first.
4. **In `deleteNode`, reuse the same method for eviction rather than hand-rolling a manual unlink.** A manual version (`dll.tail = tail.prev; dll.tail.next = null;`) breaks on `capacity = 1`, because `tail.prev` is `null` there — causes a NullPointerException. The tested `deleteNode` method already handles this edge case correctly.

---

## LRUCache class

```java
class LRUCache {
    Map<Integer, Node> map = new HashMap<>();
    DLL dll;
    int capacity;

    LRUCache(int capacity) {
        this.dll = new DLL();
        this.capacity = capacity;
    }

    public int get(int key) {
        if (map.containsKey(key)) {
            Node node = map.get(key);
            dll.deleteNode(node);
            dll.insertAtHead(node);
            return node.value;
        }
        return -1;
    }

    public void put(int key, int value) {
        if (map.containsKey(key)) {
            // Case 1: key exists — update value, refresh recency
            Node node = map.get(key);
            dll.deleteNode(node);
            node.value = value;
            dll.insertAtHead(node);
        } else if (map.size() < capacity) {
            // Case 2: new key, room available
            Node node = new Node(value, key);
            map.put(key, node);
            dll.insertAtHead(node);
        } else {
            // Case 3: new key, cache full — evict LRU (tail), then insert
            Node tail = dll.tail;
            map.remove(tail.key);
            dll.deleteNode(tail);
            Node node = new Node(value, key);
            map.put(key, node);
            dll.insertAtHead(node);
        }
    }
}
```

### Why `get()` also counts as "using" a key
Both `get` and `put` (on an existing key) move the node to the front — accessing a value, not just writing one, refreshes its recency. This is standard LRU semantics; miss this and your eviction order will be wrong.

---

## Complexity
| Operation | Time | Why |
|---|---|---|
| `get(key)` | O(1) | HashMap lookup O(1) + DLL delete/insert O(1) each (node reference already known) |
| `put(key, value)` | O(1) | Same — no searching required anywhere |
| Space | O(capacity) | Map and DLL both hold at most `capacity` entries |

---

## How this is actually asked in an interview

**Typical framing:** *"Design a data structure for a Least Recently Used (LRU) cache. Implement `get(key)` and `put(key, value)`, both in O(1) time. When the cache reaches capacity, evict the least recently used item before inserting a new one."*

### Actual question prompts, phrased as an interviewer would say them

**Opening prompt (LeetCode 146 — the canonical version, verbatim style):**
> "Design a data structure that follows the constraints of a Least Recently Used (LRU) cache.
> Implement the `LRUCache` class:
> - `LRUCache(int capacity)` Initialize the LRU cache with positive size `capacity`.
> - `int get(int key)` Return the value of the `key` if the key exists, otherwise return `-1`.
> - `void put(int key, int value)` Update the value of the `key` if the `key` exists. Otherwise, add the `key-value` pair to the cache. If the number of keys exceeds the `capacity` from this operation, evict the least recently used key.
> The functions `get` and `put` must each run in O(1) average time complexity."

**Amazon-style variant (as it was actually asked — "implement from scratch"):**
> "Implement an LRU Cache. Don't use Java's built-in `LinkedHashMap` — I want to see you build the underlying data structure yourself."

**Follow-up prompts an interviewer typically stacks on top, roughly in this order:**

1. *"Before you code — what data structures would you use, and why can't a HashMap alone solve this?"* (tests design reasoning before implementation)
2. *"Walk me through what your `Node` needs to store."* (checking you realize it needs both key AND value)
3. *"What happens on `get` when the key doesn't exist?"* (checking the -1 path is handled cleanly, no wasted work)
4. *"What happens on `put` when the key already exists — does it count as 'recently used'?"* (checking you know put-on-existing-key also refreshes recency)
5. *"Now the cache is full and you `put` a new key — walk me through eviction, step by step."* (the case-3 logic — tail identification, map removal, DLL removal, insertion)
6. *"What's the time complexity of `get` and `put`? Justify it — don't just state it."*
7. *"How would you make this thread-safe?"* (expects: `synchronized` methods or a `ReentrantLock`, not full implementation usually)
8. *"How would you add TTL/expiry to entries?"* (expects: an `expiryTime` field on `Node`, checked lazily in `get`)
9. *"What if `capacity` is 0 or negative?"* (expects: a guard clause, or explicit statement that `put` never inserts)
10. *"Can you do this with a singly linked list instead of doubly?"* (expects you to explain why O(1) deletion needs `prev` — with only `next`, you'd need to traverse to find a node's predecessor, making deletion O(n))

**A senior/staff-level extension sometimes asked after the base implementation:**
> "How would you extend this to an LFU (Least Frequently Used) cache instead?" — expects recognizing you now need to track *frequency* per key too, typically via a frequency-to-DLL-of-nodes map (a harder, related problem — LeetCode 460).

**Common interviewer twists/follow-ups to expect:**
1. **"Implement it without using Java's built-in `LinkedHashMap`."** — This is the default expectation at senior level; assume you need to build the DLL yourself unless told otherwise.
2. **"What if two threads call `get`/`put` concurrently?"** — Answer: this implementation isn't thread-safe (no synchronization). You'd add a `synchronized` block/method, or use a `ReentrantLock`, around `get`/`put` to make it safe — mention this even if not asked to implement it.
3. **"How would you support TTL (time-to-live) expiry on top of LRU?"** — Store an `expiryTime` on each `Node`, and check it in `get()`, treating an expired entry as a miss (return -1) and evicting it lazily.
4. **"What if capacity is 0?"** — Edge case worth mentioning: `put` should never insert if `capacity == 0`; add a guard at the top of `put`.
5. **"Walk me through what happens when you call `get` on a key that doesn't exist."** — Say it explicitly: map lookup fails, return -1, no DLL operations at all. Interviewers check you're not doing unnecessary work on the miss path.

**What separates a strong answer from an average one, based on this build:**
- Explaining **why** a HashMap alone or a DLL alone isn't sufficient (most candidates state the solution but don't justify why simpler options fail)
- Handling **all 4 DLL edge cases** cleanly (head, tail, only-node, middle) without hand-waving
- Getting the **delete-before-insert** ordering right when moving a node, and being able to explain what breaks if you don't
- Storing **both key and value on the node** — and explaining specifically why the key is needed there (for map eviction), since this trips up a lot of first attempts
- Confidently stating and justifying O(1) for both operations, tracing through *why* — not just asserting it

---

## Personal build notes (from your own debugging process)
You built this in stages: raw DLL insert (missed empty-list null check on first try) → DLL delete (fixed 3 separate pointer bugs across head/tail/stale-pointer cases) → removed the O(n) search loop in favor of direct node deletion → LRUCache skeleton (fixed nested-class and duplicate head/tail issues) → get() (correct on first real attempt) → put() (fixed an unsafe manual-unlink bug in the eviction branch by reusing `deleteNode`). This progression — catching and fixing your own bugs through tracing — is exactly the skill interviewers are evaluating, often more than whether the first draft is perfect.
