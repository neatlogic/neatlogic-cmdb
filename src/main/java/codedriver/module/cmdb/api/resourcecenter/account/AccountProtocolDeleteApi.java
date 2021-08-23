package codedriver.module.cmdb.api.resourcecenter.account;

import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.cmdb.dto.resourcecenter.AccountProtocolVo;
import codedriver.framework.cmdb.exception.resourcecenter.ResourceCenterAccountProtocolHasBeenReferredException;
import codedriver.framework.cmdb.exception.resourcecenter.ResourceCenterAccountProtocolNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;

import javax.annotation.Resource;

public class AccountProtocolDeleteApi extends PrivateApiComponentBase {

    @Resource
    private ResourceCenterMapper resourceCenterMapper;

    @Override
    public String getName() {
        return "删除账号管理协议";
    }

    @Override
    public String getToken() {
        return "resourcecenter/account/protocol/delete";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "协议ID"),
            @Param(name = "name", type = ApiParamType.STRING, desc = "协议名称"),
    })
    @Output({
    })
    @Description(desc = "删除账号管理协议")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        String protocol = paramObj.getString("name");
        AccountProtocolVo accountProtocolVo = resourceCenterMapper.getAccountProtocolVoByProtocolName(protocol);
        if (accountProtocolVo == null) {
            throw new ResourceCenterAccountProtocolNotFoundException(accountProtocolVo.getId());
        }
        if (resourceCenterMapper.checkAccountProtocolHasBeenReferredByprotocol(protocol) > 0) {
            throw new ResourceCenterAccountProtocolHasBeenReferredException(protocol);
        }
        resourceCenterMapper.deleteResourceAccountProtocolById(id);
        return null;
    }
}
