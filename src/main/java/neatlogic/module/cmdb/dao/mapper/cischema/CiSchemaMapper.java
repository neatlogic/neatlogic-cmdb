/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.cmdb.dao.mapper.cischema;

import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface CiSchemaMapper {
    //SchemaAuditVo getLatestSchemaAudit(Integer serverId);

    //int checkTableIsExists(@Param("dbName") String dbName, @Param("tableName") String tableName);

    int checkSchemaIsExists(String databaseName);

    int checkTableIsExists(@Param("schemaName") String schemaName, @Param("tableName") String tableName);

    int checkTableHasData(String tableName);
    //void updateSchemaAuditIsFailed(Long auditId);

    //void insertAttrSchema(@Param("tableName") String tableName, @Param("columnList") List<AttrVo.Column> columnList);
    List<Map<String, String>> testCiViewSql(String sql);

    void insertAttrToCiTable(@Param("tableName") String tableName, @Param("attrVo") AttrVo attrVo);

    void insertCiTable(@Param("tableName") String tableName);

    void insertCiView(String sql);

    //void insertCiAttrSchema(@Param("tableName") String tableName);

    //void insertRelSchema(@Param("tableName") String tableName);

    //void insertCiEntity(@Param("tableName") String tableName, @Param("ciEntityVo") CiEntityVo ciEntityVo);

    //void insertRelEntity(@Param("tableName") String tableName, @Param("ciEntityVo") CiEntityVo ciEntityVo);

    //void replaceSchemaAudit(SchemaAuditVo schemaAuditvo);

    void deleteAttrFromCiTable(@Param("tableName") String tableName, @Param("attrVo") AttrVo attrVo);

    void initCiTable(CiVo ciVo);

    void deleteCiTable(@Param("tableName") String tableName);

    void deleteCiView(@Param("tableName") String tableName);
    //void deleteRelSchema(@Param("tableName") String tableName);

    //void deleteCiEntityById(@Param("tableName") String tableName, @Param("ciEntityId") Long ciEntityId);

    //void deleteCiEntityRelByCiEntityId(@Param("tableName") String tableName, @Param("ciEntityId") Long ciEntityId);

    //void deleteSchemaAuditById(Long auditId);

}
