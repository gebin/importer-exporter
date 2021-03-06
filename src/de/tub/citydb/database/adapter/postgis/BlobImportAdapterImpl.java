package de.tub.citydb.database.adapter.postgis;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import de.tub.citydb.database.adapter.BlobImportAdapter;
import de.tub.citydb.database.adapter.TextureImageImportAdapter;
import de.tub.citydb.database.adapter.postgis.SQLAdapter.BlobType;
import de.tub.citydb.log.Logger;

public class BlobImportAdapterImpl implements TextureImageImportAdapter, BlobImportAdapter {
	protected final Logger LOG = Logger.getInstance();
	protected final Connection connection;

	private PreparedStatement psUpdate;
	private BlobType blobType;

	protected BlobImportAdapterImpl(Connection connection, BlobType blobType) throws SQLException {
		this.connection = connection;
		this.blobType = blobType;

		psUpdate = connection.prepareStatement(blobType == BlobType.TEXTURE_IMAGE ?
				"update SURFACE_DATA set TEX_IMAGE=? where ID=?" : "update IMPLICIT_GEOMETRY set LIBRARY_OBJECT=? where ID=?");
	}

	@Override
	public boolean insert(long id, InputStream in, String fileName) throws SQLException {
		try {
			psUpdate.setBinaryStream(1, in, in.available());
			psUpdate.setLong(2, id);
			psUpdate.execute();		

			return true;
		} catch (IOException e) {
			LOG.error("Failed to read " + (blobType == BlobType.TEXTURE_IMAGE ? "texture" : "library object") + " file '" + fileName + "': " + e.getMessage());
			return false;
		} catch (SQLException e) {
			LOG.error("SQL error while importing " + (blobType == BlobType.TEXTURE_IMAGE ? "texture" : "library object") + " file '" + fileName + "': " + e.getMessage());
			return false;
		}
	}

	@Override
	public void close() throws SQLException {
		psUpdate.close();
	}

}
