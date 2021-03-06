package de.tub.citydb.database.adapter.postgis;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;

import org.postgis.Geometry;
import org.postgis.PGgeometry;

import de.tub.citydb.api.database.DatabaseSrs;
import de.tub.citydb.api.database.DatabaseSrsType;
import de.tub.citydb.api.geometry.BoundingBox;
import de.tub.citydb.api.geometry.BoundingBoxCorner;
import de.tub.citydb.database.DatabaseMetaDataImpl;
import de.tub.citydb.database.IndexStatusInfo;
import de.tub.citydb.database.DatabaseMetaDataImpl.Versioning;
import de.tub.citydb.database.IndexStatusInfo.IndexType;
import de.tub.citydb.database.adapter.AbstractDatabaseAdapter;
import de.tub.citydb.database.adapter.AbstractUtilAdapter;
import de.tub.citydb.util.Util;

public class UtilAdapter extends AbstractUtilAdapter {

	protected UtilAdapter(AbstractDatabaseAdapter databaseAdapter) {
		super(databaseAdapter);
	}

	@Override
	protected void getDatabaseMetaData(DatabaseMetaDataImpl metaData, Connection connection) throws SQLException {
		Statement statement = null;
		ResultSet rs = null;

		try {
			statement = connection.createStatement();
			rs = statement.executeQuery("select * from " + databaseAdapter.getSQLAdapter().resolveDatabaseOperationName("geodb_util.db_metadata") + "()");
			if (rs.next()) {
				DatabaseSrs srs = metaData.getReferenceSystem();
				srs.setSrid(rs.getInt("SRID"));
				srs.setGMLSrsName(rs.getString("GML_SRS_NAME"));
				srs.setDatabaseSrsName(rs.getString("COORD_REF_SYS_NAME"));
				srs.setType(getSrsType(rs.getString("COORD_REF_SYS_KIND")));
				srs.setSupported(true);

				metaData.setVersioning(Versioning.NOT_SUPPORTED);
			} else
				throw new SQLException("Failed to retrieve metadata information from database.");
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					throw e;
				}

				rs = null;
			}

