/*
 * Copyright (C) 2025  深圳极向量科技有限公司 All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
