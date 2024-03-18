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

package neatlogic.module.cmdb.importexport.handler;

import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.importexport.constvalue.FrameworkImportExportHandlerType;
import neatlogic.framework.importexport.core.ImportExportHandlerBase;
import neatlogic.framework.importexport.core.ImportExportHandlerType;
import neatlogic.framework.importexport.dto.ImportExportBaseInfoVo;
import neatlogic.framework.importexport.dto.ImportExportPrimaryChangeVo;
import neatlogic.framework.importexport.dto.ImportExportVo;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.zip.ZipOutputStream;

@Component
public class CiImportExportHandler extends ImportExportHandlerBase {

    @Resource
    private CiMapper ciMapper;

//    @Resource
//    private FileMapper fileMapper;
//
//    @Resource
//    private AttrMapper attrMapper;
//
//    @Resource
//    private CiViewMapper ciViewMapper;
//
//    @Resource
//    private RelMapper relMapper;
//
//    @Resource
//    private CiAuthMapper ciAuthMapper;
//
//    @Resource
//    private CiService ciService;
//
//    @Resource
//    private AttrService attrService;

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
        CiVo ciVo = ciMapper.getCiById((Long)importExportBaseInfoVo.getPrimaryKey());
        if (ciVo == null) {
            if (StringUtils.isNotBlank(importExportBaseInfoVo.getName())) {
                throw new CiNotFoundException(importExportBaseInfoVo.getName() + "[" + importExportBaseInfoVo.getPrimaryKey() + "]");
            } else {
                throw new CiNotFoundException((Long) importExportBaseInfoVo.getPrimaryKey());
            }
        }
        return true;
//        return ciMapper.getCiByName(importExportBaseInfoVo.getName()) != null
//                || ciMapper.getCiById((Long)importExportBaseInfoVo.getPrimaryKey()) != null;
    }

    @Override
    public Object getPrimaryByName(ImportExportVo importExportVo) {
//        CiVo ci = ciMapper.getCiByName(importExportVo.getName());
//        if (ci == null) {
//            throw new CiNotFoundException(importExportVo.getName());
//        }
//        return ci.getId();
        return null;
    }

    @Override
    public Object importData(ImportExportVo importExportVo, List<ImportExportPrimaryChangeVo> primaryChangeList) {
//        CiVo ciVo = importExportVo.getData().toJavaObject(CiVo.class);
//        Long originalId = ciVo.getId();
//        CiVo oldCi = ciMapper.getCiById(ciVo.getId());
//        if (oldCi == null) {
//            oldCi = ciMapper.getCiByName(ciVo.getName());
//            if (oldCi != null) {
//                ciVo.setId(oldCi.getId());
//            }
//        }
//        importHandle(ciVo, primaryChangeList);
//        List<RelVo> relList = ciVo.getRelList();
//        for (RelVo relVo : relList) {
//            if (Objects.equals(relVo.getFromCiId(), originalId)) {
//                relVo.setFromCiId(ciVo.getId());
//            }
//            if (Objects.equals(relVo.getToCiId(), originalId)) {
//                relVo.setToCiId(ciVo.getId());
//            }
//        }
//        List<AttrVo> attrList = ciVo.getAttrList();
//        for (AttrVo attr : attrList) {
//            attr.setCiId(ciVo.getId());
//        }
//        ciVo.setAttrList(null);
//        if (oldCi == null) {
//            ciService.insertCi(ciVo);
//            List<RelVo> oldRelList = relMapper.getRelBaseInfoByCiId(ciVo.getId());
//            List<Long> oldRelIdList = oldRelList.stream().map(RelVo::getId).collect(Collectors.toList());
//            for (RelVo relVo : relList) {
//                if (oldRelIdList.contains(relVo.getId())) {
//                    relMapper.updateRel(relVo);
//                    relMapper.deleteRelativeRelByRelId(relVo.getId());
//                } else {
//                    relMapper.insertRel(relVo);
//                }
//                if (CollectionUtils.isNotEmpty(relVo.getRelativeRelList())) {
//                    for (RelativeRelVo relativeRelVo : relVo.getRelativeRelList()) {
//                        relativeRelVo.setRelId(relVo.getId());
//                        relMapper.insertRelativeRel(relativeRelVo);
//                    }
//                }
//            }
//            if (Objects.equals(ciVo.getIsVirtual(), 0)) {
//                for (AttrVo attr : attrList) {
//                    attrService.insertAttr(attr);
//                }
//            }
//        } else {
//            ciService.updateCi(ciVo);
//            List<RelVo> oldRelList = relMapper.getRelBaseInfoByCiId(oldCi.getId());
//            List<Long> oldRelIdList = oldRelList.stream().map(RelVo::getId).collect(Collectors.toList());
//            for (RelVo relVo : relList) {
//                if (oldRelIdList.contains(relVo.getId())) {
//                    relMapper.updateRel(relVo);
//                    relMapper.deleteRelativeRelByRelId(relVo.getId());
//                } else {
//                    relMapper.insertRel(relVo);
//                }
//                if (CollectionUtils.isNotEmpty(relVo.getRelativeRelList())) {
//                    for (RelativeRelVo relativeRelVo : relVo.getRelativeRelList()) {
//                        relativeRelVo.setRelId(relVo.getId());
//                        relMapper.insertRelativeRel(relativeRelVo);
//                    }
//                }
//            }
//            if (Objects.equals(ciVo.getIsVirtual(), 0)) {
//                List<AttrVo> oldAttrList = attrMapper.getDeclaredAttrListByCiId(oldCi.getId());
//                List<Long> oldAttrIdList = oldAttrList.stream().map(AttrVo::getId).collect(Collectors.toList());
//                for (AttrVo attr : attrList) {
//                    if (oldAttrIdList.contains(attr.getId())) {
//                        attrService.updateAttr(attr);
//                    } else {
//                        attrService.insertAttr(attr);
//                    }
//                }
//            }
//            relMapper.deleteRelGroupByCiId(ciVo.getId());
//            ciViewMapper.deleteCiViewByCiId(ciVo.getId());
//            ciAuthMapper.deleteCiAuthByCiId(ciVo.getId());
//            if (CollectionUtils.isNotEmpty(oldCi.getUniqueAttrIdList())) {
//                ciMapper.deleteCiUniqueByCiId(ciVo.getId());
//            }
//        }
//        List<RelGroupVo> relGroupList = ciVo.getRelGroupList();
//        for (RelGroupVo relGroup : relGroupList) {
//            relGroup.setCiId(ciVo.getId());
//            relMapper.insertRelGroup(relGroup);
//        }
//        List<CiViewVo> viewList = ciVo.getViewList();
//        for (CiViewVo view : viewList) {
//            view.setCiId(ciVo.getId());
//            ciViewMapper.insertCiView(view);
//        }
//        List<CiAuthVo> authList = ciVo.getAuthList();
//        for (CiAuthVo auth : authList) {
//            auth.setCiId(ciVo.getId());
//            ciAuthMapper.insertCiAuth(auth);
//        }
//        List<Long> uniqueAttrIdList = ciVo.getUniqueAttrIdList();
//        for (Long attrId : uniqueAttrIdList) {
//            ciMapper.insertCiUnique(ciVo.getId(), attrId);
//        }
//        return ciVo.getId();
        return null;
    }

    @Override
    protected ImportExportVo myExportData(Object primaryKey, List<ImportExportBaseInfoVo> dependencyList, ZipOutputStream zipOutputStream) {
//        Long id = (Long) primaryKey;
//        CiVo ciVo = ciMapper.getCiById(id);
//        if (ciVo == null) {
//            throw new CiNotFoundException(id);
//        }
//        if (Objects.equals(ciVo.getIsVirtual(), 1) && ciVo.getFileId() != null) {
//            String viewXml = ciMapper.getCiViewXmlById(ciVo.getId());
//            ciVo.setViewXml(viewXml);
//        }
//        List<AttrVo> attrList = attrMapper.getDeclaredAttrListByCiId(id);
//        List<RelVo> relList = relMapper.getRelBaseInfoByCiId(id);
//        for (RelVo rel : relList) {
//            List<RelativeRelVo> relativeRelList = relMapper.getRelativeRelByRelId(rel.getId());
//            rel.setRelativeRelList(relativeRelList);
//        }
//        List<RelGroupVo> relGroupList = relMapper.getRelGroupByCiId(id);
//        List<CiViewVo> viewList = ciViewMapper.getCiViewBaseInfoByCiId(id);
//        List<CiAuthVo> authList = ciAuthMapper.getCiAuthByCiId(id);
//        List<Long> uniqueAttrIdList = ciMapper.getCiUniqueByCiId(id);
//        ciVo.setAttrList(attrList);
//        ciVo.setRelList(relList);
//        ciVo.setViewList(viewList);
//        ciVo.setAuthList(authList);
//        ciVo.setUniqueAttrIdList(uniqueAttrIdList);
//        ciVo.setRelGroupList(relGroupList);
//        exportHandle(ciVo, dependencyList, zipOutputStream);
//        ImportExportVo importExportVo = new ImportExportVo(this.getType().getValue(), primaryKey, ciVo.getName());
//        importExportVo.setDataWithObject(ciVo);
//        return importExportVo;
        return null;
    }

    /**
     * 导入处理，更新依赖组件的唯一标识
     * @param ciVo
     * @param primaryChangeList
     */
