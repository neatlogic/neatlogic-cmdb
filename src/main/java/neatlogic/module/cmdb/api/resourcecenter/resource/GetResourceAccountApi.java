/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.cmdb.api.resourcecenter.resource;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountBaseVo;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountProtocolVo;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.SoftwareServiceOSVo;
import neatlogic.framework.cmdb.enums.resourcecenter.Protocol;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceCenterAccountNotFoundException;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceCenterAccountProtocolNotFoundException;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.tagent.dao.mapper.TagentMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceAccountMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


/**
 * 1、先根据resourceId 或 （ip、port、nodeName、nodeType)获取资产
 * 2、tagent则通过ip找账号， 否则根据资产绑定的账号找，找到后return
 * 3、通过对应资产的os找账号，找到后return
 * 4、找协议（端口）+用户的默认账号，找到后return
 * 5、如果账号id存在，则return对应账号
 */
@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetResourceAccountApi extends PrivateApiComponentBase {
    @Resource
    ResourceMapper resourceMapper;
    @Resource
    ResourceAccountMapper resourceAccountMapper;
    @Resource
    TagentMapper tagentMapper;

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
            @Param(name = "protocol", type = ApiParamType.STRING, desc = "协议", isRequired = true),
            @Param(name = "username", type = ApiParamType.STRING, desc = "账号名", isRequired = true),
            @Param(name = "resourceId", type = ApiParamType.LONG, desc = "资产id"),
            @Param(name = "host", type = ApiParamType.STRING, desc = "host ip"),
            @Param(name = "port", type = ApiParamType.INTEGER, desc = "端口"),
            @Param(name = "nodeName", type = ApiParamType.STRING, desc = "资产名"),
            @Param(name = "nodeType", type = ApiParamType.STRING, desc = "ci模型"),
            @Param(name = "protocolPort", type = ApiParamType.INTEGER, desc = "协议端口"),
            @Param(name = "accountId", type = ApiParamType.LONG, desc = "账号id")
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
        Long accountId = paramObj.getLong("accountId");

        AccountProtocolVo protocolVo = resourceAccountMapper.getAccountProtocolVoByProtocolName(protocol);
        if (protocolVo == null) {
            throw new ResourceCenterAccountProtocolNotFoundException(protocol);
        }

        //先找到资产
        ResourceVo resourceVo = null;
        if (resourceId == null) {
            resourceVo = resourceMapper.getResourceByIpAndPortAndNameAndTypeName(ip, port, nodeName, nodeType);
            if (resourceVo == null) {
                throw new ResourceNotFoundException(ip, port, nodeName, nodeType);
            }
        } else {
            resourceVo = resourceMapper.getResourceById(resourceId);
            if (resourceVo == null) {
                throw new ResourceNotFoundException(resourceId);
            }
        }

        //如果是tagent 则通过ip 找账号， 否则根据资产绑定的账号找
        if (!Objects.equals(protocol, Protocol.TAGENT.getValue())) {
            List<AccountVo> accountList = resourceAccountMapper.getResourceAccountByResourceIdAndProtocolAndProtocolPortAndUsername(resourceVo.getId(), protocol, protocolPort, username);
            if (CollectionUtils.isNotEmpty(accountList)) {
                AccountVo account = accountList.get(0);
                return removePasswordPlain(account);
            }
        } else {
            List<AccountBaseVo> accountList = tagentMapper.getAccountListByIpListAndProtocolId(Collections.singletonList(resourceVo.getIp()), protocolVo.getId());
            if (CollectionUtils.isNotEmpty(accountList)) {
                AccountBaseVo accountTagent = accountList.get(0);
                return removePasswordPlain(accountTagent);
            }
        }

        //通过对应资产的os 找账号
        List<SoftwareServiceOSVo> targetOsList = resourceMapper.getOsResourceListByResourceIdList(Collections.singletonList(resourceVo.getId()));
        if (CollectionUtils.isNotEmpty(targetOsList)) {
            SoftwareServiceOSVo targetOs = targetOsList.get(0);
            List<AccountVo> accountList = resourceAccountMapper.getResourceAccountByResourceIdAndProtocolAndProtocolPortAndUsername(targetOs.getOsId(), protocol, protocolPort, username);
            if (CollectionUtils.isNotEmpty(accountList)) {
                AccountVo account = accountList.get(0);
                return removePasswordPlain(account);
            }
        }

        //找协议的默认账号
        List<AccountVo> accountList = resourceAccountMapper.getDefaultAccountListByProtocolIdListAndAccount(Collections.singletonList(protocolVo.getId()), username);
        if (CollectionUtils.isNotEmpty(accountList)) {
            AccountVo account = accountList.get(0);
            return removePasswordPlain(account);
        }

        //如果账号id存在
        if (accountId != null) {
            AccountVo account = resourceAccountMapper.getAccountById(accountId);
            if (account != null) {
                return removePasswordPlain(account);
            }
        }

        throw new ResourceCenterAccountNotFoundException();
    }

    private JSONObject removePasswordPlain(AccountBaseVo account) {
        JSONObject jsonObject = JSON.parseObject(JSON.toJSONString(account));
        jsonObject.put("passwordPlain", null);
        return jsonObject;
    }
}
