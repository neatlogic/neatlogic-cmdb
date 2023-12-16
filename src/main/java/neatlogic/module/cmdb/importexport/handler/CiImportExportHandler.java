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

package neatlogic.module.cmdb.importexport.handler;

import neatlogic.framework.cmdb.dto.ci.*;
import neatlogic.framework.cmdb.enums.CmdbImportExportHandlerType;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.file.dao.mapper.FileMapper;
import neatlogic.framework.importexport.constvalue.FrameworkImportExportHandlerType;
import neatlogic.framework.importexport.core.ImportExportHandlerBase;
import neatlogic.framework.importexport.core.ImportExportHandlerType;
import neatlogic.framework.importexport.dto.ImportExportBaseInfoVo;
import neatlogic.framework.importexport.dto.ImportExportPrimaryChangeVo;
import neatlogic.framework.importexport.dto.ImportExportVo;
import neatlogic.module.cmdb.dao.mapper.ci.*;
import neatlogic.module.cmdb.service.attr.AttrService;
import neatlogic.module.cmdb.service.ci.CiService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;

@Component
public class CiImportExportHandler extends ImportExportHandlerBase {

    @Resource
    private CiMapper ciMapper;

    @Resource
    private FileMapper fileMapper;

    @Resource
    private AttrMapper attrMapper;

    @Resource
    private CiViewMapper ciViewMapper;

    @Resource
    private RelMapper relMapper;

    @Resource
    private CiAuthMapper ciAuthMapper;

    @Resource
    private CiService ciService;

    @Resource
    private AttrService attrService;

    @Override
    public ImportExportHandlerType getType() {
        return FrameworkImportExportHandlerType.CMDB_CI;
    }


    @Override
    public boolean checkImportAuth(ImportExportVo importExportVo) {
        return true;
    }

    @Override
    public boolean checkExportAuth(Object primaryKey) {
        return true;
    }

    @Override
    public boolean checkIsExists(ImportExportBaseInfoVo importExportBaseInfoVo) {
        return ciMapper.getCiByName(importExportBaseInfoVo.getName()) != null
                || ciMapper.getCiById((Long)importExportBaseInfoVo.getPrimaryKey()) != null;
    }

    @Override
    public Object getPrimaryByName(ImportExportVo importExportVo) {
        CiVo ci = ciMapper.getCiByName(importExportVo.getName());
        if (ci == null) {
            throw new CiNotFoundException(importExportVo.getName());
        }
        return ci.getId();
    }

    @Override
    public Object importData(ImportExportVo importExportVo, List<ImportExportPrimaryChangeVo> primaryChangeList) {
        CiVo ciVo = importExportVo.getData().toJavaObject(CiVo.class);
        Long originalId = ciVo.getId();
        CiVo oldCi = ciMapper.getCiById(ciVo.getId());
        if (oldCi == null) {
            oldCi = ciMapper.getCiByName(ciVo.getName());
            if (oldCi != null) {
                ciVo.setId(oldCi.getId());
            }
        }
        importHandle(ciVo, primaryChangeList);
        List<RelVo> relList = ciVo.getRelList();
        for (RelVo relVo : relList) {
            if (Objects.equals(relVo.getFromCiId(), originalId)) {
                relVo.setFromCiId(ciVo.getId());
            }
            if (Objects.equals(relVo.getToCiId(), originalId)) {
                relVo.setToCiId(ciVo.getId());
            }
        }
        List<AttrVo> attrList = ciVo.getAttrList();
        for (AttrVo attr : attrList) {
            attr.setCiId(ciVo.getId());
        }
        ciVo.setAttrList(null);
        if (oldCi == null) {
            ciService.insertCi(ciVo);
            List<RelVo> oldRelList = relMapper.getRelBaseInfoByCiId(ciVo.getId());
            List<Long> oldRelIdList = oldRelList.stream().map(RelVo::getId).collect(Collectors.toList());
            for (RelVo relVo : relList) {
                if (oldRelIdList.contains(relVo.getId())) {
                    relMapper.updateRel(relVo);
                    relMapper.deleteRelativeRelByRelId(relVo.getId());
                } else {
                    relMapper.insertRel(relVo);
                }
                if (CollectionUtils.isNotEmpty(relVo.getRelativeRelList())) {
                    for (RelativeRelVo relativeRelVo : relVo.getRelativeRelList()) {
                        relativeRelVo.setRelId(relVo.getId());
                        relMapper.insertRelativeRel(relativeRelVo);
                    }
                }
            }
            if (Objects.equals(ciVo.getIsVirtual(), 0)) {
                for (AttrVo attr : attrList) {
                    attrService.insertAttr(attr);
                }
            }
        } else {
            ciService.updateCi(ciVo);
            List<RelVo> oldRelList = relMapper.getRelBaseInfoByCiId(oldCi.getId());
            List<Long> oldRelIdList = oldRelList.stream().map(RelVo::getId).collect(Collectors.toList());
            for (RelVo relVo : relList) {
                if (oldRelIdList.contains(relVo.getId())) {
                    relMapper.updateRel(relVo);
                    relMapper.deleteRelativeRelByRelId(relVo.getId());
                } else {
                    relMapper.insertRel(relVo);
                }
                if (CollectionUtils.isNotEmpty(relVo.getRelativeRelList())) {
                    for (RelativeRelVo relativeRelVo : relVo.getRelativeRelList()) {
                        relativeRelVo.setRelId(relVo.getId());
                        relMapper.insertRelativeRel(relativeRelVo);
                    }
                }
            }
            if (Objects.equals(ciVo.getIsVirtual(), 0)) {
                List<AttrVo> oldAttrList = attrMapper.getAttrBaseInfoByCiId(oldCi.getId());
                List<Long> oldAttrIdList = oldAttrList.stream().map(AttrVo::getId).collect(Collectors.toList());
                for (AttrVo attr : attrList) {
                    if (oldAttrIdList.contains(attr.getId())) {
                        attrService.updateAttr(attr);
                    } else {
                        attrService.insertAttr(attr);
                    }
                }
            }
            relMapper.deleteRelGroupByCiId(ciVo.getId());
            ciViewMapper.deleteCiViewByCiId(ciVo.getId());
            ciAuthMapper.deleteCiAuthByCiId(ciVo.getId());
            if (CollectionUtils.isNotEmpty(oldCi.getUniqueAttrIdList())) {
                ciMapper.deleteCiUniqueByCiId(ciVo.getId());
            }
        }
        List<RelGroupVo> relGroupList = ciVo.getRelGroupList();
        for (RelGroupVo relGroup : relGroupList) {
            relGroup.setCiId(ciVo.getId());
            relMapper.insertRelGroup(relGroup);
        }
        List<CiViewVo> viewList = ciVo.getViewList();
        for (CiViewVo view : viewList) {
            view.setCiId(ciVo.getId());
            ciViewMapper.insertCiView(view);
        }
        List<CiAuthVo> authList = ciVo.getAuthList();
        for (CiAuthVo auth : authList) {
            auth.setCiId(ciVo.getId());
            ciAuthMapper.insertCiAuth(auth);
        }
        List<Long> uniqueAttrIdList = ciVo.getUniqueAttrIdList();
        for (Long attrId : uniqueAttrIdList) {
            ciMapper.insertCiUnique(ciVo.getId(), attrId);
        }
        return ciVo.getId();
    }

