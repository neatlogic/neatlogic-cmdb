/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.ci;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.exception.ci.CiAuthException;
import codedriver.framework.cmdb.exception.ci.VirtualCiSettingFileNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.util.FileUtil;
import codedriver.framework.file.dao.mapper.FileMapper;
import codedriver.framework.file.dto.FileVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.service.ci.CiAuthChecker;
import codedriver.module.cmdb.service.ci.CiService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class SaveCiApi extends PrivateApiComponentBase {

    @Resource
    private CiService ciService;

    @Resource
    private FileMapper fileMapper;


    @Override
    public String getToken() {
        return "/cmdb/ci/save";
    }

    @Override
    public String getName() {
        return "保存模型";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "id，不提供代表新增模型"),
            @Param(name = "name", type = ApiParamType.REGEX, rule = "^[a-zA-Z_\\d]+$", xss = true, isRequired = true, maxLength = 25, desc = "英文名称"),
            @Param(name = "label", type = ApiParamType.STRING, desc = "中文名称", xss = true, maxLength = 100,
                    isRequired = true),
            @Param(name = "description", type = ApiParamType.STRING, desc = "备注", maxLength = 500, xss = true),
            @Param(name = "icon", type = ApiParamType.STRING, desc = "图标"),
            @Param(name = "typeId", type = ApiParamType.LONG, desc = "类型id", isRequired = true),
            @Param(name = "parentCiId", type = ApiParamType.LONG, desc = "父配置项id"),
            @Param(name = "isAbstract", type = ApiParamType.INTEGER, defaultValue = "0", desc = "是否抽象模型"),
            @Param(name = "isVirtual", type = ApiParamType.INTEGER, defaultValue = "0", desc = "是否虚拟模型"),
            @Param(name = "isPrivate", type = ApiParamType.INTEGER, defaultValue = "0", desc = "是否私有模型"),
            @Param(name = "isMenu", type = ApiParamType.INTEGER, defaultValue = "0", desc = "是否在菜单显示")})
    @Output({@Param(name = "id", type = ApiParamType.STRING, desc = "模型id")})
    @Description(desc = "保存模型接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        CiVo ciVo = JSONObject.toJavaObject(jsonObj, CiVo.class);
        Long ciId = jsonObj.getLong("id");
        if (Objects.equals(ciVo.getIsVirtual(), 1) && ciVo.getFileId() != null) {
            FileVo fileVo = fileMapper.getFileById(ciVo.getFileId());
            if (fileVo == null) {
                throw new VirtualCiSettingFileNotFoundException();
            }
            String xml = IOUtils.toString(FileUtil.getData(fileVo.getPath()), StandardCharsets.UTF_8);
            if (StringUtils.isBlank(xml)) {
                throw new VirtualCiSettingFileNotFoundException();
            }
            ciVo.setViewXml(xml);
        }
        if (ciId == null) {
            if (!CiAuthChecker.chain().checkCiManagePrivilege().check()) {
                throw new CiAuthException();
            }
            ciService.insertCi(ciVo);
        } else {
            if (!CiAuthChecker.chain().checkCiManagePrivilege(ciId).check()) {
                throw new CiAuthException();
            }
            ciService.updateCi(ciVo);
        }
        return ciVo.getId();
    }

}
