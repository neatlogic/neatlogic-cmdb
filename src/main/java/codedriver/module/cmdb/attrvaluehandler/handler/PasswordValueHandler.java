/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.attrvaluehandler.handler;

import codedriver.framework.cmdb.attrvaluehandler.core.IAttrValueHandler;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.enums.SearchExpression;
import codedriver.framework.common.util.RC4Util;
import com.alibaba.fastjson.JSONArray;
import org.apache.commons.collections4.CollectionUtils;
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
    public boolean isCanInput() {
        return true;
    }


    @Override
    public boolean isCanImport() {
        return true;
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

    @Override
    public SearchExpression[] getSupportExpression() {
        return new SearchExpression[]{SearchExpression.NOTNULL, SearchExpression.NULL};
    }

    @Override
    public int getSort() {
        return 5;
    }

    @Override
    public void transferValueListToSave(AttrVo attrVo, JSONArray valueList) {
        if (CollectionUtils.isNotEmpty(valueList)) {
            for (int i = 0; i < valueList.size(); i++) {
                String value = valueList.getString(i);
                if (!value.startsWith("RC4:")) {
                    valueList.set(i, "RC4:" + RC4Util.encrypt(value));
                }
            }
        }
    }

    /**
     * 将值转换成显示的形式
     *
     * @param valueList 数据库的数据
     * @return 用于显示数据
     */
    @Override
    public void transferValueListToDisplay(JSONArray valueList) {
        if (CollectionUtils.isNotEmpty(valueList)) {
            for (int i = 0; i < valueList.size(); i++) {
                String value = valueList.getString(i);
                if (value.startsWith("RC4:")) {
                    valueList.set(i, RC4Util.decrypt(value.substring(4)));
                }
            }
        }
    }

    @Override
    public void transferValueListToExport(AttrVo attrVo, JSONArray valueList) {
        if (CollectionUtils.isNotEmpty(valueList)) {
            for (int i = 0; i < valueList.size(); i++) {
                valueList.set(i, "*******");
            }
        }
    }
}
