/* Copyright (c) 2001-2009, The HSQL Development Group
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the HSQL Development Group nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL HSQL DEVELOPMENT GROUP, HSQLDB.ORG,
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package org.hsqldb_voltpatches;

import java.util.Arrays;
import java.util.ArrayList; // LX FEAT2

import org.hsqldb_voltpatches.HSQLInterface.HSQLParseException;
import org.hsqldb_voltpatches.HsqlNameManager.SimpleName;
import org.hsqldb_voltpatches.ParserDQL.CompileContext;
import org.hsqldb_voltpatches.index.Index;
import org.hsqldb_voltpatches.lib.HashMap;
import org.hsqldb_voltpatches.lib.HashMappedList;
import org.hsqldb_voltpatches.lib.HashSet;
import org.hsqldb_voltpatches.lib.HsqlArrayList;
import org.hsqldb_voltpatches.lib.OrderedHashSet;
import org.hsqldb_voltpatches.lib.OrderedIntHashSet;// Added by LX
import org.hsqldb_voltpatches.navigator.RangeIterator;
import org.hsqldb_voltpatches.navigator.RowIterator;
import org.hsqldb_voltpatches.persist.PersistentStore;
import org.hsqldb_voltpatches.store.ValuePool;
import org.hsqldb_voltpatches.types.Type;

/**
 * Metadata for range variables, including conditions.
 *
 * @author Fred Toussi (fredt@users dot sourceforge.net)
 * @version 1.9.0
 * @since 1.9.0
 */
final class RangeVariable {
    // Added by LX
    private final String c_PROP1 = "PROP1";
    private final String c_PROP2 = "PROP2";
    private final String c_PROP3 = "PROP3";
    private final String c_PROP4 = "PROP4";
    private final String c_PROP5 = "PROP5";
    private final String c_LENGTH = "LENGTH";
    //End LX

    static final RangeVariable[] emptyArray = new RangeVariable[]{};

    //
    final Table            rangeTable;
    // Added by LX
    // GVoltDB extension
    final GraphView        rangeGraph;
    final boolean          isGraph;
    final boolean          isGraph2Graph; // LX FEAT4
    final String           newGraphName;  // LX FEAT4
    final String           newGraphVertex; // LX FEAT4
    final String           newGraphEdge; // LX FEAT4
    final String           chosenVertexLabel; // LX FEAT4
    final String           chosenEdgeLabel; // LX FEAT4
    boolean                isVertexes;
    boolean                isEdges;
    boolean                isPaths;
    final String           hint;
    final String[]         vertexLabels; // LX FEAT2
    final String[]         edgeLabels; // LX FEAT3
    SimpleName             vertexTableAlias; // LX FEAT4
    SimpleName             edgeTableAlias; // LX FEAT4
    // End LX
    final SimpleName       tableAlias;
    private OrderedHashSet columnAliases;
    private SimpleName[]   columnAliasNames;
    private OrderedHashSet columnNames;
    OrderedHashSet         namedJoinColumns;
    HashMap                namedJoinColumnExpressions;
    Index                  rangeIndex;
    private final Object[] emptyData;
    final boolean[]        columnsInGroupBy;
    boolean                hasKeyedColumnInGroupBy;
    final boolean[]        usedColumns;
    boolean[]              updatedColumns;

    // index conditions
    Expression indexCondition;
    Expression indexEndCondition;
    boolean    isJoinIndex;

    // non-index consitions
    Expression nonIndexJoinCondition;
    Expression nonIndexWhereCondition;

    //
    boolean              isLeftJoin;              // table joined with LEFT / FULL OUTER JOIN
    boolean              isRightJoin;             // table joined with RIGHT / FULL OUTER JOIN
    boolean              isMultiFindFirst;        // findFirst() uses multi-column index
    private Expression[] findFirstExpressions;    // expressions for column values
    private int          multiColumnCount;
    int                  level;

    //
    int rangePosition;

    // for variable and argument lists
    HashMappedList variables;

    // variable v.s. argument
    boolean isVariable;

    RangeVariable(HashMappedList variables, boolean isVariable) {

        this.variables   = variables;
        this.isVariable  = isVariable;
        rangeTable       = null;
        rangeGraph       = null;// Added by LX
        isGraph          = false;// Added by LX
        isGraph2Graph    = false;//LX FEAT4
        tableAlias       = null;
        emptyData        = null;
        columnsInGroupBy = null;
        usedColumns      = null;
        newGraphName     = null; // LX FEAT4
        newGraphVertex   = null; // LX FEAT4
        newGraphEdge     = null; // LX FEAT4
        isVertexes       = false;// Added by LX
        isEdges          = false;// Added by LX
        isPaths          = false;// Added by LX
        hint             = null;// Added by LX
        vertexLabels     = null; // LX FEAT2
        edgeLabels       = null; // LX FEAT3
        vertexTableAlias = null;// LX FEAT4
        edgeTableAlias   = null; // LX FEAT4
        chosenVertexLabel = null; // LX FEAT4
        chosenEdgeLabel = null; // LX FEAT4
    }

    RangeVariable(Table table, SimpleName alias, OrderedHashSet columnList,
                  SimpleName[] columnNameList, CompileContext compileContext) {

        rangeTable       = table;
        tableAlias       = alias;
        columnAliases    = columnList;
        columnAliasNames = columnNameList;
        emptyData        = rangeTable.getEmptyRowData();
        columnsInGroupBy = rangeTable.getNewColumnCheckList();
        usedColumns      = rangeTable.getNewColumnCheckList();
        rangeIndex       = rangeTable.getPrimaryIndex();
        // Added by LX
        isGraph          = false;
        rangeGraph       = null;
        isVertexes       = false;
        isEdges          = false;
        isPaths          = false;
        hint             = null;
        vertexLabels     = null; // LX FEAT2
        edgeLabels       = null; // LX FEAT3
        isGraph2Graph    = false;//LX FEAT4
        vertexTableAlias = null;// LX FEAT4
        edgeTableAlias   = null; // LX FEAT4
        newGraphName     = null; // LX FEAT4
        newGraphVertex   = null; // LX FEAT4
        newGraphEdge     = null; // LX FEAT4
        chosenVertexLabel = null; // LX FEAT4
        chosenEdgeLabel = null; // LX FEAT4
        // End LX
        compileContext.registerRangeVariable(this);
    }

    // Implement LX FEAT2 FEAT3
    RangeVariable(GraphView graph, int type, SimpleName alias, OrderedHashSet columnList, SimpleName[] columnNameList, CompileContext compileContext, String hint, HashSet vertexLabels, HashSet edgeLabels) {
          isGraph          = true;
          
          if (type == Tokens.VERTEXES) { 
              isVertexes = true;
              isEdges = false;
              isPaths = false;
          }
          else if (type == Tokens.EDGES) {
              isVertexes = false;
              isEdges = true;
              isPaths = false;
          }
          else if (type == Tokens.PATHS) {
              isVertexes = false;
              isEdges = false;
              isPaths = true;
          }
          else {
              isVertexes = false;
              isEdges = false;
              isPaths = false;
          }
          
          rangeGraph       = graph;
          this.hint        = hint;
          this.vertexLabels = new String[vertexLabels.size()];
          vertexLabels.toArray(this.vertexLabels);// LX FEAT2
          this.edgeLabels = new String[edgeLabels.size()];
          edgeLabels.toArray(this.edgeLabels); // LX FEAT3 
          
          rangeTable       = null;
          tableAlias       = alias;
          columnAliases    = columnList;
          columnAliasNames = columnNameList;
          emptyData        = null; //rangeGraph.getEmptyRowData();
          
          if (isGraph) {
              columnsInGroupBy = new boolean[rangeGraph.getAllPropCount()];
              usedColumns      = new boolean[rangeGraph.getAllPropCount()];
          }
          /*if (isVertexes) {
              columnsInGroupBy = new boolean[rangeGraph.getVertexPropCount()];
              usedColumns      = new boolean[rangeGraph.getVertexPropCount()];
          }
          else if (isEdges) {
              columnsInGroupBy = new boolean[rangeGraph.getEdgePropCount()];
              usedColumns      = new boolean[rangeGraph.getEdgePropCount()];          
          }
          else if (isPaths) {
              columnsInGroupBy = new boolean[rangeGraph.getPathPropCount()];
              usedColumns      = new boolean[rangeGraph.getPathPropCount()];
          */
          else { 
              columnsInGroupBy = null;
              usedColumns      = null;
          }
          rangeIndex       = null;//rangeGraph.getPrimaryIndex();
          isGraph2Graph    = false;//LX FEAT4
          vertexTableAlias = null;// LX FEAT4
          edgeTableAlias   = null; // LX FEAT4
          newGraphName     = null; // LX FEAT4
          newGraphVertex   = null; // LX FEAT4
          newGraphEdge     = null; // LX FEAT4
          chosenVertexLabel = null; // LX FEAT4
          chosenEdgeLabel = null; // LX FEAT4
          compileContext.registerRangeVariable(this);
    }

