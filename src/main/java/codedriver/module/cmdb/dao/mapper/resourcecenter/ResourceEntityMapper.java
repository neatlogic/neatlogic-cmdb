/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.dao.mapper.resourcecenter;

import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;

import java.util.List;

public interface ResourceEntityMapper {
    ResourceEntityVo getResourceEntityByName(String name);

    List<ResourceEntityVo> getAllResourceEntity();

//    void insertResourceEntityView(String sql);

    void insertResourceEntity(ResourceEntityVo resourceEntityVo);

    void updateResourceEntity(ResourceEntityVo resourceEntityVo);

//    void insertResourceEntityAttr(ResourceEntityAttrVo resourceEntityAttrVo);

    void deleteResourceEntityByName(String name);

//    void deleteResourceEntityView(String viewName);

//    void deleteResourceEntityAttrByEntity(String entityName);
}
