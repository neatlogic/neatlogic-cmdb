/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.cmdb.api.ci;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CI_MODIFY;
import neatlogic.framework.cmdb.dto.ci.*;
import neatlogic.framework.cmdb.enums.RelDirectionType;
import neatlogic.framework.cmdb.exception.attr.AttrNameRepeatException;
import neatlogic.framework.cmdb.exception.ci.*;
import neatlogic.framework.cmdb.exception.reltype.RelTypeNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.binarystream.PrivateBinaryStreamApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.ci.RelMapper;
import neatlogic.module.cmdb.dao.mapper.cischema.CiSchemaMapper;
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
import java.util.*;
import java.util.zip.ZipInputStream;

@Service
@AuthAction(action = CI_MODIFY.class)
@OperationType(type = OperationTypeEnum.CREATE)
@Transactional
public class ImportCiApi extends PrivateBinaryStreamApiComponentBase {
    @Resource
    private CiService ciService;

    @Resource
    private CiSchemaMapper ciSchemaMapper;

    @Resource
    private AttrService attrService;

    @Resource
    private RelMapper relMapper;

    @Resource
    private AttrMapper attrMapper;

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

    @Input({@Param(name = "fileList", type = ApiParamType.FILE, isRequired = true, desc = "common.file")})
    @Description(desc = "nmcac.importciapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        List<MultipartFile> multipartFileList = multipartRequest.getFiles("fileList");
        List<CiVo> newCiList = new ArrayList<>();
        Map<Long, CiVo> newCiMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(multipartFileList)) {
            for (MultipartFile multipartFile : multipartFileList) {
                ZipInputStream zin = new ZipInputStream(multipartFile.getInputStream());
                while (zin.getNextEntry() != null) {
                    ObjectInputStream objectInputStream = new ObjectInputStream(zin);
                    CiVo ciVo = (CiVo) objectInputStream.readObject();
                    if (!newCiMap.containsKey(ciVo.getId())) {
                        newCiList.add(ciVo);
                        newCiMap.put(ciVo.getId(), ciVo);
                    }
                    zin.closeEntry();
                }
                zin.close();
            }
            //校验导入数据
            JSONArray dataList = ciService.validateImportCi(newCiList);
            if (CollectionUtils.isNotEmpty(dataList)) {
                JSONArray errorList = new JSONArray();
                for (int i = 0; i < dataList.size(); i++) {
                    JSONObject dataObj = dataList.getJSONObject(i);
                    if (CollectionUtils.isNotEmpty(dataObj.getJSONArray("error"))) {
                        errorList.addAll(dataObj.getJSONArray("error"));
                    }
                    JSONArray attrList = dataObj.getJSONArray("attrList");
                    if (CollectionUtils.isNotEmpty(attrList)) {
                        for (int a = 0; a < attrList.size(); a++) {
                            JSONObject attrObj = attrList.getJSONObject(a);
                            if (CollectionUtils.isNotEmpty(attrObj.getJSONArray("error"))) {
                                errorList.addAll(attrObj.getJSONArray("error"));
                            }
                        }
                    }
                    JSONArray relList = dataObj.getJSONArray("relList");
                    if (CollectionUtils.isNotEmpty(relList)) {
                        for (int a = 0; a < relList.size(); a++) {
                            JSONObject relObj = relList.getJSONObject(a);
                            if (CollectionUtils.isNotEmpty(relObj.getJSONArray("error"))) {
                                errorList.addAll(relObj.getJSONArray("error"));
                            }
                        }
                    }
                }
                if(CollectionUtils.isNotEmpty(errorList)){
                    throw new CiImportException(errorList);
                }
            }
            //重新排序，父模型先写入
            newCiList.sort((o1, o2) -> {
                // 先比较parentId
                if (o1.getParentCiId() != null && o2.getParentCiId() != null) {
                    int parentIdComparison = Long.compare(o1.getParentCiId(), o2.getParentCiId());
                    if (parentIdComparison != 0) {
                        return parentIdComparison;
                    }
                } else if (o1.getParentCiId() != null) {
                    return -1; // o1的parentId不为空，o2的为空，o1排在前面
                } else if (o2.getParentCiId() != null) {
                    return 1; // o1的parentId为空，o2的不为空，o2排在前面
                }
                // parentId相等或者都为空时，按照id排序
                return Long.compare(o1.getId(), o2.getId());
            });
            if (CollectionUtils.isNotEmpty(newCiList)) {
                for (CiVo ciVo : newCiList) {
                    //检查类型是否存在
                    if (StringUtils.isNotBlank(ciVo.getTypeName())) {
                        CiTypeVo ciTypeVo = ciMapper.getCiTypeByName(ciVo.getTypeName());
                        if (ciTypeVo == null) {
                            throw new CiTypeNotFoundException(ciVo.getTypeName());
                        }
                        ciVo.setTypeId(ciTypeVo.getId());
                    }
                    CiVo oldCiVo = ciMapper.getCiById(ciVo.getId());
                    if (oldCiVo == null) {
                        if (ciVo.getParentCiId() != null && !newCiMap.containsKey(ciVo.getParentCiId())) {
                            CiVo parentCiVo = ciMapper.getCiBaseInfoById(ciVo.getParentCiId());
                            if (parentCiVo == null) {
                                throw new CiNotFoundException(ciVo.getParentCiName());
                            }
                        }
                        ciService.insertCi(ciVo);
                    } else {
                        if (oldCiVo.getRht() - oldCiVo.getLft() > 1) {
                            oldCiVo.setHasChildren(true);
                        } else {
                            oldCiVo.setHasChildren(false);
                        }
                        Integer rc = ciSchemaMapper.checkTableHasData(TenantContext.get().getDataDbName(), ciVo.getCiTableName(false));
                        oldCiVo.setHasData(rc != null && rc > 0);
                        if ((oldCiVo.getHasChildren() || oldCiVo.getHasData()) && !Objects.equals(oldCiVo.getIsAbstract(), ciVo.getIsAbstract())) {
                            throw new CiIsAbstractedException(CiIsAbstractedException.Type.UPDATEABSTRACT, ciVo.getLabel());
                        }
                        if (oldCiVo.getHasData() && !Objects.equals(oldCiVo.getParentCiId(), ciVo.getParentCiId())) {
                            throw new CiIsAbstractedException(CiIsAbstractedException.Type.UPDATEPARENT, ciVo.getLabel());
                        }
                        ciService.updateCi(ciVo);

                    }
                    //检查关联属性是否存在
                    if (CollectionUtils.isNotEmpty(ciVo.getAttrList())) {
                        for (AttrVo attrVo : ciVo.getAttrList()) {
                            attrVo.setCiId(ciVo.getId());
                            if (attrMapper.checkAttrNameIsRepeat(attrVo) > 0) {
                                throw new AttrNameRepeatException(attrVo.getName());
                            }
                            if (attrVo.getTargetCiId() != null && !newCiMap.containsKey(attrVo.getTargetCiId())) {
                                CiVo targetCiVo = ciMapper.getCiBaseInfoById(attrVo.getTargetCiId());
                                if (targetCiVo == null) {
                                    throw new CiNotFoundException(attrVo.getTargetCiName());
                                }
                            }
                            if (attrMapper.getAttrById(attrVo.getId()) == null) {
                                attrService.insertAttr(attrVo);
                            } else {
                                attrService.updateAttr(attrVo);
                            }
                        }
                    }
                    //唯一规则和名称属性需要等属性都写入数据库后才能操作
                    ciService.updateCiNameAttrId(ciVo);
                    ciService.updateCiUnique(ciVo.getId(), ciVo.getUniqueAttrIdList());
                    //检查关系对端是否存在
                    if (CollectionUtils.isNotEmpty(ciVo.getRelList())) {
                        for (RelVo relVo : ciVo.getRelList()) {
                            if (StringUtils.isNotBlank(relVo.getTypeText())) {
                                RelTypeVo relTypeVo = relMapper.getRelTypeByName(relVo.getTypeText());
                                if (relTypeVo == null) {
                                    throw new RelTypeNotFoundException(relVo.getTypeText());
                                }
                                relVo.setTypeId(relTypeVo.getId());
                            }
                            if (relMapper.checkRelByFromToName(relVo) > 0) {
                                throw new RelIsExistsException(relVo.getFromName(), relVo.getToName());
                            }
                            if (relMapper.checkRelByFromToLabel(relVo) > 0) {
                                throw new RelIsExistsException(relVo.getFromLabel(), relVo.getToLabel());
                            }
                            if (relVo.getDirection().equals(RelDirectionType.FROM.getValue())) {
                                if (!newCiMap.containsKey(relVo.getToCiId())) {
                                    CiVo toCiVo = ciMapper.getCiBaseInfoById(relVo.getToCiId());
                                    if (toCiVo == null) {
                                        throw new CiNotFoundException(relVo.getToCiId());
                                    }
                                }
                            } else if (relVo.getDirection().equals(RelDirectionType.TO.getValue())) {
                                if (!newCiMap.containsKey(relVo.getFromCiId())) {
                                    CiVo fromCiVo = ciMapper.getCiBaseInfoById(relVo.getFromCiId());
                                    if (fromCiVo == null) {
                                        throw new CiNotFoundException(relVo.getFromCiId());
                                    }
                                }
                            }
                            if (relMapper.getRelById(relVo.getId()) == null) {
                                relMapper.insertRel(relVo);
                            } else {
                                relMapper.updateRel(relVo);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

}
