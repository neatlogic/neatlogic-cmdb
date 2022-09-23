/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.resourcecenter.appenv;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.resourcecenter.entity.AppEnvironmentVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.resourcecenter.AppSystemMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author linbq
 * @since 2021/6/16 15:04
 **/
@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AppEnvListApi extends PrivateApiComponentBase {

    @Resource
    AppSystemMapper appSystemMapper;

    @Override
    public String getToken() {
        return "resourcecenter/app/env/list";
    }

    @Override
    public String getName() {
        return "查询应用系统环境列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, desc = "应用id"),
            @Param(name = "appModuleIdList", type = ApiParamType.JSONARRAY, desc = "应用模块id列表"),
    })
    @Output({
            @Param(explode = AppEnvironmentVo[].class, desc = "应用模块环境列表"),
    })
    @Description(desc = "查询应用系统环境列表")
    @Override
    public Object myDoService(JSONObject paramObj) {
        JSONArray appModuleIdArray = paramObj.getJSONArray("appModuleIdList");
        List<Long> appModuleIdList = null;
        if (CollectionUtils.isNotEmpty(appModuleIdArray)) {
            appModuleIdList = appModuleIdArray.toJavaList(Long.class);
        }
        Long appSystemId = paramObj.getLong("appSystemId");
        return appSystemMapper.getAppEnvListByAppSystemIdAndModuleIdList(appSystemId, appModuleIdList);
    }
}
