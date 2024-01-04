/*
Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License. 
 */

package neatlogic.module.cmdb.api.resourcecenter.resource;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.exception.AutoexecCombopProtocolCannotBeEmptyException;
import neatlogic.framework.cmdb.auth.label.CMDB;
import neatlogic.framework.cmdb.dto.resourcecenter.*;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.SoftwareServiceOSVo;
import neatlogic.framework.cmdb.enums.resourcecenter.Protocol;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceCenterAccountProtocolNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.tagent.dao.mapper.TagentMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceAccountMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceMapper;
import neatlogic.module.cmdb.service.resourcecenter.account.ResourceCenterAccountService;
import neatlogic.module.cmdb.service.resourcecenter.resource.IResourceCenterResourceService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * 校验资源信息合法性接口
 *
 * @author linbq
 * @since 2021/6/3 11:41
 **/
@Service
@AuthAction(action = CMDB.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ResourceCheckApi extends PrivateApiComponentBase {

    @Resource
    private ResourceMapper resourceMapper;
    @Resource
    private ResourceAccountMapper resourceAccountMapper;

    @Resource
    private IResourceCenterResourceService resourceCenterResourceService;

    @Resource
    private ResourceCenterAccountService resourceCenterAccountService;

    @Resource
    private TagentMapper tagentMapper;

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
            @Param(name = "inputNodeList", type = ApiParamType.JSONARRAY, desc = "输入节点列表"),
            @Param(name = "whitelist", type = ApiParamType.JSONARRAY, desc = "白名单"),
            @Param(name = "cmdbGroupType", type = ApiParamType.STRING, desc = "通过团体过滤权限")
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
        String cmdbGroupType = jsonObj.getString("cmdbGroupType");
        String protocol = null;
        if (protocolId != null) {
            AccountProtocolVo protocolVo = resourceAccountMapper.getAccountProtocolVoByProtocolId(protocolId);
            if (protocolVo == null) {
                throw new AutoexecCombopProtocolCannotBeEmptyException();
            }
            protocol = protocolVo.getName();
            if (Objects.equals(protocol, Protocol.TAGENT.getValue())) {
                executeUser = null;
            }
            // 协议和用户同时填了，才校验是否合法，协议为tagent时，无需校验用户名
            if (StringUtils.isNotBlank(executeUser)) {
                List<Long> accountIdList = resourceAccountMapper.getAccountIdListByAccountAndProtocol(executeUser, protocol);
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
        } else {
            //协议没填时不校验
            return resultArray;
        }
        List<ResourceVo> resourceListWithoutAccountByExecuteUserAndProtocol = new ArrayList<>();
        List<ResourceVo> whileResourceListWithoutAccountByExecuteUserAndProtocol = new ArrayList<>();
        JSONObject resourceObjectWithoutAccountByExecuteUserAndProtocol = new JSONObject();
        resourceObjectWithoutAccountByExecuteUserAndProtocol.put("type", "resourceListWithoutAccountByExecuteUserAndProtocol");
        resourceObjectWithoutAccountByExecuteUserAndProtocol.put("protocol", protocol);
        resourceObjectWithoutAccountByExecuteUserAndProtocol.put("executeUser", executeUser);
        resourceObjectWithoutAccountByExecuteUserAndProtocol.put("list", resourceListWithoutAccountByExecuteUserAndProtocol);
        resourceObjectWithoutAccountByExecuteUserAndProtocol.put("whitelist", whileResourceListWithoutAccountByExecuteUserAndProtocol);

        List<ResourceVo> resourceListWithoutAccountByProtocol = new ArrayList<>();
        List<ResourceVo> whileResourceListWithoutAccountByProtocol = new ArrayList<>();
        JSONObject resourceObjectWithoutAccountByProtocol = new JSONObject();
        resourceObjectWithoutAccountByProtocol.put("type", "resourceListWithoutAccountByProtocol");
        resourceObjectWithoutAccountByProtocol.put("protocol", protocol);
        resourceObjectWithoutAccountByProtocol.put("list", resourceListWithoutAccountByProtocol);
        resourceObjectWithoutAccountByProtocol.put("whitelist", whileResourceListWithoutAccountByProtocol);

        List<ResourceSearchVo> resourceSearchIsNotFoundList = new ArrayList<>();
        JSONObject resourceIsNotFoundObj = new JSONObject();
        resourceIsNotFoundObj.put("type", "resourceIsNotFound");
        resourceIsNotFoundObj.put("list", resourceSearchIsNotFoundList);

        List<AccountProtocolVo> protocolVoList = resourceAccountMapper.searchAccountProtocolListByProtocolName(new AccountProtocolVo());
        if (CollectionUtils.isNotEmpty(jsonObj.getJSONArray("inputNodeList"))) {
            ResourceSearchVo searchVo = null;
            // 如果filter不为空，说明是在执行页带有过滤器的校验输入目标，把过滤器作为进一步的筛选条件
            if (MapUtils.isNotEmpty(filter)) {
                searchVo = resourceCenterResourceService.assembleResourceSearchVo(filter);
//                // 如果过滤器下没有任何目标，不再进行下一步校验
                if (resourceMapper.getResourceCount(searchVo) == 0) {
                    JSONObject resourceIsEmpty = new JSONObject();
                    resourceIsEmpty.put("type", "resourceIsEmpty");
                    resultArray.add(resourceIsEmpty);
                    return resultObj;
                }
            }
            List<Long> idList = new ArrayList<>();
            List<ResourceSearchVo> inputNodeList = jsonObj.getJSONArray("inputNodeList").toJavaList(ResourceSearchVo.class);
            // 如果是输入的目标，首先校验目标是否存在，如果存在且协议和用户都填了，再校验是否合法
            for (ResourceSearchVo node : inputNodeList) {
                Long resourceId = null;
                if (searchVo != null) { // 如果searchVo不为null，说明有过滤器，那么加上过滤器筛选
                    searchVo.setIp(node.getIp());
                    searchVo.setPort(node.getPort());
                    searchVo.setName(node.getName());
                    searchVo.setCmdbGroupType(cmdbGroupType);
                    resourceId = resourceMapper.getResourceIdByIpAndPortAndNameWithFilter(searchVo);
                } else {
                    node.setCmdbGroupType(cmdbGroupType);
                    resourceId = resourceMapper.getResourceIdByIpAndPortAndName(node);
                }
                if (resourceId == null) {
                    resourceSearchIsNotFoundList.add(node);
                } else {
                    idList.add(resourceId);
                }
            }
            if (!idList.isEmpty()) {
                addException(executeUser, protocolId, resourceListWithoutAccountByExecuteUserAndProtocol, resourceListWithoutAccountByProtocol, protocolVoList, idList, cmdbGroupType);
            }
        } else if (MapUtils.isNotEmpty(filter)) {
            //补充opType操作类型
            filter.put("cmdbGroupType", cmdbGroupType);
            ResourceSearchVo searchVo = resourceCenterResourceService.assembleResourceSearchVo(filter);
            int rowNum = resourceMapper.getResourceCount(searchVo);
            // 先检查过滤器下是否存在资源
            if (rowNum > 0) {
                searchVo.setRowNum(rowNum);
                searchVo.setPageSize(100);
                for (int i = 1; i <= searchVo.getPageCount(); i++) {
                    searchVo.setCurrentPage(i);
                    List<Long> idList = resourceMapper.getResourceIdList(searchVo);
                    addException(executeUser, protocolId, resourceListWithoutAccountByExecuteUserAndProtocol, resourceListWithoutAccountByProtocol, protocolVoList, idList, cmdbGroupType);
                }
            } else {
                JSONObject resourceIsEmpty = new JSONObject();
                resourceIsEmpty.put("type", "resourceIsEmpty");
                resultArray.add(resourceIsEmpty);
            }
        } else if (CollectionUtils.isNotEmpty(jsonObj.getJSONArray("selectNodeList"))) {
            // 如果直接选的节点，当协议和用户都存在时，才校验是否合法
            List<ResourceVo> resourceVoList = jsonObj.getJSONArray("selectNodeList").toJavaList(ResourceVo.class);
            List<Long> resourceIsNotFoundIdList = addException(executeUser, protocolId, resourceListWithoutAccountByExecuteUserAndProtocol, resourceListWithoutAccountByProtocol, protocolVoList, resourceVoList.stream().map(ResourceVo::getId).collect(toList()), cmdbGroupType);
            if (CollectionUtils.isNotEmpty(resourceIsNotFoundIdList)) {
                List<ResourceVo> resourceIsNotFoundList = resourceVoList.stream().filter(e -> resourceIsNotFoundIdList.contains(e.getId())).collect(toList());
                if (CollectionUtils.isNotEmpty(resourceIsNotFoundList)) {
                    resourceIsNotFoundList.forEach(r -> {
                        resourceSearchIsNotFoundList.add(new ResourceSearchVo(r));
                    });
                }
            }
        }

        JSONArray whiteArray = jsonObj.getJSONArray("whitelist");
        if (CollectionUtils.isNotEmpty(whiteArray)) {
            List<ResourceVo> whitelist = whiteArray.toJavaList(ResourceVo.class);
            List<Long> resourceIsNotFoundIdList = addException(executeUser, protocolId, resourceListWithoutAccountByExecuteUserAndProtocol, whileResourceListWithoutAccountByProtocol, protocolVoList, whitelist.stream().map(ResourceVo::getId).collect(toList()), cmdbGroupType);
            if (CollectionUtils.isNotEmpty(resourceIsNotFoundIdList)) {
                List<ResourceVo> resourceIsNotFoundList = whitelist.stream().filter(e -> resourceIsNotFoundIdList.contains(e.getId())).collect(toList());
                if (CollectionUtils.isNotEmpty(resourceIsNotFoundList)) {
                    resourceIsNotFoundList.forEach(r -> {
                        resourceSearchIsNotFoundList.add(new ResourceSearchVo(r));
                    });
                }
            }
        }

        if (resourceListWithoutAccountByExecuteUserAndProtocol.size() > 0 || whileResourceListWithoutAccountByExecuteUserAndProtocol.size() > 0) {
            resultArray.add(resourceObjectWithoutAccountByExecuteUserAndProtocol);
        }
        if (resourceSearchIsNotFoundList.size() > 0) {
            resultArray.add(resourceIsNotFoundObj);
        }
        if (resourceListWithoutAccountByProtocol.size() > 0 || whileResourceListWithoutAccountByProtocol.size() > 0) {
            resultArray.add(resourceObjectWithoutAccountByProtocol);
        }
        int count = resourceListWithoutAccountByExecuteUserAndProtocol.size() + resourceSearchIsNotFoundList.size() + resourceListWithoutAccountByProtocol.size() + whileResourceListWithoutAccountByExecuteUserAndProtocol.size() + whileResourceListWithoutAccountByProtocol.size();
        resultObj.put("count", count);
        return resultObj;
    }

    private List<Long> addException(String executeUser, Long protocolId, List<ResourceVo> resourceListWithoutAccountByExecuteUserAndProtocol, List<ResourceVo> resourceListWithoutAccountByProtocol, List<AccountProtocolVo> protocolVoList, List<Long> idList, String cmdbGroupType) {
        List<Long> resourceIsNotFoundIdList = new ArrayList<>();
        AccountProtocolVo protocolVo = resourceAccountMapper.getAccountProtocolVoByProtocolId(protocolId);
        Map<Long, Long> resourceOSResourceMap = new HashMap<>();//节点resourceId->对应操作系统resourceId
        Map<String, AccountBaseVo> tagentIpAccountMap = new HashMap<>();
        if (protocolVo == null) {
            throw new ResourceCenterAccountProtocolNotFoundException(protocolId);
        }
        //补充opType操作类型
        JSONObject filterJson = new JSONObject();
        filterJson.put("cmdbGroupType", cmdbGroupType);
        ResourceSearchVo searchVo = resourceCenterResourceService.assembleResourceSearchVo(filterJson);
        searchVo.setIdList(idList);
        List<ResourceVo> resourceVoList = resourceMapper.getAuthResourceList(searchVo);
        if (CollectionUtils.isNotEmpty(resourceVoList)) {
            if (idList.size() != resourceVoList.size()) {
                List<Long> exitResourceIdList = resourceVoList.stream().map(ResourceVo::getId).collect(toList());
                resourceIsNotFoundIdList.addAll(idList.stream().filter(s -> !exitResourceIdList.contains(s)).collect(Collectors.toList()));
            }
            List<Long> resourceIncludeOsIdList = new ArrayList<>(idList);
            //查询target 对应的os
            List<SoftwareServiceOSVo> targetOsList = resourceMapper.getOsResourceListByResourceIdList(idList);
            if (CollectionUtils.isNotEmpty(targetOsList)) {
                resourceIncludeOsIdList.addAll(targetOsList.stream().map(SoftwareServiceOSVo::getOsId).collect(toList()));
                resourceOSResourceMap = targetOsList.stream().collect(toMap(SoftwareServiceOSVo::getResourceId, SoftwareServiceOSVo::getOsId));
            }
            List<AccountVo> accountByResourceList = new ArrayList<>();
            if (!Objects.equals(protocolVo.getName(), Protocol.TAGENT.getValue())) {
                accountByResourceList = resourceAccountMapper.getResourceAccountListByResourceIdAndProtocolAndAccount(resourceIncludeOsIdList, protocolId, executeUser);
            } else {
                List<AccountBaseVo> tagentAccountByIpList = tagentMapper.getAccountListByIpListAndProtocolId(resourceVoList.stream().map(ResourceVo::getIp).collect(toList()), protocolId);
                if (CollectionUtils.isNotEmpty(tagentAccountByIpList)) {
                    tagentIpAccountMap = tagentAccountByIpList.stream().collect(toMap(AccountBaseVo::getIp, o -> o));
                }
            }
            Map<Long, AccountVo> protocolDefaultAccountMap = new HashMap<>();
            List<AccountVo> defaultAccountList;
            if (CollectionUtils.isNotEmpty(protocolVoList)) {
                defaultAccountList = resourceAccountMapper.getDefaultAccountListByProtocolIdListAndAccount(protocolVoList.stream().map(AccountProtocolVo::getId).collect(toList()), executeUser);
                if (CollectionUtils.isNotEmpty(defaultAccountList)) {
                    protocolDefaultAccountMap = defaultAccountList.stream().collect(toMap(AccountVo::getProtocolId, o -> o));
                }
            }
            for (ResourceVo vo : resourceVoList) {
                AccountBaseVo account = resourceCenterAccountService.filterAccountByRules(accountByResourceList, tagentIpAccountMap, vo.getId(), protocolVo, vo.getIp(), resourceOSResourceMap, protocolDefaultAccountMap);
                if (account == null) {
                    if (StringUtils.isNotBlank(executeUser)) {
                        resourceListWithoutAccountByExecuteUserAndProtocol.add(vo);
                    } else {
                        resourceListWithoutAccountByProtocol.add(vo);
                    }
                }
            }
        } else {
            resourceIsNotFoundIdList.addAll(idList);
        }
        return resourceIsNotFoundIdList;
    }

}
