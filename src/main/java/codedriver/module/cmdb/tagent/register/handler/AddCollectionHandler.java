/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.tagent.register.handler;

import codedriver.framework.tagent.dto.TagentVo;
import codedriver.framework.tagent.register.core.AfterRegisterBase;
import codedriver.framework.transaction.core.AfterTransactionJob;
import org.springframework.stereotype.Service;

@Service
public class AddCollectionHandler extends AfterRegisterBase {
    @Override
    public void myExecute(TagentVo pTagentVo) {

    }
}
