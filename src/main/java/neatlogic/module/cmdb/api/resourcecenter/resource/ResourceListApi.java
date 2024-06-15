/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityConfigVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityFieldMappingVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import neatlogic.framework.cmdb.enums.group.GroupType;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceEntityMapper;
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
public class ResourceListApi extends PrivateApiComponentBase {

    @Resource
    private IResourceCenterResourceService resourceCenterResourceService;

    @Resource
    private ResourceMapper resourceMapper;

    @Resource
    private ResourceEntityMapper resourceEntityMapper;

    @Resource
    private AttrMapper attrMapper;

    @Resource
    private CiMapper ciMapper;

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
        ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName("scence_ipobject_detail");
        if (resourceEntityVo != null) {
            ResourceEntityConfigVo config = resourceEntityVo.getConfig();
            if (config != null) {
                List<ResourceEntityFieldMappingVo> mappingList = config.getFieldMappingList();
                if (CollectionUtils.isNotEmpty(mappingList)) {
                    Long nameAttrId = null;
                    Long ipAttrId = null;
                    for (ResourceEntityFieldMappingVo mappingVo : mappingList) {
                        if (Objects.equals(mappingVo.getField(), "name")) {
                            CiVo ciVo = ciMapper.getCiByName(mappingVo.getFromCi());
                            if (ciVo != null) {
                                AttrVo attr = attrMapper.getAttrByCiIdAndName(ciVo.getId(), mappingVo.getFromAttr());
                                if (attr != null) {
                                    nameAttrId = attr.getId();
                                }
                            }
                        } else if (Objects.equals(mappingVo.getField(), "ip")) {
                            CiVo ciVo = ciMapper.getCiByName(mappingVo.getFromCi());
                            if (ciVo != null) {
                                AttrVo attr = attrMapper.getAttrByCiIdAndName(ciVo.getId(), mappingVo.getFromAttr());
                                if (attr != null) {
                                    ipAttrId = attr.getId();
                                }
                            }
                        }
                        if (nameAttrId != null && ipAttrId != null) {
                            break;
                        }
                    }
                    searchVo.setIpFieldAttrId(ipAttrId);
                    searchVo.setNameFieldAttrId(nameAttrId);
                }
            }
        }
        if (Objects.equals(searchVo.getRowNum(), 0)) {
            int rowNum = 0;
            if (noFilterCondition(searchVo)) {
                rowNum = resourceMapper.getAllResourceCount(searchVo);
            } else {
                rowNum = resourceMapper.getResourceCount(searchVo);
            }
            if (rowNum == 0) {
                return TableResultUtil.getResult(resourceList, searchVo);
            }
            searchVo.setRowNum(rowNum);
        }
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

    /**
     * 判断是否有过滤条件
     * @param searchVo
     * @return
     */
    private boolean noFilterCondition(ResourceSearchVo searchVo) {
        if (StringUtils.isNotBlank(searchVo.getKeyword())) {
            return false;
        }
        if (CollectionUtils.isNotEmpty(searchVo.getBatchSearchList())) {
            return false;
        }
        if (CollectionUtils.isNotEmpty(searchVo.getStateIdList())) {
            return false;
        }
        if (CollectionUtils.isNotEmpty(searchVo.getVendorIdList())) {
            return false;
        }
        if (CollectionUtils.isNotEmpty(searchVo.getEnvIdList())) {
            return false;
        }
        if (searchVo.getExistNoEnv()) {
            return false;
        }
        if (CollectionUtils.isNotEmpty(searchVo.getAppSystemIdList())) {
            return false;
        }
        if (CollectionUtils.isNotEmpty(searchVo.getAppModuleIdList())) {
            return false;
        }
        if (CollectionUtils.isNotEmpty(searchVo.getDefaultValue())) {
            return false;
        }
        if (CollectionUtils.isNotEmpty(searchVo.getIdList())) {
            return false;
        }
        if (CollectionUtils.isNotEmpty(searchVo.getInspectStatusList())) {
            return false;
        }
        if (CollectionUtils.isNotEmpty(searchVo.getProtocolIdList())) {
            return false;
        }
        if (CollectionUtils.isNotEmpty(searchVo.getTagIdList())) {
            return false;
        }
        if (CollectionUtils.isNotEmpty(searchVo.getInspectJobPhaseNodeStatusList())) {
            return false;
        }
        return true;
    }

}
