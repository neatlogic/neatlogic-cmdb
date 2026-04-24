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

package neatlogic.module.cmdb.api.resourcecenter.config;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.RESOURCECENTER_MODIFY;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityConfigVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.SceneEntityVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.common.dto.ValueTextVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceEntityMapper;
import neatlogic.framework.cmdb.utils.ResourceEntityFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@AuthAction(action = RESOURCECENTER_MODIFY.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class ListApplicationAssetListDetailViewApi extends PrivateApiComponentBase {

    @Resource
    private ResourceEntityMapper resourceEntityMapper;

    @Override
    public String getName() {
        return "获取应用资产清单详情视图列表";
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "common.keyword")
    })
    @Output({
            @Param(name = "tbodyList", explode = ValueTextVo[].class, desc = "common.tbodylist")
    })
    @Description(desc = "获取应用资产清单详情视图列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        String keyword = paramObj.getString("keyword");
        List<ValueTextVo> tbodyList = new ArrayList<>();
        List<ResourceEntityVo> resourceEntityList = resourceEntityMapper.getResourceEntityList();
        for (ResourceEntityVo resourceEntityVo : resourceEntityList) {
            SceneEntityVo sceneEntityVo = ResourceEntityFactory.getSceneEntityByViewName(resourceEntityVo.getName());
            if (sceneEntityVo == null) {
                String config = resourceEntityMapper.getResourceEntityConfigByName(resourceEntityVo.getName());
                if (StringUtils.isNotBlank(config)) {
                    ResourceEntityConfigVo resourceEntityConfigVo = JSONObject.parseObject(config, ResourceEntityConfigVo.class);
                    if (resourceEntityConfigVo != null) {
                        String sceneTemplateName = resourceEntityConfigVo.getSceneTemplateName();
                        if (Objects.equals(sceneTemplateName, "scence_application_asset_list_detail")) {
                            if (StringUtils.isNotBlank(keyword)) {
                                if (!resourceEntityVo.getName().contains(keyword) && !resourceEntityVo.getLabel().contains(keyword)) {
                                    continue;
                                }
                            }
                            tbodyList.add(new ValueTextVo(resourceEntityVo.getName(), resourceEntityVo.getLabel()));
                        }
                    }
                }
            }
        }
        BasePageVo basePageVo = new BasePageVo();
        basePageVo.setCurrentPage(1);
        basePageVo.setPageSize(tbodyList.size());
        basePageVo.setRowNum(tbodyList.size());
        return TableResultUtil.getResult(tbodyList, basePageVo);
    }

    @Override
    public String getToken() {
        return "resourcecenter/application/assetlist/view/list";
    }
}
