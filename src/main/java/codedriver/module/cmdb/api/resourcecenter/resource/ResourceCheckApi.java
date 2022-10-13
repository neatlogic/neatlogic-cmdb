/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.resourcecenter.resource;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.exception.AutoexecCombopProtocolCannotBeEmptyException;
import codedriver.framework.cmdb.crossover.IResourceCenterAccountCrossoverService;
import codedriver.framework.cmdb.dto.resourcecenter.AccountProtocolVo;
import codedriver.framework.cmdb.dto.resourcecenter.AccountVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.cmdb.enums.resourcecenter.Protocol;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceAccountMapper;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceMapper;
import codedriver.module.cmdb.service.resourcecenter.resource.IResourceCenterResourceService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

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
    private ResourceMapper resourceMapper;
    @Resource
    private ResourceAccountMapper resourceAccountMapper;

    @Resource
    private IResourceCenterResourceService resourceCenterResourceService;

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
            @Param(name = "whitelist", type = ApiParamType.JSONARRAY, desc = "白名单")
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
            AccountProtocolVo protocolVo = resourceAccountMapper.getAccountProtocolVoByProtocolId(protocolId);
            if(protocolVo == null){
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
        }
        List<ResourceVo> executeUserIsNotFoundInResourceList = new ArrayList<>();
        List<ResourceVo> executeUserIsNotFoundInWhitelist = new ArrayList<>();
        JSONObject executeUserIsNotFoundInResourceObj = new JSONObject();
        executeUserIsNotFoundInResourceObj.put("type", "executeUserIsNotFoundInResource");
        executeUserIsNotFoundInResourceObj.put("protocol", protocol);
        executeUserIsNotFoundInResourceObj.put("executeUser", executeUser);
        executeUserIsNotFoundInResourceObj.put("list", executeUserIsNotFoundInResourceList);
        executeUserIsNotFoundInResourceObj.put("whitelist", executeUserIsNotFoundInWhitelist);

        List<ResourceVo> protocolIsNotFoundInResourceList = new ArrayList<>();
        List<ResourceVo> protocolIsNotFoundInWhitelist = new ArrayList<>();
        JSONObject protocolIsNotFoundInResourceObj = new JSONObject();
        protocolIsNotFoundInResourceObj.put("type", "protocolIsNotFoundInResource");
        protocolIsNotFoundInResourceObj.put("protocol", protocol);
        protocolIsNotFoundInResourceObj.put("list", protocolIsNotFoundInResourceList);
        protocolIsNotFoundInResourceObj.put("whitelist", protocolIsNotFoundInWhitelist);

        List<ResourceSearchVo> resourceIsNotFoundList = new ArrayList<>();
        JSONObject resourceIsNotFoundObj = new JSONObject();
        resourceIsNotFoundObj.put("type", "resourceIsNotFound");
        resourceIsNotFoundObj.put("list", resourceIsNotFoundList);

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
                    resourceId = resourceMapper.getResourceIdByIpAndPortAndNameWithFilter(searchVo);
                } else {
                    resourceId = resourceMapper.getResourceIdByIpAndPortAndName(node);
                }
                if (resourceId == null) {
                    resourceIsNotFoundList.add(node);
                } else {
                    idList.add(resourceId);
                }
            }
            if (!idList.isEmpty()) {
                addException(executeUser, protocolId, executeUserIsNotFoundInResourceList, protocolIsNotFoundInResourceList, protocolVoList, idList);
            }
        } else if (MapUtils.isNotEmpty(filter)) {
            ResourceSearchVo searchVo = resourceCenterResourceService.assembleResourceSearchVo(filter);
            int rowNum = resourceMapper.getResourceCount(searchVo);
            // 先检查过滤器下是否存在资源
            if (rowNum > 0) {
                searchVo.setRowNum(rowNum);
                searchVo.setPageSize(100);
                for (int i = 1; i <= searchVo.getPageCount(); i++) {
                    searchVo.setCurrentPage(i);
                    List<Long> idList = resourceMapper.getResourceIdList(searchVo);
                    addException(executeUser, protocolId, executeUserIsNotFoundInResourceList, protocolIsNotFoundInResourceList, protocolVoList, idList);
                }
            } else {
                JSONObject resourceIsEmpty = new JSONObject();
                resourceIsEmpty.put("type", "resourceIsEmpty");
                resultArray.add(resourceIsEmpty);
            }
        } else if (CollectionUtils.isNotEmpty(jsonObj.getJSONArray("selectNodeList"))) {
            // 如果直接选的节点，当协议和用户都存在时，才校验是否合法
            List<ResourceVo> resourceVoList = jsonObj.getJSONArray("selectNodeList").toJavaList(ResourceVo.class);
            addException(executeUser, protocolId, executeUserIsNotFoundInResourceList, protocolIsNotFoundInResourceList, protocolVoList, resourceVoList.stream().map(ResourceVo::getId).collect(Collectors.toList()));
        }

        JSONArray whiteArray = jsonObj.getJSONArray("whitelist");
        if (CollectionUtils.isNotEmpty(whiteArray)) {
            List<ResourceVo> whitelist = whiteArray.toJavaList(ResourceVo.class);
            addException(executeUser, protocolId, executeUserIsNotFoundInWhitelist, protocolIsNotFoundInWhitelist, protocolVoList, whitelist.stream().map(ResourceVo::getId).collect(Collectors.toList()));
        }
        if (executeUserIsNotFoundInResourceList.size() > 0 || executeUserIsNotFoundInWhitelist.size() > 0) {
            resultArray.add(executeUserIsNotFoundInResourceObj);
        }
        if (resourceIsNotFoundList.size() > 0) {
            resultArray.add(resourceIsNotFoundObj);
        }
        if (protocolIsNotFoundInResourceList.size() > 0 || protocolIsNotFoundInWhitelist.size() > 0) {
            resultArray.add(protocolIsNotFoundInResourceObj);
        }
        int count = executeUserIsNotFoundInResourceList.size() + resourceIsNotFoundList.size() + protocolIsNotFoundInResourceList.size() + executeUserIsNotFoundInWhitelist.size() + protocolIsNotFoundInWhitelist.size();
        resultObj.put("count", count);
        return resultObj;
    }

    private void addException(String executeUser, Long protocolId, List<ResourceVo> executeUserIsNotFoundInResourceList, List<ResourceVo> protocolIsNotFoundInResourceList, List<AccountProtocolVo> protocolVoList, List<Long> idList) {
        IResourceCenterAccountCrossoverService accountService = CrossoverServiceFactory.getApi(IResourceCenterAccountCrossoverService.class);
        List<ResourceVo> resourceVoList = resourceMapper.getResourceByIdList(idList);
        if(CollectionUtils.isNotEmpty(resourceVoList)) {
            List<AccountVo> accountVoList = resourceAccountMapper.getResourceAccountListByResourceIdAndProtocolAndAccount(idList, protocolId, executeUser);
            List<AccountVo> allAccountVoList = resourceAccountMapper.getAccountListByIpList(resourceVoList.stream().map(ResourceVo::getIp).collect(Collectors.toList()));
            for (ResourceVo vo : resourceVoList) {
                Optional<AccountVo> accountOp = accountService.filterAccountByRules(accountVoList, allAccountVoList, protocolVoList, vo.getId(), protocolId, vo.getIp(), vo.getPort());
                if (!accountOp.isPresent()) {
                    if (StringUtils.isNotBlank(executeUser)) {
                        executeUserIsNotFoundInResourceList.add(vo);
                    } else {
                        protocolIsNotFoundInResourceList.add(vo);
                    }
                }
            }
        }
    }

}
