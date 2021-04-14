/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.exception.ci;

import codedriver.framework.exception.core.ApiRuntimeException;
import codedriver.framework.cmdb.dto.ci.CiVo;

import java.util.List;
import java.util.stream.Collectors;

public class CiHasBeenExtendedException extends ApiRuntimeException {
    public CiHasBeenExtendedException(String ciLabel, List<CiVo> childCiList) {
        super("配置项模型：" + ciLabel + "已被以下模型继承：" + childCiList.stream().map(CiVo::getLabel).collect(Collectors.joining(",")));
    }
}
