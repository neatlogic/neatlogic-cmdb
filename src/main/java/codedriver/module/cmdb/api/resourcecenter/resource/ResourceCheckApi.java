/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.resourcecenter.resource;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.cmdb.dto.resourcecenter.AccountProtocolVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 校验资源信息合法性接口
 *
 * @author linbq
 * @since 2021/6/3 11:41
 **/
@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ResourceCheckApi extends PrivateApiComponentBase {

    @Resource
    private ResourceCenterMapper resourceCenterMapper;

    @Override
    public String getToken() {
        return "resourcecenter/resource/check";
    }

    @Override
    public String getName() {
        return "校验资源信息合法性";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "executeUser", type = ApiParamType.STRING, isRequired = true, desc = "执行用户"),
            @Param(name = "protocolId", type = ApiParamType.LONG, isRequired = true, desc = "连接协议id"),
            @Param(name = "tagList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "标签列表"),
            @Param(name = "selectNodeList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "选择节点列表"),
            @Param(name = "inputNodeList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "输入节点列表")
    })
    @Output({
            @Param(name = "count", type = ApiParamType.INTEGER, desc = "校验不成功个数"),
            @Param(name = "list", type = ApiParamType.JSONARRAY, desc = "校验不成功列表")
    })
    @Description(desc = "校验资源信息合法性")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONArray resultArray = new JSONArray();
        JSONObject resultObj = new JSONObject();
        resultObj.put("list", resultArray);
        String executeUser = jsonObj.getString("executeUser");
        Long protocolId = jsonObj.getLong("protocolId");
        AccountProtocolVo protocolVo = resourceCenterMapper.getAccountProtocolVoByProtocolId(protocolId);
        String protocol = protocolVo.getProtocol();
        List<Long> accountIdList = resourceCenterMapper.getAccountIdListByAccountAndProtocol(executeUser, protocol);
        if (CollectionUtils.isEmpty(accountIdList)) {
            JSONObject executeUserIsNotFoundInProtocolObj = new JSONObject();
            executeUserIsNotFoundInProtocolObj.put("type", "executeUserIsNotFoundInProtocol");
            executeUserIsNotFoundInProtocolObj.put("protocol", protocol);
            executeUserIsNotFoundInProtocolObj.put("executeUser", executeUser);
            resultArray.add(executeUserIsNotFoundInProtocolObj);
            resultObj.put("count", 1);
            return resultObj;
        }
        List<ResourceVo> executeUserIsNotFoundInResourceList = new ArrayList<>();
        JSONObject executeUserIsNotFoundInResourceObj = new JSONObject();
        executeUserIsNotFoundInResourceObj.put("type", "executeUserIsNotFoundInResource");
        executeUserIsNotFoundInResourceObj.put("protocol", protocol);
        executeUserIsNotFoundInResourceObj.put("executeUser", executeUser);
        executeUserIsNotFoundInResourceObj.put("list", executeUserIsNotFoundInResourceList);
        resultArray.add(executeUserIsNotFoundInResourceObj);
        String schemaName = TenantContext.get().getDataDbName();
        List<Long> tagList = jsonObj.getJSONArray("tagList").toJavaList(Long.class);
        if (CollectionUtils.isNotEmpty(tagList)) {
            List<Long> resourceIdList = resourceCenterMapper.getNoCorrespondingAccountResourceIdListByTagListAndAccountIdAndProtocol(tagList, executeUser, protocol);
            for (Long resourecId : resourceIdList) {
                ResourceVo resourceVo = resourceCenterMapper.getResourceIpPortById(resourecId, schemaName);
                if (resourceVo != null) {
                    executeUserIsNotFoundInResourceList.add(resourceVo);
                }
            }
        }
        List<ResourceVo> selectNodeList = jsonObj.getJSONArray("selectNodeList").toJavaList(ResourceVo.class);
        for (ResourceVo resourceVo : selectNodeList) {
            Long accountId = resourceCenterMapper.checkResourceIsExistsCorrespondingAccountByResourceIdAndAccountIdAndProtocol(resourceVo.getId(), executeUser, protocol);
            if (accountId == null) {
                executeUserIsNotFoundInResourceList.add(resourceVo);
            }
        }
        List<ResourceSearchVo> resourceIsNotFoundList = new ArrayList<>();
        JSONObject resourceIsNotFoundObj = new JSONObject();
        resourceIsNotFoundObj.put("type", "resourceIsNotFound");
        resourceIsNotFoundObj.put("list", resourceIsNotFoundList);
        resultArray.add(resourceIsNotFoundObj);
        List<ResourceSearchVo> inputNodeList = jsonObj.getJSONArray("inputNodeList").toJavaList(ResourceSearchVo.class);
        for (ResourceSearchVo searchVo : inputNodeList) {
            Long resourceId = resourceCenterMapper.getResourceIdByIpAndPortAndName(searchVo);
            if (resourceId == null) {
                resourceIsNotFoundList.add(searchVo);
            } else {
                Long accountId = resourceCenterMapper.checkResourceIsExistsCorrespondingAccountByResourceIdAndAccountIdAndProtocol(resourceId, executeUser, protocol);
                if (accountId == null) {
                    ResourceVo resourceVo = resourceCenterMapper.getResourceIpPortById(resourceId, schemaName);
                    executeUserIsNotFoundInResourceList.add(resourceVo);
                }
            }
        }
        int count = executeUserIsNotFoundInResourceList.size();
        count += resourceIsNotFoundList.size();
        resultObj.put("count", count);
        return resultObj;
    }
}
