/*
Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License. 
 */

package neatlogic.module.cmdb.api.resourcecenter.resource;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceMapper;
import neatlogic.module.cmdb.service.resourcecenter.resource.IResourceCenterResourceService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListResourceCustomApi extends PrivateApiComponentBase {
    @Resource
    ResourceMapper resourceMapper;

    @Resource
    private IResourceCenterResourceService resourceCenterResourceService;

    @Override
    public String getName() {
        return "高级查询资源中心数据列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "searchMode", type = ApiParamType.STRING, xss = true, desc = "搜索模式：value|text，默认搜索value"),
            @Param(name = "keyword", type = ApiParamType.STRING, xss = true, desc = "模糊搜索"),
            @Param(name = "conditionGroupList", type = ApiParamType.JSONARRAY, desc = "条件组"),
            @Param(name = "conditionGroupRelList", type = ApiParamType.JSONARRAY, desc = "条件组之间的关系"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true"),
            @Param(name = "cmdbGroupType", type = ApiParamType.STRING, desc = "通过团体过滤权限")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = ResourceVo[].class, desc = "数据列表")
    })
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        ResourceSearchVo resourceSearch = resourceCenterResourceService.assembleResourceSearchVo(paramObj);
        List<ResourceVo> resultList = new ArrayList<>();
        StringBuilder sqlSb = new StringBuilder();
        resourceSearch.buildConditionWhereSql(sqlSb, resourceSearch);
        int rowNum = resourceMapper.getResourceCountByDynamicCondition(resourceSearch, sqlSb.toString());
        if (rowNum == 0) {
            return TableResultUtil.getResult(resultList, resourceSearch);
        }
        resourceSearch.setRowNum(rowNum);
        List<Long> idList = resourceMapper.getResourceIdListByDynamicCondition(resourceSearch, sqlSb.toString());
        if (CollectionUtils.isNotEmpty(idList)) {
            List<ResourceVo> resourceList = resourceMapper.getResourceListByIdList(idList);
            if (CollectionUtils.isNotEmpty(resourceList)) {
                resourceCenterResourceService.addTagAndAccountInformation(resourceList);
            }
            //排序
            for (Long id : idList) {
                for (ResourceVo resourceVo : resourceList) {
                    if (Objects.equals(id, resourceVo.getId())) {
                        resultList.add(resourceVo);
                        break;
                    }
                }
            }
        }
        return TableResultUtil.getResult(resultList, resourceSearch);
    }

    @Override
    public String getToken() {
        return "resourcecenter/resource/custom/list";
    }
}
