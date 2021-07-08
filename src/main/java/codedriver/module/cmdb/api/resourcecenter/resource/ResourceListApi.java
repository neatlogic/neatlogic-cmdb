/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.resourcecenter.resource;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.resourcecenter.*;
import codedriver.framework.cmdb.dto.tag.TagVo;
import codedriver.framework.cmdb.exception.ci.CiNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.dto.UserVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
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
public class ResourceListApi extends PrivateApiComponentBase {

    @Resource
    private ResourceCenterMapper resourceCenterMapper;

    @Resource
    private CiMapper ciMapper;

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
            @Param(name = "protocolList", type = ApiParamType.JSONARRAY, desc = "协议列表"),
            @Param(name = "statusIdList", type = ApiParamType.JSONARRAY, desc = "状态id列表"),
            @Param(name = "envIdList", type = ApiParamType.JSONARRAY, desc = "环境id列表"),
            @Param(name = "appSystemIdList", type = ApiParamType.JSONARRAY, desc = "应用系统id列表"),
            @Param(name = "appModuleIdList", type = ApiParamType.JSONARRAY, desc = "应用模块id列表"),
            @Param(name = "typeIdList", type = ApiParamType.JSONARRAY, desc = "资源类型id列表"),
            @Param(name = "tagIdList", type = ApiParamType.JSONARRAY, desc = "标签id列表"),
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
        ResourceSearchVo searchVo = JSON.toJavaObject(jsonObj, ResourceSearchVo.class);
        Long typeId = searchVo.getTypeId();
        if (typeId != null) {
            CiVo ciVo = ciMapper.getCiById(typeId);
            if (ciVo == null) {
                throw new CiNotFoundException(typeId);
            }
            List<CiVo> ciList = ciMapper.getDownwardCiListByLR(ciVo.getLft(), ciVo.getRht());
            if (CollectionUtils.isNotEmpty(ciList)) {
                List<Long> typeIdList = ciList.stream().map(CiVo::getId).collect(Collectors.toList());
                searchVo.setTypeIdList(typeIdList);
            }
        }
        List<Long> resourceIdList = null;
        if (CollectionUtils.isNotEmpty(searchVo.getProtocolList())) {
            List<Long> idList = resourceCenterMapper.getResourceIdListByProtocolList(searchVo);
            if (resourceIdList == null) {
                resourceIdList = idList;
            } else {
                resourceIdList.retainAll(idList);
            }
        }
        if (CollectionUtils.isNotEmpty(searchVo.getTagIdList())) {
            List<Long> idList = resourceCenterMapper.getResourceIdListByTagIdList(searchVo);
            if (resourceIdList == null) {
                resourceIdList = idList;
            } else {
                resourceIdList.retainAll(idList);
            }
        }
        if (resourceIdList == null || CollectionUtils.isNotEmpty(resourceIdList)) {
            searchVo.setIdList(resourceIdList);
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
                        if (StringUtils.isNotBlank(resourceVo.getFcu())) {
                            resourceVo.setFcuVo(new UserVo(resourceVo.getFcu()));
                        }
                        if (StringUtils.isNotBlank(resourceVo.getLcu())) {
                            resourceVo.setLcuVo(new UserVo(resourceVo.getLcu()));
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
