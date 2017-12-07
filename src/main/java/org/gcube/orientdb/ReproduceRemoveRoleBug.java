package org.gcube.orientdb;

import java.io.IOException;

import com.orientechnologies.orient.client.remote.OServerAdmin;
import com.orientechnologies.orient.client.remote.OStorageRemote;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.OMetadata;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.metadata.security.OSecurity;
import com.orientechnologies.orient.core.metadata.security.OSecurityRole.ALLOW_MODES;
import com.orientechnologies.orient.core.metadata.security.OUser;
import com.tinkerpop.blueprints.impls.orient.OrientEdgeType;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;
import com.tinkerpop.blueprints.impls.orient.OrientVertexType;

public class ReproduceRemoveRoleBug {
	
	private static final String HOST = "remote:node01.acme.org";
	private static final String ROOT_USERNAME = "root";
	private static final String ROOT_PASSWORD = "ROOT_PWD";
	
	private static final String ADMIN_USERNAME = "admin";
	private static final String ADMIN_PASSWORD = "admin";
	
	private static final String DB_NAME = "mydb";
	
	private static final String DATABASE_TYPE = "graph";
	private static final String STORAGE_MODE = "plocal";
	
	private static final String DEFAULT_WRITER_ROLE = "writer";
	
	private static final String USERNAME = "MyUser";
	private static final String PASSWORD = "changeMe";
	
	private static final String[] ROLES = new String[] {"Role1", "Role2", "Role3"};
	
	private void createDB() throws IOException {
		System.out.println("Going to create DB " + DB_NAME);
		OServerAdmin serverAdmin = new OServerAdmin(HOST).connect(ROOT_USERNAME, ROOT_PASSWORD);
		serverAdmin.createDatabase(DB_NAME, DATABASE_TYPE, STORAGE_MODE);
		
		OrientGraphFactory factory = new OrientGraphFactory(HOST + "/" + DB_NAME, ADMIN_USERNAME, ADMIN_PASSWORD)
				.setupPool(1, 10);
		
		OrientGraphNoTx orientGraphNoTx = factory.getNoTx();
		OMetadata oMetadata = orientGraphNoTx.getRawGraph().getMetadata();
		
		OSchema oSchema = oMetadata.getSchema();
		OClass oRestricted = oSchema.getClass("ORestricted");
		
		OrientVertexType v = orientGraphNoTx.getVertexBaseType();
		v.addSuperClass(oRestricted);
		
		OrientEdgeType e = orientGraphNoTx.getEdgeBaseType();
		e.addSuperClass(oRestricted);
		
		orientGraphNoTx.shutdown();
		
		System.out.println("DB " + DB_NAME + " has been created.");
		
	}
	
	private void createMyUser() {
		
		OrientGraphFactory factory = new OrientGraphFactory(HOST + "/" + DB_NAME, ADMIN_USERNAME, ADMIN_PASSWORD).setupPool(1, 10);
		factory.setConnectionStrategy(OStorageRemote.CONNECTION_STRATEGY.ROUND_ROBIN_CONNECT.toString());
		
		OrientGraph orientGraph = factory.getTx();
		ODatabaseDocumentTx oDatabaseDocumentTx = orientGraph.getRawGraph();
		OSecurity oSecurity = oDatabaseDocumentTx.getMetadata().getSecurity();
		
		ORole writerRole = oSecurity.getRole(DEFAULT_WRITER_ROLE);
		
		OUser myUser = oSecurity.createUser(USERNAME, PASSWORD, writerRole);
		myUser.save();
		
		orientGraph.commit();
		orientGraph.shutdown();
		
		System.out.println("User " + USERNAME + " has been created.");
	}
	
	private void addRoles() {
		OrientGraphFactory factory = new OrientGraphFactory(HOST + "/" + DB_NAME, ADMIN_USERNAME, ADMIN_PASSWORD).setupPool(1, 10);
		factory.setConnectionStrategy(OStorageRemote.CONNECTION_STRATEGY.ROUND_ROBIN_CONNECT.toString());
		
		OrientGraph orientGraph = factory.getTx();
		ODatabaseDocumentTx oDatabaseDocumentTx = orientGraph.getRawGraph();
		OSecurity oSecurity = oDatabaseDocumentTx.getMetadata().getSecurity();
		
		OUser myUser = oSecurity.getUser(USERNAME);
		
		ORole writerRole = oSecurity.getRole(DEFAULT_WRITER_ROLE);
		
		for(String role : ROLES) {
			
			ORole roleToAdd = oSecurity.createRole(role, writerRole, ALLOW_MODES.DENY_ALL_BUT);
			roleToAdd.save();
			
			myUser.addRole(roleToAdd);
			myUser.save();
		}
		
		orientGraph.commit();
		orientGraph.shutdown();
		
		System.out.println("Roles created and added to " + USERNAME);
	}
	
	private void removeRoles() {
		OrientGraphFactory factory = new OrientGraphFactory(HOST + "/" + DB_NAME, ADMIN_USERNAME, ADMIN_PASSWORD).setupPool(1, 10);
		factory.setConnectionStrategy(OStorageRemote.CONNECTION_STRATEGY.ROUND_ROBIN_CONNECT.toString());
		
		OrientGraph orientGraph = factory.getTx();
		ODatabaseDocumentTx oDatabaseDocumentTx = orientGraph.getRawGraph();
		OSecurity oSecurity = oDatabaseDocumentTx.getMetadata().getSecurity();
		
		OUser myUser = oSecurity.getUser(USERNAME);
	
		for(String role : ROLES) {
			myUser.removeRole(role);
			myUser.save();
		}
		
		orientGraph.commit();
		orientGraph.shutdown();
		
		System.out.println("Roles removed from " + USERNAME);
		
	}
	
	public static void main(String args[]) throws Exception {
		ReproduceRemoveRoleBug reproduceBugRemoveRole = new ReproduceRemoveRoleBug();
		reproduceBugRemoveRole.createDB();
		reproduceBugRemoveRole.createMyUser();
		reproduceBugRemoveRole.addRoles();
		reproduceBugRemoveRole.removeRoles();

	}
	
}
