/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.cmdb.api.ci;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CI_MODIFY;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.enums.RelDirectionType;
import neatlogic.framework.cmdb.exception.ci.ParentCiNotFoundException;
import neatlogic.framework.cmdb.exception.ci.RelCiNotFoundException;
import neatlogic.framework.cmdb.exception.ci.TargetCiNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import neatlogic.framework.util.RegexUtils;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.ci.RelMapper;
import neatlogic.module.cmdb.service.attr.AttrService;
import neatlogic.module.cmdb.service.ci.CiService;
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
@Transactional
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
        return "nmcac.importciapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "name", type = ApiParamType.REGEX, rule = RegexUtils.ENGLISH_NUMBER_NAME, xss = true, isRequired = true, maxLength = 25, desc = "common.uniquename"),
            @Param(name = "label", type = ApiParamType.STRING, desc = "common.cnname", xss = true, maxLength = 100, isRequired = true),
            @Param(name = "typeId", type = ApiParamType.LONG, desc = "common.typeid", isRequired = true),
            @Param(name = "icon", type = ApiParamType.STRING, isRequired = true, desc = "common.icon"), @Param(name = "file", type = ApiParamType.FILE, isRequired = true, desc = "common.file")})
    @Description(desc = "nmcac.importciapi.getname")
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
