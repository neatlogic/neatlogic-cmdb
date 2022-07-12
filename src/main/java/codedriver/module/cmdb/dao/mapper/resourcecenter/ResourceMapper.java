/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.dao.mapper.resourcecenter;

import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;

import java.util.List;

public interface ResourceMapper {

    int getResourceCount(String sql);

    List<Long> getResourceIdList(String sql);

    Long getResourceId(String sql);

    List<ResourceVo> getResourceListByIdList(String sql);
}
