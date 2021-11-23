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
import codedriver.module.cmdb.service.resourcecenter.resource.ResourceCenterResourceService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
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

    @Resource
    private ResourceCenterResourceService resourceCenterResourceService;

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
            @Param(name = "executeUser", type = ApiParamType.STRING, desc = "执行用户"),
            @Param(name = "protocolId", type = ApiParamType.LONG, desc = "连接协议id"),
            @Param(name = "filter", type = ApiParamType.JSONOBJECT, desc = "过滤器"),
            @Param(name = "selectNodeList", type = ApiParamType.JSONARRAY, desc = "选择节点列表"),
            @Param(name = "inputNodeList", type = ApiParamType.JSONARRAY, desc = "输入节点列表")
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
        JSONObject filter = jsonObj.getJSONObject("filter");
        String protocol = null;
        if (protocolId != null) {
            AccountProtocolVo protocolVo = resourceCenterMapper.getAccountProtocolVoByProtocolId(protocolId);
            protocol = protocolVo.getName();
            // 协议和用户同时填了，才校验是否合法
            if (StringUtils.isNotBlank(executeUser)) {
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
            }
        }
        List<ResourceVo> executeUserIsNotFoundInResourceList = new ArrayList<>();
        JSONObject executeUserIsNotFoundInResourceObj = new JSONObject();
        executeUserIsNotFoundInResourceObj.put("type", "executeUserIsNotFoundInResource");
        executeUserIsNotFoundInResourceObj.put("protocol", protocol);
        executeUserIsNotFoundInResourceObj.put("executeUser", executeUser);
        executeUserIsNotFoundInResourceObj.put("list", executeUserIsNotFoundInResourceList);

        List<ResourceSearchVo> resourceIsNotFoundList = new ArrayList<>();
        JSONObject resourceIsNotFoundObj = new JSONObject();
        resourceIsNotFoundObj.put("type", "resourceIsNotFound");
        resourceIsNotFoundObj.put("list", resourceIsNotFoundList);

        String schemaName = TenantContext.get().getDataDbName();
        if (CollectionUtils.isNotEmpty(jsonObj.getJSONArray("inputNodeList"))) {
            ResourceSearchVo searchVo = null;
            // 如果filter不为空，说明是在执行页带有过滤器的校验输入目标，把过滤器作为进一步的筛选条件
            if (MapUtils.isNotEmpty(filter)) {
                searchVo = resourceCenterResourceService.assembleResourceSearchVo(filter);
                // 如果过滤器下没有任何目标，不再进行下一步校验
                if (resourceCenterMapper.getResourceCount(searchVo) == 0) {
                    JSONObject resourceIsEmpty = new JSONObject();
                    resourceIsEmpty.put("type", "resourceIsEmpty");
                    resultArray.add(resourceIsEmpty);
                    return resultObj;
                }
            }
            List<ResourceSearchVo> inputNodeList = jsonObj.getJSONArray("inputNodeList").toJavaList(ResourceSearchVo.class);
            // 如果是输入的目标，首先校验目标是否存在，如果存在且协议和用户都填了，再校验是否合法
            for (ResourceSearchVo node : inputNodeList) {
                Long resourceId;
                if (searchVo != null) { // 如果searchVo不为null，说明有过滤器，那么加上过滤器筛选
                    searchVo.setIp(node.getIp());
                    searchVo.setPort(node.getPort());
                    searchVo.setName(node.getName());
                    resourceId = resourceCenterMapper.getResourceIdByIpAndPortAndNameWithFilter(searchVo);
                } else {
                    resourceId = resourceCenterMapper.getResourceIdByIpAndPortAndName(node);
                }
                if (resourceId == null) {
                    resourceIsNotFoundList.add(node);
                } else if (StringUtils.isNotBlank(protocol) && StringUtils.isNotBlank(executeUser)) {
                    Long accountId = resourceCenterMapper.checkResourceIsExistsCorrespondingAccountByResourceIdAndAccountIdAndProtocol(resourceId, executeUser, protocol);
                    if (accountId == null) {
                        ResourceVo resourceVo = resourceCenterMapper.getResourceIpPortById(resourceId, schemaName);
                        executeUserIsNotFoundInResourceList.add(resourceVo);
                    }
                }
            }
        } else if (MapUtils.isNotEmpty(filter)) {
            ResourceSearchVo searchVo = resourceCenterResourceService.assembleResourceSearchVo(filter);
            // 先检查过滤器下是否存在资源
            if (resourceCenterMapper.getResourceCount(searchVo) > 0) {
                // 找出在过滤器的条件下，没有绑定protocol与executeUser的资源
                if (StringUtils.isNotBlank(protocol) && StringUtils.isNotBlank(executeUser)) {
                    searchVo.setProtocol(protocol);
                    searchVo.setAccount(executeUser);
                    List<ResourceVo> list = resourceCenterMapper.getNoCorrespondingResourceListByAccountIdAndProtocol(searchVo);
                    if (CollectionUtils.isNotEmpty(list)) {
                        executeUserIsNotFoundInResourceList.addAll(list);
                    }
                }
            } else {
                JSONObject resourceIsEmpty = new JSONObject();
                resourceIsEmpty.put("type", "resourceIsEmpty");
                resultArray.add(resourceIsEmpty);
            }
        } else if (CollectionUtils.isNotEmpty(jsonObj.getJSONArray("selectNodeList"))) {
            // 如果直接选的节点，当协议和用户都存在时，才校验是否合法
            List<ResourceVo> selectNodeList = jsonObj.getJSONArray("selectNodeList").toJavaList(ResourceVo.class);
            if (StringUtils.isNotBlank(protocol) && StringUtils.isNotBlank(executeUser)) {
                for (ResourceVo resourceVo : selectNodeList) {
                    Long accountId = resourceCenterMapper.checkResourceIsExistsCorrespondingAccountByResourceIdAndAccountIdAndProtocol(resourceVo.getId(), executeUser, protocol);
                    if (accountId == null) {
                        executeUserIsNotFoundInResourceList.add(resourceVo);
                    }
                }
            }
        }

        if (executeUserIsNotFoundInResourceList.size() > 0) {
            resultArray.add(executeUserIsNotFoundInResourceObj);
        }
        if (resourceIsNotFoundList.size() > 0) {
            resultArray.add(resourceIsNotFoundObj);
        }
        int count = executeUserIsNotFoundInResourceList.size();
        count += resourceIsNotFoundList.size();
        resultObj.put("count", count);
        return resultObj;
    }
}
