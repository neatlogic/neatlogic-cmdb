/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.dao.mapper.resourcecenter;

import codedriver.framework.cmdb.crossover.IAppSystemMapper;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.cmdb.dto.resourcecenter.entity.AppEnvironmentVo;
import codedriver.framework.cmdb.dto.resourcecenter.entity.AppModuleVo;
import codedriver.framework.cmdb.dto.resourcecenter.entity.AppSystemVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AppSystemMapper extends IAppSystemMapper {
//    List<AppSystemVo> searchAppSystem(AppSystemVo appSystemVo);

    AppSystemVo getAppSystemByAbbrName(@Param("abbrName")String abbrName, @Param("schemaName") String schemaName);

    AppSystemVo getAppSystemById(@Param("id")Long id, @Param("schemaName") String schemaName);

    AppModuleVo getAppModuleByAbbrName(@Param("abbrName")String abbrName, @Param("schemaName") String schemaName);

    AppModuleVo getAppModuleById(@Param("id")Long id, @Param("schemaName") String schemaName);

    List<AppEnvironmentVo> getAppEnvListByAppSystemIdAndModuleIdList(@Param("appResourceId") Long appResourceId, @Param("moduleResourceIdList") List<Long> moduleIdList, @Param("schemaName") String schemaName);

    Integer getAppSystemIdListCount(ResourceSearchVo searchVo);

    List<Long> getAppSystemIdListByAppModuleName(@Param("keyword") String keyword, @Param("schemaName") String schemaName);

}
