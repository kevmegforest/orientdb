/*
 *
 *  *  Copyright 2015 Orient Technologies LTD (info(at)orientdb.com)
 *  *
 *  *  Licensed under the Apache License, Version 2.0 (the "License");
 *  *  you may not use this file except in compliance with the License.
 *  *  You may obtain a copy of the License at
 *  *
 *  *       http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *  Unless required by applicable law or agreed to in writing, software
 *  *  distributed under the License is distributed on an "AS IS" BASIS,
 *  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  See the License for the specific language governing permissions and
 *  *  limitations under the License.
 *  *
 *  * For more information: http://www.orientdb.com
 *
 */

package com.orientechnologies.orient.graph.console;

import com.orientechnologies.orient.console.OConsoleDatabaseApp;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.orientechnologies.orient.graph.graphml.OGraphMLReader;
import com.orientechnologies.orient.graph.graphml.OGraphSONReader;
import com.orientechnologies.orient.graph.graphml.OIgnoreGraphMLImportStrategy;
import com.orientechnologies.orient.graph.graphml.ORenameGraphMLImportStrategy;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONMode;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONWriter;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.util.List;

/**
 * Test cases for gremlin console.
 * 
 * @author Luca Garulli
 */
public class OGremlinConsoleTest {

  private class TestOGremlinConsole extends OGremlinConsole {

    public ByteArrayOutputStream output = new ByteArrayOutputStream();

    public TestOGremlinConsole(String[] args) {
      super(args);
      setOutput(new PrintStream(output));
    }

  }

  @Test
  public void testGraphMLImport() {
    final String INPUT_FILE = "src/test/resources/graph-example-2.xml";
    String dbUrl = "memory:testGraphMLImport";
    StringBuilder builder = new StringBuilder();
    builder.append("create database " + dbUrl + ";\n");
    builder.append("import database " + INPUT_FILE + ";\n");
    OConsoleDatabaseApp console = new TestOGremlinConsole(new String[] { builder.toString() });
    try {
      console.run();

      ODatabaseDocumentTx db = new ODatabaseDocumentTx(dbUrl);
      db.open("admin", "admin");
      try {
        List<ODocument> result = db.query(new OSQLSynchQuery<ODocument>("select from V"));
        Assert.assertFalse(result.isEmpty());
        result = db.query(new OSQLSynchQuery<ODocument>("select from V where name is null"));
        Assert.assertEquals(1, result.size());

      } finally {
        db.close();
      }
    } finally {
      console.close();
    }
  }

  @Test
  public void testGraphMLExport() {
    final String INPUT_FILE = "src/test/resources/graph-example-2.xml";
    final String OUTPUT_FILE = "target/test/resources/graph-example-2.xml";
    String dbUrl = "memory:testGraphMLExport";
    StringBuilder builder = new StringBuilder();
    builder.append("create database " + dbUrl + ";\n");
    builder.append("import database " + INPUT_FILE + ";\n");
    builder.append("export database " + OUTPUT_FILE + ";\n");
    OConsoleDatabaseApp console = new TestOGremlinConsole(new String[] { builder.toString() });
    try {
      console.run();

      final File f = new File(OUTPUT_FILE);
      Assert.assertTrue(f.exists());
      Assert.assertTrue(f.length() > 0);

    } finally {
      console.close();
    }
  }

  @Test
  public void testMoveVertexCommand() {
    final String INPUT_FILE = "src/test/resources/graph-example-2.xml";
    String dbUrl = "memory:testMoveVertexCommand";
    StringBuilder builder = new StringBuilder();
    builder.append("create database " + dbUrl + ";\n");
    builder.append("import database " + INPUT_FILE + " batchSize=10;\n");
    builder.append("create class newposition extends V;\n");
    builder.append("move vertex (select from V) to class:newposition;\n");
    OConsoleDatabaseApp console = new TestOGremlinConsole(new String[] { builder.toString() });

    try {
      console.run();

      ODatabaseDocumentTx db = new ODatabaseDocumentTx(dbUrl);
      db.open("admin", "admin");
      try {
        List<ODocument> result = db.query(new OSQLSynchQuery<ODocument>("select from newposition"));
        Assert.assertFalse(result.isEmpty());

      } finally {
        db.close();
      }
    } finally {
      console.close();
    }

  }

