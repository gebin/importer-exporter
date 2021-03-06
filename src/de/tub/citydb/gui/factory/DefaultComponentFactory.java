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
package de.tub.citydb.gui.factory;

import de.tub.citydb.api.gui.BoundingBoxPanel;
import de.tub.citydb.api.gui.ComponentFactory;
import de.tub.citydb.api.gui.DatabaseSrsComboBox;
import de.tub.citydb.api.gui.StandardEditingPopupMenuDecorator;
import de.tub.citydb.config.Config;
import de.tub.citydb.gui.components.bbox.BoundingBoxPanelImpl;

public class DefaultComponentFactory implements ComponentFactory {
	private static DefaultComponentFactory instance;
	private final Config config;
	
	private DefaultComponentFactory(Config config) {
		this.config = config;
	}
	
	public static synchronized DefaultComponentFactory getInstance(Config config) {
		if (instance == null)
			instance = new DefaultComponentFactory(config);
		
		return instance;
	}
	
	@Override
	public DatabaseSrsComboBox createDatabaseSrsComboBox() {
		return SrsComboBoxFactory.getInstance(config).createSrsComboBox(true);
	}

	@Override
	public StandardEditingPopupMenuDecorator createPopupMenuDecorator() {
		return PopupMenuDecorator.getInstance();
	}

	@Override
	public BoundingBoxPanel createBoundingBoxPanel() {
		return new BoundingBoxPanelImpl(config);
	}

}
