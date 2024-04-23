/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.cmdb.dao.mapper.cischema;

import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.module.cmdb.annotation.CiId;
import neatlogic.module.cmdb.annotation.CreateCiView;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface CiSchemaMapper {


    int checkSchemaIsExists(String databaseName);

    int checkColumnIsExists(@Param("schemaName") String schemaName, @Param("ciId") Long ciId, @Param("attrId") Long attrId);

    int checkIndexIsExists(@Param("schemaName") String schemaName, @Param("ciId") Long ciId, @Param("attrId") Long attrId);

    int getIndexCount(@Param("schemaName") String schemaName, @Param("ciId") Long ciId);

    int checkTableIsExists(@Param("schemaName") String schemaName, @Param("tableName") String tableName);

    Integer checkTableHasData(@Param("schema") String schema, @Param("table") String table);
    //void updateSchemaAuditIsFailed(Long auditId);

    //void insertAttrSchema(@Param("tableName") String tableName, @Param("columnList") List<AttrVo.Column> columnList);
    List<Map<String, String>> testCiViewSql(String sql);

    @CreateCiView
    void insertAttrToCiTable(@CiId Long ciId, @Param("tableName") String tableName, @Param("attrVo") AttrVo attrVo);

    //@CreateCiView
    void insertCiTable(@CiId Long ciId, @Param("tableName") String tableName);

    void insertCiView(String sql);

    void addAttrIndex(@Param("tableName") String tableName, @Param("attrId") Long attrId);

    //void insertCiAttrSchema(@Param("tableName") String tableName);

    //void insertRelSchema(@Param("tableName") String tableName);

    //void insertCiEntity(@Param("tableName") String tableName, @Param("ciEntityVo") CiEntityVo ciEntityVo);

    //void insertRelEntity(@Param("tableName") String tableName, @Param("ciEntityVo") CiEntityVo ciEntityVo);

    //void replaceSchemaAudit(SchemaAuditVo schemaAuditvo);
    void deleteAttrIndex(@Param("tableName") String tableName, @Param("attrId") Long attrId);

    @CreateCiView
    void deleteAttrFromCiTable(@CiId Long ciId, @Param("tableName") String tableName, @Param("attrVo") AttrVo attrVo);

    @CreateCiView
    void initCiTable(@CiId Long ciId, @Param("ciVo") CiVo ciVo);


    void deleteCiTable(@CiId Long ciId, @Param("tableName") String tableName);

    void deleteCiView(@Param("tableName") String tableName);
    //void deleteRelSchema(@Param("tableName") String tableName);

    //void deleteCiEntityById(@Param("tableName") String tableName, @Param("ciEntityId") Long ciEntityId);

    //void deleteCiEntityRelByCiEntityId(@Param("tableName") String tableName, @Param("ciEntityId") Long ciEntityId);

    //void deleteSchemaAuditById(Long auditId);

}
