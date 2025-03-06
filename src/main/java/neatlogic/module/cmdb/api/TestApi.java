/*
 * Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package neatlogic.module.cmdb.api;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.NotifyPolicyUtil;
import neatlogic.module.cmdb.notify.handler.CmdbNotifyPolicyHandler;
import neatlogic.module.cmdb.notify.handler.CmdbNotifyTriggerType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class TestApi extends PrivateApiComponentBase {


    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        //发送通知
        /*NotifyPolicyUtil.execute(CmdbNotifyPolicyHandler.class.getSimpleName(), CmdbNotifyTriggerType.CIMODITY, DeployJobMessageHandler.class
                , notifyPolicyVo, null, null, receiverMap
                , jobInfo, null, notifyAuditMessage);

        NotifyPolicyUtil.execute(notifyPolicyVo.getHandler(), trigger, DeployJobMessageHandler.class
                , notifyPolicyVo, null, null, receiverMap
                , jobInfo, null, notifyAuditMessage);*/
        CiVo ciVo = new CiVo();
        ciVo.setName("ciName");
        ciVo.setLabel("模型名称");
        List<CiEntityVo> ciEntityList = new ArrayList<>();
        ciEntityList.add(new CiEntityVo(){{this.setName("aaa");}});
        ciEntityList.add(new CiEntityVo(){{this.setName("bbb");}});
        NotifyPolicyUtil.executeAsync(CmdbNotifyPolicyHandler.class, CmdbNotifyTriggerType.CIENTITYINVALID, ciEntityList);

        return CmdbNotifyPolicyHandler.class.getName();
    }

    @Override
    public String getToken() {
        return "cmdb/test";
    }

    @Override
    public String getName() {
        return "测试通知";
    }
}
