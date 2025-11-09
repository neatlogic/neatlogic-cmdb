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

package neatlogic.module.cmdb.api.resourcecenter.resourcetype;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceTypeVo;
import neatlogic.framework.cmdb.resourcecenter.datasource.core.IResourceCenterDataSource;
import neatlogic.framework.cmdb.resourcecenter.datasource.core.ResourceCenterDataSourceFactory;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 查询资源类型树列表接口
 *
 * @author linbq
 * @since 2021/5/27 16:14
 **/
@Service
@AuthAction(action = CMDB.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ResourceTypeListTreeApi extends PrivateApiComponentBase {

    @Override
    public String getToken() {
        return "resourcecenter/resourcetype/listtree";
    }

    @Override
    public String getName() {
        return "查询所有资产类型列表树";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "common.keyword")
    })
    @Output({
            @Param(explode = ResourceTypeVo[].class, desc = "common.tbodylist")
    })
    @Description(desc = "nmcarr.resourcetypetreeapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        String keyword = jsonObj.getString("keyword");
        IResourceCenterDataSource resourceCenterDataSource = ResourceCenterDataSourceFactory.getResourceCenterDataSource();
        List<ResourceTypeVo> tbodyList = resourceCenterDataSource.getResourceTypeListTree(keyword);
        return TableResultUtil.getResult(tbodyList);
    }
}
