/*Copyright (C) 2023  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

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