  @Test
  public void testGremlin() {
    final String INPUT_FILE = "src/test/resources/graph-example-2.xml";
    String dbUrl = "memory:testGremlin";
    StringBuilder builder = new StringBuilder();
    builder.append("create database " + dbUrl + ";\n");
    builder.append("import database " + INPUT_FILE + " batchSize=10;\n");
    builder.append("gremlin g.V;\n");
    OConsoleDatabaseApp console = new TestOGremlinConsole(new String[] { builder.toString() });

    try {
      console.run();

      ODatabaseDocumentTx db = new ODatabaseDocumentTx(dbUrl);
      db.open("admin", "admin");
      try {
        long totalVertices = db.countClass("V");
        Assert.assertTrue(totalVertices > 0);

        long totalEdges = db.countClass("E");
        Assert.assertTrue(totalEdges > 0);

      } finally {
        db.close();
      }

    } finally {
      console.close();
    }

  }

  @Test
  public void testGraphMLImportWithSmallBatch() {
    final String INPUT_FILE = "src/test/resources/graph-example-2.xml";
    String dbUrl = "memory:testGraphMLImportWithSmallBatch";
    StringBuilder builder = new StringBuilder();
    builder.append("create database " + dbUrl + ";\n");
    builder.append("import database " + INPUT_FILE + " batchSize=10;\n");
    OConsoleDatabaseApp console = new TestOGremlinConsole(new String[] { builder.toString() });

    try {
      console.run();

      ODatabaseDocumentTx db = new ODatabaseDocumentTx(dbUrl);
      db.open("admin", "admin");
      try {
        List<ODocument> result = db.query(new OSQLSynchQuery<ODocument>("select from V"));
        Assert.assertFalse(result.isEmpty());
      } finally {
        db.close();
      }
    } finally {
      console.close();
    }

  }

  @Test
  public void testGraphMLImportIgnoreVAttribute() throws IOException {
    final String INPUT_FILE = "src/test/resources/graph-example-fromexport.xml";
    String dbUrl = "memory:testGraphMLImportIgnoreVAttribute";

    final OGraphMLReader graphml = new OGraphMLReader(new OrientGraphNoTx(dbUrl))
        .defineVertexAttributeStrategy("__type__", new OIgnoreGraphMLImportStrategy()).inputGraph(INPUT_FILE);

    ODatabaseDocumentTx db = new ODatabaseDocumentTx(dbUrl);
    db.open("admin", "admin");
    try {
      List<ODocument> result = db.query(new OSQLSynchQuery<ODocument>("select from V"));
      Assert.assertFalse(result.isEmpty());

      for (ODocument d : result) {
        Assert.assertFalse(d.containsField("__type__"));
      }
    } finally {
      db.close();
    }
  }

  @Test
  public void testGraphMLImportDirect() throws IOException {
    final String INPUT_FILE = "src/test/resources/graph-example-fromexport.xml";
    String dbUrl = "memory:testGraphMLImportDirect";

    new OGraphMLReader(new OrientGraphNoTx(dbUrl)).inputGraph(INPUT_FILE);

    ODatabaseDocumentTx db = new ODatabaseDocumentTx(dbUrl);
    db.open("admin", "admin");
    try {
      boolean foundTypeVAttr = false;
      boolean foundFriendEAttr = false;

      List<ODocument> result = db.query(new OSQLSynchQuery<ODocument>("select from V"));
      Assert.assertFalse(result.isEmpty());
      for (ODocument d : result) {
        if (d.containsField("__type__"))
          foundTypeVAttr = true;
      }

      Assert.assertTrue(foundTypeVAttr);

      result = db.query(new OSQLSynchQuery<ODocument>("select from E"));
      Assert.assertFalse(result.isEmpty());
      for (ODocument d : result) {
        if (d.containsField("friend"))
          foundFriendEAttr = true;
      }

      Assert.assertTrue(foundFriendEAttr);
    } finally {
      db.close();
    }
  }

