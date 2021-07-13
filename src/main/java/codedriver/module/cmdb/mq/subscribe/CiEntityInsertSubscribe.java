/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.mq.subscribe;

import codedriver.framework.mq.core.SubscribeHandlerBase;
import org.springframework.stereotype.Service;

import javax.jms.JMSException;
import javax.jms.TextMessage;

@Service
public class CiEntityInsertSubscribe extends SubscribeHandlerBase {
    @Override
    public String getName() {
        return "配置添加处理组件";
    }

    @Override
    protected void myOnMessage(TextMessage m) {
        try {
            System.out.println(m.getText());
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
