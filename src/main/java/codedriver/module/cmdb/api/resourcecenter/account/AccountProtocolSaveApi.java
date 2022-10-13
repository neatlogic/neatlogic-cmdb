package codedriver.module.cmdb.api.resourcecenter.account;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.resourcecenter.AccountProtocolVo;
import codedriver.framework.cmdb.exception.resourcecenter.ResourceCenterAccountProtocolRepeatException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.cmdb.auth.label.RESOURCECENTER_ACCOUNT_MODIFY;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceAccountMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = RESOURCECENTER_ACCOUNT_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class AccountProtocolSaveApi extends PrivateApiComponentBase {

    @Resource
    private ResourceAccountMapper resourceAccountMapper;

    @Override
    public String getName() {
        return "保存账户管理协议";
    }

    @Override
    public String getToken() {
        return "resourcecenter/account/protocol/save";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG,  desc = "协议ID"),
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "协议名称"),
            @Param(name = "port", type = ApiParamType.INTEGER, desc = "协议端口"),

    })
    @Output({
    })
    @Description(desc = "保存账户管理协议")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        AccountProtocolVo accountProtocolVo = JSON.toJavaObject(paramObj, AccountProtocolVo.class);
        if (resourceAccountMapper.checkAccountProtocolIsRepeats(accountProtocolVo) > 0) {
            throw new ResourceCenterAccountProtocolRepeatException(accountProtocolVo.getName(),accountProtocolVo.getPort());
        }
        Long id = paramObj.getLong("id");
        if (id != null) {
            resourceAccountMapper.updateAccountProtocol(accountProtocolVo);
        } else {
            resourceAccountMapper.insertAccountProtocol(accountProtocolVo);
        }
        return null;
    }


}
