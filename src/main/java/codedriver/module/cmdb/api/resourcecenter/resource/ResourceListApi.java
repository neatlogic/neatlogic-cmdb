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
import codedriver.framework.cmdb.dto.resourcecenter.*;
import codedriver.framework.cmdb.dto.tag.TagVo;
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
import java.util.*;
import java.util.stream.Collectors;

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
                    Map<Long, List<AccountVo>> resourceAccountVoMap = new HashMap<>();
                    List<ResourceAccountVo> resourceAccountVoList = resourceCenterMapper.getResourceAccountListByResourceIdList(idList);
                    if (CollectionUtils.isNotEmpty(resourceAccountVoList)) {
                        Set<Long> accountIdSet = resourceAccountVoList.stream().map(ResourceAccountVo::getAccountId).collect(Collectors.toSet());
                        List<AccountVo> accountList = resourceCenterMapper.getAccountListByIdList(new ArrayList<>(accountIdSet));
                        Map<Long, AccountVo> accountMap = accountList.stream().collect(Collectors.toMap(e -> e.getId(), e -> e));
                        for (ResourceAccountVo resourceAccountVo : resourceAccountVoList) {
                            resourceAccountVoMap.computeIfAbsent(resourceAccountVo.getResourceId(), k -> new ArrayList<>()).add(accountMap.get(resourceAccountVo.getAccountId()));
                        }
                    }
                    Map<Long, List<TagVo>> resourceTagVoMap = new HashMap<>();
                    List<ResourceTagVo> resourceTagVoList = resourceCenterMapper.getResourceTagListByResourceIdList(idList);
                    if (CollectionUtils.isNotEmpty(resourceTagVoList)) {
                        Set<Long> tagIdSet = resourceTagVoList.stream().map(ResourceTagVo::getTagId).collect(Collectors.toSet());
                        List<TagVo> tagList = resourceCenterMapper.getTagListByIdList(new ArrayList<>(tagIdSet));
                        Map<Long, TagVo> tagMap = tagList.stream().collect(Collectors.toMap(e -> e.getId(), e -> e));
                        for (ResourceTagVo resourceTagVo : resourceTagVoList) {
                            resourceTagVoMap.computeIfAbsent(resourceTagVo.getResourceId(), k -> new ArrayList<>()).add(tagMap.get(resourceTagVo.getTagId()));
                        }
                    }
                    Map<Long, ResourceScriptVo> resourceScriptVoMap = new HashMap<>();
                    List<ResourceScriptVo> resourceScriptVoList = resourceCenterMapper.getResourceScriptListByResourceIdList(idList);
                    if (CollectionUtils.isNotEmpty(resourceScriptVoList)) {
                        for (ResourceScriptVo resourceScriptVo : resourceScriptVoList) {
                            resourceScriptVoMap.put(resourceScriptVo.getResourceId(), resourceScriptVo);
                        }
                    }

                    resourceVoList = resourceCenterMapper.getResourceListByIdList(idList, TenantContext.get().getDataDbName());
                    for (ResourceVo resourceVo : resourceVoList) {
                        List<TagVo> tagVoList = resourceTagVoMap.get(resourceVo.getId());
                        if (CollectionUtils.isNotEmpty(tagVoList)) {
                            resourceVo.setTagList(tagVoList.stream().map(TagVo::getName).collect(Collectors.toList()));
                        }
                        List<AccountVo> accountVoList = resourceAccountVoMap.get(resourceVo.getId());
                        if (CollectionUtils.isNotEmpty(accountVoList)) {
                            resourceVo.setAccountList(accountVoList);
                        }
                        ResourceScriptVo scriptVo = resourceScriptVoMap.get(resourceVo.getId());
                        resourceVo.setScript(scriptVo);
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
