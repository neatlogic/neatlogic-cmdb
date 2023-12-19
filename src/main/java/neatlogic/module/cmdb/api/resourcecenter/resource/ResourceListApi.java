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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB;
import neatlogic.framework.cmdb.crossover.IResourceListApiCrossoverService;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceInfo;
import neatlogic.framework.cmdb.enums.group.GroupType;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceMapper;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import neatlogic.module.cmdb.service.resourcecenter.resource.IResourceCenterResourceService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * 查询资源中心数据列表接口
 *
 * @author linbq
 * @since 2021/5/27 16:14
 **/
@Service
@AuthAction(action = CMDB.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ResourceListApi extends PrivateApiComponentBase implements IResourceListApiCrossoverService {

    @Resource
    private IResourceCenterResourceService resourceCenterResourceService;

    @Resource
    private ResourceMapper resourceMapper;

    @Override
    public String getToken() {
        return "resourcecenter/resource/list";
    }

    @Override
    public String getName() {
        return "查询资源中心数据列表";
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
            @Param(name = "keyword", type = ApiParamType.STRING, xss = true, desc = "模糊搜索"),
            @Param(name = "typeId", type = ApiParamType.LONG, desc = "类型id"),
            @Param(name = "protocolIdList", type = ApiParamType.JSONARRAY, desc = "协议id列表"),
            @Param(name = "stateIdList", type = ApiParamType.JSONARRAY, desc = "状态id列表"),
            @Param(name = "vendorIdList", type = ApiParamType.JSONARRAY, desc = "厂商id列表"),
            @Param(name = "envIdList", type = ApiParamType.JSONARRAY, desc = "环境id列表"),
            @Param(name = "appSystemIdList", type = ApiParamType.JSONARRAY, desc = "应用系统id列表"),
            @Param(name = "appModuleIdList", type = ApiParamType.JSONARRAY, desc = "应用模块id列表"),
            @Param(name = "typeIdList", type = ApiParamType.JSONARRAY, desc = "资源类型id列表"),
            @Param(name = "tagIdList", type = ApiParamType.JSONARRAY, desc = "标签id列表"),
            @Param(name = "inspectStatusList", type = ApiParamType.JSONARRAY, desc = "巡检状态列表"),
            @Param(name = "searchField", type = ApiParamType.STRING, desc = "批量搜索字段"),
            @Param(name = "batchSearchList", type = ApiParamType.JSONARRAY, desc = "批量搜索值"),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "用于回显的资源ID列表"),
            @Param(name = "cmdbGroupType", type = ApiParamType.STRING, desc = "通过团体过滤权限"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = ResourceVo[].class, desc = "数据列表"),
            @Param(name = "errorList", explode = ResourceEntityVo[].class, desc = "数据初始化配置异常信息列表"),
            @Param(name = "unavailableResourceInfoList", explode = ResourceInfo[].class, desc = "数据初始化配置异常信息列表")
    })
    @Description(desc = "查询资源中心数据列表")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        List<ResourceVo> resourceList = new ArrayList<>();
        List<ResourceVo> resultList = new ArrayList<>();
        ResourceSearchVo searchVo;
        JSONArray defaultValue = jsonObj.getJSONArray("defaultValue");
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            searchVo = new ResourceSearchVo();
            searchVo.setDefaultValue(defaultValue);
        } else {
            searchVo = resourceCenterResourceService.assembleResourceSearchVo(jsonObj);
        }
        resourceCenterResourceService.handleBatchSearchList(searchVo);

        int rowNum = resourceMapper.getResourceCount(searchVo);
        if (rowNum == 0) {
            return TableResultUtil.getResult(resourceList, searchVo);
        }
        searchVo.setRowNum(rowNum);
        if (StringUtils.isNotBlank(searchVo.getKeyword())) {
            int ipKeywordCount = resourceMapper.getResourceCountByIpKeyword(searchVo);
            if (ipKeywordCount > 0) {
                searchVo.setIsIpFieldSort(1);
            } else {
                int nameKeywordCount = resourceMapper.getResourceCountByNameKeyword(searchVo);
                if (nameKeywordCount > 0) {
                    searchVo.setIsNameFieldSort(1);
                }
            }
        }
        List<Long> idList = resourceMapper.getResourceIdList(searchVo);
        if (CollectionUtils.isEmpty(idList)) {
            return TableResultUtil.getResult(resourceList, searchVo);
        }
        resourceList = resourceMapper.getResourceListByIdList(idList);
        if (CollectionUtils.isNotEmpty(resourceList)) {
            resourceCenterResourceService.addTagAndAccountInformation(resourceList);
        }

        Set<Long> typeIdList = new HashSet<>();
        List<Long> canDeleteTypeIdList = new ArrayList<>();
        List<Long> canEditTypeIdList = new ArrayList<>();
        //排序
        for (Long id : idList) {
            for (ResourceVo resourceVo : resourceList) {
                if (Objects.equals(id, resourceVo.getId())) {
                    resultList.add(resourceVo);
                    typeIdList.add(resourceVo.getTypeId());
                    break;
                }
            }
        }

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
