-- ORTHOPHOTO_IMP.sql
--
-- Authors:     Prof. Dr. Thomas H. Kolbe <thomas.kolbe@tum.de>
--              Prof. Dr. Lutz Pluemer <pluemer@ikg.uni-bonn.de>
--              Dr. Gerhard Groeger <groeger@ikg.uni-bonn.de>
--              Joerg Schmittwilken <schmittwilken@ikg.uni-bonn.de>
--              Viktor Stroh <stroh@ikg.uni-bonn.de>
--              Dr. Andreas Poth <poth@lat-lon.de>
--
-- Copyright:   (c) 2007,      Institute for Geodesy and Geoinformation Science,
--                             Technische Universit�t Berlin, Germany
--                             http://www.igg.tu-berlin.de
--              (c) 2004-2006, Institute for Cartography and Geoinformation,
--                             Universit�t Bonn, Germany
--                             http://www.ikg.uni-bonn.de
--              (c) 2005-2006, lat/lon GmbH, Germany
--                             http://www.lat-lon.de
--
--              This skript is free software under the LGPL Version 2.1.
--              See the GNU Lesser General Public License at
--              http://www.gnu.org/copyleft/lgpl.html
--              for more details.
-------------------------------------------------------------------------------
-- About:
--
--
-------------------------------------------------------------------------------
--
-- ChangeLog:
--
-- Version | Date       | Description                               | Author
-- 2.0       2007-12-10   release version                             TKol
--                                                                    LPlu
--                                                                    GGro
--                                                                    JSch
--                                                                    VStr
--                                                                    APot
--

-- DROP TABLE "ORTHOPHOTO_IMP" CASCADE CONSTRAINT PURGE;

CREATE TABLE "ORTHOPHOTO_IMP" (
"ID" NUMBER NOT NULL,
"ORTHOPHOTOPROPERTY" MDSYS.SDO_GEORASTER,
"FILENAME" VARCHAR2 (4000),
"FOOTPRINT" MDSYS.SDO_GEOMETRY );

ALTER TABLE "ORTHOPHOTO_IMP"
ADD CONSTRAINT "ORTHOPHOTO_IMP_PK" PRIMARY KEY ( "ID" ) ENABLE;
