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
            @Param(name = "resourceId", type = ApiParamType.LONG, isRequired = true, desc = "资产id"),
            @Param(name = "host", type = ApiParamType.STRING, xss = true, desc = "host ip"),
            @Param(name = "port", type = ApiParamType.INTEGER, desc = "端口"),
            @Param(name = "protocol", type = ApiParamType.STRING, isRequired = true, desc = "协议"),
            @Param(name = "protocolPort", type = ApiParamType.INTEGER, isRequired = true, desc = "协议端口"),
            @Param(name = "username", type = ApiParamType.STRING, isRequired = true, desc = "账号名")
    })
    @Output({
            @Param(name = "name", explode = AccountVo[].class, desc = "账号"),
    })
    @Description(desc="根据资产id、账号名和协议获取账号密码")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        AccountVo accountVo = resourceCenterMapper.getResourceAccountByResourceIdAndProtocolAndProtocolPortAndAccount(paramObj.getLong("resourceId"),paramObj.getString("protocol"),paramObj.getInteger("protocolPort"),paramObj.getString("username"));
        if(accountVo == null){
            throw new ResourceCenterAccountNotFoundException();
        }
        return "{ENCRYPTED}"+RC4Util.encrypt(AutoexecJobVo.AUTOEXEC_RC4_KEY,RC4Util.decrypt(accountVo.getPasswordCipher().replace("RC4:", StringUtils.EMPTY)));
    }
}
