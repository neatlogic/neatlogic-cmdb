package codedriver.module.cmdb.api.resourcecenter.account;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.cmdb.dto.resourcecenter.AccountProtocolVo;
import codedriver.framework.cmdb.exception.resourcecenter.ResourceCenterAccountProtocolHasBeenReferredException;
import codedriver.framework.cmdb.exception.resourcecenter.ResourceCenterAccountProtocolNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.RESOURCECENTER_ACCOUNT_MODIFY;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = RESOURCECENTER_ACCOUNT_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
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
    })
    @Output({
    })
    @Description(desc = "删除账号管理协议")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        AccountProtocolVo accountProtocolVo = resourceCenterMapper.getAccountProtocolVoByProtocolId(id);
        if (id == null) {
            throw new ResourceCenterAccountProtocolNotFoundException(id);
        }
        if (resourceCenterMapper.checkAccountProtocolHasBeenReferredByProtocolId(id) > 0) {
            throw new ResourceCenterAccountProtocolHasBeenReferredException(accountProtocolVo.getName());
        }
        resourceCenterMapper.deleteResourceAccountProtocolById(id);
        return null;
    }
}
