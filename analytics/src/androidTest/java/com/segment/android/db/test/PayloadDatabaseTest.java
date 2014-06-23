/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Segment.io, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.segment.android.db.test;

import android.util.Pair;
import com.segment.android.db.PayloadDatabase;
import com.segment.android.models.BasePayload;
import com.segment.android.models.EasyJSONObject;
import com.segment.android.models.Identify;
import com.segment.android.test.BaseTest;
import com.segment.android.test.TestCases;
import java.util.LinkedList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class PayloadDatabaseTest extends BaseTest {

  private PayloadDatabase database;

  @Override
  protected void setUp() {
    super.setUp();

    database = PayloadDatabase.getInstance(context);

    // clean out database beforehand from unsuccessful tests
    database.removeEvents(0, 99999999);
  }

  @Test
  public void testSingle() {

    List<Pair<Long, BasePayload>> events = database.getEvents(50);
    Assert.assertEquals(0, events.size());

    Identify identify = TestCases.identify();

    boolean success = database.addPayload(identify);
    Assert.assertTrue(success);

    events = database.getEvents(50);
    Assert.assertEquals(1, events.size());
    boolean equals = EasyJSONObject.equals(identify, events.get(0).second);
    Assert.assertTrue(equals);

    // check that our row counter is correct
    Assert.assertEquals(1, database.getRowCount());

    // now let's remove that event
    long minId = events.get(0).first;
    long maxId = events.get(events.size() - 1).first;
    int removed = database.removeEvents(minId, maxId);
    Assert.assertEquals(1, removed);
    // now let's check that there's nothing in the database
    events = database.getEvents(50);
    Assert.assertEquals(0, events.size());

    // check that our row counter was decremented by the remove
    Assert.assertEquals(0, database.getRowCount());
  }

  @Test
  public void testPerformance() {

    int msPerInsert = 250;
    int added = 100;

    List<BasePayload> payloads = new LinkedList<BasePayload>();

    long start = System.currentTimeMillis();

    for (int i = 0; i < added; i += 1) {
      BasePayload payload = TestCases.random();
      payloads.add(payload);
      boolean success = database.addPayload(payload);
      Assert.assertTrue(success);
    }

    long duration = System.currentTimeMillis() - start;

    Assert.assertTrue(duration < msPerInsert * added);

    // check that our row counter is correct
    Assert.assertEquals(added, database.getRowCount());

    int left = added;
    int queryLimit = 50;

    while (left > 0) {
      // check that we can get those items
      List<Pair<Long, BasePayload>> events = database.getEvents(queryLimit);

      // we expect the full query size
      int expected = queryLimit;
      // unless the query is larger than what we have left
      if (left - queryLimit < 0) expected = left;

      Assert.assertEquals(events.size(), expected);

      // now let's remove these events
      long minId = events.get(0).first;
      long maxId = events.get(events.size() - 1).first;
      int removed = database.removeEvents(minId, maxId);
      Assert.assertEquals(expected, removed);

      left -= queryLimit;
    }

    // now let's check that there's nothing in the database
    List<Pair<Long, BasePayload>> events = database.getEvents(queryLimit);
    Assert.assertEquals(0, events.size());

    // make sure the database row count is 0
    Assert.assertEquals(0, database.getRowCount());
  }
}