/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.resourcecenter.resource;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.crossover.IResourceListApiCrossoverService;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceCenterConfigVo;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceInfo;
import codedriver.framework.cmdb.exception.resourcecenter.ResourceCenterConfigNotFoundException;
import codedriver.framework.cmdb.utils.ResourceSearchGenerateSqlUtil;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceCenterConfigMapper;
import codedriver.module.cmdb.service.resourcecenter.resource.IResourceCenterResourceService;
import codedriver.module.cmdb.service.resourcecenter.resource.ResourceCenterCommonGenerateSqlService;
import codedriver.module.cmdb.service.resourcecenter.resource.ResourceCenterCustomGenerateSqlService;
import codedriver.module.cmdb.utils.ResourceEntityViewBuilder;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import net.sf.jsqlparser.statement.select.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.BiConsumer;
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
    private IResourceCenterResourceService resourceCenterResourceService;

    @Resource
    private ResourceCenterCommonGenerateSqlService resourceCenterCommonGenerateSqlService;

    @Resource
    private ResourceCenterCustomGenerateSqlService resourceCenterCustomGenerateSqlService;

    @Resource
    private ResourceCenterConfigMapper resourceCenterConfigMapper;

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
            @Param(name = "tbodyList", explode = ResourceVo[].class, desc = "数据列表"),
            @Param(name = "errorList", explode = ResourceEntityVo[].class, desc = "数据初始化配置异常信息列表"),
            @Param(name = "unavailableResourceInfoList", explode = ResourceInfo[].class, desc = "数据初始化配置异常信息列表")
    })
    @Description(desc = "查询资源中心数据列表")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        List<ResourceVo> resourceVoList = new ArrayList<>();
        ResourceSearchVo searchVo;
        JSONArray defaultValue = jsonObj.getJSONArray("defaultValue");
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            searchVo = new ResourceSearchVo();
            searchVo.setDefaultValue(defaultValue);
        } else {
            searchVo = resourceCenterResourceService.assembleResourceSearchVo(jsonObj);
        }

        ResourceCenterConfigVo configVo = resourceCenterConfigMapper.getResourceCenterConfig();
        if (configVo == null) {
            throw new ResourceCenterConfigNotFoundException();
        }
        ResourceEntityViewBuilder builder = new ResourceEntityViewBuilder(configVo.getConfig());
        List<ResourceEntityVo> resourceEntityList = builder.getResourceEntityList();

        List<ResourceInfo> unavailableResourceInfoList = new ArrayList<>();
        JSONObject paramObj = (JSONObject) JSONObject.toJSON(searchVo);
        List<BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect>> biConsumerList = new ArrayList<>();
        biConsumerList.add(resourceCenterCustomGenerateSqlService.getBiConsumerByCommonCondition(paramObj, unavailableResourceInfoList));
        biConsumerList.add(resourceCenterCustomGenerateSqlService.getBiConsumerByProtocolIdList(searchVo.getProtocolIdList(), unavailableResourceInfoList));
        biConsumerList.add(resourceCenterCustomGenerateSqlService.getBiConsumerByTagIdList(searchVo.getTagIdList(), unavailableResourceInfoList));
        biConsumerList.add(resourceCenterCustomGenerateSqlService.getBiConsumerByKeyword(searchVo.getKeyword(), unavailableResourceInfoList));
        resourceVoList = resourceCenterCommonGenerateSqlService.getResourceList("resource_ipobject", resourceCenterCustomGenerateSqlService.getTheadList(), biConsumerList, searchVo, unavailableResourceInfoList);
        if (CollectionUtils.isNotEmpty(resourceVoList)) {
            List<Long> idList = resourceVoList.stream().map(ResourceVo::getId).collect(Collectors.toList());
            resourceCenterResourceService.addResourceAccount(idList, resourceVoList);
            resourceCenterResourceService.addResourceTag(idList, resourceVoList);
        }
        JSONObject resultObj = TableResultUtil.getResult(resourceVoList, searchVo);
        List<ResourceEntityVo> errorList = new ArrayList<>();
        for (ResourceEntityVo resourceEntityVo : resourceEntityList) {
            if (StringUtils.isNotBlank(resourceEntityVo.getError())) {
                errorList.add(resourceEntityVo);
            }
        }
        if (CollectionUtils.isNotEmpty(errorList)) {
            resultObj.put("errorList", errorList);
        }
        if (CollectionUtils.isNotEmpty(unavailableResourceInfoList)) {
            resultObj.put("unavailableResourceInfoList", unavailableResourceInfoList);
        }
        return resultObj;
    }

}
