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
package org.apache.jackrabbit.test.api.query;

import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.qom.QueryObjectModel;
import javax.jcr.query.qom.Ordering;
import javax.jcr.query.qom.DynamicOperand;

import java.util.Calendar;
import java.util.Collections;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.math.BigDecimal;

/**
 * Abstract base class for all order by tests. Provides utility methods.
 */
class AbstractOrderByTest extends AbstractQueryTest {

    /** If <code>true</code> this repository supports sql queries */
    protected boolean checkSQL;

    private String[] nodeNames;

    protected void setUp() throws Exception {
        super.setUp();
        checkSQL = isSupported(Repository.OPTION_QUERY_SQL_SUPPORTED);
        nodeNames = new String[]{nodeName1, nodeName2, nodeName3, nodeName4};
    }

    /**
     * Populates the workspace with child nodes under <code>testroot</code> with
     * each node has a String value set in property with name
     * <code>propertyname1</code>.
     * @param values the String values.
     */
    protected void populate(String[] values) throws RepositoryException {
        for (int i = 0; i < values.length; i++) {
            Node node = testRootNode.addNode(nodeNames[i], testNodeType);
            node.setProperty(propertyName1, values[i]);
        }
        superuser.save();
    }

    /**
     * Populates the workspace with child nodes under <code>testroot</code> with
     * each node has a value set in property with name
     * <code>propertyname1</code>. The actual value is created by using the
     * sessions value factory and the given <code>type</code>.
     *
     * @param values the String values.
     * @param type a JCR property type.
     */
    protected void populate(String[] values, int type) throws RepositoryException {
        for (int i = 0; i < values.length; i++) {
            Node node = testRootNode.addNode(nodeNames[i], testNodeType);
            node.setProperty(propertyName1, vf.createValue(values[i], type));
        }
        superuser.save();
    }

    /**
     * Populates the workspace with child nodes under <code>testroot</code> with
     * each node has a calendar value set in property with name
     * <code>propertyname1</code>.
     * @param values the calendar values.
     */
    protected void populate(Calendar[] values) throws RepositoryException {
        for (int i = 0; i < values.length; i++) {
            Node node = testRootNode.addNode(nodeNames[i], testNodeType);
            node.setProperty(propertyName1, values[i]);
        }
        superuser.save();
    }

    /**
     * Populates the workspace with child nodes under <code>testroot</code> with
     * each node has a long value set in property with name
     * <code>propertyname1</code>.
     * @param values the long values.
     */
    protected void populate(long[] values) throws RepositoryException {
        for (int i = 0; i < values.length; i++) {
            Node node = testRootNode.addNode(nodeNames[i], testNodeType);
            node.setProperty(propertyName1, values[i]);
        }
        superuser.save();
    }

    /**
     * Populates the workspace with child nodes under <code>testroot</code> with
     * each node has a double value set in property with name
     * <code>propertyname1</code>.
     * @param values the double values.
     */
    protected void populate(double[] values) throws RepositoryException {
        for (int i = 0; i < values.length; i++) {
            Node node = testRootNode.addNode(nodeNames[i], testNodeType);
            node.setProperty(propertyName1, values[i]);
        }
        superuser.save();
    }

    /**
     * Populates the workspace with child nodes under <code>testroot</code> with
     * each node has a decimal value set in property with name
     * <code>propertyname1</code>.
     * @param values the decimal values.
     */
    protected void populate(BigDecimal[] values) throws RepositoryException {
        for (int i = 0; i < values.length; i++) {
            Node node = testRootNode.addNode(nodeNames[i], testNodeType);
            node.setProperty(propertyName1, values[i]);
        }
        superuser.save();
    }

    /**
     * Runs queries on the workspace and checks if the ordering is according
     * to the <code>nodeNames</code>.
     * @param nodeNames the sequence of node names required in the result set.
     */
    protected void checkOrder(String[] nodeNames) throws RepositoryException {
        // first check ascending

        String sql = createSQL();
        String xpath = createXPath();
        Query q;
        QueryResult result;
        if (sql != null) {
            q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
            result = q.execute();
            checkResultOrder(result, nodeNames);
        }

        if (xpath != null) {
            q = superuser.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
            result = q.execute();
            checkResultOrder(result, nodeNames);
        }

        q = createQOM(true);
        result = q.execute();
        checkResultOrder(result, nodeNames);

        // then check descending
        Collections.reverse(Arrays.asList(nodeNames));

        if (sql != null) {
            q = superuser.getWorkspace().getQueryManager().createQuery(sql + " DESC", Query.SQL);
            result = q.execute();
            checkResultOrder(result, nodeNames);
        }

        if (xpath != null) {
            q = superuser.getWorkspace().getQueryManager().createQuery(xpath + " descending", Query.XPATH);
            result = q.execute();
            checkResultOrder(result, nodeNames);
        }

        q = createQOM(false);
        result = q.execute();
        checkResultOrder(result, nodeNames);
    }

    /**
     * Checks if the node ordering in <code>result</code> is according to
     * <code>nodeNames</code>.
     * @param result the query result.
     * @param nodeNames the node names.
     */
    protected void checkResultOrder(QueryResult result, String[] nodeNames)
            throws RepositoryException {
        List nodes = new ArrayList();
        for (NodeIterator it = result.getNodes(); it.hasNext();) {
            nodes.add(it.nextNode());
        }
        assertEquals("Wrong hit count:", nodeNames.length, nodes.size());

        for (int i = 0; i < nodeNames.length; i++) {
            String name = ((Node) nodes.get(i)).getName();
            assertEquals("Wrong order of nodes:", nodeNames[i], name);
        }
    }

    /**
     * @return a basic QOM to test order by queries.
     * @throws RepositoryException if an error occurs.
     */
    protected QueryObjectModel createQOM(boolean ascending)
            throws RepositoryException {
        DynamicOperand op = createOrderingOperand();
        Ordering ordering;
        if (ascending) {
            ordering = qf.ascending(op);
        } else {
            ordering = qf.descending(op);
        }
        return qf.createQuery(
                qf.selector(testNodeType, "s"),
                qf.descendantNode("s", testRoot),
                new Ordering[]{ordering},
                null
        );
    }

    /**
     * @return a dynamic operand that is used in the QOM created by
     *         {@link #createQOM(boolean)}.
     * @throws RepositoryException if an error occurs.
     */
    protected DynamicOperand createOrderingOperand()
            throws RepositoryException {
        return qf.propertyValue("s", propertyName1);
    }

    /**
     * @return a basic SQL statement to test order by queries. Returns
     *         <code>null</code> if SQL is not supported.
     */
    protected String createSQL() {
        if (checkSQL) {
            return "SELECT " + escapeIdentifierForSQL(propertyName1) +
                    " FROM "+ escapeIdentifierForSQL(testNodeType) + " WHERE " +
                    jcrPath + " LIKE '" + testRoot + "/%' ORDER BY " +
                    escapeIdentifierForSQL(propertyName1);
        } else {
            return null;
        }
    }

    /**
     * @return a basic XPath statement to test order by queries. Returns
     *         <code>null</code> is XPath is not supported.
     * @throws RepositoryException if an error occurs.
     */
    protected String createXPath() throws RepositoryException {
        List languages = Arrays.asList(superuser.getWorkspace().getQueryManager().getSupportedQueryLanguages());
        if (languages.contains(Query.XPATH)) {
            return xpathRoot + "/*[@jcr:primaryType='" + testNodeType + "'] order by @" + propertyName1;
        } else {
            return null;
        }
    }

}
