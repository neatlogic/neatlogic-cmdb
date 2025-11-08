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

package neatlogic.module.cmdb.api.resourcecenter.resource;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountComponentVo;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.cmdb.service.resourcecenter.resource.IResourceCenterResourceService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ResourceAccountSelectComponentApi extends PrivateApiComponentBase {

    @Resource
    private IResourceCenterResourceService resourceCenterResourceService;

    @Override
    public String getToken() {
        return "resourcecenter/resource/account/component/select";
    }

    @Override
    public String getName() {
        return "表单扩展选择资源中心账号组件";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", isRequired = false, type = ApiParamType.STRING),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(name = "tbodyList", explode = AccountVo[].class, desc = "账号列表"),
    })
    @Description(desc = "表单扩展选择资源中心账号组件")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        AccountComponentVo searchVo = JSON.toJavaObject(paramObj, AccountComponentVo.class);
        List<AccountComponentVo> accountComponentVoList = resourceCenterResourceService.searchAccountComponent(searchVo);
        Integer rowNum = resourceCenterResourceService.searchAccountComponentCount(searchVo);
        searchVo.setRowNum(rowNum);
        return TableResultUtil.getResult(accountComponentVoList, searchVo);

    }


}
