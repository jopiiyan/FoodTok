package com.example.foodtok.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.foodtok.data.RecipePriorityQueue.ScoredRecipe;
import com.example.foodtok.models.Recipe;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/** Unit tests for the custom max-heap {@link RecipePriorityQueue}. */
public class
RecipePriorityQueueTest {

  private RecipePriorityQueue pq;

  @Before
  public void setUp() {
    pq = new RecipePriorityQueue();
  }

  @Test
  public void emptyQueue_sizeIsZero() {
    assertEquals(0, pq.size());
    assertTrue(pq.isEmpty());
  }

  @Test
  public void pollOnEmpty_returnsNull() {
    assertNull(pq.poll());
  }

  @Test
  public void peekOnEmpty_returnsNull() {
    assertNull(pq.peek());
  }

  @Test
  public void insertSingle_peekReturnsSameEntry() {
    Recipe r = recipe("r1");
    pq.insert(new ScoredRecipe(r, 10.0));
    assertEquals(1, pq.size());
    assertNotNull(pq.peek());
    assertEquals("r1", pq.peek().getRecipe().getId());
    assertEquals(10.0, pq.peek().getScore(), 0.001);
  }

  @Test
  public void insertMultiple_peekReturnsHighestScore() {
    pq.insert(new ScoredRecipe(recipe("low"), 5.0));
    pq.insert(new ScoredRecipe(recipe("high"), 100.0));
    pq.insert(new ScoredRecipe(recipe("mid"), 50.0));

    assertEquals("high", pq.peek().getRecipe().getId());
  }

  @Test
  public void pollReturnsDescendingOrder() {
    pq.insert(new ScoredRecipe(recipe("a"), 30.0));
    pq.insert(new ScoredRecipe(recipe("b"), 10.0));
    pq.insert(new ScoredRecipe(recipe("c"), 50.0));
    pq.insert(new ScoredRecipe(recipe("d"), 20.0));
    pq.insert(new ScoredRecipe(recipe("e"), 40.0));

    double prev = Double.MAX_VALUE;
    while (!pq.isEmpty()) {
      ScoredRecipe sr = pq.poll();
      assertTrue(sr.getScore() <= prev);
      prev = sr.getScore();
    }
  }

  @Test
  public void pollTop_returnsRequestedCount() {
    for (int i = 0; i < 10; i++) {
      pq.insert(new ScoredRecipe(recipe("r" + i), i * 10.0));
    }
    List<ScoredRecipe> top3 = pq.pollTop(3);
    assertEquals(3, top3.size());
    assertEquals(90.0, top3.get(0).getScore(), 0.001);
    assertEquals(80.0, top3.get(1).getScore(), 0.001);
    assertEquals(70.0, top3.get(2).getScore(), 0.001);
    assertEquals(7, pq.size());
  }

  @Test
  public void pollTop_moreThanSize_returnsAll() {
    pq.insert(new ScoredRecipe(recipe("only"), 5.0));
    List<ScoredRecipe> result = pq.pollTop(100);
    assertEquals(1, result.size());
    assertTrue(pq.isEmpty());
  }

  @Test
  public void clear_emptiesTheQueue() {
    pq.insert(new ScoredRecipe(recipe("a"), 1.0));
    pq.insert(new ScoredRecipe(recipe("b"), 2.0));
    pq.clear();
    assertTrue(pq.isEmpty());
    assertEquals(0, pq.size());
  }

  @Test
  public void equalScores_bothRetained() {
    pq.insert(new ScoredRecipe(recipe("x"), 42.0));
    pq.insert(new ScoredRecipe(recipe("y"), 42.0));
    assertEquals(2, pq.size());
    ScoredRecipe first = pq.poll();
    ScoredRecipe second = pq.poll();
    assertEquals(42.0, first.getScore(), 0.001);
    assertEquals(42.0, second.getScore(), 0.001);
  }

  @Test
  public void negativeScores_heapOrderMaintained() {
    pq.insert(new ScoredRecipe(recipe("neg1"), -5.0));
    pq.insert(new ScoredRecipe(recipe("neg2"), -20.0));
    pq.insert(new ScoredRecipe(recipe("pos"), 10.0));

    assertEquals("pos", pq.poll().getRecipe().getId());
    assertEquals("neg1", pq.poll().getRecipe().getId());
    assertEquals("neg2", pq.poll().getRecipe().getId());
  }

  @Test
  public void largeInsert_maintainsHeapProperty() {
    for (int i = 0; i < 1000; i++) {
      pq.insert(new ScoredRecipe(recipe("r" + i), Math.random() * 1000));
    }
    assertEquals(1000, pq.size());

    double prev = Double.MAX_VALUE;
    while (!pq.isEmpty()) {
      double score = pq.poll().getScore();
      assertTrue(score <= prev);
      prev = score;
    }
  }

  private Recipe recipe(String id) {
    return new Recipe(id, "http://example.com/" + id + ".mp4");
  }
}
