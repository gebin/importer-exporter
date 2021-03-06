/*
 * This file is part of the 3D City Database Importer/Exporter.
 * Copyright (c) 2007 - 2013
 * Institute for Geodesy and Geoinformation Science
 * Technische Universitaet Berlin, Germany
 * http://www.gis.tu-berlin.de/
 * 
 * The 3D City Database Importer/Exporter program is free software:
 * you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program. If not, see 
 * <http://www.gnu.org/licenses/>.
 * 
 * The development of the 3D City Database Importer/Exporter has 
 * been financially supported by the following cooperation partners:
 * 
 * Business Location Center, Berlin <http://www.businesslocationcenter.de/>
 * virtualcitySYSTEMS GmbH, Berlin <http://www.virtualcitysystems.de/>
 * Berlin Senate of Business, Technology and Women <http://www.berlin.de/sen/wtf/>
 */
package de.tub.citydb.modules.citygml.importer.database.xlink.importer;

import java.sql.SQLException;
import java.util.HashMap;

import de.tub.citydb.api.event.Event;
import de.tub.citydb.api.event.EventDispatcher;
import de.tub.citydb.database.adapter.AbstractDatabaseAdapter;
import de.tub.citydb.modules.citygml.common.database.cache.CacheManager;
import de.tub.citydb.modules.citygml.common.database.cache.CacheTable;
import de.tub.citydb.modules.citygml.common.database.cache.model.CacheTableModelEnum;

public class DBXlinkImporterManager {
	private final CacheManager dbTempTableManager;
	private final AbstractDatabaseAdapter databaseAdapter;
	private final EventDispatcher eventDispatcher;
	private HashMap<DBXlinkImporterEnum, DBXlinkImporter> dbImporterMap;

	public DBXlinkImporterManager(CacheManager dbTempTableManager, AbstractDatabaseAdapter databaseAdapter, EventDispatcher eventDispatcher) {
		this.dbTempTableManager = dbTempTableManager;
		this.databaseAdapter = databaseAdapter;
		this.eventDispatcher = eventDispatcher;

		dbImporterMap = new HashMap<DBXlinkImporterEnum, DBXlinkImporter>();
	}

	public DBXlinkImporter getDBImporterXlink(DBXlinkImporterEnum xlinkType) throws SQLException {
		DBXlinkImporter dbImporter = dbImporterMap.get(xlinkType);

		if (dbImporter == null) {
			// firstly create tmp table
			CacheTable tempTable = null;

			switch (xlinkType) {
			case SURFACE_GEOMETRY:
				tempTable = dbTempTableManager.createCacheTable(CacheTableModelEnum.SURFACE_GEOMETRY);
				break;
			case LINEAR_RING:
				tempTable = dbTempTableManager.createCacheTable(CacheTableModelEnum.LINEAR_RING);
				break;
			case XLINK_BASIC:
				tempTable = dbTempTableManager.createCacheTable(CacheTableModelEnum.BASIC);
				break;
			case XLINK_TEXTUREPARAM:
				tempTable = dbTempTableManager.createCacheTable(CacheTableModelEnum.TEXTUREPARAM);
				break;
			case XLINK_TEXTUREASSOCIATION:
				tempTable = dbTempTableManager.createCacheTable(CacheTableModelEnum.TEXTUREASSOCIATION);
				break;
			case TEXTURE_FILE:
				tempTable = dbTempTableManager.createCacheTable(CacheTableModelEnum.TEXTURE_FILE);
				break;
			case LIBRARY_OBJECT:
				tempTable = dbTempTableManager.createCacheTable(CacheTableModelEnum.LIBRARY_OBJECT);
				break;
			case XLINK_DEPRECATED_MATERIAL:
				tempTable = dbTempTableManager.createCacheTable(CacheTableModelEnum.DEPRECATED_MATERIAL);
				break;
			case GROUP_TO_CITYOBJECT:
				tempTable = dbTempTableManager.createCacheTable(CacheTableModelEnum.GROUP_TO_CITYOBJECT);
				break;
			}

			if (tempTable != null) {
				// initialize DBImporter
				switch (xlinkType) {
				case SURFACE_GEOMETRY:
					dbImporter = new DBXlinkImporterSurfaceGeometry(tempTable, this);
					break;
				case LINEAR_RING:
					dbImporter = new DBXlinkImporterLinearRing(tempTable, this);
					break;
				case XLINK_BASIC:
					dbImporter = new DBXlinkImporterBasic(tempTable, this);
					break;
				case XLINK_TEXTUREPARAM:
					dbImporter = new DBXlinkImporterTextureParam(tempTable, this);
					break;
				case XLINK_TEXTUREASSOCIATION:
					dbImporter = new DBXlinkImporterTextureAssociation(tempTable, this);
					break;
				case TEXTURE_FILE:
					dbImporter = new DBXlinkImporterTextureFile(tempTable, this);
					break;
				case LIBRARY_OBJECT:
					dbImporter = new DBXlinkImporterLibraryObject(tempTable, this);
					break;
				case XLINK_DEPRECATED_MATERIAL:
					dbImporter = new DBXlinkImporterDeprecatedMaterial(tempTable, this);
					break;
				case GROUP_TO_CITYOBJECT:
					dbImporter = new DBXlinkImporterGroupToCityObject(tempTable, this);
					break;
				}

				if (dbImporter != null)
					dbImporterMap.put(xlinkType, dbImporter);
			}
		}

		return dbImporter;
	}

	public void propagateEvent(Event event) {
		eventDispatcher.triggerEvent(event);
	}
	
	public AbstractDatabaseAdapter getDatabaseAdapter() {
		return databaseAdapter;
	}

	public void executeBatch() throws SQLException {
		for (DBXlinkImporter dbImporter : dbImporterMap.values())
			dbImporter.executeBatch();
	}
	
	public void close() throws SQLException {
		for (DBXlinkImporter dbImporter : dbImporterMap.values())
			dbImporter.close();
	}
}
