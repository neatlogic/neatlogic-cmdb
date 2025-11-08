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

package neatlogic.module.cmdb.api.customview;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CUSTOMVIEW_MODIFY;
import neatlogic.framework.cmdb.dto.customview.CustomViewVo;
import neatlogic.framework.cmdb.enums.CmdbFromType;
import neatlogic.framework.cmdb.enums.customview.CustomViewType;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.dependency.core.DependencyManager;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.cmdb.service.customview.CustomViewService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AuthAction(action = CUSTOMVIEW_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchPublicCustomViewApi extends PrivateApiComponentBase {

    @Resource
    private CustomViewService customViewService;

    @Override
    public String getName() {
        return "查询公共自定义视图";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "keyword", type = ApiParamType.STRING, desc = "关键字", xss = true),
            @Param(name = "tagId", type = ApiParamType.LONG, desc = "标签"),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "回显值"),
            @Param(name = "currentPage",
                    type = ApiParamType.INTEGER,
                    desc = "当前页"),
            @Param(name = "pageSize",
                    type = ApiParamType.INTEGER,
                    desc = "每页数据条目"),
            @Param(name = "needPage",
                    type = ApiParamType.BOOLEAN,
                    desc = "是否需要分页，默认true")
    })
    @Output({@Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = CustomViewVo[].class)})
    @Description(desc = "查询公共自定义视图接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        CustomViewVo customViewVo = JSON.toJavaObject(paramObj, CustomViewVo.class);
        customViewVo.setType(CustomViewType.PUBLIC.getValue());
        List<CustomViewVo> viewList = customViewService.searchCustomView(customViewVo);
        if (CollectionUtils.isNotEmpty(viewList)) {
            List<Long> idList = viewList.stream().map(CustomViewVo::getId).collect(Collectors.toList());
            Map<Object, Integer> countMap = DependencyManager.getBatchDependencyCount(CmdbFromType.CMDBCUSTOMVIEW, idList);
            for (CustomViewVo customView : viewList) {
                Integer count = countMap.get(customView.getId());
                if (count == null) {
                    count = 0;
                }
                customView.setReferenceCount(count);
            }
        }
        return TableResultUtil.getResult(viewList, customViewVo);
    }

    @Override
    public String getToken() {
        return "/cmdb/customview/public/search";
    }
}
