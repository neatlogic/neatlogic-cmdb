/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.cmdb.api.resourcecenter.resource;

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
                    ResourceSearchVo node = inputNodeObj.toJavaObject(inputNodeObj, ResourceSearchVo.class);
                    searchVo.setIp(node.getIp());
                    searchVo.setPort(node.getPort());
                    searchVo.setName(node.getName());
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
                    ResourceSearchVo node = inputNodeObj.toJavaObject(inputNodeObj, ResourceSearchVo.class);
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
            for (int i = 0; i < inputNodeList.size(); i++) {
                JSONObject inputNodeObj = inputNodeList.getJSONObject(i);
                ResourceSearchVo node = inputNodeObj.toJavaObject(inputNodeObj, ResourceSearchVo.class);
                Long resourceId = resourceMapper.getResourceIdByIpAndPortAndName(node);
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
