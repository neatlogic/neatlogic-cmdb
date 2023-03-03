package neatlogic.module.cmdb.api.resourcecenter.account;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountProtocolVo;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceCenterAccountProtocolHasBeenReferredException;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceCenterAccountProtocolNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.RESOURCECENTER_ACCOUNT_MODIFY;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceAccountMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = RESOURCECENTER_ACCOUNT_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class AccountProtocolDeleteApi extends PrivateApiComponentBase {

    @Resource
    private ResourceAccountMapper resourceAccountMapper;

    @Override
    public String getName() {
        return "删除帐号管理协议";
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
    })
    @Output({
    })
    @Description(desc = "删除帐号管理协议")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        AccountProtocolVo accountProtocolVo = resourceAccountMapper.getAccountProtocolVoByProtocolId(id);
        if (accountProtocolVo == null) {
            throw new ResourceCenterAccountProtocolNotFoundException(id);
        }
        if (resourceAccountMapper.checkAccountProtocolHasBeenReferredByProtocolId(id) > 0) {
            throw new ResourceCenterAccountProtocolHasBeenReferredException(accountProtocolVo.getName());
        }
        resourceAccountMapper.deleteResourceAccountProtocolById(id);
        return null;
    }
}
