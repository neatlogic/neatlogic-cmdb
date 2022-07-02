/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.ci;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.ci.RelVo;
import codedriver.framework.cmdb.enums.RelDirectionType;
import codedriver.framework.cmdb.exception.ci.ParentCiNotFoundException;
import codedriver.framework.cmdb.exception.ci.RelCiNotFoundException;
import codedriver.framework.cmdb.exception.ci.TargetCiNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import codedriver.framework.util.RegexUtils;
import codedriver.module.cmdb.auth.label.CI_MODIFY;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dao.mapper.ci.RelMapper;
import codedriver.module.cmdb.service.attr.AttrService;
import codedriver.module.cmdb.service.ci.CiService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ObjectInputStream;
import java.util.zip.ZipInputStream;

@Service
@AuthAction(action = CI_MODIFY.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class ImportCiApi extends PrivateBinaryStreamApiComponentBase {
    @Resource
    private CiService ciService;

    @Resource
    private AttrService attrService;

    @Resource
    private RelMapper relMapper;

    @Resource
    private CiMapper ciMapper;


    @Override
    public String getToken() {
        return "/cmdb/ci/import";
    }

    @Override
    public String getName() {
        return "导入配置项模型";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "name", type = ApiParamType.REGEX, rule = RegexUtils.ENGLISH_NUMBER_NAME, xss = true, isRequired = true, maxLength = 25, desc = "唯一标识"),
            @Param(name = "label", type = ApiParamType.STRING, desc = "中文名称", xss = true, maxLength = 100, isRequired = true),
            @Param(name = "typeId", type = ApiParamType.LONG, desc = "类型id", isRequired = true),
            @Param(name = "icon", type = ApiParamType.STRING, isRequired = true, desc = "图标"), @Param(name = "file", type = ApiParamType.FILE, isRequired = true, desc = "模型文件")})
    @Description(desc = "导入配置项模型接口")
    @Transactional
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        MultipartFile multipartFile = multipartRequest.getFile("file");
        CiVo newCiVo = JSONObject.toJavaObject(paramObj, CiVo.class);
        if (multipartFile != null) {
            ZipInputStream zin = new ZipInputStream(multipartFile.getInputStream());
            while (zin.getNextEntry() != null) {
                ObjectInputStream objectInputStream = new ObjectInputStream(zin);
                CiVo ciVo = (CiVo) objectInputStream.readObject();
                ciVo.setName(newCiVo.getName());
                ciVo.setIcon(newCiVo.getIcon());
                ciVo.setLabel(newCiVo.getLabel());
                ciVo.setTypeId(newCiVo.getTypeId());
                zin.closeEntry();
                //检查父模型是否存在
                if (StringUtils.isNotBlank(ciVo.getParentCiName())) {
                    CiVo parentCiVo = ciMapper.getCiByName(ciVo.getParentCiName());
                    if (parentCiVo == null) {
                        throw new ParentCiNotFoundException(ciVo.getParentCiName());
                    }
                    ciVo.setParentCiId(parentCiVo.getId());
                }
                //检查关联属性是否存在
                if (CollectionUtils.isNotEmpty(ciVo.getAttrList())) {
                    for (AttrVo attrVo : ciVo.getAttrList()) {
                        attrVo.setCiId(ciVo.getId());
                        if (StringUtils.isNotBlank(attrVo.getTargetCiName())) {
                            CiVo targetCiVo = ciMapper.getCiByName(attrVo.getTargetCiName());
                            if (targetCiVo == null) {
                                throw new TargetCiNotFoundException(attrVo.getName(), attrVo.getTargetCiName());
                            }
                            attrVo.setTargetCiId(targetCiVo.getId());
                        }
                    }
                }
                //检查关系对端是否存在
                if (CollectionUtils.isNotEmpty(ciVo.getRelList())) {
                    for (RelVo relVo : ciVo.getRelList()) {
                        if (relVo.getDirection().equals(RelDirectionType.FROM.getValue())) {
                            relVo.setFromCiId(ciVo.getId());
                            relVo.setFromName(ciVo.getName());
                            relVo.setFromLabel(ciVo.getLabel());
                            CiVo toCiVo = ciMapper.getCiByName(relVo.getToCiName());
                            if (toCiVo == null) {
                                throw new RelCiNotFoundException(relVo.getToName(), relVo.getToCiName());
                            }
                            relVo.setToCiId(toCiVo.getId());
                        } else if (relVo.getDirection().equals(RelDirectionType.TO.getValue())) {
                            relVo.setToCiId(ciVo.getId());
                            relVo.setToName(ciVo.getName());
                            relVo.setToLabel(ciVo.getLabel());
                            CiVo fromCiVo = ciMapper.getCiByName(relVo.getFromCiName());
                            if (fromCiVo == null) {
                                throw new RelCiNotFoundException(relVo.getFromName(), relVo.getFromCiName());
                            }
                            relVo.setFromCiId(fromCiVo.getId());
                        }
                    }
                }

                ciService.insertCi(ciVo);
                if (CollectionUtils.isNotEmpty(ciVo.getAttrList())) {
                    for (AttrVo attrVo : ciVo.getAttrList()) {
                        attrService.insertAttr(attrVo);
                    }
                }
                if (CollectionUtils.isNotEmpty(ciVo.getRelList())) {
                    for (RelVo relVo : ciVo.getRelList()) {
                        relMapper.insertRel(relVo);
                    }
                }
            }
            zin.close();
        } else {
            throw new ParamNotExistsException("file");
        }

        return null;
    }

}
