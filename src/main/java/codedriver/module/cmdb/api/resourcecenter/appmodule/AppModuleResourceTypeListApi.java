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
import org.apache.commons.lang3.StringUtils;
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
        return "查询当前模块的各环境资产类型列表";
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
            @Param(name = "ciName", type = ApiParamType.STRING, isRequired = true, desc = "模型名称"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, isRequired = true, desc = "应用模块id（模型id）")
    })
    @Output({
            @Param(desc = "当前模块的各环境资产类型列表")
    })
    @Description(desc = "当前模块的各环境资产类型列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        CiEntityVo ciEntityVo = paramObj.toJavaObject(CiEntityVo.class);
        JSONArray returnArray = new JSONArray();
        String ciName = paramObj.getString("ciName");
        //获取当前模块信息
        CiVo moduleCiVo = ciMapper.getCiByName(ciName);
        if (moduleCiVo == null) {
            throw new CiNotFoundException(ciName);
        }
        //获取模块的环境个数
        int rowNum = ciEntityMapper.getCiEntityIdCountByCiId(moduleCiVo.getId());
        if (rowNum > 0) {
            //定义需要采集的类型
            Map<String, String> typeNameActionMap = new HashMap<>();
            typeNameActionMap.put("OS", "OS");
            typeNameActionMap.put("APPIns", "APPIns");
            typeNameActionMap.put("APPInsCluster", "ipObject");
            typeNameActionMap.put("DBIns", "DBIns");
            typeNameActionMap.put("DBCluster", "ipObject");
            typeNameActionMap.put("AccessEndPoint", "ipObject");
            typeNameActionMap.put("Database", "ipObject");
            List<CiVo> resourceCiVoList = ciMapper.getCiListByNameList(new ArrayList<>(typeNameActionMap.keySet()));
            //获取环境模型list
            ciEntityVo.setCiId(moduleCiVo.getId());
            List<Long> idList = ciEntityMapper.getCiEntityIdByCiId(ciEntityVo);
            List<CiEntityVo> ciEntityList = ciEntityMapper.getCiEntityBaseInfoByIdList(idList);
            //获取所有的模型，用于通过id去获得对应的模型
            Map<Long, CiVo> allCiVoMap = new HashMap<>();
            List<CiVo> ciVoList = ciMapper.getAllCi(null);
            for (CiVo ci : ciVoList) {
                allCiVoMap.put(ci.getId(), ci);
            }

            ResourceSearchVo searchVo = new ResourceSearchVo();
            searchVo.setAppModuleId(paramObj.getLong("appModuleId"));
            for (CiEntityVo ciEntity : ciEntityList) {
                JSONObject returnObj = new JSONObject();
                returnObj.put("env", ciEntity);
                searchVo.setEnvId(ciEntity.getId());
                //获取当前的环境的模型类型idList
                List<Long> resourceTypeIdList = new ArrayList<>();
                Set<Long> resourceTypeIdSet = resourceCenterMapper.getIpObjectResourceTypeIdListByAppModuleIdAndEnvId(searchVo);
                resourceTypeIdList.addAll(resourceTypeIdSet);
                List<CiVo> ciList = new ArrayList<>();
                if (CollectionUtils.isNotEmpty(resourceTypeIdSet)) {
                    resourceTypeIdSet = resourceCenterMapper.getOsResourceTypeIdListByAppModuleIdAndEnvId(searchVo);
                    resourceTypeIdList.addAll(resourceTypeIdSet);
                }
                if (CollectionUtils.isNotEmpty(resourceTypeIdList)) {
                    for (Long resourceTypeId : resourceTypeIdList) {
                        CiVo ciVo = allCiVoMap.get(resourceTypeId);
                        if (ciVo == null) {
                            throw new CiNotFoundException(resourceTypeId);
                        }
                        String resourceTypeName = resourceCenterResourceService.getResourceTypeName(resourceCiVoList, ciVo);
                        String actionKey = typeNameActionMap.get(resourceTypeName);
                        if (StringUtils.isBlank(actionKey)) {
                            continue;
                        }
                        if (typeNameActionMap.containsKey(resourceTypeName)) {
                            ciList.add(ciVo);
                        }
                    }
                }
                if (CollectionUtils.isNotEmpty(ciList)) {
                    returnObj.put("ciVoList", ciList);
                } else {
                    returnObj.put("ciVoList", new ArrayList<>());
                }
                returnArray.add(returnObj);
            }
        }
        return returnArray;
    }
}
