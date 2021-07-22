/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.notify.handler;

import codedriver.framework.autoexec.auth.AUTOEXEC_COMBOP_ADD;
import codedriver.framework.dto.ConditionParamVo;
import codedriver.framework.notify.core.INotifyPolicyHandlerGroup;
import codedriver.framework.notify.core.NotifyHandlerType;
import codedriver.framework.notify.core.NotifyPolicyHandlerBase;
import codedriver.framework.notify.dto.NotifyTriggerTemplateVo;
import codedriver.framework.notify.dto.NotifyTriggerVo;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: linbq
 * @since: 2021/4/15 9:47
 **/
@Component
public class CmdbNotifyPolicyHandler extends NotifyPolicyHandlerBase {
    @Override
    public String getName() {
        return "配置管理";
    }

    /**
     * 绑定权限，每种handler对应不同的权限
     */
    @Override
    public String getAuthName() {
        return AUTOEXEC_COMBOP_ADD.class.getSimpleName();
    }

    @Override
    public INotifyPolicyHandlerGroup getGroup() {
        return null;
    }

    @Override
    protected List<NotifyTriggerVo> myNotifyTriggerList() {
        return new ArrayList<>();
    }

    @Override
    protected List<NotifyTriggerTemplateVo> myNotifyTriggerTemplateList(NotifyHandlerType type) {
        return new ArrayList<>();
    }

    @Override
    protected List<ConditionParamVo> mySystemParamList() {
        return new ArrayList<>();
    }

    @Override
    protected List<ConditionParamVo> mySystemConditionOptionList() {
        return new ArrayList<>();
    }

    @Override
    protected void myAuthorityConfig(JSONObject config) {

    }
}
