/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dto.condition.ConditionGroupRelVo;
import neatlogic.framework.dto.condition.ConditionGroupVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceMapper;
import neatlogic.module.cmdb.service.resourcecenter.resource.IResourceCenterResourceService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = CMDB.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class CheckResourceInputNodeListApi extends PrivateApiComponentBase {

    @Resource
    private ResourceMapper resourceMapper;

    @Resource
    private IResourceCenterResourceService resourceCenterResourceService;

    @Override
    public String getName() {
        return "检查输入节点列表的节点是否合法";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "filter", type = ApiParamType.JSONOBJECT, desc = "过滤条件", help="简单过滤条件和高级过滤条件都用这个字段"),
            @Param(name = "cmdbGroupType", type = ApiParamType.STRING, desc = "通过团体过滤权限"),
            @Param(name = "inputNodeList", type = ApiParamType.JSONARRAY, isRequired = true, minSize = 1, desc = "输入节点列表"),
    })
    @Output({
            @Param(name = "existList", type = ApiParamType.INTEGER, desc = "存在的资源列表"),
            @Param(name = "nonExistList", type = ApiParamType.JSONARRAY, desc = "不存在的资源列表")
    })
    @Description(desc = "检查输入节点列表的节点是否合法")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONArray existList = new JSONArray();
        JSONArray nonExistList = new JSONArray();
        JSONArray inputNodeList = paramObj.getJSONArray("inputNodeList");
        JSONObject filter = paramObj.getJSONObject("filter");
        String cmdbGroupType = paramObj.getString("cmdbGroupType");
        if (MapUtils.isNotEmpty(filter)) {
            // 判断过滤条件是简单模式还是高级模式
            if (filter.containsKey("conditionGroupList")) {
                // 高级模式
                ResourceSearchVo searchVo = new ResourceSearchVo();
                JSONArray conditionGroupArray = filter.getJSONArray("conditionGroupList");
                if (CollectionUtils.isNotEmpty(conditionGroupArray)) {
                    List<ConditionGroupVo> conditionGroupList = conditionGroupArray.toJavaList(ConditionGroupVo.class);
                    searchVo.setConditionGroupList(conditionGroupList);
                }
                JSONArray conditionGroupRelArray = filter.getJSONArray("conditionGroupRelList");
                if (CollectionUtils.isNotEmpty(conditionGroupRelArray)) {
                    List<ConditionGroupRelVo> conditionGroupRelList = conditionGroupRelArray.toJavaList(ConditionGroupRelVo.class);
                    searchVo.setConditionGroupRelList(conditionGroupRelList);
                }
                StringBuilder sqlSb = new StringBuilder();
                searchVo.buildConditionWhereSql(sqlSb, searchVo);
                searchVo.setPageSize(1);
                for (int i = 0; i < inputNodeList.size(); i++) {
                    JSONObject inputNodeObj = inputNodeList.getJSONObject(i);
                    ResourceSearchVo node = JSON.toJavaObject(inputNodeObj, ResourceSearchVo.class);
                    searchVo.setCmdbGroupType(cmdbGroupType);
                    searchVo.setIp(node.getIp());
                    searchVo.setPort(node.getPort());
                    searchVo.setName(node.getName());
                    searchVo.setCmdbGroupType(cmdbGroupType);
                    List<Long> idList =  resourceMapper.getResourceIdListByDynamicCondition(searchVo, sqlSb.toString());
                    if (CollectionUtils.isEmpty(idList)) {
                        nonExistList.add(inputNodeObj);
                    } else {
                        existList.add(inputNodeObj);
                    }
                }
            } else {
                // 简单模式
                ResourceSearchVo searchVo = resourceCenterResourceService.assembleResourceSearchVo(filter);
                for (int i = 0; i < inputNodeList.size(); i++) {
                    JSONObject inputNodeObj = inputNodeList.getJSONObject(i);
                    ResourceSearchVo node = JSON.toJavaObject(inputNodeObj, ResourceSearchVo.class);
                    searchVo.setCmdbGroupType(cmdbGroupType);
                    searchVo.setIp(node.getIp());
                    searchVo.setPort(node.getPort());
                    searchVo.setName(node.getName());
                    Long resourceId = resourceMapper.getResourceIdByIpAndPortAndNameWithFilter(searchVo);
                    if (resourceId == null) {
                        nonExistList.add(inputNodeObj);
                    } else {
                        existList.add(inputNodeObj);
                    }
                }
            }
        } else {
            // 没有过滤条件
            ResourceSearchVo searchVo = resourceCenterResourceService.assembleResourceSearchVo(new JSONObject());
            for (int i = 0; i < inputNodeList.size(); i++) {
                JSONObject inputNodeObj = inputNodeList.getJSONObject(i);
                ResourceSearchVo node = JSON.toJavaObject(inputNodeObj, ResourceSearchVo.class);
                searchVo.setCmdbGroupType(cmdbGroupType);
                searchVo.setIp(node.getIp());
                searchVo.setPort(node.getPort());
                searchVo.setName(node.getName());
                Long resourceId = resourceMapper.getResourceIdByIpAndPortAndName(searchVo);
                if (resourceId == null) {
                    nonExistList.add(inputNodeObj);
                } else {
                    existList.add(inputNodeObj);
                }
            }
        }
        JSONObject resultObj = new JSONObject();
        resultObj.put("existList", existList);
        resultObj.put("nonExistList", nonExistList);
        return resultObj;
    }

    @Override
    public String getToken() {
        return "resourcecenter/resource/inputnodelist/check";
    }
}
