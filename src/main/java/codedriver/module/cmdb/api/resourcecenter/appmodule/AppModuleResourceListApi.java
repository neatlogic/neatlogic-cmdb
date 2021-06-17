/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.resourcecenter.appmodule;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceTypeVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.cmdb.exception.resourcecenter.AppModuleNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author linbq
 * @since 2021/6/17 11:54
 **/
@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AppModuleResourceListApi extends PrivateApiComponentBase {

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
            @Param(name = "envId", type = ApiParamType.LONG, isRequired = true, desc = "环境id")
    })
    @Output({
            @Param(name = "resourceTypeList", type = ApiParamType.JSONARRAY, desc = "资源类型列表"),
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
        Long envId = paramObj.getLong("envId");
        List<ResourceTypeVo> resourceTypeList = resourceCenterMapper.getResourceTypeListByAppModuleIdAndEnvId(appModuleId, envId, schemaName);
        if (CollectionUtils.isNotEmpty(resourceTypeList)) {
            ResourceSearchVo searchVo = new ResourceSearchVo();
            searchVo.setAppModuleId(appModuleId);
            searchVo.setEnvId(envId);
            for (ResourceTypeVo resourceTypeVo : resourceTypeList) {
                List<ResourceVo> resourceVoList = null;
                searchVo.setTypeId(resourceTypeVo.getId());
                int rowNum = resourceCenterMapper.getResourceCount(searchVo);
                if (rowNum > 0) {
                    searchVo.setRowNum(rowNum);
                    List<Long> idList = resourceCenterMapper.getResourceIdList(searchVo);
                    if (CollectionUtils.isNotEmpty(idList)) {
                        resourceVoList = resourceCenterMapper.getResourceListByIdList(idList, TenantContext.get().getDataDbName());
                    }
                }
                if (resourceVoList == null) {
                    resourceVoList = new ArrayList<>();
                }
                JSONObject tableObj = new JSONObject();
                tableObj.put("tbodyList", resourceVoList);
                tableObj.put("rowNum", rowNum);
                tableObj.put("pageCount", searchVo.getPageCount());
                tableObj.put("currentPage", searchVo.getCurrentPage());
                tableObj.put("pageSize", searchVo.getPageSize());
                tableList.add(tableObj);
            }
        }
        JSONObject resultObj = new JSONObject();
        resultObj.put("resourceTypeList", resourceTypeList);
        resultObj.put("tableList", tableList);
        return resultObj;
    }
}
