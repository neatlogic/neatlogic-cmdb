package codedriver.module.cmdb.api.resourcecenter.account;

import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.cmdb.dto.resourcecenter.AccountProtocolVo;
import codedriver.framework.cmdb.exception.resourcecenter.ResourceCenterAccountProtocolNotFoundException;
import codedriver.framework.cmdb.exception.resourcecenter.ResourceCenterAccountProtocolRepeatException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.dto.FieldValidResultVo;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.IValid;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Resource;

public class AccountProtocolSaveApi extends PrivateApiComponentBase {

    @Resource
    private ResourceCenterMapper resourceCenterMapper;

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
            @Param(name = "id", type = ApiParamType.LONG, desc = "协议ID"),
            @Param(name = "name", type = ApiParamType.STRING, desc = "协议名称"),

    })
    @Output({
    })
    @Description(desc = "保存账户管理协议")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        AccountProtocolVo accountProtocolVo = JSON.toJavaObject(paramObj, AccountProtocolVo.class);
        Long id = paramObj.getLong("id");
        if (resourceCenterMapper.checkAccountProtocolIsRepeats(accountProtocolVo) > 0) {
            throw new ResourceCenterAccountProtocolRepeatException(accountProtocolVo.getName());
        }
        if (id != null) {
            if (resourceCenterMapper.checkAccountProtocolIsExistsById(id) == 0) {
                throw new ResourceCenterAccountProtocolNotFoundException(id);
            }
            resourceCenterMapper.updateAccountProtocol(accountProtocolVo);
        } else {
            resourceCenterMapper.insertAccountProtocol(accountProtocolVo);
        }
        return null;
    }

    public IValid name() {
        return value -> {
            AccountProtocolVo accountProtocolVo = JSON.toJavaObject(value, AccountProtocolVo.class);
            if (resourceCenterMapper.checkAccountProtocolIsRepeats(accountProtocolVo) > 0) {
                return new FieldValidResultVo(new ResourceCenterAccountProtocolRepeatException(accountProtocolVo.getName()));
            }
            return new FieldValidResultVo();
        };
    }


}
