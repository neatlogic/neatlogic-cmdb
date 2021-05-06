/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.attrvaluehandler.handler;

import codedriver.framework.cmdb.attrvaluehandler.core.IAttrValueHandler;
import codedriver.framework.cmdb.enums.SearchExpression;
import org.springframework.stereotype.Service;


@Service
public class TextValueHandler implements IAttrValueHandler {

    @Override
    public String getType() {
        return "text";
    }

    @Override
    public String getName() {
        return "文本框";
    }

    @Override
    public String getIcon() {
        return "ts-code";
    }

    @Override
    public boolean isCanSearch() {
        return true;
    }

    @Override
    public boolean isSimple() {
        return true;
    }

    @Override
    public boolean isNeedWholeRow() {
        return false;
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
    public SearchExpression[] getSupportExpression() {
        return new SearchExpression[]{SearchExpression.EQ, SearchExpression.NE, SearchExpression.LI,
                SearchExpression.NL, SearchExpression.NOTNULL, SearchExpression.NULL};

    }

    @Override
    public int getSort() {
        return 1;
    }


}