    // LX FEAT4
    RangeVariable(GraphView graph, boolean hasV, boolean hasE, SimpleName valias, SimpleName ealias, String newGraphName, String newGraphVertex, String newGraphEdge, String chosenVertexLabel, String chosenEdgeLabel, CompileContext compileContext) {
          isGraph          = true;
          isGraph2Graph    = true;
          this.newGraphName     = newGraphName; 
          this.newGraphVertex   = newGraphVertex; 
          this.newGraphEdge     = newGraphEdge; 
          if (hasV) { 
              isVertexes = true;
              isEdges = false;
              isPaths = false;
          }
          else if (hasE) {
              isVertexes = false;
              isEdges = true;
              isPaths = false;
          }
          
          
          rangeGraph       = graph;
          hint = null;
          vertexLabels = null;
          edgeLabels = null;
          
          rangeTable       = null;
          vertexTableAlias = valias;
          edgeTableAlias   = ealias;
          tableAlias       = null;
          columnAliases    = null;
          columnAliasNames = null;
          emptyData        = null; //rangeGraph.getEmptyRowData();
          
          if (isGraph) {
              columnsInGroupBy = new boolean[rangeGraph.getAllPropCount()];
              usedColumns      = new boolean[rangeGraph.getAllPropCount()];
          }
          else { 
              columnsInGroupBy = null;
              usedColumns      = null;
          }
          rangeIndex       = null;//rangeGraph.getPrimaryIndex();
          this.chosenVertexLabel = chosenVertexLabel;
          this.chosenEdgeLabel = chosenEdgeLabel;
          compileContext.registerRangeVariable(this);
    }
/*
    RangeVariable(Table table, String alias, OrderedHashSet columnList,
                  Index index, CompileContext compileContext) {

        rangeTable       = table;
        tableAlias       = alias;
        columnAliases    = columnList;
        emptyData        = rangeTable.getEmptyRowData();
        columnsInGroupBy = rangeTable.getNewColumnCheckList();
        usedColumns      = rangeTable.getNewColumnCheckList();
        rangeIndex       = index;

        compileContext.registerRangeVariable(this);
    }
*/
    RangeVariable(RangeVariable range) {

        rangeTable       = range.rangeTable;
        tableAlias       = null;
        emptyData        = rangeTable.getEmptyRowData();
        columnsInGroupBy = rangeTable.getNewColumnCheckList();
        usedColumns      = rangeTable.getNewColumnCheckList();
        rangeIndex       = rangeTable.getPrimaryIndex();
        rangePosition    = range.rangePosition;
        level            = range.level;
        // Added by LX
        isGraph          = false;
        rangeGraph       = null;
        isVertexes       = false;
        isEdges          = false;
        isPaths          = false;
        hint             = null;
        vertexLabels      = null;
        edgeLabels       = null;
        isGraph2Graph    = false;//LX FEAT4
        vertexTableAlias = null;// LX FEAT4
        edgeTableAlias   = null; // LX FEAT4
        newGraphName     = null; // LX FEAT4
        newGraphVertex   = null; // LX FEAT4
        newGraphEdge     = null; // LX FEAT4
        chosenVertexLabel = null; // LX FEAT4
        chosenEdgeLabel = null; // LX FEAT4
        // End LX
    }

    // Added by LX
    Index getIndexForColumns(OrderedIntHashSet set) {
        if (isGraph) {
            //  System.out.println("RangeVariable.getIndexForColumns: Indexs for a graph are not supported.");
            return null;
        }else {
            return rangeTable.getIndexForColumns(set);
        }
    }
    // End LX
    void setJoinType(boolean isLeft, boolean isRight) {
        isLeftJoin  = isLeft;
        isRightJoin = isRight;
    }

    public void addNamedJoinColumns(OrderedHashSet columns) {
        namedJoinColumns = columns;
    }

    public void addColumn(int columnIndex) {
        usedColumns[columnIndex] = true;
    }

    void addNamedJoinColumnExpression(String name, Expression e) {

        if (namedJoinColumnExpressions == null) {
            namedJoinColumnExpressions = new HashMap();
        }

        namedJoinColumnExpressions.put(name, e);
    }

    ExpressionColumn getColumnExpression(String name) {

        return namedJoinColumnExpressions == null ? null
                                                  : (ExpressionColumn) namedJoinColumnExpressions
                                                  .get(name);
    }

    Table getTable() {
        return rangeTable;
    }

    // Added by LX
    GraphView getGraph() {
        return rangeGraph;
    }
    // End LX

    // Implement LX FEAT2
    String[] getVertexLabels(){
        return vertexLabels;
    }

    // LX FEAT3
    String[] getEdgeLabels() {
        return edgeLabels;
    }
    // LX FEAT2
    boolean isGraph() {
        return isGraph;
    }

    // LX FEAT4
    boolean isGraph2Graph() {
        return isGraph2Graph;
    }

    // LX FEAT4
    boolean hasEdge() {
        return isEdges;
    }

    // LX FEAT4
    void setHasEdge(boolean t) {
        isEdges = t;
    }

    // LX FEAT4
    void setHasVertex(boolean t) {
        isVertexes = t;
    }

    // LX FEAT4
    boolean hasVertex() {
        return isVertexes;
    }

    // LX FEAT4
    SimpleName getEdgeTableAlias() {
        return edgeTableAlias;
    }

    // LX FEAT4
    SimpleName getVertexTableAlias() {
        return vertexTableAlias;
    }

    // LX FEAT4
    void setEdgeTableAlias(SimpleName ename) {
        edgeTableAlias = ename;
    }

    // LX FEAT4
    void setVertexTableAlias(SimpleName vname) {
        vertexTableAlias = vname;
    }

    // LX FEAT4
    String getNewGraphName() {
        return newGraphName;
    }

    // LX FEAT4
    String getNewGraphVertex() {
        return newGraphVertex;
    }

    // LX FEAT4
    String getNewGraphEdge() {
        return newGraphEdge;
    }

    public OrderedHashSet getColumnNames() {

        if (columnNames == null) {
            columnNames = new OrderedHashSet();

            rangeTable.getColumnNames(this.usedColumns, columnNames);
        }

        return columnNames;
    }

    public OrderedHashSet getUniqueColumnNameSet() {

        OrderedHashSet set = new OrderedHashSet();

        if (columnAliases != null) {
            set.addAll(columnAliases);

            return set;
        }

        for (int i = 0; i < rangeTable.columnList.size(); i++) {
            String  name  = rangeTable.getColumn(i).getName().name;
            boolean added = set.add(name);

            if (!added) {
                throw Error.error(ErrorCode.X_42578, name);
            }
        }

        return set;
    }

