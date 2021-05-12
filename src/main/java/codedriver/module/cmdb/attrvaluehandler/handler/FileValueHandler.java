/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.attrvaluehandler.handler;

import codedriver.framework.cmdb.attrvaluehandler.core.IAttrValueHandler;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.enums.SearchExpression;
import com.alibaba.fastjson.JSONArray;
import org.springframework.stereotype.Service;


@Service
public class FileValueHandler implements IAttrValueHandler {

    @Override
    public String getType() {
        return "file";
    }

    @Override
    public String getName() {
        return "附件";
    }

    @Override
    public String getIcon() {
        return "ts-file";
    }

    @Override
    public boolean isCanSearch() {
        return false;
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public boolean isNeedTargetCi() {
        return false;
    }

    @Override
    public boolean isNeedConfig() {
        return true;
    }

    @Override
    public JSONArray getActualValueList(AttrVo attrVo, JSONArray valueList) {

        return null;
    }

    @Override
    public boolean isNeedWholeRow() {
        return false;
    }

    @Override
    public SearchExpression[] getSupportExpression() {
        return new SearchExpression[]{SearchExpression.NOTNULL, SearchExpression.NULL};
    }

    @Override
    public int getSort() {
        return 6;
    }
}
