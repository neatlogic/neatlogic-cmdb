/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.exception.ci;

import codedriver.framework.exception.core.ApiRuntimeException;

public class CiIsNotEmptyException extends ApiRuntimeException {
    public CiIsNotEmptyException(Long ciId, int ciEntityCount) {
        super("配置项模型：" + ciId + "拥有：" + ciEntityCount + "个配置项，请先删除");
    }
}
