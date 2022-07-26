/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.resourcecenter.account;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.resourcecenter.AccountVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
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
import java.util.ArrayList;
import java.util.List;

/**
 * 查询资源中心账号列表（下拉列表专用）接口
 * @author linbq
 * @since 2021/5/30 14:42
 **/
@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AccountListForSelectApi extends PrivateApiComponentBase {

    @Resource
    private ResourceAccountMapper resourceAccountMapper;

    @Override
    public String getToken() {
        return "resourcecenter/account/list/forselect";
    }

    @Override
    public String getName() {
        return "查询资源中心账号列表（下拉列表专用）";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "模糊搜索")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = AccountVo[].class, desc = "账号列表")
    })
    @Description(desc = "查询资源中心账号列表（下拉列表专用）")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        List<AccountVo> accountVoList =new ArrayList<>();
        AccountVo accountVo = JSON.toJavaObject(paramObj, AccountVo.class);
        int accountCount = resourceAccountMapper.getAccountCount(accountVo);
        if (accountCount > 0) {
            accountVo.setRowNum(accountCount);
            accountVoList = resourceAccountMapper.getAccountListForSelect(accountVo);
        }
        return TableResultUtil.getResult(accountVoList, accountVo);
    }

}
