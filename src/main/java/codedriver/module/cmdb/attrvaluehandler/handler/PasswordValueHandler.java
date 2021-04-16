/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.attrvaluehandler.handler;

import codedriver.framework.cmdb.attrvaluehandler.core.IAttrValueHandler;
import org.springframework.stereotype.Service;


@Service
public class PasswordValueHandler implements IAttrValueHandler {

    @Override
    public String getType() {
        return "password";
    }

    @Override
    public String getName() {
        return "密码";
    }

    @Override
    public String getIcon() {
        return "ts-eye-close";
    }

    @Override
    public boolean isCanSearch() {
        return false;
    }

    @Override
    public boolean isSimple() {
        return true;
    }

    @Override
    public boolean isNeedTargetCi() {
        return false;
    }

    @Override
    public boolean isNeedConfig() {
        return false;
    }

    @Override
    public boolean isNeedWholeRow() {
        return false;
    }

}
