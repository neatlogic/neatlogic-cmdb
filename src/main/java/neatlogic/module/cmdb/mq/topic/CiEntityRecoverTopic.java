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

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import neatlogic.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import neatlogic.framework.mq.core.TopicBase;
import org.springframework.stereotype.Service;

@Service
public class CiEntityRecoverTopic extends TopicBase<CiEntityTransactionVo> {
    @Override
    public String getName() {
        return "cmdb/cientity/recover";
    }

    @Override
    public String getLabel() {
        return "配置项恢复";
    }

    @Override
    public String getDescription() {
        return "配置项恢复并生效后触发此主题";
    }

    @Override
    protected JSONObject generateTopicContent(CiEntityTransactionVo content) {
        return JSONObject.parseObject(JSONObject.toJSONString(content, SerializerFeature.DisableCircularReferenceDetect));
    }
}