    /**
     * Returns the index for column, given only the column name.
     *
     * @param columnName name of column
     * @return int index or -1 if not found
     */
    public int findColumn(String columnName) {
        return findColumn(null, columnName);
    }

    /**
     * Returns the index for the column given the column's table name
     * and column name.  If the table name is null, there is no table
     * name specified.  For example, in a query "select C from T" there
     * is no table name, so tableName would be null.  In the query
     * "select T.C from T" tableName would be the string "T".  Don't
     * return any column found in a USING join condition.
     *
     * @param tableName
     * @param columnName
     * @return the column index or -1 if the column name is in a using list.
     */
    public int findColumn(String tableName, String columnName) {
        // The namedJoinColumnExpressions are ExpressionColumn objects
        // for columns named in USING conditions.  Each range variable
        // has a possibly empty list of these.  If two range variables are
        // operands of a join with a USING condition, both get the same list
        // of USING columns.  In our semantics the query
        //      select T2.C from T1 join T2 using(C);
        // selects T2.C.  This is not standard behavior, but it seems to
        // be common to mysql and postgresql.  The query
        //      select C from T1 join T2 using(C);
        // selects the C from T1 or T2, since the using clause says
        // they will have the same value.  In the query
        //      select C from T1 join T2 using(C), T3;
        // where T3 has a column named C, there is an ambiguity, since
        // the first join tree (T1 join T2 using(C)) has a column named C and
        // T3 has another C column.  In this case we need the T1.C notation.
        // The query
        //      select T1.C from T1 join T2 using(C), T3;
        // will select the C from the first join tree, and
        //      select T3.C from T1 join T2 using(C), T3;
        // will select the C from the second join tree, which is just T3.

        // If we don't have a table name and there are some USING columns,
        // then look into them.  If the name is in the USING columns, it
        // is not in this range variable.  The function getColumnExpression
        // will fetch this using variable in another search.
        if (namedJoinColumnExpressions != null
                && tableName == null
                && namedJoinColumnExpressions.containsKey(columnName)) {
            return -1;
        }
// System.out.println("RangeVariable:560:" + columnName);
        if (variables != null) {
            return variables.getIndex(columnName);
        } else if (columnAliases != null) {
            return columnAliases.getIndex(columnName);
        } else {
            // Commented by LX
            // return rangeTable.findColumn(columnName);
            // LX FEAT4
            
                
            if (isGraph2Graph) {
                int res1 = rangeGraph.findVertexProp(columnName);
                int res2 = rangeGraph.findEdgeProp(columnName);
                if (isVertexes && isEdges) {
                    return res1 == -1 ? res2 : res1;
                }
                else if (isVertexes)
                    return res1;
                else if (isEdges)
                    return res2;
            }
            // Added by LX
            if (isGraph && isVertexes)
                return rangeGraph.findVertexProp(columnName);
            else if (isGraph && isEdges)
                return rangeGraph.findEdgeProp(columnName);
            else if (isGraph && isPaths) {
                //System.out.println("RangeVariable.findColumn() 347 column = " + columnName);
                return rangeGraph.findPathProp(columnName);
            }
            else 
                return rangeTable.findColumn(columnName);
            // End LX
        }
    }

    // Added by LX
    // TODO merge with findColumn(String tableName, String columnName)
    public int findColumn(String tableName, String objectName, String columnName) {
// System.out.println("RangeVariable:389:" + tableName + "," + objectName + "," + columnName);
        if (namedJoinColumnExpressions != null
                && tableName == null
                && namedJoinColumnExpressions.containsKey(columnName)) {
            return -1;
        }

        if (variables != null) {
            return variables.getIndex(columnName);
        } 
        else if (columnAliases != null) {
            return columnAliases.getIndex(columnName);
        } 
        else if (isGraph2Graph) {
            int res1 = rangeGraph.findVertexProp(columnName);
            int res2 = rangeGraph.findEdgeProp(columnName);
            // this is a col for returning graph
            if (res1 == -1 && res2 == -1)
                return rangeGraph.findGraphProp(columnName);
            // this col is in where clause
            if (isVertexes && isEdges) {
                return res1 == -1 ? res2 : res1;
            }
            else if (isVertexes)
                return res1;
            else 
                return res2;
        }
        else {
            if (objectName.equals(Tokens.getKeyword(Tokens.EDGES)))
                return rangeGraph.findEdgeProp(columnName);
            else if (objectName.equals(Tokens.getKeyword(Tokens.VERTEXES)) ||
                     objectName.equals(Tokens.getKeyword(Tokens.STARTVERTEX)) ||
                     objectName.equals(Tokens.getKeyword(Tokens.ENDVERTEX))
                    ) 
                return rangeGraph.findVertexProp(columnName);
            else if (objectName.equals(Tokens.getKeyword(Tokens.PATHS)))
                return rangeGraph.findPathProp(columnName);
            else  
                return rangeTable.findColumn(columnName);
        }
    }
    // End LX
    ColumnSchema getColumn(String columnName) {

        int index = findColumn(columnName);

        return index < 0 ? null
                         : rangeTable.getColumn(index);
    }

    ColumnSchema getColumn(int i) {

        if (variables != null) {
            return (ColumnSchema) variables.get(i);
        } else {
            // Add LX
            if (isGraph && isVertexes) 
                return rangeGraph.getVertexProp(i);
            else if (isGraph && isEdges)
                return rangeGraph.getEdgeProp(i);
            else if (isGraph && isPaths)
                return rangeGraph.getPathProp(i);
            // End LX
            return rangeTable.getColumn(i);
        }
    }

    String getColumnAlias(int i) {

        SimpleName name = getColumnAliasName(i);

        return name.name;
    }

    public SimpleName getColumnAliasName(int i) {

        if (columnAliases != null) {
            return columnAliasNames[i];
        } else {
            return rangeTable.getColumn(i).getName();
        }
    }

    boolean hasColumnAliases() {
        return columnAliases != null;
    }

    boolean resolvesTableName(ExpressionColumn e) {

        if (e.tableName == null) {
            return true;
        }

        // Added by LX
        String tablename;
        String schemaname;
        if (isGraph) { // GVoltDB extension
            tablename = rangeGraph.GraphName.name;
            schemaname = rangeGraph.GraphName.schema.name;
        }
        else {
            tablename = rangeTable.tableName.name;
            schemaname = rangeTable.tableName.schema.name;
        }
        
        //org.voltdb.VLog.GLog("RangeVariable", "resolvesTableName", 3935, 
        //      " e.columnName = " + e.columnName
        //    + " e.tableName = "+e.tableName + " tablename = "+tablename
        //    + " tableAlias.name = "+tableAlias.name
        //    + " e.schema = "+ e.schema + " schemaname = " + schemaname
        //    );
        // End LX
        if (e.schema == null) {
            if (tableAlias == null) {
              // System.out.println("RangeVariable:513:" + tablename + ", " + e.tableName);
                // if (e.tableName.equals(rangeTable.tableName.name)) { comment LX
                if (e.tableName.equals(tablename)) { // Added by LX
                    return true;
                }
            } else if (e.tableName.equals(tableAlias.name)) {
                return true;
            }
        } else {
            // if (e.tableName.equals(rangeTable.tableName.name)
                    // && e.schema.equals(rangeTable.tableName.schema.name)) { Comment LX
            
            if (e.tableName.equals(tablename) && e.schema.equals(schemaname)) { //Added by LX
                return true;
            }
        }

        return false;
    }

    public boolean resolvesTableName(String name) {

        if (name == null) {
            return true;
        }

        if (tableAlias == null) {
            if (name.equals(rangeTable.tableName.name)) {
                return true;
            }
        } else if (name.equals(tableAlias.name)) {
            return true;
        }

        return false;
    }

    boolean resolvesSchemaName(String name) {

        if (name == null) {
            return true;
        }

        if (tableAlias != null) {
            return false;
        }

        return name.equals(rangeTable.tableName.schema.name);
    }

