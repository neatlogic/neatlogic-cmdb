package neatlogic.module.cmdb.api.resourcecenter.account;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountProtocolVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.framework.cmdb.auth.label.CMDB;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceAccountMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = CMDB.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AccountProtocolSearchApi extends PrivateApiComponentBase {

    @Resource
    private ResourceAccountMapper resourceAccountMapper;

    @Override
    public String getName() {
        return "获取帐号协议";
    }

    @Override
    public String getToken() {
        return "resourcecenter/account/protocol/search";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, xss = true, desc = "关键词"),
            @Param(name = "isExcludeTagent", rule = "0,1", type = ApiParamType.ENUM, defaultValue = "0", desc = "是否排除tagent"),
    })
    @Output({
            @Param(name = "tbodyList", explode = AccountProtocolVo[].class, desc = "协议列表")
    })
    @Description(desc = "查找帐号管理协议列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        AccountProtocolVo searchVo = JSON.toJavaObject(paramObj, AccountProtocolVo.class);
        List<AccountProtocolVo> accountProtocolList = resourceAccountMapper.searchAccountProtocolListByProtocolName(searchVo);
        return TableResultUtil.getResult(accountProtocolList, searchVo);
    }


}
