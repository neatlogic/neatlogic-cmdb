/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.constvalue;

import codedriver.framework.autoexec.dto.AutoexecJobSourceVo;
import codedriver.framework.autoexec.source.IAutoexecJobSource;

import java.util.ArrayList;
import java.util.List;

public enum JobSource implements IAutoexecJobSource {
    DISCOVERY("自动发现", "discovery");
    private final String text;
    private final String value;

    JobSource(String _text, String _value) {
        this.text = _text;
        this.value = _value;
    }

    public String getValue() {
        return value;
    }

    public String getText() {
        return text;
    }

    public static String getText(String _status) {
        for (JobSource s : JobSource.values()) {
            if (s.getValue().equals(_status)) {
                return s.getText();
            }
        }
        return "";
    }

    /**
     * @return 返回对应的来源
     */
    @Override
    public List<AutoexecJobSourceVo> getSource() {
        List<AutoexecJobSourceVo> list = new ArrayList<>();
        for (JobSource s : JobSource.values()) {
            AutoexecJobSourceVo source = new AutoexecJobSourceVo();
            source.setSource(s.value);
            source.setSourceName(s.text);
            list.add(source);
        }
        return list;
    }

}
