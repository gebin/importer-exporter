-- WATERBODY.sql
--
-- Authors:     Prof. Dr. Thomas H. Kolbe <kolbe@igg.tu-berlin.de>
--              Gerhard K�nig <gerhard.koenig@tu-berlin.de>
--              Claus Nagel <nagel@igg.tu-berlin.de>
--              Alexandra Stadler <stadler@igg.tu-berlin.de>
--
-- Copyright:   (c) 2007-2008  Institute for Geodesy and Geoinformation Science,
--                             Technische Universit�t Berlin, Germany
--                             http://www.igg.tu-berlin.de
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
-- 2.0.0     2007-11-23   release version                             TKol
--                                                                    GKoe
--                                                                    CNag
--                                                                    ASta
--
CREATE TABLE WATERBODY
(
ID NUMBER NOT NULL,
NAME VARCHAR2(1000),
NAME_CODESPACE VARCHAR2(4000),
DESCRIPTION VARCHAR2(4000),
CLASS VARCHAR2(256),
FUNCTION VARCHAR2(1000),
USAGE VARCHAR2(1000),
LOD0_MULTI_CURVE MDSYS.SDO_GEOMETRY,
LOD1_MULTI_CURVE MDSYS.SDO_GEOMETRY,
LOD1_SOLID_ID NUMBER,
LOD2_SOLID_ID NUMBER,
LOD3_SOLID_ID NUMBER,
LOD4_SOLID_ID NUMBER,
LOD0_MULTI_SURFACE_ID NUMBER,
LOD1_MULTI_SURFACE_ID NUMBER
)
;
ALTER TABLE WATERBODY
ADD CONSTRAINT WATERBODY_PK PRIMARY KEY
(
ID
)
 ENABLE
;