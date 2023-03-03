/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.cmdb.mq.topic;

import neatlogic.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import neatlogic.framework.mq.core.TopicBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class CiEntityDeleteTopic extends TopicBase<CiEntityTransactionVo> {
    @Override
    public String getName() {
        return "cmdb/cientity/delete";
    }

    @Override
    public String getLabel() {
        return "配置项删除";
    }

    @Override
    public String getDescription() {
        return "配置项删除并生效后触发此主题";
    }

    @Override
    protected JSONObject generateTopicContent(CiEntityTransactionVo content) {
        //FIXME 补充内容
        JSONObject dataObj = new JSONObject();
        dataObj.put("attrEntityData", content.getAttrEntityData());
        dataObj.put("relEntityData", content.getRelEntityData());
        return dataObj;
    }
}
