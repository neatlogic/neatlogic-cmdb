/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.cmdb.dao.mapper.resourcecenter;

import neatlogic.framework.cmdb.crossover.IAppSystemMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.AppEnvironmentVo;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.AppModuleVo;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.AppSystemVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AppSystemMapper extends IAppSystemMapper {

    AppSystemVo getAppSystemByAbbrName(String abbrName);

    AppSystemVo getAppSystemById(Long id);

    List<AppSystemVo> getAppSystemListByIdList(List<Long> idList);

    AppModuleVo getAppModuleByAbbrName(String abbrName);

    AppModuleVo getAppModuleById(Long id);

    List<AppModuleVo> getAppModuleListByIdList(List<Long> idList);

    List<AppEnvironmentVo> getAppEnvListByAppSystemIdAndModuleIdList(@Param("appResourceId") Long appResourceId, @Param("moduleResourceIdList") List<Long> moduleIdList);

}