    /**
     * Add all columns to a list of expressions
     */
    void addTableColumns(HsqlArrayList exprList) {

        if (namedJoinColumns != null) {
            int count    = exprList.size();
            int position = 0;

            for (int i = 0; i < count; i++) {
                Expression e          = (Expression) exprList.get(i);
                String     columnName = e.getColumnName();

                if (namedJoinColumns.contains(columnName)) {
                    if (position != i) {
                        exprList.remove(i);
                        exprList.add(position, e);
                    }

                    e = getColumnExpression(columnName);

                    exprList.set(position, e);

                    position++;
                }
            }
        }
        // Commented by LX
        // addTableColumns(exprList, exprList.size(), namedJoinColumns);
        // Added by LX
        if (!isGraph)
            addTableColumns(exprList, exprList.size(), namedJoinColumns);
        // TODO Should we do it?
        else addGraphAllProps(exprList, exprList.size(), namedJoinColumns);
        // End LX
    }

    // Added by LX
    private int addGraphAllProps(HsqlArrayList expList, int position, HashSet exclude) {

        GraphView graph = getGraph();
        int   count = graph.getAllPropCount();
// System.out.println("RangeVariable:807:count is " + count);
        for (int i = 0; i < count; i++) {
            ColumnSchema column = null;
            if (isEdges && graph.isEdge(i)) {
                // column = graph.getEdgeProp(i);
                // LX FEAT3
                for (String edgeLabel: edgeLabels)
                  if (edgeLabel.equals(graph.getEdgeLabelByIndex(graph.getELabelIdxByIndex(i))))
                    column = graph.getEdgeProp(i);
            }
            else if (isVertexes && graph.isVertex(i)) {
                // LX FEAT2
                for (String vertexLabel: vertexLabels)
                  if (vertexLabel.equals(graph.getVertexLabelByIndex(graph.getVLabelIdxByIndex(i))))
                    column = graph.getVertexProp(i);
            }
                
            else if (isPaths && graph.isPath(i))
                column = graph.getPathProp(i);
            else if (isGraph2Graph && graph.isGraph(i))
                // LX FEAT4
                column = graph.getGraphProp(i);
            
            // LX FEAT2
            if (column != null) {
                String columnName = columnAliases == null ? column.getName().name : (String) columnAliases.get(i);

                if (exclude != null && exclude.contains(columnName)) {
                    continue;
                }

                Expression e = new ExpressionColumn(this, column, i);

                expList.add(position++, e);  
            }
        }
        return position;
    }
    // End LX
    /**
     * Add all columns to a list of expressions
     */
    int addTableColumns(HsqlArrayList expList, int position, HashSet exclude) {

        Table table = getTable();
        int   count = table.getColumnCount();

        for (int i = 0; i < count; i++) {
            ColumnSchema column = table.getColumn(i);
            String columnName = columnAliases == null ? column.getName().name
                                                      : (String) columnAliases
                                                          .get(i);

            if (exclude != null && exclude.contains(columnName)) {
                continue;
            }

            Expression e = new ExpressionColumn(this, column, i);

            expList.add(position++, e);
        }

        return position;
    }

    void addTableColumns(Expression expression, HashSet exclude) {

        HsqlArrayList list  = new HsqlArrayList();
        Table         table = getTable();
        int           count = table.getColumnCount();

        for (int i = 0; i < count; i++) {
            ColumnSchema column = table.getColumn(i);
            String columnName = columnAliases == null ? column.getName().name
                                                      : (String) columnAliases
                                                          .get(i);

            if (exclude != null && exclude.contains(columnName)) {
                continue;
            }

            Expression e = new ExpressionColumn(this, column, i);

            list.add(e);
        }

        Expression[] nodes = new Expression[list.size()];

        list.toArray(nodes);

        expression.nodes = nodes;
    }

    /**
     * Removes reference to Index to avoid possible memory leaks after alter
     * table or drop index
     */
    void setForCheckConstraint() {
        rangeIndex = null;
    }

    /**
     *
     * @param e condition
     * @param index Index object
     * @param isJoin whether a join or not
     */
    void addIndexCondition(Expression e, Index index, boolean isJoin) {

        rangeIndex  = index;
        isJoinIndex = isJoin;

        switch (e.getType()) {

            case OpTypes.NOT :
                indexCondition = e;
                break;

            case OpTypes.IS_NULL :
                indexEndCondition = e;
                break;

            case OpTypes.EQUAL :
                indexCondition    = e;
                indexEndCondition = indexCondition;
                break;

            case OpTypes.GREATER :
            case OpTypes.GREATER_EQUAL :
            	indexCondition = makeConjunction(indexCondition, e);
                break;

            case OpTypes.SMALLER :
            case OpTypes.SMALLER_EQUAL :
            	indexEndCondition = makeConjunction(indexEndCondition, e);
                break;

            default :
                Error.runtimeError(ErrorCode.U_S0500, "Expression");
        }
    }

    private static Expression makeConjunction(Expression existingExpr, Expression newExpr) {
    	return (existingExpr == null) ? newExpr : new ExpressionLogical(OpTypes.AND, existingExpr, newExpr);
    }

    /**
     *
     * @param e a join condition
     */
    void addJoinCondition(Expression e) {
        nonIndexJoinCondition =
            ExpressionLogical.andExpressions(nonIndexJoinCondition, e);
    }

    /**
     *
     * @param e a where condition
     */
    void addWhereCondition(Expression e) {
        nonIndexWhereCondition =
            ExpressionLogical.andExpressions(nonIndexWhereCondition, e);
    }

    void addCondition(Expression e, boolean isJoin) {

        if (isJoin) {
            addJoinCondition(e);
        } else {
            addWhereCondition(e);
        }
    }

    /**
     * Only multiple EQUAL conditions are used
     *
     * @param exprList list of expressions
     * @param index Index to use
     * @param isJoin whether a join or not
     */
    void addIndexCondition(Expression[] exprList, Index index, int colCount,
                           boolean isJoin) {
// VoltDB extension
        if (rangeIndex == index && isJoinIndex && (!isJoin) &&
                (multiColumnCount > 0) && (colCount == 0)) {
            // This is one particular set of conditions which broke the classification of
            // ON and WHERE clauses.
            return;
        }
// End of VoltDB extension
        rangeIndex  = index;
        isJoinIndex = isJoin;

        for (int i = 0; i < colCount; i++) {
            Expression e = exprList[i];

            indexEndCondition =
                ExpressionLogical.andExpressions(indexEndCondition, e);
        }

        if (colCount == 1) {
            indexCondition = exprList[0];
        } else {
            findFirstExpressions = exprList;
            isMultiFindFirst     = true;
            multiColumnCount     = colCount;
        }
    }

    boolean hasIndexCondition() {
        return indexCondition != null;
    }

