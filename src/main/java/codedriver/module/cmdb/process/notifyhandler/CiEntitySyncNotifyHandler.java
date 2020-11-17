package codedriver.module.cmdb.process.notifyhandler;

import java.util.List;

import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.common.dto.ValueTextVo;
import codedriver.framework.dto.ConditionParamVo;
import codedriver.framework.notify.core.NotifyPolicyHandlerBase;

@Component
public class CiEntitySyncNotifyHandler extends NotifyPolicyHandlerBase {

    @Override
    public String getName() {
        return "配置项同步";
    }

    @Override
    protected List<ValueTextVo> myNotifyTriggerList() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected List<ConditionParamVo> mySystemParamList() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected List<ConditionParamVo> mySystemConditionOptionList() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void myAuthorityConfig(JSONObject config) {
        // TODO Auto-generated method stub

    }

}