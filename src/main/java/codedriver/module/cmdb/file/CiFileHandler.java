/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.file;

import codedriver.framework.cmdb.exception.ci.CiViewSqlIrregularException;
import codedriver.framework.common.util.FileUtil;
import codedriver.framework.file.core.FileTypeHandlerBase;
import codedriver.framework.file.dto.FileVo;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.IOUtils;
import org.dom4j.DocumentHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class CiFileHandler extends FileTypeHandlerBase {

    @Override
    public boolean valid(String userUuid, FileVo fileVo, JSONObject jsonObj) {
        return true;
    }

    @Override
    public String getDisplayName() {
        return "虚拟模型配置文件";
    }

    @Override
    protected boolean myDeleteFile(Long fileId) {
        return false;
    }

    @Override
    public void afterUpload(FileVo fileVo, JSONObject jsonObj) {
        try {
            String xml = IOUtils.toString(FileUtil.getData(fileVo.getPath()), StandardCharsets.UTF_8);
            DocumentHelper.parseText(xml);
        } catch (Exception e) {
            throw new CiViewSqlIrregularException();
        }
    }

    @Override
    public String getName() {
        return "CI";
    }

}
