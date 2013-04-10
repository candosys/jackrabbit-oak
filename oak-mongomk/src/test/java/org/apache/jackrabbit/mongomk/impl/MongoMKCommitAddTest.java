/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.mongomk.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.jackrabbit.mongomk.BaseMongoMicroKernelTest;
import org.json.simple.JSONObject;
import org.junit.Ignore;
import org.junit.Test;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;

/**
 * Tests for {@link MongoMicroKernel#commit(String, String, String, String)}
 * with emphasis on add node and property operations.
 */
public class MongoMKCommitAddTest extends BaseMongoMicroKernelTest {

    @Test
    @Ignore    
    public void addSingleNode() throws Exception {
        mk.commit("/", "+\"a\" : {}", null, null);

        long childCount = mk.getChildNodeCount("/", null);
        assertEquals(1, childCount);

        String nodes = mk.getNodes("/", null, -1 /*depth*/, 0 /*offset*/, -1 /*maxChildNodes*/, null /*filter*/);
        JSONObject obj = parseJSONObject(nodes);
        assertPropertyValue(obj, ":childNodeCount", 1L);
    }

    @Test
    public void addNodeWithChildren() throws Exception {
        mk.commit("/", "+\"a\" : { \"b\": {}, \"c\": {}, \"d\" : {} }", null, null);

        assertTrue(mk.nodeExists("/a",null));
        assertTrue(mk.nodeExists("/a/b",null));
        assertTrue(mk.nodeExists("/a/c",null));
        assertTrue(mk.nodeExists("/a/d",null));
    }

    @Test
    public void addNodeWithNestedChildren() throws Exception {
        mk.commit("/", "+\"a\" : { \"b\": { \"c\" : { \"d\" : {} } } }", null, null);

        assertTrue(mk.nodeExists("/a",null));
        assertTrue(mk.nodeExists("/a/b",null));
        assertTrue(mk.nodeExists("/a/b/c",null));
        assertTrue(mk.nodeExists("/a/b/c/d",null));
    }

    @Test
    public void addNodeWithNestedChildren2() throws Exception {
        mk.commit("/", "+\"a\" : {}", null, null);
        mk.commit("/a", "+\"b\" : {}", null, null);
        mk.commit("/a/b", "+\"c\" : {}", null, null);
        mk.commit("/a", "+\"d\" : {}", null, null);

        assertTrue(mk.nodeExists("/a",null));
        assertTrue(mk.nodeExists("/a/b",null));
        assertTrue(mk.nodeExists("/a/b/c",null));
        assertTrue(mk.nodeExists("/a/d", null));
    }

    @Test
    @Ignore    
    public void addNodeWithParanthesis() throws Exception {
        mk.commit("/", "+\"Test({0})\" : {}", null, null);
        String nodes = mk.getNodes("/Test({0})", null, 1, 0, -1, null);
        JSONObject obj = parseJSONObject(nodes);
        assertPropertyValue(obj, ":childNodeCount", 0L);
    }


    @Test
    public void addIntermediataryNodes() throws Exception {
        String rev1 = mk.commit("/", "+\"a\" : { \"b\" : { \"c\": {} }}", null, null);
        String rev2 = mk.commit("/", "+\"a/d\" : {} +\"a/b/e\" : {}", null, null);

        assertTrue(mk.nodeExists("/a/b/c", rev1));
        assertFalse(mk.nodeExists("/a/b/e", rev1));
        assertFalse(mk.nodeExists("/a/d", rev1));

        assertTrue(mk.nodeExists("/a/b/c", rev2));
        assertTrue(mk.nodeExists("/a/b/e", rev2));
        assertTrue(mk.nodeExists("/a/d", rev2));
    }

    @Test
    public void addDuplicateNode() throws Exception {
        mk.commit("/", "+\"a\" : {}", null, null);
        try {
            mk.commit("/", "+\"a\" : {}", null, null);
            fail("Exception expected");
        } catch (Exception expected) {}
    }

    @Test
    @Ignore    
    public void setSingleProperty() throws Exception {
        mk.commit("/", "+\"a\" : {} ^\"a/key1\" : \"value1\"", null, null);

        long childCount = mk.getChildNodeCount("/", null);
        assertEquals(1, childCount);

        String nodes = mk.getNodes("/", null, 1 /*depth*/, 0 /*offset*/, -1 /*maxChildNodes*/, null /*filter*/);
        JSONObject obj = parseJSONObject(nodes);
        assertPropertyValue(obj, ":childNodeCount", 1L);
        assertPropertyValue(obj, "a/key1", "value1");
    }