			if (statement != null) {
				try {
					statement.close();
				} catch (SQLException e) {
					throw e;
				}

				statement = null;
			}
		}
	}

	@Override
	protected void getSrsInfo(DatabaseSrs srs, Connection connection) throws SQLException {
		Statement statement = null;
		ResultSet rs = null;

		try {
			statement = connection.createStatement();
			rs = statement.executeQuery("select split_part(srtext, '\"', 2) as coord_ref_sys_name, split_part(srtext, '[', 1) as coord_ref_sys_kind FROM spatial_ref_sys WHERE SRID = " + srs.getSrid());

			if (rs.next()) {
				srs.setSupported(true);
				srs.setDatabaseSrsName(rs.getString(1));
				srs.setType(getSrsType(rs.getString(2)));
			} else {
				DatabaseSrs tmp = DatabaseSrs.createDefaultSrs();
				srs.setDatabaseSrsName(tmp.getDatabaseSrsName());
				srs.setType(tmp.getType());
				srs.setSupported(false);
			}

		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					throw e;
				}

				rs = null;
			}

			if (statement != null) {
				try {
					statement.close();
				} catch (SQLException e) {
					throw e;
				}

				statement = null;
			}
		}
	}

	@Override
	protected String[] createDatabaseReport(Connection connection) throws SQLException {
		try {
			interruptableCallableStatement = connection.prepareCall("{? = call " + databaseAdapter.getSQLAdapter().resolveDatabaseOperationName("geodb_stat.table_contents") + "()}");
			interruptableCallableStatement.registerOutParameter(1, Types.ARRAY);
			interruptableCallableStatement.executeUpdate();

			Array result = interruptableCallableStatement.getArray(1);
			return (String[])result.getArray();
		} catch (SQLException e) {
			if (!isInterrupted)
				throw e;
		} finally {
			if (interruptableCallableStatement != null) {
				try {
					interruptableCallableStatement.close();
				} catch (SQLException e) {
					throw e;
				}

				interruptableCallableStatement = null;
			}

			isInterrupted = false;
		}

		return null;
	}

	@Override
	protected BoundingBox calcBoundingBox(List<Integer> classIds, Connection connection) throws SQLException {
		BoundingBox bbox = null;
		ResultSet rs = null;

		try {		
			String query = "select ST_Extent(ST_Force_2d(envelope))::geometry from cityobject where envelope is not null";
			if (!classIds.isEmpty()) 
				query += " and class_id in (" + Util.collection2string(classIds, ", ") +") ";

			BoundingBoxCorner lowerCorner = new BoundingBoxCorner(Double.MAX_VALUE);
			BoundingBoxCorner upperCorner = new BoundingBoxCorner(-Double.MAX_VALUE);

			interruptableStatement = connection.createStatement();
			rs = interruptableStatement.executeQuery(query);

			if (rs.next()) {
				PGgeometry pgGeom = (PGgeometry)rs.getObject(1);		
				if (!rs.wasNull() && pgGeom != null) {
					Geometry geom = pgGeom.getGeometry();
					int dim = geom.getDimension();	
					if (dim == 2 || dim == 3) {
						double xmin, ymin, xmax, ymax;
						xmin = ymin = Double.MAX_VALUE;
						xmax = ymax = -Double.MAX_VALUE;

						xmin = (geom.getPoint(0).x);
						ymin = (geom.getPoint(0).y);
						xmax = (geom.getPoint(2).x);
						ymax = (geom.getPoint(2).y);

						lowerCorner.setX(xmin);
						lowerCorner.setY(ymin);
						upperCorner.setX(xmax);
						upperCorner.setY(ymax);	
					}		
				}
			}

			if (!isInterrupted)
				bbox = new BoundingBox(lowerCorner, upperCorner);

		} catch (SQLException e) {
			if (!isInterrupted)
				throw e;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					throw e;
				}

				rs = null;
			}

			if (interruptableStatement != null) {
				try {
					interruptableStatement.close();
				} catch (SQLException e) {
					throw e;
				}

				interruptableStatement = null;
			}

			isInterrupted = false;
		}

		return bbox;
	}

	@Override
	protected IndexStatusInfo manageIndexes(String operation, IndexType type, Connection connection) throws SQLException {
		try {
			String call = "{? = call " + databaseAdapter.getSQLAdapter().resolveDatabaseOperationName(operation) + "()}";
			interruptableCallableStatement = connection.prepareCall(call);
			interruptableCallableStatement.registerOutParameter(1, Types.ARRAY);
			interruptableCallableStatement.executeUpdate();

			Array result = interruptableCallableStatement.getArray(1);
			return IndexStatusInfo.createFromDatabaseQuery((String[])result.getArray(), type);
		} catch (SQLException e) {
			if (!isInterrupted)
				throw e;
		} finally {
			if (interruptableCallableStatement != null) {
				try {
					interruptableCallableStatement.close();
				} catch (SQLException e) {
					throw e;
				}

				interruptableCallableStatement = null;
			}

			isInterrupted = false;
		}

		return null;
	}

	@Override
	protected BoundingBox transformBBox(BoundingBox bbox, DatabaseSrs sourceSrs, DatabaseSrs targetSrs, Connection connection) throws SQLException {
		BoundingBox result = new BoundingBox(bbox);
		PreparedStatement psQuery = null;
		ResultSet rs = null;

		try {
			int sourceSrid = get2DSrid(sourceSrs, connection);
			int targetSrid = get2DSrid(targetSrs, connection);

			StringBuilder boxGeom = new StringBuilder()
			.append("SRID=" + sourceSrid + ";POLYGON((")
			.append(bbox.getLowerLeftCorner().getX()).append(" ").append(bbox.getLowerLeftCorner().getY()).append(",")
			.append(bbox.getLowerLeftCorner().getX()).append(" ").append(bbox.getUpperRightCorner().getY()).append(",")
			.append(bbox.getUpperRightCorner().getX()).append(" ").append(bbox.getUpperRightCorner().getY()).append(",")
			.append(bbox.getUpperRightCorner().getX()).append(" ").append(bbox.getLowerLeftCorner().getY()).append(",")
			.append(bbox.getLowerLeftCorner().getX()).append(" ").append(bbox.getLowerLeftCorner().getY()).append("))");

			StringBuilder query = new StringBuilder()
			.append("select ST_Transform(ST_GeomFromEWKT(?), ").append(targetSrid).append(')');
			
			psQuery = connection.prepareStatement(query.toString());			
			psQuery.setString(1, boxGeom.toString());

			rs = psQuery.executeQuery();
			if (rs.next()) {
				PGgeometry pgGeom = (PGgeometry)rs.getObject(1);
				if (!rs.wasNull() && pgGeom != null) {
					Geometry geom = pgGeom.getGeometry();
					result.getLowerLeftCorner().setX(geom.getPoint(0).x);
					result.getLowerLeftCorner().setY(geom.getPoint(0).y);
					result.getUpperRightCorner().setX(geom.getPoint(2).x);
					result.getUpperRightCorner().setY(geom.getPoint(2).y);
					result.setSrs(targetSrs);
				}
			}
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException sqlEx) {
					throw sqlEx;
				}

				rs = null;
			}

			if (psQuery != null) {
				try {
					psQuery.close();
				} catch (SQLException sqlEx) {
					throw sqlEx;
				}

				psQuery = null;
			}
		}

		return result;
	}

	@Override
	protected int get2DSrid(DatabaseSrs srs, Connection connection) throws SQLException {
		if (!srs.is3D())
			return srs.getSrid();

		// at the moment the spatial_ref_sys-table does not hold 3D-SRIDs by default
		// for proper INSERT-Statements check www.spatialreference.org
		// unfortunately Geographic2D and Geographic3D are equally classified as GEOGCS
		// so the function is3D() wouldn't detect Geographic3D unless the INSERT-command is not changed
		// e.g. srtext: "GEOGCS["WGS 84 (3D)", ... to "GEOGCS3D["WGS 84 (3D)", ...

		Statement stmt = null;
		ResultSet rs = null;

		try {
			stmt = connection.createStatement();
			rs = stmt.executeQuery(srs.getType() == DatabaseSrsType.COMPOUND ?
					// get id of compound-reference-system from srtext-field of spatial_ref_sys-table
					"select split_part((split_part(srtext,'AUTHORITY[\"EPSG\",\"',5)),'\"',1) from spatial_ref_sys where auth_srid = " + srs.getSrid() :
						// searching 2D equivalent for 3D SRID
						"select min(crs2d.auth_srid) from spatial_ref_sys crs3d, spatial_ref_sys crs2d " +
						"where (crs3d.auth_srid = " + srs.getSrid() + " " +
						"and split_part(crs3d.srtext, '[', 1) LIKE 'GEOGCS' AND split_part(crs2d.srtext, '[', 1) LIKE 'GEOGCS' " +
						//do they have the same Datum_ID?
						"and split_part((split_part(crs3d.srtext,'AUTHORITY[\"EPSG\",\"',3)),'\"',1) = split_part((split_part(crs2d.srtext,'AUTHORITY[\"EPSG\",\"',3)),'\"',1)) OR " +
						// if srtext has been changed for Geographic3D
						"(crs3d.auth_srid = " + srs.getSrid() + " " +
						"and split_part(crs3d.srtext, '[', 1) LIKE 'GEOGCS3D' AND split_part(crs2d.srtext, '[', 1) LIKE 'GEOGCS' " +
						//do they have the same Datum_ID?
					"and split_part((split_part(crs3d.srtext,'AUTHORITY[\"EPSG\",\"',3)),'\"',1) = split_part((split_part(crs2d.srtext,'AUTHORITY[\"EPSG\",\"',3)),'\"',1))");

			return rs.next() ? rs.getInt(1) : -1;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					throw e;
				}

				rs = null;
			}

			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException e) {
					throw e;
				}

				stmt = null;
			}
		}
	}

	private DatabaseSrsType getSrsType(String srsType) {
		if ("PROJCS".equals(srsType))
			return DatabaseSrsType.PROJECTED;
		else if ("GEOGCS".equals(srsType))
			return DatabaseSrsType.GEOGRAPHIC2D;
		else if ("GEOCCS".equals(srsType))
			return DatabaseSrsType.GEOCENTRIC;
		else if ("VERT_CS".equals(srsType))
			return DatabaseSrsType.VERTICAL;
		else if ("LOCAL_CS".equals(srsType))
			return DatabaseSrsType.ENGINEERING;
		else if ("COMPD_CS".equals(srsType))
			return DatabaseSrsType.COMPOUND;
		else if ("GEOGCS3D".equals(srsType))
			return DatabaseSrsType.GEOGRAPHIC3D;
		else
			return DatabaseSrsType.UNKNOWN;
	}

}
