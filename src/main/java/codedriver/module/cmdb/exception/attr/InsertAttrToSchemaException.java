/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.exception.attr;

import codedriver.framework.exception.core.ApiRuntimeException;

public class InsertAttrToSchemaException extends ApiRuntimeException {
    public InsertAttrToSchemaException(String attrName) {
        super("无法将属性：" + attrName + " 添加到数据表，具体错误请查看系统日志");
    }
}
