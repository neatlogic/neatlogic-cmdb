package codedriver.module.cmdb.api.resourcecenter.appmodule;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.cmdb.exception.ci.CiNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import codedriver.module.cmdb.service.resourcecenter.resource.IResourceCenterResourceService;
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
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AppModuleResourceTypeListApi extends PrivateApiComponentBase {

    @Resource
    private CiMapper ciMapper;

    @Resource
    private CiEntityMapper ciEntityMapper;

    @Resource
    ResourceCenterMapper resourceCenterMapper;

    @Resource
    private IResourceCenterResourceService resourceCenterResourceService;

    @Override
    public String getName() {
        return "查询当前模块各环境的模型列表";
    }

    @Override
    public String getToken() {
        return "resourcecenter/appmodule/resource/type/list";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appModuleId", type = ApiParamType.LONG, isRequired = true, desc = "应用模块id（实例id）")
    })
    @Output({
            @Param(desc = "当前模块各环境的模型列表")
    })
    @Description(desc = "当前模块各环境的模型列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONArray returnArray = new JSONArray();
        Long appModuleId = paramObj.getLong("appModuleId");
        //获取应用环境模型
        CiVo envCiVo = ciMapper.getCiByName("APPEnv");
        if (envCiVo == null) {
            throw new CiNotFoundException("APPEnv");
        }
        //获取应用环境实例个数
        int rowNum = ciEntityMapper.getCiEntityIdCountByCiId(envCiVo.getId());
        if (rowNum > 0) {
            //定义需要采集的模型
            List<String> resourceTypeNameList = Arrays.asList("OS", "APPIns", "APPInsCluster", "DBIns", "DBCluster", "AccessEndPoint", "Database");
            List<CiVo> resourceCiVoList = new ArrayList<>();
            //获取应用环境实例list
            CiEntityVo envCiEntityVo = new CiEntityVo();
            envCiEntityVo.setCiId(envCiVo.getId());
            List<Long> idList = ciEntityMapper.getCiEntityIdByCiId(envCiEntityVo);
            List<CiEntityVo> envCiEntityList = ciEntityMapper.getCiEntityBaseInfoByIdList(idList);
            //获取数据库所有的模型，用于通过id去获得对应的模型
            Map<Long, CiVo> allCiVoMap = new HashMap<>();
            List<CiVo> ciVoList = ciMapper.getAllCi(null);
            for (CiVo ci : ciVoList) {
                allCiVoMap.put(ci.getId(), ci);
                if (resourceTypeNameList.contains(ci.getName())) {
                    resourceCiVoList.add(ci);
                }
            }

            ResourceSearchVo searchVo = new ResourceSearchVo();
            searchVo.setAppModuleId(appModuleId);
            for (CiEntityVo envCiEntity : envCiEntityList) {
                JSONObject returnObj = new JSONObject();
                returnObj.put("env", envCiEntity);
                searchVo.setEnvId(envCiEntity.getId());
                //根据模块id和环境id，获取当前环境下含有资产的 模型idList（resourceTypeIdList）
                List<Long> resourceTypeIdList = new ArrayList<>();
                Set<Long> resourceTypeIdSet = resourceCenterMapper.getIpObjectResourceTypeIdListByAppModuleIdAndEnvId(searchVo);
                resourceTypeIdList.addAll(resourceTypeIdSet);
                Set<CiVo> ciVoSet = new HashSet<>();
                if (CollectionUtils.isNotEmpty(resourceTypeIdSet)) {
                    resourceTypeIdSet = resourceCenterMapper.getOsResourceTypeIdListByAppModuleIdAndEnvId(searchVo);
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
                            ciVoSet.add(ciVo);
                        }
                    }
                }
                if (CollectionUtils.isNotEmpty(ciVoSet)) {
                    returnObj.put("ciVoList", new ArrayList<>(ciVoSet));
                } else {
                    returnObj.put("ciVoList", new ArrayList<>());
                }
                returnArray.add(returnObj);
            }
        }
        return returnArray;
    }
}