    @Override
    protected ImportExportVo myExportData(Object primaryKey, List<ImportExportBaseInfoVo> dependencyList, ZipOutputStream zipOutputStream) {
        Long id = (Long) primaryKey;
        CiVo ciVo = ciMapper.getCiById(id);
        if (ciVo == null) {
            throw new CiNotFoundException(id);
        }
        if (Objects.equals(ciVo.getIsVirtual(), 1) && ciVo.getFileId() != null) {
            String viewXml = ciMapper.getCiViewXmlById(ciVo.getId());
            ciVo.setViewXml(viewXml);
        }
        List<AttrVo> attrList = attrMapper.getAttrBaseInfoByCiId(id);
        List<RelVo> relList = relMapper.getRelBaseInfoByCiId(id);
        for (RelVo rel : relList) {
            List<RelativeRelVo> relativeRelList = relMapper.getRelativeRelByRelId(rel.getId());
            rel.setRelativeRelList(relativeRelList);
        }
        List<RelGroupVo> relGroupList = relMapper.getRelGroupByCiId(id);
        List<CiViewVo> viewList = ciViewMapper.getCiViewBaseInfoByCiId(id);
        List<CiAuthVo> authList = ciAuthMapper.getCiAuthByCiId(id);
        List<Long> uniqueAttrIdList = ciMapper.getCiUniqueByCiId(id);
        ciVo.setAttrList(attrList);
        ciVo.setRelList(relList);
        ciVo.setViewList(viewList);
        ciVo.setAuthList(authList);
        ciVo.setUniqueAttrIdList(uniqueAttrIdList);
        ciVo.setRelGroupList(relGroupList);
        exportHandle(ciVo, dependencyList, zipOutputStream);
        ImportExportVo importExportVo = new ImportExportVo(this.getType().getValue(), primaryKey, ciVo.getName());
        importExportVo.setDataWithObject(ciVo);
        return importExportVo;
    }

    /**
     * 导入处理，更新依赖组件的唯一标识
     * @param ciVo
     * @param primaryChangeList
     */
    private void importHandle(CiVo ciVo, List<ImportExportPrimaryChangeVo> primaryChangeList) {
        handleDependency(IMPORT, ciVo, null, null, primaryChangeList);
    }

    /**
     * 导出处理，先导出依赖组件
     * @param ciVo
     * @param dependencyList
     * @param zipOutputStream
     */
    private void exportHandle(CiVo ciVo, List<ImportExportBaseInfoVo> dependencyList, ZipOutputStream zipOutputStream) {
        handleDependency(EXPORT, ciVo, dependencyList, zipOutputStream, null);
    }

