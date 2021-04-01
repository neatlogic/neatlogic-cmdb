/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.exception.attr;

import codedriver.framework.exception.core.ApiRuntimeException;

public class AttrTargetCiIdNotFoundException extends ApiRuntimeException {
    public AttrTargetCiIdNotFoundException(String attrName) {
        super("配置项模型属性：" + attrName + " 没有关联目标模型");
    }
}