    /**
     * Retreives a String representation of this obejct. <p>
     *
     * The returned String describes this object's table, alias
     * access mode, index, join mode, Start, End and And conditions.
     *
     * @return a String representation of this object
     */
    public String describe(Session session) {

        StringBuffer sb;
        String       temp;
        Index        index;
        Index        primaryIndex;
        int[]        primaryKey;
        boolean      hidden;
        boolean      fullScan;

        sb           = new StringBuffer();
        index        = rangeIndex;
        primaryIndex = rangeTable.getPrimaryIndex();
        primaryKey   = rangeTable.getPrimaryKey();
        hidden       = false;
        fullScan     = (indexCondition == null && indexEndCondition == null);

        if (index == null) {
            index = primaryIndex;
        }

        if (index == primaryIndex && primaryKey.length == 0) {
            hidden   = true;
            fullScan = true;
        }

        sb.append(super.toString()).append('\n');
        sb.append("table=[").append(rangeTable.getName().name).append("]\n");

        if (tableAlias != null) {
            sb.append("alias=[").append(tableAlias.name).append("]\n");
        }

        sb.append("access=[").append(fullScan ? "FULL SCAN"
                                              : "INDEX PRED").append("]\n");
        sb.append("index=[");
        sb.append(index == null ? "NONE"
                                : index.getName() == null ? "UNNAMED"
                                                          : index.getName()
                                                          .name);
        sb.append(hidden ? "[HIDDEN]]\n"
                         : "]\n");

        temp = "INNER";

        if (isLeftJoin) {
            temp = "LEFT OUTER";

            if (isRightJoin) {
                temp = "FULL";
            }
        } else if (isRightJoin) {
            temp = "RIGHT OUTER";
        }

        sb.append("joinType=[").append(temp).append("]\n");

        temp = indexCondition == null ? "null"
                                      : indexCondition.describe(session);

        if (findFirstExpressions != null) {
            StringBuffer sbt = new StringBuffer();

            for (int i = 0; i < multiColumnCount; i++) {
                sbt.append(findFirstExpressions[i].describe(session));
            }

            temp = sbt.toString();
        }

        sb.append("eStart=[").append(temp).append("]\n");

        temp = indexEndCondition == null ? "null"
                                         : indexEndCondition.describe(session);

        sb.append("eEnd=[").append(temp).append("]\n");

        temp = nonIndexJoinCondition == null ? "null"
                                             : nonIndexJoinCondition.describe(
                                             session);

        sb.append("eAnd=[").append(temp).append("]");

        return sb.toString();
    }

    public RangeIteratorMain getIterator(Session session) {

        RangeIteratorMain it = new RangeIteratorMain(session, this);

        session.sessionContext.setRangeIterator(it);

        return it;
    }

    public RangeIteratorMain getFullIterator(Session session,
            RangeIteratorMain mainIterator) {

        RangeIteratorMain it = new FullRangeIterator(session, this,
            mainIterator);

        session.sessionContext.setRangeIterator(it);

        return it;
    }

    public static RangeIteratorMain getIterator(Session session,
            RangeVariable[] rangeVars) {

        if (rangeVars.length == 1) {
            return rangeVars[0].getIterator(session);
        }

        RangeIteratorMain[] iterators =
            new RangeIteratorMain[rangeVars.length];

        for (int i = 0; i < rangeVars.length; i++) {
            iterators[i] = rangeVars[i].getIterator(session);
        }

        return new JoinedRangeIterator(iterators);
    }

    public static class RangeIteratorBase implements RangeIterator {

        Session         session;
        int             rangePosition;
        RowIterator     it;
        PersistentStore store;
        Object[]        currentData;
        Row             currentRow;
        boolean         isBeforeFirst;

        RangeIteratorBase() {}

        public RangeIteratorBase(Session session, PersistentStore store,
                                 TableBase t, int position) {

            this.session       = session;
            this.rangePosition = position;
            this.store         = store;
            it                 = t.rowIterator(store);
            isBeforeFirst      = true;
        }

        @Override
        public boolean isBeforeFirst() {
            return isBeforeFirst;
        }

        @Override
        public boolean next() {

            if (isBeforeFirst) {
                isBeforeFirst = false;
            } else {
                if (it == null) {
                    return false;
                }
            }

            currentRow = it.getNextRow();

            if (currentRow == null) {
                return false;
            } else {
                currentData = currentRow.getData();

                return true;
            }
        }

        @Override
        public Row getCurrentRow() {
            return currentRow;
        }

        @Override
        public Object[] getCurrent() {
            return currentData;
        }

        @Override
        public long getRowid() {
            return currentRow == null ? 0
                                      : currentRow.getId();
        }

        @Override
        public Object getRowidObject() {
            return currentRow == null ? null
                                      : Long.valueOf(currentRow.getId());
        }

        @Override
        public void remove() {}

        @Override
        public void reset() {

            if (it != null) {
                it.release();
            }

            it            = null;
            currentRow    = null;
            isBeforeFirst = true;
        }

        @Override
        public int getRangePosition() {
            return rangePosition;
        }
    }

    public static class RangeIteratorMain extends RangeIteratorBase {

        boolean       hasOuterRow;
        boolean       isFullIterator;
        RangeVariable rangeVar;

        //
        Table           lookupTable;
        PersistentStore lookupStore;

        RangeIteratorMain() {
            super();
        }

        public RangeIteratorMain(Session session, RangeVariable rangeVar) {

            this.rangePosition = rangeVar.rangePosition;
            this.store = session.sessionData.getRowStore(rangeVar.rangeTable);
            this.session       = session;
            this.rangeVar      = rangeVar;
            isBeforeFirst      = true;

            if (rangeVar.isRightJoin) {
                lookupTable = TableUtil.newLookupTable(session.database);
                lookupStore = session.sessionData.getRowStore(lookupTable);
            }
        }

        @Override
        public boolean isBeforeFirst() {
            return isBeforeFirst;
        }

        @Override
        public boolean next() {

            if (isBeforeFirst) {
                isBeforeFirst = false;

                initialiseIterator();
            } else {
                if (it == null) {
                    return false;
                }
            }

            return findNext();
        }

        @Override
        public void remove() {}

        @Override
        public void reset() {

            if (it != null) {
                it.release();
            }

            it            = null;
            currentData   = rangeVar.emptyData;
            currentRow    = null;
            hasOuterRow   = false;
            isBeforeFirst = true;
        }

        @Override
        public int getRangePosition() {
            return rangeVar.rangePosition;
        }

        /**
         */
        protected void initialiseIterator() {

            hasOuterRow = rangeVar.isLeftJoin;

            if (rangeVar.isMultiFindFirst) {
                getFirstRowMulti();

                if (!rangeVar.isJoinIndex) {
                    hasOuterRow = false;
                }
            } else if (rangeVar.indexCondition == null) {
                if (rangeVar.indexEndCondition == null
                        || rangeVar.indexEndCondition.getType()
                           == OpTypes.IS_NULL) {
                    it = rangeVar.rangeIndex.firstRow(session, store);
                } else {
                    it = rangeVar.rangeIndex.findFirstRowNotNull(session,
                            store);
                }
            } else {

                // only NOT NULL
                if (rangeVar.indexCondition.getType() == OpTypes.NOT) {
                    it = rangeVar.rangeIndex.findFirstRowNotNull(session,
                            store);
                } else {
                    getFirstRow();
                }

                if (!rangeVar.isJoinIndex) {
                    hasOuterRow = false;
                }
            }
        }

        /**
         */
        private void getFirstRow() {

            Object value =
                rangeVar.indexCondition.getRightNode().getValue(session);
            Type valueType =
                rangeVar.indexCondition.getRightNode().getDataType();
            Type targetType =
                rangeVar.indexCondition.getLeftNode().getDataType();
            int exprType = rangeVar.indexCondition.getType();
            int range    = 0;

            if (targetType != valueType) {
                range = targetType.compareToTypeRange(value);
            }

            if (range == 0) {
                value = targetType.convertToType(session, value, valueType);
                it = rangeVar.rangeIndex.findFirstRow(session, store, value,
                                                      exprType);
            } else if (range < 0) {
                switch (exprType) {

                    case OpTypes.GREATER_EQUAL :
                    case OpTypes.GREATER :
                        it = rangeVar.rangeIndex.findFirstRowNotNull(session,
                                store);
                        break;

                    default :
                        it = rangeVar.rangeIndex.emptyIterator();
                }
            } else {
                switch (exprType) {

                    case OpTypes.SMALLER_EQUAL :
                    case OpTypes.SMALLER :
                        it = rangeVar.rangeIndex.findFirstRowNotNull(session,
                                store);
                        break;

                    default :
                        it = rangeVar.rangeIndex.emptyIterator();
                }
            }

            return;
        }

