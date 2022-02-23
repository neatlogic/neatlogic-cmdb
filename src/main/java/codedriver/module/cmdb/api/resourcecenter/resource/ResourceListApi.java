/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.resourcecenter.resource;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.framework.cmdb.crossover.IResourceListApiCrossoverService;
import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceScriptVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.service.resourcecenter.resource.IResourceCenterResourceService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 查询资源中心数据列表接口
 *
 * @author linbq
 * @since 2021/5/27 16:14
 **/
@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ResourceListApi extends PrivateApiComponentBase implements IResourceListApiCrossoverService {

    @Resource
    private ResourceCenterMapper resourceCenterMapper;

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private IResourceCenterResourceService resourceCenterResourceService;

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
            @Param(name = "envIdList", type = ApiParamType.JSONARRAY, desc = "环境id列表"),
            @Param(name = "appSystemIdList", type = ApiParamType.JSONARRAY, desc = "应用系统id列表"),
            @Param(name = "appModuleIdList", type = ApiParamType.JSONARRAY, desc = "应用模块id列表"),
            @Param(name = "typeIdList", type = ApiParamType.JSONARRAY, desc = "资源类型id列表"),
            @Param(name = "tagIdList", type = ApiParamType.JSONARRAY, desc = "标签id列表"),
            @Param(name = "inspectStatusList", type = ApiParamType.JSONARRAY, desc = "巡检状态列表"),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "用于回显的资源ID列表"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = ResourceVo[].class, desc = "数据列表")
    })
    @Description(desc = "查询资源中心数据列表")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject resultObj = new JSONObject();
        List<ResourceVo> resourceVoList = null;
        ResourceSearchVo searchVo;
        JSONArray defaultValue = jsonObj.getJSONArray("defaultValue");
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            searchVo = new ResourceSearchVo();
            searchVo.setIdList(defaultValue.toJavaList(Long.class));
        } else {
            searchVo = resourceCenterResourceService.assembleResourceSearchVo(jsonObj);
        }
        if (searchVo.getIdList() == null || CollectionUtils.isNotEmpty(searchVo.getIdList())) {
            int rowNum = resourceCenterMapper.getResourceCount(searchVo);
            if (rowNum > 0) {
                searchVo.setRowNum(rowNum);
                List<Long> idList = resourceCenterMapper.getResourceIdList(searchVo);
                if (CollectionUtils.isNotEmpty(idList)) {
                    resourceVoList = resourceCenterMapper.getResourceListByIdList(idList, TenantContext.get().getDataDbName());
                    if (CollectionUtils.isNotEmpty(resourceVoList)) {
                        resourceCenterResourceService.addResourceAccount(idList, resourceVoList);
                        resourceCenterResourceService.addResourceTag(idList, resourceVoList);
                        Map<Long, ResourceScriptVo> resourceScriptVoMap = new HashMap<>();
                        List<ResourceScriptVo> resourceScriptVoList = resourceCenterMapper.getResourceScriptListByResourceIdList(idList);
                        if (CollectionUtils.isNotEmpty(resourceScriptVoList)) {
                            for (ResourceScriptVo resourceScriptVo : resourceScriptVoList) {
                                resourceScriptVoMap.put(resourceScriptVo.getResourceId(), resourceScriptVo);
                            }
                        }
                        for (ResourceVo resourceVo : resourceVoList) {
                            ResourceScriptVo scriptVo = resourceScriptVoMap.get(resourceVo.getId());
                            resourceVo.setScript(scriptVo);
                        }
                    }

                }
            }
        }

        if (resourceVoList == null) {
            resourceVoList = new ArrayList<>();
        }
        resultObj.put("tbodyList", resourceVoList);
        resultObj.put("rowNum", searchVo.getRowNum());
        resultObj.put("pageCount", searchVo.getPageCount());
        resultObj.put("currentPage", searchVo.getCurrentPage());
        resultObj.put("pageSize", searchVo.getPageSize());
        return resultObj;
    }
}
