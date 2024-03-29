package neatlogic.module.cmdb.api.resourcecenter.appmodule;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.enums.resourcecenter.AppModuleResourceType;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceMapper;
import neatlogic.module.cmdb.service.resourcecenter.resource.IResourceCenterResourceService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * @author longrf
 * @date 2022/3/2 4:10 下午
 */
@Service
@AuthAction(action = CMDB.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AppModuleResourceTypeListApi extends PrivateApiComponentBase {

    @Resource
    private CiMapper ciMapper;

    @Resource
    private CiEntityMapper ciEntityMapper;

    @Resource
    ResourceMapper resourceMapper;

    @Resource
    private IResourceCenterResourceService resourceCenterResourceService;

    @Override
    public String getName() {
        return "查询当前模块各环境的需要显示的模型列表";
    }

    @Override
    public String getToken() {
        return "resourcecenter/appmodule/resource/type/list";
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
            @Param(name = "appModuleId", type = ApiParamType.LONG, isRequired = true, desc = "应用模块id（实例id）")
    })
    @Output({
            @Param(desc = "当前模块各环境的需要显示的模型列表")
    })
    @Description(desc = "当前模块各环境的需要显示的模型列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONArray returnArray = new JSONArray();
        Long appModuleId = paramObj.getLong("appModuleId");
        //获取应用环境模型
        CiVo envCiVo = ciMapper.getCiByName("APPEnv");
        if (envCiVo == null) {
            throw new CiNotFoundException("APPEnv");
        }
        //获取需要采集的模型
        List<String> resourceTypeNameList = AppModuleResourceType.getNameList();
        List<CiVo> resourceCiVoList = new ArrayList<>();
        //获取应用环境实例list
        CiEntityVo envCiEntityVo = new CiEntityVo();
        envCiEntityVo.setCiId(envCiVo.getId());
        List<Long> envIdList = ciEntityMapper.getCiEntityIdByCiId(envCiEntityVo);
        List<ResourceVo> envResourceList = resourceMapper.searchAppEnvListByIdList(envIdList);
        //获取数据库所有的模型，用于通过id去获得对应的模型
        Map<Long, CiVo> allCiVoMap = new HashMap<>();
        List<CiVo> allCiVoList = ciMapper.getAllCi(null);
        for (CiVo ci : allCiVoList) {
            allCiVoMap.put(ci.getId(), ci);
            if (resourceTypeNameList.contains(ci.getName())) {
                resourceCiVoList.add(ci);
            }
        }
        ResourceSearchVo searchVo = new ResourceSearchVo();
        searchVo.setAppModuleId(appModuleId);
        //无配置环境
        ResourceVo noSettingEnvResourceVo = new ResourceVo();
        noSettingEnvResourceVo.setId(-2L);
        noSettingEnvResourceVo.setName("未配置");
        envResourceList.add(noSettingEnvResourceVo);
        for (ResourceVo envResource : envResourceList) {
            JSONObject returnObj = new JSONObject();
            searchVo.setEnvId(envResource.getId());
            //根据模块id和环境id，获取当前环境下含有资产的 模型idList（resourceTypeIdList）
            Set<Long> resourceTypeIdSet = resourceMapper.getIpObjectResourceTypeIdListByAppModuleIdAndEnvId(searchVo);
            List<Long> resourceTypeIdList = new ArrayList<>(resourceTypeIdSet);
            Set<CiVo> returnCiVoSet = new HashSet<>();
            if (CollectionUtils.isNotEmpty(resourceTypeIdSet)) {
                resourceTypeIdSet = resourceMapper.getOsResourceTypeIdListByAppModuleIdAndEnvId(searchVo);
                resourceTypeIdList.addAll(resourceTypeIdSet);
            }

            //循环resourceTypeIdList，将其父级模型的name存在于resourceTypeNameList中的 模型 返回给前端
            if (CollectionUtils.isNotEmpty(resourceTypeIdList)) {
                for (Long resourceTypeId : resourceTypeIdList) {
                    CiVo ciVo = allCiVoMap.get(resourceTypeId);
                    if (ciVo == null) {
                        throw new CiNotFoundException(resourceTypeId);
                    }
                    String resourceTypeName = resourceCenterResourceService.getResourceTypeName(resourceCiVoList, ciVo);
                    if (resourceTypeNameList.contains(resourceTypeName)) {
                        returnCiVoSet.add(ciVo);
                    }
                }
            }
            if (CollectionUtils.isNotEmpty(returnCiVoSet)) {
                returnObj.put("env", envResource);
                returnObj.put("ciVoList", returnCiVoSet);
                returnArray.add(returnObj);
            }
        }
        return returnArray;
    }
}
