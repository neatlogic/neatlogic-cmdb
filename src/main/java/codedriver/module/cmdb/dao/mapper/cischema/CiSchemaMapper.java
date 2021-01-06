package codedriver.module.cmdb.dao.mapper.cischema;

import codedriver.module.cmdb.dto.ci.AttrVo;
import codedriver.module.cmdb.dto.cientity.CiEntityVo;
import codedriver.module.cmdb.dto.schema.SchemaAuditVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CiSchemaMapper {
    SchemaAuditVo getLatestSchemaAudit(Integer serverId);

    int checkDatabaseIsExists(String databaseName);

    void updateSchemaAuditIsFailed(Long auditId);

    void insertAttr(@Param("tableName") String tableName, @Param("attrVo") AttrVo attrVo);

    void insertCiSchema(@Param("tableName") String tableName, @Param("attrList") List<AttrVo> attrList);

    void insertRelSchema(@Param("tableName") String tableName);

    void insertCiEntity(@Param("tableName") String tableName, @Param("ciEntityVo") CiEntityVo ciEntityVo);

    void insertRelEntity(@Param("tableName") String tableName, @Param("ciEntityVo") CiEntityVo ciEntityVo);

    void replaceSchemaAudit(SchemaAuditVo schemaAuditvo);

    void deleteAttr(@Param("tableName") String tableName, @Param("attrName") String attrName);

    void deleteCiSchema(@Param("tableName") String tableName);

    void deleteRelSchema(@Param("tableName") String tableName);

    void deleteCiEntityById(@Param("tableName") String tableName, @Param("ciEntityId") Long ciEntityId);

    void deleteCiEntityRelByCiEntityId(@Param("tableName") String tableName, @Param("ciEntityId") Long ciEntityId);

    void deleteSchemaAuditById(Long auditId);

}
