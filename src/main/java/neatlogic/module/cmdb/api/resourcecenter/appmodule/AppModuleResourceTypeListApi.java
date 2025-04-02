package neatlogic.module.cmdb.api.resourcecenter.appmodule;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.cmdb.resourcecenter.datasource.core.IResourceCenterDataSource;
import neatlogic.framework.cmdb.resourcecenter.datasource.core.ResourceCenterDataSourceFactory;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceMapper;
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
    ResourceMapper resourceMapper;

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
        List<ResourceVo> envResourceList = new ArrayList<>();
        BasePageVo search = new BasePageVo();
        search.setCurrentPage(1);
        search.setPageSize(100);
        List<Long> envIdList = resourceMapper.searchAppEnvIdList(search);
        if (CollectionUtils.isNotEmpty(envIdList)) {
            envResourceList = resourceMapper.searchAppEnvListByIdList(envIdList);
        }
        //获取数据库所有的模型，用于通过id去获得对应的模型
        Map<Long, CiVo> allCiVoMap = new HashMap<>();
        List<CiVo> allCiVoList = ciMapper.getAllCi(null);
        for (CiVo ci : allCiVoList) {
            allCiVoMap.put(ci.getId(), ci);
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
            Set<Long> typeIdSet = new HashSet<>();
            IResourceCenterDataSource resourceCenterDataSource = ResourceCenterDataSourceFactory.getResourceCenterDataSource();
            Map<String, List<Long>> viewName2TypeIdListMap = resourceCenterDataSource.getAppResourceTypeIdListByAppSystemIdAndAppModuleIdAndEnvId(null, appModuleId, envResource.getId());
            for (Map.Entry<String, List<Long>> entry : viewName2TypeIdListMap.entrySet()) {
                String viewName = entry.getKey();
                searchVo.setViewName(viewName);
                typeIdSet.addAll(entry.getValue());
            }
            Set<CiVo> returnCiVoSet = new HashSet<>();
            for (Long typeId : typeIdSet) {
                CiVo ciVo = allCiVoMap.get(typeId);
                if (ciVo == null) {
                    throw new CiNotFoundException(typeId);
                }
                returnCiVoSet.add(ciVo);
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
