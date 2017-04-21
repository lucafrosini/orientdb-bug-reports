/**
 * 
 */
package org.gcube.orientdb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.orientechnologies.orient.client.remote.OServerAdmin;
import com.orientechnologies.orient.client.remote.OStorageRemote;
import com.orientechnologies.orient.core.metadata.OMetadata;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientEdgeType;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;
import com.tinkerpop.blueprints.impls.orient.OrientVertexType;

/**
 * @author Luca Frosini (ISTI - CNR)
 * This class reproduce orient issue 7354 - https://github.com/orientechnologies/orientdb/issues/7354
 */
public class ReproduceBug7354 {
	
	private static final String HOST = "remote:node01.acme.org;node02.acme.org;node03.acme.org";
	private static final String ROOT_USERNAME = "root";
	private static final String ROOT_PASSWORD = "ROOT_PWD";
	
	private static final String ADMIN_USERNAME = "admin";
	private static final String ADMIN_PASSWORD = "admin";

	private static final String DB_NAME = "mydb";
	
	private static final String DATABASE_TYPE = "graph";
	private static final String STORAGE_MODE = "plocal";

	public static final String MEMORY_FACET = "MemoryFacet";
	
	public UUID createDBTypesAndContext() throws IOException{
		
		System.out.println("Going to create DB " + DB_NAME);
		
		OServerAdmin serverAdmin = new OServerAdmin(HOST)
		.connect(ROOT_USERNAME,ROOT_PASSWORD);
	
		serverAdmin.createDatabase(DB_NAME, DATABASE_TYPE,
				STORAGE_MODE);
		
		OrientGraphFactory factory = new OrientGraphFactory(HOST + "/" + DB_NAME,
				ADMIN_USERNAME, ADMIN_PASSWORD).setupPool(1, 10);
		
		OrientGraphNoTx orientGraphNoTx = factory.getNoTx();
		
		OMetadata oMetadata = orientGraphNoTx.getRawGraph().getMetadata();
		
		OSchema oSchema = oMetadata.getSchema();
		OClass oRestricted = oSchema.getClass("ORestricted");

		OrientVertexType v = orientGraphNoTx.getVertexBaseType();
		v.addSuperClass(oRestricted);

		OrientEdgeType e = orientGraphNoTx.getEdgeBaseType();
		e.addSuperClass(oRestricted);

		orientGraphNoTx.createVertexType(MEMORY_FACET);
		
		orientGraphNoTx.shutdown();
		
		System.out.println("DB " + DB_NAME + " has been created. Going to create security Context");
		
		OrientGraph orientGraph = factory.getTx();
		
		UUID contextUUID = UUID.randomUUID();
		// Create Reader and Writers Roles and Users for Context identified by provided UUID
		SecurityContext.createSecurityContext(orientGraph, contextUUID); 
		orientGraph.commit();
		orientGraph.shutdown();
		
		return contextUUID;
	}
	
	public OrientGraphFactory getFactory(UUID contextUUID){
		
		String username = SecurityContext.getSecurityRoleOrUserName(
				SecurityContext.PermissionMode.WRITER,
				SecurityContext.SecurityType.USER, contextUUID);
		
		String password = SecurityContext.WRITER_PASSWORD;
		
		OrientGraphFactory factory = new OrientGraphFactory(HOST + "/" + DB_NAME,
				username, password).setupPool(1, 10);
		factory.setConnectionStrategy(OStorageRemote.CONNECTION_STRATEGY
				.ROUND_ROBIN_CONNECT.toString());
		
		return factory;
	}
	
	public void createVertex(OrientGraphFactory factory, UUID contextUUID){
		
		System.out.println("Going to create " + MEMORY_FACET + " instance");
		
		OrientGraph orientGraph = factory.getTx();
		
		OrientVertex memory = orientGraph.addVertex("class:" + MEMORY_FACET);
		SecurityContext.addToSecurityContext(orientGraph, memory, contextUUID);
		
		List<ODocument> list = new ArrayList<>();
		ODocument oDocument1 = new ODocument();
		oDocument1 = oDocument1.fromJSON("{\"key1\":\"Value1\"}");
		list.add(oDocument1);
		
		ODocument oDocument2 = new ODocument();
		oDocument2 = oDocument2.fromJSON("{\"key2\":\"Value2\"}");
		list.add(oDocument2);
		memory.setProperty("test", list, OType.EMBEDDEDLIST);
		memory.save();
		
		orientGraph.commit();
		orientGraph.shutdown();
	}
	
	
	public void updateVertex(OrientGraphFactory factory, UUID contextUUID){
		System.out.println("Going to update " + MEMORY_FACET + " instance.");
		
		OrientGraph orientGraph = factory.getTx();
		Iterable<Vertex> vertexes = orientGraph.getVerticesOfClass(MEMORY_FACET);
		
		Vertex v = vertexes.iterator().next();
		
		List<ODocument> list2 = new ArrayList<>();
		ODocument oDocument3 = new ODocument();
		oDocument3 = oDocument3.fromJSON("{\"key3\":\"Value3\"}");
		list2.add(oDocument3);
		
		ODocument oDocument4 = new ODocument();
		oDocument4 = oDocument4.fromJSON("{\"key4\":\"Value4\"}");
		list2.add(oDocument4);
		((OrientVertex) v).setProperty("test", list2, OType.EMBEDDEDLIST);
		
		orientGraph.commit();
		
		System.out.println("Changes to " + MEMORY_FACET + " committed.");
		
		orientGraph.shutdown();
		
	}

	public void execute() throws IOException {
		UUID contextUUID =  createDBTypesAndContext();
		OrientGraphFactory factory = getFactory(contextUUID);
		createVertex(factory, contextUUID);
		updateVertex(factory, contextUUID);
		System.out.println("DONE");
	}
	
	public static void main(String args[]) throws Exception {
		ReproduceBug7354 reproduceBug7354 = new ReproduceBug7354();
		reproduceBug7354.execute();
	}
	
}
