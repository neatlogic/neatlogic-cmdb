/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.cmdb.api.resourcecenter.resource;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.crossover.IResourceAccountCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceCenterAccountNotFoundException;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceAccountMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetResourceAccountApi extends PrivateApiComponentBase {
    @Resource
    ResourceMapper resourceMapper;
    @Resource
    ResourceAccountMapper resourceAccountMapper;

    @Override
    public String getName() {
        return "获取账号密码";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "resourcecenter/resource/account/get";
    }

    @Input({
            @Param(name = "resourceId", type = ApiParamType.LONG, desc = "资产id"),
            @Param(name = "host", type = ApiParamType.STRING, desc = "host ip"),
            @Param(name = "port", type = ApiParamType.INTEGER, desc = "端口"),
            @Param(name = "nodeName", type = ApiParamType.STRING, desc = "资产名"),
            @Param(name = "nodeType", type = ApiParamType.STRING, desc = "ci模型"),
            @Param(name = "protocol", type = ApiParamType.STRING, desc = "协议", isRequired = true),
            @Param(name = "protocolPort", type = ApiParamType.INTEGER, desc = "协议端口"),
            @Param(name = "username", type = ApiParamType.STRING, desc = "账号名", isRequired = true)
    })
    @Output({
            @Param(type = ApiParamType.STRING, desc = "密码"),
    })
    @Description(desc = "根据资产id、账号名和协议获取账号密码")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {

        Long resourceId = paramObj.getLong("resourceId");
        String ip = paramObj.getString("host");
        Integer port = paramObj.getInteger("port");
        String nodeName = paramObj.getString("nodeName");
        String nodeType = paramObj.getString("nodeType");
        String protocol = paramObj.getString("protocol");
        Integer protocolPort = paramObj.getInteger("protocolPort");
        String username = paramObj.getString("username");


        ResourceVo resourceVo = null;
        if (resourceId == null) {
            resourceVo = resourceMapper.getResourceByIpAndPortAndNameAndTypeName(ip, port, nodeName, nodeType);
            if (resourceVo == null) {
                throw new ResourceNotFoundException();
            }
        } else {
            resourceVo = resourceMapper.getResourceById(resourceId);
        }

        //根据资产绑定的账号找
        List<AccountVo> accountList = resourceAccountMapper.getResourceAccountByResourceIdAndProtocolAndProtocolPortAndUsername(resourceVo.getId(), protocol, protocolPort, username);
        if (CollectionUtils.isNotEmpty(accountList)) {
            return accountList.get(0).getPasswordCipher();
        }
        //根据ip、protocol、username、protocolPort找
        if (Objects.equals("tagent", protocol)) {
            username = null;
        }
        IResourceAccountCrossoverMapper resourceAccountCrossoverMapper = CrossoverServiceFactory.getApi(IResourceAccountCrossoverMapper.class);
        accountList = resourceAccountCrossoverMapper.getAccountListByIpAndProtocolNameAndAccountAndProtocolPort(ip, protocol, username, protocolPort);
        if (CollectionUtils.isNotEmpty(accountList)) {
            return accountList.get(0).getPasswordCipher();
        }
        //根据protocol、username、protocolPort找
        accountList = resourceAccountCrossoverMapper.getAccountListByProtocolNameAndAccountAndProtocolPort(protocol, username, protocolPort);
        if (CollectionUtils.isNotEmpty(accountList)) {
            return accountList.get(0).getPasswordCipher();
        }

        throw new ResourceCenterAccountNotFoundException();
    }
}
