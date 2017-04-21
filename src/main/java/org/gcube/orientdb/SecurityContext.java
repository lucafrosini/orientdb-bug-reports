/**
 * 
 */
package org.gcube.orientdb;

import java.util.Iterator;
import java.util.UUID;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.security.ORestrictedOperation;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.metadata.security.OSecurity;
import com.orientechnologies.orient.core.metadata.security.OSecurityRole.ALLOW_MODES;
import com.orientechnologies.orient.core.metadata.security.OUser;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph;
import com.tinkerpop.blueprints.impls.orient.OrientEdge;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

/**
 * @author Luca Frosini (ISTI - CNR)
 * 
 */
public class SecurityContext {

	public static final String DEFAULT_WRITER_ROLE = "writer";
	public static final String DEFAULT_READER_ROLE = "reader";

	public static final String READER_PASSSWORD = "reader";
	public static final String WRITER_PASSWORD = "writer";

	public static void addToSecurityContext(OrientGraph orientGraph, Edge edge,
			UUID context) {
		OSecurity oSecurity = orientGraph.getRawGraph().getMetadata()
				.getSecurity();
		OrientEdge orientEdge = (OrientEdge) edge;
		SecurityContext.allowSecurityContextRoles(oSecurity,
				orientEdge.getRecord(), context);
	}
	
	public static void addToSecurityContext(OrientGraph orientGraph,
			Vertex vertex, UUID context) {
		OSecurity oSecurity = orientGraph.getRawGraph().getMetadata()
				.getSecurity();
		OrientVertex orientVertex = (OrientVertex) vertex;

		SecurityContext.allowSecurityContextRoles(oSecurity,
				orientVertex.getRecord(), context);
		orientVertex.save();

		Iterable<Edge> iterable = vertex.getEdges(Direction.BOTH);
		Iterator<Edge> iterator = iterable.iterator();
		while (iterator.hasNext()) {
			OrientEdge edge = (OrientEdge) iterator.next();
			SecurityContext.allowSecurityContextRoles(oSecurity,
					edge.getRecord(), context);
			edge.save();
		}
	}
	
	private static void allowSecurityContextRoles(OSecurity oSecurity,
			ODocument oDocument, UUID context) {
		oSecurity.allowRole(
				oDocument,
				ORestrictedOperation.ALLOW_ALL,
				getSecurityRoleOrUserName(PermissionMode.WRITER,
						SecurityType.ROLE, context));

		oSecurity.allowRole(
				oDocument,
				ORestrictedOperation.ALLOW_READ,
				getSecurityRoleOrUserName(PermissionMode.READER,
						SecurityType.ROLE, context));

		oDocument.save();
	}

	
	public static String getSecurityRoleOrUserName(
			PermissionMode permissionMode, SecurityType securityType,
			UUID context) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(permissionMode);
		stringBuilder.append(securityType);
		stringBuilder.append("_");
		stringBuilder.append(context.toString());
		return stringBuilder.toString();
	}

	public enum SecurityType {
		ROLE("Role"), USER("User");

		private final String name;

		private SecurityType(String name) {
			this.name = name;
		}

		public String toString() {
			return name;
		}
	}

	public enum PermissionMode {
		READER("Reader"), WRITER("Writer");

		private final String name;

		private PermissionMode(String name) {
			this.name = name;
		}

		public String toString() {
			return name;
		}
	}



	

	public static void createSecurityContext(OrientBaseGraph orientBaseGraph,
			UUID context) {

		ODatabaseDocumentTx oDatabaseDocumentTx = orientBaseGraph.getRawGraph();
		OSecurity oSecurity = oDatabaseDocumentTx.getMetadata().getSecurity();

		ORole writer = oSecurity.getRole(DEFAULT_WRITER_ROLE);
		ORole reader = oSecurity.getRole(DEFAULT_READER_ROLE);

		String writeRoleName = getSecurityRoleOrUserName(PermissionMode.WRITER,
				SecurityType.ROLE, context);
		ORole writerRole = oSecurity.createRole(writeRoleName, writer,
				ALLOW_MODES.DENY_ALL_BUT);
		writerRole.save();

		String readerRoleName = getSecurityRoleOrUserName(
				PermissionMode.READER, SecurityType.ROLE, context);
		ORole readerRole = oSecurity.createRole(readerRoleName, reader,
				ALLOW_MODES.DENY_ALL_BUT);
		readerRole.save();

		String writerUserName = getSecurityRoleOrUserName(
				PermissionMode.WRITER, SecurityType.USER, context);
		OUser writerUser = oSecurity.createUser(writerUserName,
				WRITER_PASSWORD, writerRole);
		writerUser.save();

		String readerUserName = getSecurityRoleOrUserName(
				PermissionMode.READER, SecurityType.USER, context);
		OUser readerUser = oSecurity.createUser(readerUserName,
				READER_PASSSWORD, readerRole);
		readerUser.save();

		oDatabaseDocumentTx.commit();

	}

}
