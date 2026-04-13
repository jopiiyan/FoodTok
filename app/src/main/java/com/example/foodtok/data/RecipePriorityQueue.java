package com.example.foodtok.data;

import com.example.foodtok.models.Recipe;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom max-heap (priority queue) for ranking recipes by score.
 *
 * <p>Backed by an {@link ArrayList} with standard heap operations:
 * insert in O(log n), poll in O(log n), peek in O(1). This is a
 * custom implementation for the 50.004 data structures requirement
 * — it does not delegate to {@link java.util.PriorityQueue}.
 */
public class RecipePriorityQueue {

  /** A recipe paired with its computed recommendation score. */
  public static class ScoredRecipe {
    private final Recipe recipe;
    private final double score;

    public ScoredRecipe(Recipe recipe, double score) {
      this.recipe = recipe;
      this.score = score;
    }

    public Recipe getRecipe() {
      return recipe;
    }

    public double getScore() {
      return score;
    }
  }

  private final ArrayList<ScoredRecipe> heap;

  public RecipePriorityQueue() {
    this.heap = new ArrayList<>();
  }

  /** Returns the number of entries in the queue. */
  public int size() {
    return heap.size();
  }

  /** Returns {@code true} if the queue contains no entries. */
  public boolean isEmpty() {
    return heap.isEmpty();
  }

  /**
   * Inserts a scored recipe into the heap in O(log n).
   *
   * @param entry the scored recipe to insert
   */
  public void insert(ScoredRecipe entry) {
    heap.add(entry);
    heapifyUp(heap.size() - 1);
  }

  /**
   * Returns the highest-scored recipe without removing it.
   *
   * @return the top entry, or {@code null} if the queue is empty
   */
  public ScoredRecipe peek() {
    if (heap.isEmpty()) {
      return null;
    }
    return heap.get(0);
  }

  /**
   * Removes and returns the highest-scored recipe in O(log n).
   *
   * @return the top entry, or {@code null} if the queue is empty
   */
  public ScoredRecipe poll() {
    if (heap.isEmpty()) {
      return null;
    }
    ScoredRecipe top = heap.get(0);
    int lastIndex = heap.size() - 1;
    heap.set(0, heap.get(lastIndex));
    heap.remove(lastIndex);
    if (!heap.isEmpty()) {
      heapifyDown(0);
    }
    return top;
  }

  /**
   * Drains up to {@code count} entries from the heap, returning
   * them in descending score order.
   *
   * @param count maximum number of entries to drain
   * @return list of scored recipes, highest score first
   */
  public List<ScoredRecipe> pollTop(int count) {
    List<ScoredRecipe> result = new ArrayList<>();
    for (int i = 0; i < count && !heap.isEmpty(); i++) {
      result.add(poll());
    }
    return result;
  }

  /** Removes all entries from the queue. */
  public void clear() {
    heap.clear();
  }

  // ── Heap internals ────────────────────────────────────────────────

  /**
   * Restores the max-heap property by bubbling the element at
   * {@code index} upward until it is no larger than its parent.
   */
  private void heapifyUp(int index) {
    while (index > 0) {
      int parentIndex = (index - 1) / 2;
      if (heap.get(index).score > heap.get(parentIndex).score) {
        swap(index, parentIndex);
        index = parentIndex;
      } else {
        break;
      }
    }
  }

  /**
   * Restores the max-heap property by sinking the element at
   * {@code index} downward until it is no smaller than its children.
   */
  private void heapifyDown(int index) {
    int size = heap.size();
    while (true) {
      int largest = index;
      int left = 2 * index + 1;
      int right = 2 * index + 2;

      if (left < size
          && heap.get(left).score > heap.get(largest).score) {
        largest = left;
      }
      if (right < size
          && heap.get(right).score > heap.get(largest).score) {
        largest = right;
      }

      if (largest != index) {
        swap(index, largest);
        index = largest;
      } else {
        break;
      }
    }
  }

  private void swap(int i, int j) {
    ScoredRecipe temp = heap.get(i);
    heap.set(i, heap.get(j));
    heap.set(j, temp);
  }
}
