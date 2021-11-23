/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.resourcecenter.resource;

import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.cmdb.dto.resourcecenter.AccountVo;
import codedriver.framework.cmdb.exception.resourcecenter.ResourceCenterAccountNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.util.RC4Util;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.publicapi.PublicApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class ResourceAccountGetPublicApi extends PublicApiComponentBase {
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
            @Param(name = "accountId", type = ApiParamType.LONG, desc = "协议id"),
            @Param(name = "username", type = ApiParamType.STRING, desc = "账号名")
    })
    @Output({
            @Param(name = "name", explode = AccountVo[].class, desc = "账号"),
    })
    @Description(desc = "根据资产id、账号名和协议获取账号密码")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        /*
         * 按以下规则顺序匹配account
         * 1、通过 ”资产id+账号id“ 匹配
         * 2、通过 ”组合工具配置的执行节点的ip+账号id“ 匹配 账号表
         * 3、通过 ”组合工具配置的执行节点的ip+端口“ 匹配 账号表
         */
        Long resourceId = paramObj.getLong("resourceId");
        Long accountId = paramObj.getLong("accountId");
        String host = paramObj.getString("host");
        Integer port = paramObj.getInteger("port");
        AccountVo accountVo = null;

        //1
        if (resourceId != null && accountId != null) {
            accountVo = resourceCenterMapper.getResourceAccountByResourceIdAndAccountId(resourceId, accountId);
        }
        //2
        if (accountVo == null && accountId != null && StringUtils.isNotBlank(host)) {
            accountVo = resourceCenterMapper.getResourceAccountByIpAndAccountId(host, accountId);
        }
        //3
        if (accountVo == null && StringUtils.isNotBlank(host) && port != null) {
            accountVo = resourceCenterMapper.getResourceAccountByIpAndPort(host, port);
        }
        if (accountVo == null) {
            throw new ResourceCenterAccountNotFoundException();
        }
        return "{ENCRYPTED}" + RC4Util.encrypt(AutoexecJobVo.AUTOEXEC_RC4_KEY, RC4Util.decrypt(accountVo.getPasswordCipher().replace("RC4:", StringUtils.EMPTY)));
    }
}
