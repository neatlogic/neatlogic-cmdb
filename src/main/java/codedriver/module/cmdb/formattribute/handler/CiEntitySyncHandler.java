package codedriver.module.cmdb.formattribute.handler;

import codedriver.framework.common.constvalue.ParamType;
import codedriver.framework.process.dto.AttributeDataVo;
import codedriver.framework.process.exception.form.AttributeValidException;
import codedriver.framework.process.formattribute.core.FormHandlerBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;

@Component
public class CiEntitySyncHandler extends FormHandlerBase {

    @Override
    public String getHandler() {
        return "cientityselect";
    }

    @Override
    public boolean valid(AttributeDataVo attributeDataVo, JSONObject configObj) throws AttributeValidException {
        return true;
    }

    @Override
    public Object valueConversionText(AttributeDataVo attributeDataVo, JSONObject configObj) {
        if (!attributeDataVo.dataIsEmpty()) {
            return "已更新";
        } else {
            return "";
        }
    }

    @Override
    public String getHandlerName() {
        return "配置项修改组件";
    }

    @Override
    public String getIcon() {
        return "ts-m-cmdb";
    }

    @Override
    public ParamType getParamType() {
        return null;
    }

    @Override
    public String getDataType() {
        return null;
    }

    @Override
    public boolean isConditionable() {
        return false;
    }

    @Override
    public boolean isShowable() {
        return true;
    }

    @Override
    public boolean isValueable() {
        return false;
    }

    @Override
    public boolean isFilterable() {
        return false;
    }

    @Override
    public boolean isExtendable() {
        return true;
    }

    @Override
    public String getModule() {
        return "cmdb";
    }

    @Override
    public boolean isForTemplate() {
        return false;
    }

    @Override
    public int getSort() {
        return 10;
    }

    @Override
    public boolean isAudit() {
        return true;
    }

    @Override
    public String getHandlerType(String model) {
        return null;
    }

}