        /**
         * Uses multiple EQUAL expressions
         */
        private void getFirstRowMulti() {

            boolean convertible = true;
            Object[] currentJoinData =
                new Object[rangeVar.rangeIndex.getVisibleColumns()];

            for (int i = 0; i < rangeVar.multiColumnCount; i++) {
                Type valueType =
                    rangeVar.findFirstExpressions[i].getRightNode()
                        .getDataType();
                Type targetType =
                    rangeVar.findFirstExpressions[i].getLeftNode()
                        .getDataType();
                Object value =
                    rangeVar.findFirstExpressions[i].getRightNode().getValue(
                        session);

                if (targetType.compareToTypeRange(value) != 0) {
                    convertible = false;

                    break;
                }

                currentJoinData[i] = targetType.convertToType(session, value,
                        valueType);
            }

            it = convertible
                 ? rangeVar.rangeIndex.findFirstRow(session, store,
                     currentJoinData, rangeVar.multiColumnCount)
                 : rangeVar.rangeIndex.emptyIterator();
        }

        /**
         * Advances to the next available value. <p>
         *
         * @return true if a next value is available upon exit
         */
        protected boolean findNext() {

            boolean result = false;

            while (true) {
                currentRow = it.getNextRow();

                if (currentRow == null) {
                    break;
                }

                currentData = currentRow.getData();

                if (rangeVar.indexEndCondition != null
                        && !rangeVar.indexEndCondition.testCondition(
                            session)) {
                    if (!rangeVar.isJoinIndex) {
                        hasOuterRow = false;
                    }

                    break;
                }

                if (rangeVar.nonIndexJoinCondition != null
                        && !rangeVar.nonIndexJoinCondition.testCondition(
                            session)) {
                    continue;
                }

                if (rangeVar.nonIndexWhereCondition != null
                        && !rangeVar.nonIndexWhereCondition.testCondition(
                            session)) {
                    hasOuterRow = false;

                    continue;
                }

                addFoundRow();

                result = true;

                break;
            }

            if (result) {
                hasOuterRow = false;

                return true;
            }

            it.release();

            currentRow  = null;
            currentData = rangeVar.emptyData;

            if (hasOuterRow) {
                result = (rangeVar.nonIndexWhereCondition == null
                          || rangeVar.nonIndexWhereCondition.testCondition(
                              session));
            }

            hasOuterRow = false;

            return result;
        }

        protected void addFoundRow() {

            if (rangeVar.isRightJoin) {
                try {
                    lookupTable.insertData(
                        lookupStore,
                        new Object[]{ ValuePool.getInt(currentRow.getPos()) });
                } catch (HsqlException e) {}
            }
        }
    }

    public static class FullRangeIterator extends RangeIteratorMain {

        public FullRangeIterator(Session session, RangeVariable rangeVar,
                                 RangeIteratorMain rangeIterator) {

            this.rangePosition = rangeVar.rangePosition;
            this.store = session.sessionData.getRowStore(rangeVar.rangeTable);
            this.session       = session;
            this.rangeVar      = rangeVar;
            isBeforeFirst      = true;
            lookupTable        = rangeIterator.lookupTable;
            lookupStore        = rangeIterator.lookupStore;
            it                 = rangeVar.rangeIndex.firstRow(session, store);
        }

        @Override
        protected void initialiseIterator() {}

        @Override
        protected boolean findNext() {

            boolean result;

            while (true) {
                currentRow = it.getNextRow();

                if (currentRow == null) {
                    result = false;

                    break;
                }

                RowIterator lookupIterator =
                    lookupTable.indexList[0].findFirstRow(session,
                        lookupStore, ValuePool.getInt(currentRow.getPos()),
                        OpTypes.EQUAL);

                result = !lookupIterator.hasNext();

                lookupIterator.release();

                if (result) {
                    currentData = currentRow.getData();

                    if (rangeVar.nonIndexWhereCondition != null
                            && !rangeVar.nonIndexWhereCondition.testCondition(
                                session)) {
                        continue;
                    }

                    isBeforeFirst = false;

                    return true;
                }
            }

            it.release();

            currentRow  = null;
            currentData = rangeVar.emptyData;

            return result;
        }
    }

    public static class JoinedRangeIterator extends RangeIteratorMain {

        RangeIteratorMain[] rangeIterators;
        int                 currentIndex = 0;

        public JoinedRangeIterator(RangeIteratorMain[] rangeIterators) {
            this.rangeIterators = rangeIterators;
        }

        @Override
        public boolean isBeforeFirst() {
            return isBeforeFirst;
        }

        @Override
        public boolean next() {

            while (currentIndex >= 0) {
                RangeIteratorMain it = rangeIterators[currentIndex];

                if (it.next()) {
                    if (currentIndex < rangeIterators.length - 1) {
                        currentIndex++;

                        continue;
                    }

                    currentRow  = rangeIterators[currentIndex].currentRow;
                    currentData = currentRow.getData();

                    return true;
                } else {
                    it.reset();

                    currentIndex--;

                    continue;
                }
            }

            currentData =
                rangeIterators[rangeIterators.length - 1].rangeVar.emptyData;
            currentRow = null;

            for (int i = 0; i < rangeIterators.length; i++) {
                rangeIterators[i].reset();
            }

            return false;
        }

        @Override
        public void reset() {}
    }

    /************************* Volt DB Extensions *************************/

    /**
     * VoltDB added method to get a non-catalog-dependent
     * representation of this HSQLDB object.
     * @param session The current Session object may be needed to resolve
     * some names.
     * @return XML, correctly indented, representing this object.
     * @throws HSQLParseException
     */
    VoltXMLElement voltGetRangeVariableXML(Session session)
    throws org.hsqldb_voltpatches.HSQLInterface.HSQLParseException
    {
        Index        index;
        Index        primaryIndex;

        index        = rangeIndex;
        primaryIndex = rangeTable.getPrimaryIndex();

        // get the index for this scan (/filter)
        // note: ignored if scan if full table scan
        if (index == null)
            index = primaryIndex;

        // output open tag
        VoltXMLElement scan = new VoltXMLElement("tablescan");

        if (rangeTable.tableType == TableBase.SYSTEM_SUBQUERY) {
            if (rangeTable instanceof TableDerived) {
                if (tableAlias == null || tableAlias.name == null) {
                    // VoltDB require derived sub select table with user specified alias
                    throw new org.hsqldb_voltpatches.HSQLInterface.HSQLParseException(
                            "SQL Syntax error: Every derived table must have its own alias.");
                }
                scan.attributes.put("table", tableAlias.name.toUpperCase());

                VoltXMLElement subQuery = ((TableDerived) rangeTable).dataExpression.voltGetXML(session);
                scan.children.add(subQuery);
            }
        } else {
            scan.attributes.put("table", rangeTable.getName().name.toUpperCase());
        }

        if (tableAlias != null && !rangeTable.getName().name.equals(tableAlias)) {
            scan.attributes.put("tablealias", tableAlias.name.toUpperCase());
        }

        // note if this is an outer join
        if (isLeftJoin && isRightJoin) {
            scan.attributes.put("jointype", "full");
        } else if (isLeftJoin) {
            scan.attributes.put("jointype", "left");
        } else if (isRightJoin) {
            scan.attributes.put("jointype", "right");
        } else {
            scan.attributes.put("jointype", "inner");
        }

        Expression joinCond = null;
        Expression whereCond = null;
        // if isJoinIndex and indexCondition are set then indexCondition is join condition
        // else if indexCondition is set then it is where condition
        if (isJoinIndex == true) {
            joinCond = indexCondition;
            if (indexEndCondition != null) {
            	joinCond = makeConjunction(joinCond, indexEndCondition);
            }
            // then go to the nonIndexJoinCondition
            if (nonIndexJoinCondition != null) {
            	joinCond = makeConjunction(joinCond, nonIndexJoinCondition);
            }
            // then go to the nonIndexWhereCondition
            whereCond = nonIndexWhereCondition;
        } else {
            joinCond = nonIndexJoinCondition;

            whereCond = indexCondition;
            if (indexEndCondition != null) {
            	whereCond = makeConjunction(whereCond, indexEndCondition);
            }
            // then go to the nonIndexWhereCondition
            if (nonIndexWhereCondition != null) {
            	whereCond = makeConjunction(whereCond, nonIndexWhereCondition);
            }

        }
        if (joinCond != null) {
            joinCond = joinCond.eliminateDuplicates(session);
            VoltXMLElement joinCondEl = new VoltXMLElement("joincond");
            joinCondEl.children.add(joinCond.voltGetXML(session));
            scan.children.add(joinCondEl);
        }

        if (whereCond != null) {
            whereCond = whereCond.eliminateDuplicates(session);
            VoltXMLElement whereCondEl = new VoltXMLElement("wherecond");
            whereCondEl.children.add(whereCond.voltGetXML(session));
            scan.children.add(whereCondEl);
        }

        return scan;
    }

