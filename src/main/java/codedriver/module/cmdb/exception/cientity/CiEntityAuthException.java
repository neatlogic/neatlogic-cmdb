/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.exception.cientity;

import codedriver.framework.exception.core.ApiRuntimeException;

public class CiEntityAuthException extends ApiRuntimeException {
    public CiEntityAuthException(String action) {
        super("您没有权限" + action + "配置项");
    }

    public CiEntityAuthException(String ciLabel, String action) {
        super("您没有权限" + action + ciLabel + "的配置项");
    }
}
