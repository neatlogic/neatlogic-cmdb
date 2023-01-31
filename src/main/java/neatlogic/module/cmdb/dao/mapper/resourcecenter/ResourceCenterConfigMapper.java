/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.cmdb.dao.mapper.resourcecenter;

import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceCenterConfigVo;

public interface ResourceCenterConfigMapper {

    int checkResourceCenterConfigIsExists(Long id);

    ResourceCenterConfigVo getResourceCenterConfig();

    ResourceCenterConfigVo getResourceCenterConfigLimitOne();

    int insertResourceCenterConfig(ResourceCenterConfigVo resourceCenterConfigVo);

    void updateResourceCenterConfig(ResourceCenterConfigVo resourceCenterConfigVo);
}