    // Added by LX
    /*
     * Help function to dig into conditions of the query
     * used by voltGetGraphRangeVariableXML
     */
    void updatescan(VoltXMLElement cond, VoltXMLElement scan) {
        
        for (VoltXMLElement c: cond.children) {
            if (c.attributes.get("optype") == "and")
                updatescan(c, scan);
            else if (c.attributes.get("optype") == "equal") {
                int i = 0;
                for (VoltXMLElement ccc: c.children) {
                    if (ccc.attributes.containsKey("column") && 
                        ccc.attributes.get("column").equals("STARTVERTEXID")) {
                        VoltXMLElement value = c.children.get(i+1);
                        scan.attributes.put("startvertexid", value.attributes.get("value"));
                    }
                    else if (ccc.attributes.containsKey("column") && 
                             ccc.attributes.get("column").equals("ENDVERTEXID")) {
                        VoltXMLElement value = c.children.get(i+1);
                        scan.attributes.put("endvertexid", value.attributes.get("value"));
                    }
                    else if (ccc.attributes.containsKey("column") && 
                             ccc.attributes.get("column").equals(c_PROP1)) {
                        VoltXMLElement value = c.children.get(i+1);
                        scan.attributes.put("prop1", value.attributes.get("value"));
                    }
                    else if (ccc.attributes.containsKey("column") && 
                             ccc.attributes.get("column").equals(c_PROP2)) {
                        VoltXMLElement value = c.children.get(i+1);
                        scan.attributes.put("prop2", value.attributes.get("value"));
                    }
                    else if (ccc.attributes.containsKey("column") && 
                             ccc.attributes.get("column").equals(c_PROP3)) {
                        VoltXMLElement value = c.children.get(i+1);
                        scan.attributes.put("prop3", value.attributes.get("value"));
                    }
                    else if (ccc.attributes.containsKey("column") && 
                             ccc.attributes.get("column").equals(c_PROP4)) {
                        VoltXMLElement value = c.children.get(i+1);
                        scan.attributes.put("prop4", value.attributes.get("value"));
                    }
                    else if (ccc.attributes.containsKey("column") && 
                             ccc.attributes.get("column").equals(c_PROP5)) {
                        VoltXMLElement value = c.children.get(i+1);
                        scan.attributes.put("prop5", value.attributes.get("value"));
                    }
                    else if (ccc.attributes.containsKey("column") && 
                             ccc.attributes.get("column").equals(c_LENGTH)) {
                        VoltXMLElement value = c.children.get(i+1);
                        scan.attributes.put("length", value.attributes.get("value"));
                    }
                    i++;
                }
            }
        }
    }    
    // End LX

    // LX FEAT4
    VoltXMLElement voltGetGraphG2GVariableXML(Session session) throws org.hsqldb_voltpatches.HSQLInterface.HSQLParseException 
    {
        Index        index;
        Index        primaryIndex;

        index        = rangeIndex;
        primaryIndex = null;//rangeTable.getPrimaryIndex();

        // get the index for this scan (/filter)
        // note: ignored if scan is full table scan
        if (index == null)
            index = primaryIndex;

        // output open tag
        VoltXMLElement scan;
        scan = new VoltXMLElement("graphscan");

        scan.attributes.put("newgraph", newGraphName);
        scan.attributes.put("newv", newGraphVertex);
        scan.attributes.put("newe", newGraphEdge);
        scan.attributes.put("oldgraph", rangeGraph.getName().name.toUpperCase());
        if(chosenVertexLabel != null)
            scan.attributes.put("chosenvertexlabel", chosenVertexLabel);
        if(chosenEdgeLabel != null)
            scan.attributes.put("chosenedgelabel", chosenEdgeLabel);
        
        if (isVertexes) {
            scan.attributes.put("hasvertex", "true");
        }
        if (isEdges) {
            scan.attributes.put("hasedge", "true");
        }
               
        if (vertexTableAlias != null)
          scan.attributes.put("vertextablealias", vertexTableAlias.name);
        if (edgeTableAlias != null)
          scan.attributes.put("edgetablealias", edgeTableAlias.name);

        if (tableAlias != null && !rangeGraph.getName().name.equals(tableAlias)) {
            scan.attributes.put("tablealias", tableAlias.name.toUpperCase());
        }

        // note if this is an outer join
        if (isLeftJoin && isRightJoin) {
            scan.attributes.put("jointype", "full");
        } else if (isLeftJoin) {
            scan.attributes.put("jointype", "left");
        } else if (isRightJoin) {
            scan.attributes.put("jointype", "right");
        } else {
            scan.attributes.put("jointype", "inner");
        }

        Expression joinCond = null;
        Expression whereCond = null;
        // if isJoinIndex and indexCondition are set then indexCondition is join condition
        // else if indexCondition is set then it is where condition
        if (isJoinIndex == true) {
            joinCond = indexCondition;
            if (indexEndCondition != null) {
                joinCond = makeConjunction(joinCond, indexEndCondition);
            }
            // then go to the nonIndexJoinCondition
            if (nonIndexJoinCondition != null) {
                joinCond = makeConjunction(joinCond, nonIndexJoinCondition);
            }
            // then go to the nonIndexWhereCondition
            whereCond = nonIndexWhereCondition;
        } else {
            joinCond = nonIndexJoinCondition;

            whereCond = indexCondition;
            if (indexEndCondition != null) {
                whereCond = makeConjunction(whereCond, indexEndCondition);
            }
            // then go to the nonIndexWhereCondition
            if (nonIndexWhereCondition != null) {
                whereCond = makeConjunction(whereCond, nonIndexWhereCondition);
            }

        }
        
        if (joinCond != null) {
            joinCond = joinCond.eliminateDuplicates(session);
            VoltXMLElement joinCondEl = new VoltXMLElement("joincond");
            
            VoltXMLElement cond = joinCond.voltGetXML(session);
            
            updatescan(cond, scan);
            
            joinCondEl.children.add(joinCond.voltGetXML(session));
            scan.children.add(joinCondEl);
        }

        if (whereCond != null) {
            whereCond = whereCond.eliminateDuplicates(session);
            VoltXMLElement whereCondEl = new VoltXMLElement("wherecond");
            whereCondEl.children.add(whereCond.voltGetXML(session));
            scan.children.add(whereCondEl);
        }
        
        if (hint != null) scan.attributes.put("hint", hint);        
        
        return scan;
    }

