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
package de.tub.citydb.config.project.importer;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name="XMLValidationType", propOrder={
		"useXMLValidation",
		"reportOneErrorPerFeature"
})
public class XMLValidation {
	@XmlElement(required=true, defaultValue="false")
	private Boolean useXMLValidation = false;
	@XmlElement(defaultValue="false")
	private Boolean reportOneErrorPerFeature = false;

	public XMLValidation() {
	}

	public boolean isSetUseXMLValidation() {
		if (useXMLValidation != null)
			return useXMLValidation.booleanValue();

		return false;
	}

	public Boolean getUseXMLValidation() {
		return useXMLValidation;
	}

	public void setUseXMLValidation(Boolean useXMLValidation) {
		this.useXMLValidation = useXMLValidation;
	}

	public boolean isSetReportOneErrorPerFeature() {
		if (reportOneErrorPerFeature != null)
			return reportOneErrorPerFeature.booleanValue();
		
		return false;
	}
	
	public Boolean getReportOneErrorPerFeature() {
		return reportOneErrorPerFeature;
	}

	public void setReportOneErrorPerFeature(Boolean reportOneErrorPerFeature) {
		this.reportOneErrorPerFeature = reportOneErrorPerFeature;
	}
	
}
