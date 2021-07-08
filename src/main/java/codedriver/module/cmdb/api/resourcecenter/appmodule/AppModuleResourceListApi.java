/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.resourcecenter.appmodule;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceTypeVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.cmdb.exception.ci.CiNotFoundException;
import codedriver.framework.cmdb.exception.resourcecenter.AppModuleNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.util.PageUtil;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author linbq
 * @since 2021/6/17 11:54
 **/
@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AppModuleResourceListApi extends PrivateApiComponentBase {

    @Resource
    private CiMapper ciMapper;

    @Resource
    private ResourceCenterMapper resourceCenterMapper;

    @Override
    public String getToken() {
        return "resourcecenter/appmodule/resource/list";
    }

    @Override
    public String getName() {
        return "查询应用模块中资源列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appModuleId", type = ApiParamType.LONG, isRequired = true, desc = "应用模块id"),
            @Param(name = "envId", type = ApiParamType.LONG, isRequired = true, desc = "环境id"),
            @Param(name = "typeId", type = ApiParamType.LONG, desc = "类型id"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(name = "tableList", type = ApiParamType.JSONARRAY, desc = "资源环境列表")
    })
    @Description(desc = "查询资源环境列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONArray tableList = new JSONArray();
        String schemaName = TenantContext.get().getDataDbName();
        Long appModuleId = paramObj.getLong("appModuleId");
        if (resourceCenterMapper.checkAppModuleIsExists(appModuleId, schemaName) == 0) {
            throw new AppModuleNotFoundException(appModuleId);
        }

        List<String> resourceTypeList = new ArrayList<>();
        resourceTypeList.add("OS");
        resourceTypeList.add("Storage");
        resourceTypeList.add("NetDev");
        resourceTypeList.add("APPIns");
        resourceTypeList.add("APPInsCluster");
        resourceTypeList.add("DBIns");
        resourceTypeList.add("DBCluster");
        List<CiVo> resourceCiVoList = new ArrayList<>();
        Map<Long, CiVo> ciVoMap = new HashMap<>();
        List<CiVo> ciVoList = ciMapper.getAllCi();
        for (CiVo ciVo : ciVoList) {
            ciVoMap.put(ciVo.getId(), ciVo);
            if (resourceTypeList.contains(ciVo.getName())) {
                resourceCiVoList.add(ciVo);
            }
        }
        List<ResourceTypeVo> resourceTypeVoList = null;
        Long envId = paramObj.getLong("envId");
        Long typeId = paramObj.getLong("typeId");
        if (typeId != null) {
            CiVo ciVo = ciVoMap.get(typeId);
            if (ciVo == null) {
                throw new CiNotFoundException(typeId);
            }
            resourceTypeVoList = new ArrayList<>();
            resourceTypeVoList.add(new ResourceTypeVo(ciVo.getId(), ciVo.getParentCiId(), ciVo.getLabel(), ciVo.getName()));
        } else {
            resourceTypeVoList = resourceCenterMapper.getResourceTypeListByAppModuleIdAndEnvId(appModuleId, envId, schemaName);
        }

        if (CollectionUtils.isNotEmpty(resourceTypeVoList)) {
            ResourceSearchVo searchVo = new ResourceSearchVo();
            searchVo.setAppModuleId(appModuleId);
            searchVo.setEnvId(envId);
            Integer currentPage = paramObj.getInteger("currentPage");
            if (currentPage != null) {
                searchVo.setCurrentPage(currentPage);
            }
            Integer pageSize = paramObj.getInteger("pageSize");
            if (pageSize != null) {
                searchVo.setPageSize(pageSize);
            }
            Boolean needPage = paramObj.getBoolean("needPage");
            if (needPage != null) {
                searchVo.setNeedPage(needPage);
            }
            for (ResourceTypeVo resourceTypeVo : resourceTypeVoList) {
                Long resourceTypeId = resourceTypeVo.getId();
                CiVo ciVo = ciVoMap.get(resourceTypeId);
                if (ciVo == null) {
                    throw new CiNotFoundException(resourceTypeId);
                }
                String resourceTypeName = getResourceTypeName(resourceCiVoList, ciVo);
                if (StringUtils.isBlank(resourceTypeName)) {
                    continue;
                }
                searchVo.setTypeId(resourceTypeId);
                List<ResourceVo> tbodyList = getTbodyList(searchVo, resourceTypeName);
                JSONObject tableObj = new JSONObject();
                tableObj.put("tbodyList", tbodyList);
                tableObj.put("type", resourceTypeVo);
                tableObj.put("rowNum", searchVo.getRowNum());
                tableObj.put("pageCount", PageUtil.getPageCount(searchVo.getRowNum(), pageSize));
                tableObj.put("currentPage", searchVo.getCurrentPage());
                tableObj.put("pageSize", searchVo.getPageSize());
                tableList.add(tableObj);
            }
        }
        JSONObject resultObj = new JSONObject();
        resultObj.put("tableList", tableList);
        return resultObj;
    }

    public String getResourceTypeName(List<CiVo> resourceCiVoList, CiVo resourceCiVo){
        for (CiVo ciVo : resourceCiVoList) {
            if (ciVo.getLft() <= resourceCiVo.getLft() && ciVo.getRht() >= resourceCiVo.getRht()) {
                return ciVo.getName();
            }
        }
        return null;
    }
    public List<ResourceVo> getTbodyList(ResourceSearchVo searchVo, String resourceTypeName) {
        if ("OS".equals(resourceTypeName)) {
            int rowNum = resourceCenterMapper.getOsResourceCount(searchVo);
            if (rowNum > 0) {
                searchVo.setRowNum(rowNum);
                List<Long> idList = resourceCenterMapper.getOsResourceIdList(searchVo);
                if (CollectionUtils.isNotEmpty(idList)) {
                    return resourceCenterMapper.getOsResourceListByIdList(idList, TenantContext.get().getDataDbName());
                }
            }
        } else if ("Storage".equals(resourceTypeName)) {
            int rowNum = resourceCenterMapper.getStorageResourceCount(searchVo);
            if (rowNum > 0) {
                searchVo.setRowNum(rowNum);
                List<Long> idList = resourceCenterMapper.getStorageResourceIdList(searchVo);
                if (CollectionUtils.isNotEmpty(idList)) {
                    return resourceCenterMapper.getStorageResourceListByIdList(idList, TenantContext.get().getDataDbName());
                }
            }
        } else if ("NetDev".equals(resourceTypeName)) {
            int rowNum = resourceCenterMapper.getNetDevResourceCount(searchVo);
            if (rowNum > 0) {
                searchVo.setRowNum(rowNum);
                List<Long> idList = resourceCenterMapper.getNetDevResourceIdList(searchVo);
                if (CollectionUtils.isNotEmpty(idList)) {
                    return resourceCenterMapper.getNetDevResourceListByIdList(idList, TenantContext.get().getDataDbName());
                }
            }
        } else if ("APPIns".equals(resourceTypeName)) {
            int rowNum = resourceCenterMapper.getAppInstanceResourceCount(searchVo);
            if (rowNum > 0) {
                searchVo.setRowNum(rowNum);
                List<Long> idList = resourceCenterMapper.getAppInstanceResourceIdList(searchVo);
                if (CollectionUtils.isNotEmpty(idList)) {
                    return resourceCenterMapper.getAppInstanceResourceListByIdList(idList, TenantContext.get().getDataDbName());
                }
            }
        } else if ("APPInsCluster".equals(resourceTypeName)) {
            int rowNum = resourceCenterMapper.getAppInstanceClusterResourceCount(searchVo);
            if (rowNum > 0) {
                searchVo.setRowNum(rowNum);
                List<Long> idList = resourceCenterMapper.getAppInstanceClusterResourceIdList(searchVo);
                if (CollectionUtils.isNotEmpty(idList)) {
                    return resourceCenterMapper.getAppInstanceClusterResourceListByIdList(idList, TenantContext.get().getDataDbName());
                }
            }
        } else if ("DBIns".equals(resourceTypeName)) {
            int rowNum = resourceCenterMapper.getDbInstanceResourceCount(searchVo);
            if (rowNum > 0) {
                searchVo.setRowNum(rowNum);
                List<Long> idList = resourceCenterMapper.getDbInstanceResourceIdList(searchVo);
                if (CollectionUtils.isNotEmpty(idList)) {
                    return resourceCenterMapper.getDbInstanceResourceListByIdList(idList, TenantContext.get().getDataDbName());
                }
            }
        } else if ("DBCluster".equals(resourceTypeName)) {
            int rowNum = resourceCenterMapper.getDbInstanceClusterResourceCount(searchVo);
            if (rowNum > 0) {
                searchVo.setRowNum(rowNum);
                List<Long> idList = resourceCenterMapper.getDbInstanceClusterResourceIdList(searchVo);
                if (CollectionUtils.isNotEmpty(idList)) {
                    return resourceCenterMapper.getDbInstanceClusterResourceListByIdList(idList, TenantContext.get().getDataDbName());
                }
            }
        }
        return new ArrayList<>();
    }
}
