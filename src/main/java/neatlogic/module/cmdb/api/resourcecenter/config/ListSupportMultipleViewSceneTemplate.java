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
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.SceneEntityVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.common.dto.ValueTextVo;
import neatlogic.framework.common.util.PageUtil;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.cmdb.utils.ResourceEntityFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = RESOURCECENTER_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListSupportMultipleViewSceneTemplate extends PrivateApiComponentBase {
    @Override
    public String getName() {
        return "资源中心支持创建多张视图的场景模板列表";
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "common.keyword"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, defaultValue = "1", desc = "common.currentpage"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, defaultValue = "20", desc = "common.pagesize")
    })
    @Output({
            @Param(name = "tbodyList", explode = ResourceEntityVo[].class, desc = "common.tbodylist")
    })
    @Description(desc = "资源中心支持创建多张视图的场景模板列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        BasePageVo basePageVo = paramObj.toJavaObject(BasePageVo.class);
        List<ResourceEntityVo> tbodyList = new ArrayList<>();
        List<SceneEntityVo> sceneEntityList = ResourceEntityFactory.getMultipleSceneEntityList();
        if (CollectionUtils.isNotEmpty(sceneEntityList)) {
            String keyword = basePageVo.getKeyword();
            for (SceneEntityVo sceneEntityVo : sceneEntityList) {
                if (StringUtils.isNotBlank(keyword)) {
                    if (!sceneEntityVo.getName().contains(keyword) && !sceneEntityVo.getLabel().contains(keyword)) {
                        continue;
                    }
                }
                ResourceEntityVo resourceEntityVo = new ResourceEntityVo();
                resourceEntityVo.setName(sceneEntityVo.getName());
                resourceEntityVo.setLabel(sceneEntityVo.getLabel());
                List<ValueTextVo> fieldList = ResourceEntityFactory.getFieldListByViewName(sceneEntityVo.getName());
                resourceEntityVo.setFieldList(fieldList);
                tbodyList.add(resourceEntityVo);
            }
            basePageVo.setRowNum(tbodyList.size());
            tbodyList = PageUtil.subList(tbodyList, basePageVo);
        }
        return TableResultUtil.getResult(tbodyList, basePageVo);
    }

    @Override
    public String getToken() {
        return "resourcecenter/suportmultipleview/scenetemplate";
    }
}
