/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.cmdb.dao.mapper.cischema;

import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.module.cmdb.annotation.CiId;
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

    //@CreateCiView
    void insertAttrToCiTable(@CiId Long ciId, @Param("tableName") String tableName, @Param("attrVo") AttrVo attrVo);

    void updateAttrConfig(@CiId Long ciId, @Param("tableName") String tableName, @Param("attrVo") AttrVo attrVo);

    //创建模型和编辑模型时使用
    void insertCiTable(@CiId Long ciId, @Param("tableName") String tableName);

    void insertCiView(String sql);

    void addAttrIndex(@Param("tableName") String tableName, @Param("attrId") Long attrId);

    void deleteAttrIndex(@Param("tableName") String tableName, @Param("attrId") Long attrId);

    //@CreateCiView
    void deleteAttrFromCiTable(@CiId Long ciId, @Param("tableName") String tableName, @Param("attrVo") AttrVo attrVo);

    //@CreateCiView
    void initCiTable(@CiId Long ciId, @Param("ciVo") CiVo ciVo);


    //@DeleteCiView
    void deleteCiTable(@CiId Long ciId, @Param("tableName") String tableName);

    void deleteCiView(@Param("tableName") String tableName);

}
