/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.mq.topic;

import codedriver.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import codedriver.framework.mq.core.TopicBase;
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
