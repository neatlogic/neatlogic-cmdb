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
            @Param(name = "preCondition", type = ApiParamType.JSONOBJECT, desc = "前置过滤器"),
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
        JSONObject preCondition = paramObj.getJSONObject("preCondition");
        String cmdbGroupType = paramObj.getString("cmdbGroupType");
        List<String> selectFieldNameList = Arrays.asList("id", "name", "ip", "port");
        List<ResourceVo> nodeList = new ArrayList<>();
        if (preCondition == null) {
            preCondition = new JSONObject();
        }
        ResourceSearchVo searchVo = resourceCenterResourceService.assembleResourceSearchVo(preCondition);
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