    private void handleDependency(String action, CiVo ciVo, List<ImportExportBaseInfoVo> dependencyList, ZipOutputStream zipOutputStream, List<ImportExportPrimaryChangeVo> primaryChangeList) {
        if (ciVo.getParentCiId() != null) {
            if (action == IMPORT) {
                Object newPrimaryKey = getNewPrimaryKey(FrameworkImportExportHandlerType.CMDB_CI, ciVo.getParentCiId(), primaryChangeList);
                if (newPrimaryKey != null) {
                    ciVo.setParentCiId((Long) newPrimaryKey);
                }
            } else if (action == EXPORT) {
                doExportData(FrameworkImportExportHandlerType.CMDB_CI, ciVo.getParentCiId(), dependencyList, zipOutputStream);
            }
        }
        if (ciVo.getTypeId() != null) {
            if (action == IMPORT) {
                Object newPrimaryKey = getNewPrimaryKey(CmdbImportExportHandlerType.CI_TYPE, ciVo.getTypeId(), primaryChangeList);
                if (newPrimaryKey != null) {
                    ciVo.setTypeId((Long) newPrimaryKey);
                }
            } else if (action == EXPORT) {
                doExportData(CmdbImportExportHandlerType.CI_TYPE, ciVo.getTypeId(), dependencyList, zipOutputStream);
            }
        }
        if (Objects.equals(ciVo.getIsVirtual(), 1) && ciVo.getFileId() != null && fileMapper.getFileById(ciVo.getFileId()) != null) {
            if (action == IMPORT) {
                Object newPrimaryKey = getNewPrimaryKey(FrameworkImportExportHandlerType.FILE, ciVo.getFileId(), primaryChangeList);
                if (newPrimaryKey != null) {
                    ciVo.setFileId((Long) newPrimaryKey);
                }
            } else if (action == EXPORT) {
                doExportData(FrameworkImportExportHandlerType.FILE, ciVo.getFileId(), dependencyList, zipOutputStream);
            }
        }
        List<AttrVo> attrList = ciVo.getAttrList();
        if (CollectionUtils.isNotEmpty(attrList)) {
            for (AttrVo attr : attrList) {
                if (attr.getTargetCiId() != null) {
                    if (action == IMPORT) {
                        Object newPrimaryKey = getNewPrimaryKey(FrameworkImportExportHandlerType.CMDB_CI, attr.getTargetCiId(), primaryChangeList);
                        if (newPrimaryKey != null) {
                            attr.setTargetCiId((Long) newPrimaryKey);
                        }
                    } else if (action == EXPORT) {
                        doExportData(FrameworkImportExportHandlerType.CMDB_CI, attr.getTargetCiId(), dependencyList, zipOutputStream);
                    }
                }
                if (attr.getValidatorId() != null) {
                    if (action == IMPORT) {
                        Object newPrimaryKey = getNewPrimaryKey(CmdbImportExportHandlerType.CI_VALIDATOR, attr.getValidatorId(), primaryChangeList);
                        if (newPrimaryKey != null) {
                            attr.setValidatorId((Long) newPrimaryKey);
                        }
                    } else if (action == EXPORT) {
                        doExportData(CmdbImportExportHandlerType.CI_VALIDATOR, attr.getValidatorId(), dependencyList, zipOutputStream);
                    }
                }
            }
        }
        List<RelVo> relList = ciVo.getRelList();
        if (CollectionUtils.isNotEmpty(relList)) {
            for (RelVo rel : relList) {
                if (rel.getTypeId() != null) {
                    if (action == IMPORT) {
                        Object newPrimaryKey = getNewPrimaryKey(CmdbImportExportHandlerType.CI_REL_TYPE, rel.getTypeId(), primaryChangeList);
                        if (newPrimaryKey != null) {
                            rel.setTypeId((Long) newPrimaryKey);
                        }
                    } else if (action == EXPORT) {
                        doExportData(CmdbImportExportHandlerType.CI_REL_TYPE, rel.getTypeId(), dependencyList, zipOutputStream);
                    }
                }
                if (rel.getFromCiId() != null && !Objects.equals(rel.getFromCiId(), ciVo.getId())) {
                    if (action == IMPORT) {
                        Object newPrimaryKey = getNewPrimaryKey(FrameworkImportExportHandlerType.CMDB_CI, rel.getFromCiId(), primaryChangeList);
                        if (newPrimaryKey != null) {
                            rel.setFromCiId((Long) newPrimaryKey);
                        }
                    } else if (action == EXPORT) {
                        doExportData(FrameworkImportExportHandlerType.CMDB_CI, rel.getFromCiId(), dependencyList, zipOutputStream);
                    }
                }
                if (rel.getToCiId() != null && !Objects.equals(rel.getToCiId(), ciVo.getId())) {
                    if (action == IMPORT) {
                        Object newPrimaryKey = getNewPrimaryKey(FrameworkImportExportHandlerType.CMDB_CI, rel.getToCiId(), primaryChangeList);
                        if (newPrimaryKey != null) {
                            rel.setToCiId((Long) newPrimaryKey);
                        }
                    } else if (action == EXPORT) {
                        doExportData(FrameworkImportExportHandlerType.CMDB_CI, rel.getToCiId(), dependencyList, zipOutputStream);
                    }
                }
            }
        }
    }
}
