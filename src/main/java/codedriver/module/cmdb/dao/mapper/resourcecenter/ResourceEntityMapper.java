/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.dao.mapper.resourcecenter;

import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface ResourceEntityMapper {
    List<ResourceEntityVo> getAllResourceEntity();

    String checkTableOrViewIsExists(@Param("tableSchema") String tableSchema, @Param("tableName") String tableName);

    List<String> getTableOrViewAllColumnNameList(@Param("tableSchema") String tableSchema, @Param("tableName") String tableName);

    void insertResourceEntityView(String sql);

    void insertResourceEntity(ResourceEntityVo resourceEntityVo);

    void updateResourceEntity(ResourceEntityVo resourceEntityVo);

//    void insertResourceEntityAttr(ResourceEntityAttrVo resourceEntityAttrVo);

    void deleteResourceEntityByName(String name);

    void deleteResourceEntityView(String viewName);

    void deleteResourceEntityTable(String tableName);

//    void deleteResourceEntityAttrByEntity(String entityName);
}
