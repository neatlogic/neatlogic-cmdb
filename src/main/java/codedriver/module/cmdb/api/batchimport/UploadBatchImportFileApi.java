package codedriver.module.cmdb.api.batchimport;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.util.FileUtil;
import codedriver.framework.exception.file.*;
import codedriver.framework.exception.user.NoTenantException;
import codedriver.framework.file.core.FileTypeHandlerFactory;
import codedriver.framework.file.core.IFileTypeHandler;
import codedriver.framework.file.core.LocalFileSystemHandler;
import codedriver.framework.file.core.MinioFileSystemHandler;
import codedriver.framework.file.dao.mapper.FileMapper;
import codedriver.framework.file.dto.FileTypeVo;
import codedriver.framework.file.dto.FileVo;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import codedriver.module.cmdb.dao.mapper.batchimport.ImportMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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

@AuthAction(name = "CIENTITY_BATCH_IMPORT")
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
		return "上传批量导入文件";
	}

	@Override
	public String getConfig() {
		return null;
	}

	@Input({
			@Param(name = "param", type = ApiParamType.STRING, desc = "附件参数名称", isRequired = true),
			@Param(name = "type", type = ApiParamType.STRING, desc = "附件类型", isRequired = true)
	})
	@Output({@Param(explode = FileVo.class)})
	@Description(desc = "上传批量导入文件")
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

		if (multipartFile != null && multipartFile.getName() != null) {
			String userUuid = UserContext.get().getUserUuid(true);
			String oldFileName = multipartFile.getOriginalFilename();
			if(!oldFileName.endsWith(".xls") && !oldFileName.endsWith(".xlsx")){
				throw new ExcelFormatIllegalException(".xls或.xlsx");
			}
			Long size = multipartFile.getSize();
			// 如果配置为空代表不受任何限制
			if (fileTypeConfigVo != null) {
				boolean isAllowed = false;
				Long maxSize = 0L;
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
				if (maxSize != null && maxSize > 0 && size > maxSize) {
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
			String filePath = null;
			try {
				filePath = FileUtil.saveData(MinioFileSystemHandler.NAME, tenantUuid, multipartFile.getInputStream(), fileVo.getId().toString(), fileVo.getContentType(), fileVo.getType().toLowerCase());
			} catch (Exception ex) {
				// 如果minio出现异常，则上传到本地
				logger.error(ex.getMessage(), ex);
				filePath = FileUtil.saveData(LocalFileSystemHandler.NAME, tenantUuid, multipartFile.getInputStream(), fileVo.getId().toString(), fileVo.getContentType(), fileVo.getType().toLowerCase());
			}
			fileVo.setPath(filePath);
			fileMapper.insertFile(fileVo);
			fileTypeHandler.afterUpload(fileVo, paramObj);
			FileVo file = fileMapper.getFileById(fileVo.getId());
			file.setUrl("api/binary/file/download?id=" + fileVo.getId());
			/** 插入cmdb批量导入文件表 */
			importMapper.insertCmdbImportFile(file);
			return file;
		}
		return null;
	}

}