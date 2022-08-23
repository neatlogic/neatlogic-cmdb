package codedriver.module.cmdb.api.resourcecenter.account;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.resourcecenter.AccountProtocolVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceAccountMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AccountProtocolSearchApi extends PrivateApiComponentBase {

    @Resource
    private ResourceAccountMapper resourceAccountMapper;

    @Override
    public String getName() {
        return "获取账户协议";
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
    @Description(desc = "查找账户管理协议列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        AccountProtocolVo searchVo = JSON.toJavaObject(paramObj, AccountProtocolVo.class);
        List<AccountProtocolVo> accountProtocolList = resourceAccountMapper.searchAccountProtocolListByProtocolName(searchVo);
        return TableResultUtil.getResult(accountProtocolList, searchVo);
    }


}
