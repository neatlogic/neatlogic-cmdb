/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.dao.mapper.cischema;

import codedriver.framework.cmdb.dto.ci.AttrVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface CiSchemaMapper {
    //SchemaAuditVo getLatestSchemaAudit(Integer serverId);

    //int checkTableIsExists(@Param("dbName") String dbName, @Param("tableName") String tableName);

    int checkDatabaseIsExists(String databaseName);

    //void updateSchemaAuditIsFailed(Long auditId);

    //void insertAttrSchema(@Param("tableName") String tableName, @Param("columnList") List<AttrVo.Column> columnList);
    List<Map<String, String>> testCiViewSql(String sql);

    void insertAttrToCiSchema(@Param("tableName") String tableName, @Param("attrVo") AttrVo attrVo);

    void insertCiSchema(@Param("tableName") String tableName);

    void insertCiView(String sql);

    //void insertCiAttrSchema(@Param("tableName") String tableName);

    //void insertRelSchema(@Param("tableName") String tableName);

    //void insertCiEntity(@Param("tableName") String tableName, @Param("ciEntityVo") CiEntityVo ciEntityVo);

    //void insertRelEntity(@Param("tableName") String tableName, @Param("ciEntityVo") CiEntityVo ciEntityVo);

    //void replaceSchemaAudit(SchemaAuditVo schemaAuditvo);

    void deleteAttrFromCiSchema(@Param("tableName") String tableName, @Param("attrVo") AttrVo attrVo);

    void deleteSchema(@Param("tableName") String tableName);

    void deleteView(@Param("tableName") String tableName);
    //void deleteRelSchema(@Param("tableName") String tableName);

    //void deleteCiEntityById(@Param("tableName") String tableName, @Param("ciEntityId") Long ciEntityId);

    //void deleteCiEntityRelByCiEntityId(@Param("tableName") String tableName, @Param("ciEntityId") Long ciEntityId);

    //void deleteSchemaAuditById(Long auditId);

}