  @Test
  public void testGraphMLImportIgnoreEAttribute() throws IOException {
    final String INPUT_FILE = "src/test/resources/graph-example-fromexport.xml";
    String dbUrl = "memory:testGraphMLImportIgnoreEAttribute";

    new OGraphMLReader(new OrientGraphNoTx(dbUrl)).defineEdgeAttributeStrategy("friend", new OIgnoreGraphMLImportStrategy())
        .inputGraph(INPUT_FILE);

    ODatabaseDocumentTx db = new ODatabaseDocumentTx(dbUrl);
    db.open("admin", "admin");
    try {
      List<ODocument> result = db.query(new OSQLSynchQuery<ODocument>("select from E"));
      Assert.assertFalse(result.isEmpty());

      for (ODocument d : result) {
        Assert.assertFalse(d.containsField("friend"));
      }
    } finally {
      db.close();
    }
  }

  @Test
  public void testGraphMLImportRenameVAttribute() throws IOException {
    final String INPUT_FILE = "src/test/resources/graph-example-fromexport.xml";
    String dbUrl = "memory:testGraphMLImportRenameVAttribute";

    final OrientGraphNoTx graph = new OrientGraphNoTx(dbUrl);
    try {
      new OGraphMLReader(graph).defineVertexAttributeStrategy("__type__", new ORenameGraphMLImportStrategy("t"))
          .inputGraph(INPUT_FILE);

      ODatabaseDocumentTx db = new ODatabaseDocumentTx(dbUrl);
      db.open("admin", "admin");
      try {
        List<ODocument> result = db.query(new OSQLSynchQuery<ODocument>("select from Person"));
        Assert.assertFalse(result.isEmpty());

        for (ODocument d : result) {
          Assert.assertTrue(d.containsField("t"));
          Assert.assertFalse(d.containsField("__type__"));
        }
      } finally {
        db.close();
      }
    } finally {
      graph.shutdown();
    }
  }

  @Test
  public void testGraphSONImport() throws IOException {
    final String INPUT_FILE = "src/test/resources/graph-example-fromexport.xml";
    String dbUrl1 = "memory:testGraphSONImport1";
    String dbUrl2 = "memory:testGraphSONImport2";

    final OrientGraphNoTx g1 = new OrientGraphNoTx(dbUrl1);
    new OGraphMLReader(g1).inputGraph(INPUT_FILE);

    // EXPORT IN GRAPHSON FORMAT
    final ByteArrayOutputStream output = new ByteArrayOutputStream();
    new GraphSONWriter(g1).outputGraph(output, null, null, GraphSONMode.NORMAL);

    final OrientGraphNoTx g2 = new OrientGraphNoTx(dbUrl2);
    ByteArrayInputStream is = new ByteArrayInputStream(output.toByteArray());
    new OGraphSONReader(g2).inputGraph(is);

    ODatabaseDocumentTx db = new ODatabaseDocumentTx(dbUrl2);
    db.open("admin", "admin");
    try {
      boolean foundTypeVAttr = false;
      boolean foundFriendEAttr = false;

      List<ODocument> result = db.query(new OSQLSynchQuery<ODocument>("select from V"));
      Assert.assertFalse(result.isEmpty());
      for (ODocument d : result) {
        if (d.containsField("__type__"))
          foundTypeVAttr = true;
      }

      Assert.assertTrue(foundTypeVAttr);

      result = db.query(new OSQLSynchQuery<ODocument>("select from E"));
      Assert.assertFalse(result.isEmpty());
      for (ODocument d : result) {
        if (d.containsField("friend"))
          foundFriendEAttr = true;
      }

      Assert.assertTrue(foundFriendEAttr);
    } finally {
      db.close();
    }
  }

}
