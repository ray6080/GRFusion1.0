#ifndef GRAPHVIEWFACTORY_H
#define GRAPHVIEWFACTORY_H

//#include "GraphView.h"
#include "Edge.h"
#include "Vertex.h"
#include <string>
#include <vector>
using namespace std;

namespace catalog {
class GraphView;
}

namespace voltdb {

class GraphView;
class TupleSchema;

class GraphViewFactory
{
public:
	GraphViewFactory(void);
	~GraphViewFactory(void);

	static GraphView* createGraphView(string graphViewName, bool isDirected);
	// static GraphView* createGraphView(const std::string &graphViewName, const bool isDirected,
	// 		  Table* vTable, Table* eTable, Table* pTable, TupleSchema* vSchema, TupleSchema* eSchema,
	// 		  vector<std::string> vertexColumnNames, vector<std::string> edgeColumnNames,
	// 		  vector<int> columnIdsInVertexTable, vector<int> columnIdsInEdgeTable,
	//           voltdb::CatalogId databaseId, char *signature);
	static GraphView* createGraphView(const std::string &graphViewName, const bool isDirected, vector<std::string> vLabels,
			  vector<Table*> vTables, vector<std::string> eLabels, vector<Table*> eTables, 
			  vector<std::string> startVLabels, vector<std::string> endVLabels, Table* pTable, 
			  TupleSchema* vSchema, TupleSchema* eSchema,
			  vector<std::string> vertexColumnNames, vector<std::string> edgeColumnNames,
			  vector<int> columnIdsInVertexTable, vector<int> columnIdsInEdgeTable,
	          voltdb::CatalogId databaseId, char *signature); //LX FEAT2

	static GraphView* createSubGraphView(const std::string &graphViewName, const bool isDirected, string filterHint, bool postfilter, vector<std::string> vLabels,
			  vector<Table*> vTables, vector<std::string> eLabels, vector<Table*> eTables, 
			  vector<std::string> startVLabels, vector<std::string> endVLabels, Table* pTable, 
			  TupleSchema* vSchema, TupleSchema* eSchema,
			  vector<std::string> vertexColumnNames, vector<std::string> edgeColumnNames,
			  vector<int> columnIdsInVertexTable, vector<int> columnIdsInEdgeTable,
	          voltdb::CatalogId databaseId, char *signature, const string& subGraphVPredicate, const string& subGraphEPredicate, int inputGraphSize, std::string joinVEPredicate, GraphView* oldGraphName, std::string vlabelName, std::string elabelName, bool isV); 

	static void loadGraph(GraphView* vw, vector<Vertex* > vertexes, vector<Edge* > edges);
	static void printGraphView(GraphView* gview);
};

}

#endif
