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
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceMapper;
import neatlogic.module.cmdb.service.resourcecenter.resource.IResourceCenterResourceService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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
            @Param(name = "filter", type = ApiParamType.JSONOBJECT, desc = "过滤条件", help = "简单过滤条件和高级过滤条件都用这个字段"),
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
        JSONObject filter = paramObj.getJSONObject("preCondition");
        String cmdbGroupType = paramObj.getString("cmdbGroupType");
        List<String> selectFieldNameList = Arrays.asList("id", "name", "ip", "port");
        List<ResourceVo> nodeList = new ArrayList<>();
        ResourceSearchVo searchVo = resourceCenterResourceService.assembleResourceSearchVo(filter);
        searchVo.setCmdbGroupType(cmdbGroupType);
        for (int i = 0; i < inputNodeList.size(); i++) {
            JSONObject inputNodeObj = inputNodeList.getJSONObject(i);
            ResourceVo node = JSON.toJavaObject(inputNodeObj, ResourceVo.class);
            nodeList.add(node);
            if (nodeList.size() > 100) {
                searchVo.setInputNodeList(nodeList);
                searchVo.setPageSize(500);
                List<ResourceVo> resourceList = new ArrayList<>();
                List<Long> idList = resourceCenterResourceService.getResourceIdList(searchVo);
                if (CollectionUtils.isNotEmpty(idList)) {
                    resourceList = resourceCenterResourceService.getResourceListByIdList(idList, selectFieldNameList);
                }
                existsOrNot(nodeList, resourceList, existList, nonExistList);
                nodeList.clear();
            }
        }
        if (CollectionUtils.isNotEmpty(nodeList)) {
            searchVo.setInputNodeList(nodeList);
            searchVo.setPageSize(500);
            List<ResourceVo> resourceList = new ArrayList<>();
            List<Long> idList = resourceCenterResourceService.getResourceIdList(searchVo);
            if (CollectionUtils.isNotEmpty(idList)) {
                resourceList = resourceCenterResourceService.getResourceListByIdList(idList, selectFieldNameList);
            }
            existsOrNot(nodeList, resourceList, existList, nonExistList);
            nodeList.clear();
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

    private void existsOrNot(List<ResourceVo> nodeList, List<ResourceVo> resourceList, JSONArray existList, JSONArray nonExistList) {
        for (ResourceVo node : nodeList) {
            boolean flag = false;
            for (ResourceVo resourceVo : resourceList) {
                if (Objects.equals(node.getIp(), resourceVo.getIp())
                        && Objects.equals(node.getPort(), resourceVo.getPort())
                        && (node.getName() == null || Objects.equals(node.getName(), resourceVo.getName()))
                ) {
                    flag = true;
                    break;
                }
            }
            JSONObject inputNodeObj = new JSONObject();
            inputNodeObj.put("ip", node.getIp());
            inputNodeObj.put("port", node.getPort());
            inputNodeObj.put("name", node.getName());
            if (flag) {
                if (!existList.contains(inputNodeObj)) {
                    existList.add(inputNodeObj);
                }
            } else {
                if (!nonExistList.contains(inputNodeObj)) {
                    nonExistList.add(inputNodeObj);
                }
            }
        }
    }
}
