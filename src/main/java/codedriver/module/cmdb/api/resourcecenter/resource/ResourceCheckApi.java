/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.resourcecenter.resource;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
            @Param(name = "protocol", type = ApiParamType.ENUM, rule = "application,db,tagent,ssh", isRequired = true, desc = "连接协议"),
            @Param(name = "tagList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "标签列表"),
            @Param(name = "selectNodeList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "选择节点列表"),
            @Param(name = "inputNodeList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "输入节点列表")
    })
    @Output({
            @Param(name = "Return", type = ApiParamType.JSONARRAY, desc = "校验不成功节点列表")
    })
    @Description(desc = "校验资源信息合法性")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Set<String> resultSet = new HashSet<>();
        String executeUser = jsonObj.getString("executeUser");
        String protocol = jsonObj.getString("protocol");
        List<Long> accountIdList = resourceCenterMapper.getAccountIdListByAccountAndProtocol(executeUser, protocol);
        if (CollectionUtils.isEmpty(accountIdList)) {
            resultSet.add("'" + protocol + "'协议中不存在'" + executeUser + "'");
            return resultSet;
        }
        List<Long> tagList = jsonObj.getJSONArray("tagList").toJavaList(Long.class);
        if (CollectionUtils.isNotEmpty(tagList)) {
            List<Long> resourceIdList = resourceCenterMapper.getNoCorrespondingAccountResourceIdListByTagListAndAccountIdAndProtocol(tagList, executeUser, protocol);
            for (Long resourecId : resourceIdList) {
                ResourceVo resourceVo = resourceCenterMapper.getResourceIpPortById(resourecId, TenantContext.get().getDataDbName());
                if (resourceVo != null) {
                    String resourceInfo = resourceVo.getIp();
                    if (resourceVo.getPort() != null) {
                        resourceInfo += ":" + resourceVo.getPort();
                    }
                    resultSet.add(resourceInfo + "未找到" + executeUser + "执行用户");
                }
            }
        }
        List<ResourceVo> selectNodeList = jsonObj.getJSONArray("selectNodeList").toJavaList(ResourceVo.class);
        for (ResourceVo resourceVo : selectNodeList) {
            Long resourceId = resourceCenterMapper.checkResourceIsExistsCorrespondingAccountByResourceIdAndAccountIdAndProtocol(resourceVo.getId(), executeUser, protocol);
            if (resourceId == null) {
                String resourceInfo = resourceVo.getIp();
                if (resourceVo.getPort() != null) {
                    resourceInfo += ":" + resourceVo.getPort();
                }
                resultSet.add(resourceInfo + "未找到" + executeUser + "执行用户");
            }
        }
        List<ResourceSearchVo> inputNodeList = jsonObj.getJSONArray("inputNodeList").toJavaList(ResourceSearchVo.class);
        for (ResourceSearchVo searchVo : inputNodeList) {
            String resourceInfo = searchVo.getIp();
            if (StringUtils.isNotBlank(searchVo.getPort())) {
                resourceInfo += ":" + searchVo.getPort();
                if (StringUtils.isNotBlank(searchVo.getName())) {
                    resourceInfo += "/" + searchVo.getName();
                }
            }
            Long resourceId = resourceCenterMapper.getResourceIdByIpAndPortAndName(searchVo);
            if (resourceId == null) {
                resultSet.add(resourceInfo + "未在系统中找到对应目标");
            } else {
                resourceId = resourceCenterMapper.checkResourceIsExistsCorrespondingAccountByResourceIdAndAccountIdAndProtocol(resourceId, executeUser, protocol);
                if (resourceId == null) {
                    resultSet.add(resourceInfo + "未找到" + executeUser + "执行用户");
                }
            }
        }
        return resultSet;
    }
}
