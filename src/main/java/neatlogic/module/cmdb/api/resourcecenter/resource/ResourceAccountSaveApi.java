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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.RESOURCECENTER_MODIFY;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.service.resourcecenter.account.ResourceCenterAccountService;
import neatlogic.module.cmdb.service.resourcecenter.resource.IResourceCenterResourceService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author linbq
 * @since 2021/6/22 15:55
 **/
@Service
@Transactional
@AuthAction(action = RESOURCECENTER_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class ResourceAccountSaveApi extends PrivateApiComponentBase {

    @Resource
    private IResourceCenterResourceService resourceCenterResourceService;
    @Resource
    private ResourceCenterAccountService resourceCenterAccountService;

    @Override
    public String getToken() {
        return "resourcecenter/resource/account/save";
    }

    @Override
    public String getName() {
        return "nmcarr.resourceaccountsaveapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "resourceId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.resourceid"),
            @Param(name = "accountIdList", type = ApiParamType.JSONARRAY, desc = "term.cmdb.accountidlist")
    })
    @Description(desc = "nmcarr.resourceaccountsaveapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long resourceId = paramObj.getLong("resourceId");
        if (resourceCenterResourceService.getResourceIdByResourceId(resourceId) == null) {
            throw new ResourceNotFoundException(resourceId);
        }
        List<Long> accountIdList = new ArrayList<>();
        JSONArray accountIdArray = paramObj.getJSONArray("accountIdList");
        if (CollectionUtils.isNotEmpty(accountIdArray)) {
            accountIdList = accountIdArray.toJavaList(Long.class);
        }
        return resourceCenterAccountService.saveResourceAccount(resourceId, accountIdList);
    }
}
