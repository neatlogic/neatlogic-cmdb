/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.resourcecenter.account;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.cmdb.dto.resourcecenter.AccountVo;
import codedriver.framework.cmdb.exception.resourcecenter.ResourceCenterAccountHasBeenReferredException;
import codedriver.framework.cmdb.exception.resourcecenter.ResourceCenterAccountNotFoundException;
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
public class AccountDeleteApi extends PrivateApiComponentBase {

    @Resource
    private ResourceCenterMapper resourceCenterMapper;

    @Override
    public String getToken() {
        return "resourcecenter/account/delete";
    }

    @Override
    public String getName() {
        return "删除资源中心账号";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "账号ID")
    })
    @Output({
    })
    @Description(desc = "删除资源中心账号")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        AccountVo account = resourceCenterMapper.getAccountById(id);
        if (account == null) {
            throw new ResourceCenterAccountNotFoundException(id);
        }
        if (resourceCenterMapper.checkAccountHasBeenReferredById(id) > 0) {
            throw new ResourceCenterAccountHasBeenReferredException(account.getName());
        }
        resourceCenterMapper.deleteAccountById(id);
        resourceCenterMapper.deleteAccountTagByAccountId(id);
        return null;
    }

}