//    private void importHandle(CiVo ciVo, List<ImportExportPrimaryChangeVo> primaryChangeList) {
//        handleDependency(IMPORT, ciVo, null, null, primaryChangeList);
//    }

    /**
     * 导出处理，先导出依赖组件
     * @param ciVo
     * @param dependencyList
     * @param zipOutputStream
     */
//    private void exportHandle(CiVo ciVo, List<ImportExportBaseInfoVo> dependencyList, ZipOutputStream zipOutputStream) {
//        handleDependency(EXPORT, ciVo, dependencyList, zipOutputStream, null);
//    }

//    private void handleDependency(String action, CiVo ciVo, List<ImportExportBaseInfoVo> dependencyList, ZipOutputStream zipOutputStream, List<ImportExportPrimaryChangeVo> primaryChangeList) {
//        if (ciVo.getParentCiId() != null) {
//            if (action == IMPORT) {
//                Object newPrimaryKey = getNewPrimaryKey(FrameworkImportExportHandlerType.CMDB_CI, ciVo.getParentCiId(), primaryChangeList);
//                if (newPrimaryKey != null) {
//                    ciVo.setParentCiId((Long) newPrimaryKey);
//                }
//            } else if (action == EXPORT) {
//                doExportData(FrameworkImportExportHandlerType.CMDB_CI, ciVo.getParentCiId(), dependencyList, zipOutputStream);
//            }
//        }
//        if (ciVo.getTypeId() != null) {
//            if (action == IMPORT) {
//                Object newPrimaryKey = getNewPrimaryKey(CmdbImportExportHandlerType.CI_TYPE, ciVo.getTypeId(), primaryChangeList);
//                if (newPrimaryKey != null) {
//                    ciVo.setTypeId((Long) newPrimaryKey);
//                }
//            } else if (action == EXPORT) {
//                doExportData(CmdbImportExportHandlerType.CI_TYPE, ciVo.getTypeId(), dependencyList, zipOutputStream);
//            }
//        }
//        if (Objects.equals(ciVo.getIsVirtual(), 1) && ciVo.getFileId() != null && fileMapper.getFileById(ciVo.getFileId()) != null) {
//            if (action == IMPORT) {
//                Object newPrimaryKey = getNewPrimaryKey(FrameworkImportExportHandlerType.FILE, ciVo.getFileId(), primaryChangeList);
//                if (newPrimaryKey != null) {
//                    ciVo.setFileId((Long) newPrimaryKey);
//                }
//            } else if (action == EXPORT) {
//                doExportData(FrameworkImportExportHandlerType.FILE, ciVo.getFileId(), dependencyList, zipOutputStream);
//            }
//        }
//        List<AttrVo> attrList = ciVo.getAttrList();
//        if (CollectionUtils.isNotEmpty(attrList)) {
//            for (AttrVo attr : attrList) {
//                if (attr.getTargetCiId() != null) {
//                    if (action == IMPORT) {
//                        Object newPrimaryKey = getNewPrimaryKey(FrameworkImportExportHandlerType.CMDB_CI, attr.getTargetCiId(), primaryChangeList);
//                        if (newPrimaryKey != null) {
//                            attr.setTargetCiId((Long) newPrimaryKey);
//                        }
//                    } else if (action == EXPORT) {
//                        doExportData(FrameworkImportExportHandlerType.CMDB_CI, attr.getTargetCiId(), dependencyList, zipOutputStream);
//                    }
//                }
//                if (attr.getValidatorId() != null) {
//                    if (action == IMPORT) {
//                        Object newPrimaryKey = getNewPrimaryKey(CmdbImportExportHandlerType.CI_VALIDATOR, attr.getValidatorId(), primaryChangeList);
//                        if (newPrimaryKey != null) {
//                            attr.setValidatorId((Long) newPrimaryKey);
//                        }
//                    } else if (action == EXPORT) {
//                        doExportData(CmdbImportExportHandlerType.CI_VALIDATOR, attr.getValidatorId(), dependencyList, zipOutputStream);
//                    }
//                }
//            }
//        }
//        List<RelVo> relList = ciVo.getRelList();
//        if (CollectionUtils.isNotEmpty(relList)) {
//            for (RelVo rel : relList) {
//                if (rel.getTypeId() != null) {
//                    if (action == IMPORT) {
//                        Object newPrimaryKey = getNewPrimaryKey(CmdbImportExportHandlerType.CI_REL_TYPE, rel.getTypeId(), primaryChangeList);
//                        if (newPrimaryKey != null) {
//                            rel.setTypeId((Long) newPrimaryKey);
//                        }
//                    } else if (action == EXPORT) {
//                        doExportData(CmdbImportExportHandlerType.CI_REL_TYPE, rel.getTypeId(), dependencyList, zipOutputStream);
//                    }
//                }
//                if (rel.getFromCiId() != null && !Objects.equals(rel.getFromCiId(), ciVo.getId())) {
//                    if (action == IMPORT) {
//                        Object newPrimaryKey = getNewPrimaryKey(FrameworkImportExportHandlerType.CMDB_CI, rel.getFromCiId(), primaryChangeList);
//                        if (newPrimaryKey != null) {
//                            rel.setFromCiId((Long) newPrimaryKey);
//                        }
//                    } else if (action == EXPORT) {
//                        doExportData(FrameworkImportExportHandlerType.CMDB_CI, rel.getFromCiId(), dependencyList, zipOutputStream);
//                    }
//                }
//                if (rel.getToCiId() != null && !Objects.equals(rel.getToCiId(), ciVo.getId())) {
//                    if (action == IMPORT) {
//                        Object newPrimaryKey = getNewPrimaryKey(FrameworkImportExportHandlerType.CMDB_CI, rel.getToCiId(), primaryChangeList);
//                        if (newPrimaryKey != null) {
//                            rel.setToCiId((Long) newPrimaryKey);
//                        }
//                    } else if (action == EXPORT) {
//                        doExportData(FrameworkImportExportHandlerType.CMDB_CI, rel.getToCiId(), dependencyList, zipOutputStream);
//                    }
//                }
//            }
//        }
//    }
}
