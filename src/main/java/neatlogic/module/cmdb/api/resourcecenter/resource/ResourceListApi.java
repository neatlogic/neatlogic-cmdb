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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.enums.group.GroupType;
import neatlogic.framework.cmdb.resourcecenter.datasource.core.IResourceCenterDataSource;
import neatlogic.framework.cmdb.resourcecenter.datasource.core.ResourceCenterDataSourceFactory;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import neatlogic.module.cmdb.service.resourcecenter.resource.IResourceCenterResourceService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 查询资源中心数据列表接口
 *
 * @author linbq
 * @since 2021/5/27 16:14
 **/
@Service
@AuthAction(action = CMDB.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ResourceListApi extends PrivateApiComponentBase {

    @Resource
    private IResourceCenterResourceService resourceCenterResourceService;

    @Override
    public String getToken() {
        return "resourcecenter/resource/list";
    }

    @Override
    public String getName() {
        return "nmcarr.resourcelistapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public boolean disableReturnCircularReferenceDetect() {
        return true;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, xss = true, desc = "common.keyword"),
            @Param(name = "typeId", type = ApiParamType.LONG, desc = "common.typeid"),
            @Param(name = "protocolIdList", type = ApiParamType.JSONARRAY, desc = "term.cmdb.protocolidlist"),
            @Param(name = "stateIdList", type = ApiParamType.JSONARRAY, desc = "term.cmdb.stateidlist"),
            @Param(name = "vendorIdList", type = ApiParamType.JSONARRAY, desc = "term.cmdb.vendoridlist"),
            @Param(name = "envIdList", type = ApiParamType.JSONARRAY, desc = "term.cmdb.envidlist"),
            @Param(name = "appSystemIdList", type = ApiParamType.JSONARRAY, desc = "term.appsystemidlist"),
            @Param(name = "appModuleIdList", type = ApiParamType.JSONARRAY, desc = "term.cmdb.appmoduleidlist"),
            @Param(name = "typeIdList", type = ApiParamType.JSONARRAY, desc = "term.cmdb.typeidlist"),
            @Param(name = "tagIdList", type = ApiParamType.JSONARRAY, desc = "common.tagidlist"),
            @Param(name = "inspectStatusList", type = ApiParamType.JSONARRAY, desc = "term.inspect.inspectstatuslist"),
            @Param(name = "searchField", type = ApiParamType.STRING, desc = "term.cmdb.searchfield"),
            @Param(name = "batchSearchList", type = ApiParamType.JSONARRAY, desc = "term.cmdb.batchsearchlist"),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "common.defaultvalue"),
            @Param(name = "cmdbGroupType", type = ApiParamType.STRING, desc = "term.cmdb.cmdbgrouptype"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "common.currentpage"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "common.pagesize"),
            @Param(name = "rowNum", type = ApiParamType.INTEGER, desc = "common.rownum"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "common.isneedpage")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = ResourceVo[].class, desc = "common.tbodylist")
    })
    @Description(desc = "nmcarr.resourcelistapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        ResourceSearchVo searchVo;
        ResourceSearchVo preCondition = null;
        JSONArray defaultValue = jsonObj.getJSONArray("defaultValue");
        //先处理前置过滤器
        if (jsonObj.containsKey("preCondition")) {
            preCondition = resourceCenterResourceService.assembleResourceSearchVo(jsonObj.getJSONObject("preCondition"));
        }
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            searchVo = new ResourceSearchVo();
            searchVo.setDefaultValue(defaultValue);
        } else {
            searchVo = resourceCenterResourceService.assembleResourceSearchVo(jsonObj);
        }
        searchVo.setPreCondition(preCondition);
//        resourceCenterResourceService.handleBatchSearchList(searchVo);
//        resourceCenterResourceService.setIpFieldAttrIdAndNameFieldAttrId(searchVo);
        IResourceCenterDataSource resourceCenterDataSource = ResourceCenterDataSourceFactory.getResourceCenterDataSource();
        List<ResourceVo> resultList = resourceCenterDataSource.getResourceList(searchVo);
        if (CollectionUtils.isNotEmpty(resultList)) {
            resourceCenterResourceService.addTagAndAccountInformation(resultList);
        }

        Set<Long> typeIdList = resultList.stream().map(ResourceVo::getTypeId).collect(Collectors.toSet());
        List<Long> canDeleteTypeIdList = new ArrayList<>();
        List<Long> canEditTypeIdList = new ArrayList<>();

        //补充配置项权限
        Set<Long> withoutCiAuthCiEntityList = new HashSet<>();
        for (Long typeId : typeIdList) {
            if (CiAuthChecker.chain().checkCiEntityUpdatePrivilege(typeId).check()) {
                canEditTypeIdList.add(typeId);
            }
            if (CiAuthChecker.chain().checkCiEntityDeletePrivilege(typeId).check()) {
                canDeleteTypeIdList.add(typeId);
            }
        }
        //模型权限
        for (ResourceVo resourceVo : resultList) {
            if (canEditTypeIdList.contains(resourceVo.getTypeId())) {
                resourceVo.setIsCanEdit(true);
            } else {
                withoutCiAuthCiEntityList.add(resourceVo.getId());
            }
            if (canDeleteTypeIdList.contains(resourceVo.getTypeId())) {
                resourceVo.setIsCanDelete(true);
            } else {
                withoutCiAuthCiEntityList.add(resourceVo.getId());
            }
        }
        //团体权限
        List<Long> hasMaintainCiEntityIdList = CiAuthChecker.isCiEntityInGroup(new ArrayList<>(withoutCiAuthCiEntityList), GroupType.MAINTAIN);
        for (ResourceVo resourceVo : resultList) {
            if (hasMaintainCiEntityIdList.contains(resourceVo.getId())) {
                resourceVo.setIsCanEdit(true);
                resourceVo.setIsCanDelete(true);
            }
        }
        return TableResultUtil.getResult(resultList, searchVo);
    }

}
