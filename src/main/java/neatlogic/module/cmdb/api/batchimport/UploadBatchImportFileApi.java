/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.cmdb.api.batchimport;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CIENTITY_BATCH_IMPORT;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.util.FileUtil;
import neatlogic.framework.exception.file.*;
import neatlogic.framework.exception.user.NoTenantException;
import neatlogic.framework.file.core.FileTypeHandlerFactory;
import neatlogic.framework.file.core.IFileTypeHandler;
import neatlogic.framework.file.dao.mapper.FileMapper;
import neatlogic.framework.file.dto.FileTypeVo;
import neatlogic.framework.file.dto.FileVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.batchimport.ImportMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@AuthAction(action = CIENTITY_BATCH_IMPORT.class)
@Service
@OperationType(type = OperationTypeEnum.CREATE)
@Transactional
public class UploadBatchImportFileApi extends PrivateBinaryStreamApiComponentBase {
    static Logger logger = LoggerFactory.getLogger(UploadBatchImportFileApi.class);

    @Autowired
    private FileMapper fileMapper;

    @Autowired
    private ImportMapper importMapper;

    @Override
    public String getToken() {
        return "/cmdb/import/file/upload";
    }

    @Override
    public String getName() {
        return "nmcab.uploadbatchimportfileapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "param", type = ApiParamType.STRING, desc = "common.fileparamname", isRequired = true),
            @Param(name = "type", type = ApiParamType.STRING, desc = "common.filetype", isRequired = true)
    })
    @Output({@Param(explode = FileVo.class)})
    @Description(desc = "nmcab.uploadbatchimportfileapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String tenantUuid = TenantContext.get().getTenantUuid();
        if (StringUtils.isBlank(tenantUuid)) {
            throw new NoTenantException();
        }
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        String paramName = paramObj.getString("param");
        String type = paramObj.getString("type");

        List<FileTypeVo> fileTypeList = FileTypeHandlerFactory.getActiveFileTypeHandler();
        FileTypeVo fileTypeVo = null;
        for (FileTypeVo f : fileTypeList) {
            if (f.getName().equalsIgnoreCase(type)) {
                fileTypeVo = f;
                break;
            }
        }
        if (fileTypeVo == null) {
            throw new FileTypeHandlerNotFoundException(type);
        }
        FileTypeVo fileTypeConfigVo = fileMapper.getFileTypeConfigByType(fileTypeVo.getName());

        MultipartFile multipartFile = multipartRequest.getFile(paramName);

        if (multipartFile != null) {
            String userUuid = UserContext.get().getUserUuid(true);
            String oldFileName = multipartFile.getOriginalFilename();
            if (StringUtils.isBlank(oldFileName) || (!oldFileName.endsWith(".xls") && !oldFileName.endsWith(".xlsx"))) {
                throw new ExcelFormatIllegalException();
            }
            long size = multipartFile.getSize();
            // 如果配置为空代表不受任何限制
            if (fileTypeConfigVo != null) {
                boolean isAllowed = false;
                long maxSize = 0L;
                String fileExt = oldFileName.substring(oldFileName.lastIndexOf(".") + 1).toLowerCase();
                JSONObject configObj = fileTypeConfigVo.getConfigObj();
                JSONArray whiteList = new JSONArray();
                JSONArray blackList = new JSONArray();
                if (size == 0) {
                    throw new EmptyFileException();
                }
                if (configObj != null) {
                    whiteList = configObj.getJSONArray("whiteList");
                    blackList = configObj.getJSONArray("blackList");
                    maxSize = configObj.getLongValue("maxSize");
                }
                if (whiteList != null && whiteList.size() > 0) {
                    for (int i = 0; i < whiteList.size(); i++) {
                        if (fileExt.equalsIgnoreCase(whiteList.getString(i))) {
                            isAllowed = true;
                            break;
                        }
                    }
                } else if (blackList != null && blackList.size() > 0) {
                    isAllowed = true;
                    for (int i = 0; i < blackList.size(); i++) {
                        if (fileExt.equalsIgnoreCase(blackList.getString(i))) {
                            isAllowed = false;
                            break;
                        }
                    }
                } else {
                    isAllowed = true;
                }
                if (!isAllowed) {
                    throw new FileExtNotAllowedException(fileExt);
                }
                if (maxSize > 0 && size > maxSize) {
                    throw new FileTooLargeException(size, maxSize);
                }
            }

            IFileTypeHandler fileTypeHandler = FileTypeHandlerFactory.getHandler(type);
            if (fileTypeHandler == null) {
                throw new FileTypeHandlerNotFoundException(type);
            }

            FileVo fileVo = new FileVo();
            fileVo.setName(oldFileName);
            fileVo.setSize(size);
            fileVo.setUserUuid(userUuid);
            fileVo.setType(type);
            fileVo.setContentType(multipartFile.getContentType());
            String filePath = FileUtil.saveData(tenantUuid, multipartFile.getInputStream(), fileVo);
            fileVo.setPath(filePath);
            fileMapper.insertFile(fileVo);
            fileTypeHandler.afterUpload(fileVo, paramObj);
            FileVo file = fileMapper.getFileById(fileVo.getId());
            file.setUrl("api/binary/file/download?id=" + fileVo.getId());
            /* 插入cmdb批量导入文件表 */
            importMapper.insertCmdbImportFile(file);
            return file;
        }
        return null;
    }

}