    // Added by LX
    /**
     * VoltDB added method to get a non-catalog-dependent
     * representation of this HSQLDB object.
     * @param session The current Session object may be needed to resolve
     * some names.
     * @return XML, correctly indented, representing this object.
     * @throws HSQLParseException
     */
    VoltXMLElement voltGetGraphRangeVariableXML(Session session, String label) throws org.hsqldb_voltpatches.HSQLInterface.HSQLParseException // modified by LX FEAT2
    {
        Index        index;
        Index        primaryIndex;

        index        = rangeIndex;
        primaryIndex = null;//rangeTable.getPrimaryIndex();

        // get the index for this scan (/filter)
        // note: ignored if scan is full table scan
        if (index == null)
            index = primaryIndex;

        // output open tag
        VoltXMLElement scan;
        if (isVertexes) {
            scan = new VoltXMLElement("vertexscan");
            // LX FEAT2
            scan.attributes.put("vlabel", label);
        }
        else if (isEdges){
            scan = new VoltXMLElement("edgescan");
            // LX FEAT3
            scan.attributes.put("elabel", label);
        }
        else if (isPaths)
            scan = new VoltXMLElement("pathscan");
        else scan = new VoltXMLElement("graphscan");

        scan.attributes.put("graph", rangeGraph.getName().name.toUpperCase());

        if (tableAlias != null && !rangeGraph.getName().name.equals(tableAlias)) {
            scan.attributes.put("tablealias", tableAlias.name.toUpperCase());
        }

        // note if this is an outer join
        if (isLeftJoin && isRightJoin) {
            scan.attributes.put("jointype", "full");
        } else if (isLeftJoin) {
            scan.attributes.put("jointype", "left");
        } else if (isRightJoin) {
            scan.attributes.put("jointype", "right");
        } else {
            scan.attributes.put("jointype", "inner");
        }

        Expression joinCond = null;
        Expression whereCond = null;
        // if isJoinIndex and indexCondition are set then indexCondition is join condition
        // else if indexCondition is set then it is where condition
        if (isJoinIndex == true) {
            joinCond = indexCondition;
            if (indexEndCondition != null) {
                joinCond = makeConjunction(joinCond, indexEndCondition);
            }
            // then go to the nonIndexJoinCondition
            if (nonIndexJoinCondition != null) {
                joinCond = makeConjunction(joinCond, nonIndexJoinCondition);
            }
            // then go to the nonIndexWhereCondition
            whereCond = nonIndexWhereCondition;
        } else {
            joinCond = nonIndexJoinCondition;

            whereCond = indexCondition;
            if (indexEndCondition != null) {
                whereCond = makeConjunction(whereCond, indexEndCondition);
            }
            // then go to the nonIndexWhereCondition
            if (nonIndexWhereCondition != null) {
                whereCond = makeConjunction(whereCond, nonIndexWhereCondition);
            }

        }
        
        if (joinCond != null) {
            joinCond = joinCond.eliminateDuplicates(session);
            VoltXMLElement joinCondEl = new VoltXMLElement("joincond");
            
            VoltXMLElement cond = joinCond.voltGetXML(session);
            
            updatescan(cond, scan);
            /*
            for (VoltXMLElement c: cond.children) {
                //if (c.attributes.get("optype") == "and")
                    //for (VoltXMLElement cc: c.children) {
                        //boolean flag = true;
                        if (c.attributes.get("optype") == "equal") {
                            int i = 0;
                            for (VoltXMLElement ccc: c.children) {
                                if (ccc.attributes.get("column") == "STARTVERTEXID") {
                                    VoltXMLElement value = c.children.get(i+1);
                                    scan.attributes.put("startvertexid", value.attributes.get("value"));
                                    //flag = false;
                                }
                                else if (ccc.attributes.get("column") == "ENDVERTEXID") {
                                    VoltXMLElement value = c.children.get(i+1);
                                    scan.attributes.put("endvertexid", value.attributes.get("value"));
                                    //flag = false;
                                }
                                i++;
                            }
                        }
                        //if (flag) joinCondEl.children.add(c);
                    //}
            }
            */
            joinCondEl.children.add(joinCond.voltGetXML(session));
            scan.children.add(joinCondEl);
        }

        if (whereCond != null) {
            whereCond = whereCond.eliminateDuplicates(session);
            VoltXMLElement whereCondEl = new VoltXMLElement("wherecond");
            whereCondEl.children.add(whereCond.voltGetXML(session));
            scan.children.add(whereCondEl);
        }
        
        if (hint != null) scan.attributes.put("hint", hint);        
        
        return scan;
    }    
    // End LX
    // Not in voltdb 6.7 LX
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(columnAliasNames);
        result = prime * result + Arrays.hashCode(columnsInGroupBy);
        result = prime * result + Arrays.hashCode(emptyData);
        result = prime * result + (hasKeyedColumnInGroupBy ? 1231 : 1237);
        result = prime * result + (isJoinIndex ? 1231 : 1237);
        result = prime * result + (isLeftJoin ? 1231 : 1237);
        result = prime * result + (isMultiFindFirst ? 1231 : 1237);
        result = prime * result + (isRightJoin ? 1231 : 1237);
        result = prime * result + (isVariable ? 1231 : 1237);
        result = prime * result + level;
        result = prime * result + multiColumnCount;
        result = prime * result + ((namedJoinColumns == null) ? 0
                : namedJoinColumns.hashCode());
        result = prime * result
                + ((rangeIndex == null) ? 0 : rangeIndex.hashCode());
        result = prime * result + rangePosition;
        result = prime * result
                + ((rangeTable == null) ? 0 : rangeTable.hashCode());
        result = prime * result
                + ((tableAlias == null) ? 0 : tableAlias.hashCode());
        result = prime * result + Arrays.hashCode(updatedColumns);
        result = prime * result + Arrays.hashCode(usedColumns);
        result = prime * result
                + ((variables == null) ? 0 : variables.hashCode());
        return result;
    }

    // Not in voltdb 6.7 LX
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        RangeVariable other = (RangeVariable) obj;
        if (!Arrays.equals(columnAliasNames, other.columnAliasNames)) {
            return false;
        }
        if (!Arrays.equals(columnsInGroupBy, other.columnsInGroupBy)) {
            return false;
        }
        if (!Arrays.equals(emptyData, other.emptyData)) {
            return false;
        }
        if (hasKeyedColumnInGroupBy != other.hasKeyedColumnInGroupBy) {
            return false;
        }
        if (isJoinIndex != other.isJoinIndex) {
            return false;
        }
        if (isLeftJoin != other.isLeftJoin) {
            return false;
        }
        if (isMultiFindFirst != other.isMultiFindFirst) {
            return false;
        }
        if (isRightJoin != other.isRightJoin) {
            return false;
        }
        if (isVariable != other.isVariable) {
            return false;
        }
        if (level != other.level) {
            return false;
        }
        if (multiColumnCount != other.multiColumnCount) {
            return false;
        }
        if (namedJoinColumns == null) {
            if (other.namedJoinColumns != null) {
                return false;
            }
        } else if (!namedJoinColumns.equals(other.namedJoinColumns)) {
            return false;
        }
        if (rangeIndex == null) {
            if (other.rangeIndex != null) {
                return false;
            }
        } else if (!rangeIndex.equals(other.rangeIndex)) {
            return false;
        }
        if (rangePosition != other.rangePosition) {
            return false;
        }
        if (rangeTable == null) {
            if (other.rangeTable != null) {
                return false;
            }
        } else if (!rangeTable.equals(other.rangeTable)) {
            return false;
        }
        if (tableAlias == null) {
            if (other.tableAlias != null) {
                return false;
            }
        } else if (!tableAlias.equals(other.tableAlias)) {
            return false;
        }
        if (!Arrays.equals(updatedColumns, other.updatedColumns)) {
            return false;
        }
        if (!Arrays.equals(usedColumns, other.usedColumns)) {
            return false;
        }
        if (variables == null) {
            if (other.variables != null) {
                return false;
            }
        } else if (!variables.equals(other.variables)) {
            return false;
        }
        return true;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        String name = "";
        if (rangeTable != null) {
            name = ":" + rangeTable.getName().name;
        }
        return super.toString() + name;
    }
    /**********************************************************************/
}