    @Test
    @Ignore    
    public void setMultipleProperties() throws Exception {
        mk.commit("/", "+\"a\" : {} ^\"a/key1\" : \"value1\"", null, null);
        mk.commit("/", "^\"a/key2\" : 2", null, null);
        mk.commit("/", "^\"a/key3\" : false", null, null);
        mk.commit("/", "^\"a/key4\" : 0.25", null, null);

        long childCount = mk.getChildNodeCount("/", null);
        assertEquals(1, childCount);

        String nodes = mk.getNodes("/", null, 1 /*depth*/, 0 /*offset*/, -1 /*maxChildNodes*/, null /*filter*/);
        JSONObject obj = parseJSONObject(nodes);
        assertPropertyValue(obj, ":childNodeCount", 1L);
        assertPropertyValue(obj, "a/key1", "value1");
        assertPropertyValue(obj, "a/key2", 2L);
        assertPropertyValue(obj, "a/key3", false);
        assertPropertyValue(obj, "a/key4", 0.25);
    }

    // See http://www.mongodb.org/display/DOCS/Legal+Key+Names
    @Test
    @Ignore    
    public void setPropertyIllegalKey() throws Exception {
        mk.commit("/", "+\"a\" : {}", null, null);

        mk.commit("/", "^\"a/ke.y1\" : \"value\"", null, null);
        String nodes = mk.getNodes("/", null, 1 /*depth*/, 0 /*offset*/, -1 /*maxChildNodes*/, null /*filter*/);
        JSONObject obj = parseJSONObject(nodes);
        assertPropertyValue(obj, "a/ke.y1", "value");

        mk.commit("/", "^\"a/ke.y.1\" : \"value\"", null, null);
        nodes = mk.getNodes("/", null, 1 /*depth*/, 0 /*offset*/, -1 /*maxChildNodes*/, null /*filter*/);
        obj = parseJSONObject(nodes);
        assertPropertyValue(obj, "a/ke.y.1", "value");

        mk.commit("/", "^\"a/$key1\" : \"value\"", null, null);
        nodes = mk.getNodes("/", null, 1 /*depth*/, 0 /*offset*/, -1 /*maxChildNodes*/, null /*filter*/);
        obj = parseJSONObject(nodes);
        assertPropertyValue(obj, "a/$key1", "value");
    }

    @Test
    public void setPropertyWithoutAddingNode() throws Exception {
        try {
            mk.commit("/", "^\"a/key1\" : \"value1\"", null, null);
            fail("Exception expected");
        } catch (Exception expected) {}
    }

    @Test
    @Ignore        
    public void setOverwritingProperty() throws Exception {
        String rev1 = mk.commit("/", "+\"a\" : {} ^\"a/key1\" : \"value1\"", null, null);

        // Commit with rev1
        mk.commit("/", "^\"a/key1\" : \"value2\"", rev1, null);

        // Commit with rev1 again (to overwrite rev2)
        mk.commit("/", "^\"a/key1\" : \"value3\"", rev1, null);

        String nodes = mk.getNodes("/", null, 1 /*depth*/, 0 /*offset*/, -1 /*maxChildNodes*/, null /*filter*/);
        JSONObject obj = parseJSONObject(nodes);
        assertPropertyValue(obj, "a/key1", "value3");
   }

    // This is a test to make sure commit time stays the same as time goes on.
    @Test
    @Ignore("OAK-461")
    public void commitTime() throws Exception {
        boolean debug = false;
        Monitor commitMonitor = MonitorFactory.getTimeMonitor("commit");
        for (int i = 0; i < 1000; i++) {
            commitMonitor.start();
            String diff = "+\"a"+i+"\" : {} +\"b"+i+"\" : {} +\"c"+i+"\" : {}";
            if (debug) System.out.println("Committing: " + diff);
            mk.commit("/", diff, null, null);
            commitMonitor.stop();
            if (debug) System.out.println("Committed in " + commitMonitor.getLastValue() + "ms");
        }
        if (debug) System.out.println("Final Result:" + commitMonitor);
    }

    @Test
    @Ignore    
    public void existingNodesMerged() throws Exception {
        String rev = mk.commit("/", "+\"a\" : {}", null, null);
        mk.commit("/", "+\"a/b\" : {}", null, null);
        mk.commit("/", "^\"a/key1\" : \"value1\"", null, null);

        // Commit to rev before key1 and b were added
        mk.commit("/", "^\"a/key2\" : \"value2\"", rev, null);

        // Check that key1 and b were merged
        String nodes = mk.getNodes("/", null, 1 /*depth*/, 0 /*offset*/, -1 /*maxChildNodes*/, null /*filter*/);
        JSONObject obj = parseJSONObject(nodes);
        assertPropertyValue(obj, ":childNodeCount", 1L);
        assertPropertyValue(obj, "a/key1", "value1");
        assertPropertyValue(obj, "a/key2", "value2");
    }
}