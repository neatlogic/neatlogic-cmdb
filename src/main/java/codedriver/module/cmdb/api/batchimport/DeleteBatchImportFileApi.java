/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.batchimport;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.exception.file.FileAccessDeniedException;
import codedriver.framework.exception.file.FileNotFoundException;
import codedriver.framework.exception.file.FileTypeHandlerNotFoundException;
import codedriver.framework.exception.user.NoTenantException;
import codedriver.framework.file.core.FileTypeHandlerFactory;
import codedriver.framework.file.core.IFileTypeHandler;
import codedriver.framework.file.dao.mapper.FileMapper;
import codedriver.framework.file.dto.FileVo;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CIENTITY_BATCH_IMPORT;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@AuthAction(action = CIENTITY_BATCH_IMPORT.class)
@Service
@OperationType(type = OperationTypeEnum.DELETE)
public class DeleteBatchImportFileApi extends PrivateApiComponentBase {

    @Autowired
    private FileMapper fileMapper;

    @Override
    public String getToken() {
        return "/cmdb/import/file/delete";
    }

    @Override
    public String getName() {
        return "删除批量导入文件";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "fileId", type = ApiParamType.LONG, isRequired = true, desc = "文件ID")
    })
    @Description(desc = "删除批量导入文件")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long fileId = paramObj.getLong("fileId");
        FileVo fileVo = fileMapper.getFileById(fileId);
        String tenantUuid = TenantContext.get().getTenantUuid();
        if (StringUtils.isBlank(tenantUuid)) {
            throw new NoTenantException();
        }
        if (fileVo != null) {
            IFileTypeHandler fileTypeHandler = FileTypeHandlerFactory.getHandler(fileVo.getType());
            if (fileTypeHandler != null) {
                if (fileTypeHandler.valid(UserContext.get().getUserUuid(), fileVo, null)) {
                    fileTypeHandler.deleteFile(fileId);
                } else {
                    throw new FileAccessDeniedException(fileVo.getName(), OperationTypeEnum.DELETE.getText());
                }
            } else {
                throw new FileTypeHandlerNotFoundException(fileVo.getType());
            }
        } else {
            throw new FileNotFoundException(fileId);
        }
        return null;
    }
}
