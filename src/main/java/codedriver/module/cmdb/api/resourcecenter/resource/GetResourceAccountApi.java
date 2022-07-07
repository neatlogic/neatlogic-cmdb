/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.resourcecenter.resource;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.cmdb.dto.resourcecenter.AccountVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.cmdb.exception.resourcecenter.ResourceCenterAccountNotFoundException;
import codedriver.framework.cmdb.exception.resourcecenter.ResourceNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.util.RC4Util;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class GetResourceAccountApi extends PrivateApiComponentBase {
    @Resource
    ResourceCenterMapper resourceCenterMapper;

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
            @Param(name = "name", explode = AccountVo[].class, desc = "账号"),
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

        if (resourceId == null) {
            ResourceVo resourceVo = resourceCenterMapper.getResourceByIpAndPortAndNameAndTypeName(TenantContext.get().getDataDbName(), ip, port, nodeName, nodeType);
            if (resourceVo == null) {
                throw new ResourceNotFoundException();
            }
            resourceId = resourceVo.getId();
        }

        List<AccountVo> accountVoList = resourceCenterMapper.getResourceAccountByResourceIdAndProtocolAndProtocolPortAndUsername(resourceId, protocol, protocolPort, username);
        if (CollectionUtils.isEmpty(accountVoList)) {
            throw new ResourceCenterAccountNotFoundException();
        }
        return "{ENCRYPTED}" + RC4Util.encrypt(AutoexecJobVo.AUTOEXEC_RC4_KEY, RC4Util.decrypt(accountVoList.get(0).getPasswordCipher().replace("RC4:", StringUtils.EMPTY)));
    }
}
