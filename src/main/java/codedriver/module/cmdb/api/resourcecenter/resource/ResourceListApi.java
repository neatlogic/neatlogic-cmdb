/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.resourcecenter.resource;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.crossover.IResourceListApiCrossoverService;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceInfo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import codedriver.framework.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceMapper;
import codedriver.module.cmdb.service.resourcecenter.resource.IResourceCenterResourceService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
@AuthAction(action = CMDB_BASE.class)
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
            @Param(name = "envIdList", type = ApiParamType.JSONARRAY, desc = "环境id列表"),
            @Param(name = "appSystemIdList", type = ApiParamType.JSONARRAY, desc = "应用系统id列表"),
            @Param(name = "appModuleIdList", type = ApiParamType.JSONARRAY, desc = "应用模块id列表"),
            @Param(name = "typeIdList", type = ApiParamType.JSONARRAY, desc = "资源类型id列表"),
            @Param(name = "tagIdList", type = ApiParamType.JSONARRAY, desc = "标签id列表"),
            @Param(name = "inspectStatusList", type = ApiParamType.JSONARRAY, desc = "巡检状态列表"),
            @Param(name = "searchField", type = ApiParamType.STRING, desc = "批量搜索字段"),
            @Param(name = "searchValue", type = ApiParamType.STRING, desc = "批量搜索值"),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "用于回显的资源ID列表"),
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
        ResourceSearchVo searchVo;
        JSONArray defaultValue = jsonObj.getJSONArray("defaultValue");
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            searchVo = new ResourceSearchVo();
            searchVo.setDefaultValue(defaultValue);
        } else {
            searchVo = resourceCenterResourceService.assembleResourceSearchVo(jsonObj);
        }

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
        //排序
        List<ResourceVo> resultList = new ArrayList<>();
        for (Long id : idList) {
            for (ResourceVo resourceVo : resourceList) {
                if (Objects.equals(id, resourceVo.getId())) {
                    resultList.add(resourceVo);
                    break;
                }
            }
        }
        return TableResultUtil.getResult(resultList, searchVo);
    }

}
