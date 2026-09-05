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

package neatlogic.module.cmdb.service.ci;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.cmdb.crossover.ICiCrossoverService;
import neatlogic.framework.cmdb.crossover.ICiSchemaViewCrossoverMapper;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiViewVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.customview.CustomViewVo;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrVo;
import neatlogic.framework.cmdb.enums.RelDirectionType;
import neatlogic.framework.cmdb.exception.attr.AttrIsUsedInExpressionException;
import neatlogic.framework.cmdb.exception.attr.AttrIsUsedInUniqueRuleException;
import neatlogic.framework.cmdb.exception.attr.InsertAttrToSchemaException;
import neatlogic.framework.cmdb.exception.ci.*;
import neatlogic.framework.cmdb.utils.RelUtil;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.dao.mapper.DataBaseViewInfoMapper;
import neatlogic.framework.dao.mapper.SchemaMapper;
import neatlogic.framework.dto.DataBaseViewInfoVo;
import neatlogic.framework.exception.database.DataBaseNotFoundException;
import neatlogic.framework.lrcode.LRCodeManager;
import neatlogic.framework.transaction.core.AfterTransactionJob;
import neatlogic.framework.transaction.core.EscapeTransactionJob;
import neatlogic.framework.util.Md5Util;
import neatlogic.framework.util.SnowflakeUtil;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiViewMapper;
import neatlogic.module.cmdb.dao.mapper.ci.RelMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import neatlogic.module.cmdb.dao.mapper.cischema.CiSchemaMapper;
import neatlogic.module.cmdb.dao.mapper.globalattr.GlobalAttrMapper;
import neatlogic.module.cmdb.service.cientity.CiEntityService;
import neatlogic.module.cmdb.utils.VirtualCiSqlBuilder;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CiServiceImpl implements CiService, ICiCrossoverService {

    private void normalizeImportCiList(List<CiVo> newCiList) {
        Map<Long, Long> ciIdMap = new HashMap<>();
        for (CiVo ciVo : newCiList) {
            Long originalCiId = ciVo.getId();
            CiVo oldCiByName = StringUtils.isNotBlank(ciVo.getName()) ? ciMapper.getCiByName(ciVo.getName()) : null;
            if (oldCiByName != null) {
                ciVo.setId(oldCiByName.getId());
            } else {
                CiVo oldCiById = ciMapper.getCiBaseInfoById(originalCiId);
                if (oldCiById != null && !Objects.equals(oldCiById.getName(), ciVo.getName())) {
                    ciVo.setId(SnowflakeUtil.uniqueLong());
                }
            }
            ciIdMap.put(originalCiId, ciVo.getId());
        }

        for (CiVo ciVo : newCiList) {
            ciVo.setParentCiId(resolveCiId(ciVo.getParentCiId(), ciVo.getParentCiName(), ciIdMap));
            Map<Long, Long> attrIdMap = new HashMap<>();
            if (CollectionUtils.isNotEmpty(ciVo.getAttrList())) {
                for (AttrVo attrVo : ciVo.getAttrList()) {
                    normalizeImportAttr(ciVo, attrVo, ciIdMap, attrIdMap);
                }
            }
            remapCiAttrConfig(ciVo, attrIdMap);

            if (CollectionUtils.isNotEmpty(ciVo.getRelList())) {
                for (RelVo relVo : ciVo.getRelList()) {
                    if (Objects.equals(relVo.getDirection(), RelDirectionType.FROM.getValue())) {
                        relVo.setFromCiId(ciVo.getId());
                        relVo.setToCiId(resolveCiId(relVo.getToCiId(), relVo.getToCiName(), ciIdMap));
                    } else if (Objects.equals(relVo.getDirection(), RelDirectionType.TO.getValue())) {
                        relVo.setToCiId(ciVo.getId());
                        relVo.setFromCiId(resolveCiId(relVo.getFromCiId(), relVo.getFromCiName(), ciIdMap));
                    } else {
                        relVo.setFromCiId(resolveCiId(relVo.getFromCiId(), relVo.getFromCiName(), ciIdMap));
                        relVo.setToCiId(resolveCiId(relVo.getToCiId(), relVo.getToCiName(), ciIdMap));
                    }
                }
            }
        }
    }

    private void normalizeImportAttr(CiVo ciVo, AttrVo attrVo, Map<Long, Long> ciIdMap, Map<Long, Long> attrIdMap) {
        Long originalAttrId = attrVo.getId();
        AttrVo oldAttrVo = attrMapper.getAttrByCiIdAndName(ciVo.getId(), attrVo.getName());
        if (oldAttrVo != null) {
            attrVo.setId(oldAttrVo.getId());
        } else {
            AttrVo oldAttrById = attrMapper.getAttrById(originalAttrId);
            if (oldAttrById != null) {
                attrVo.setId(SnowflakeUtil.uniqueLong());
            }
        }
        attrIdMap.put(originalAttrId, attrVo.getId());
        attrVo.setCiId(ciVo.getId());
        attrVo.setTargetCiId(resolveCiId(attrVo.getTargetCiId(), attrVo.getTargetCiName(), ciIdMap));
    }

    /**
     * 先按原始id判断模型是否存在，不存在时再用模型唯一标识兜底。
     */
    private Long resolveCiId(Long ciId, String ciName, Map<Long, Long> ciIdMap) {
        if (ciId == null) {
            return null;
        }
        CiVo ciVo = ciMapper.getCiBaseInfoById(ciId);
        if (ciVo != null) {
            return ciVo.getId();
        }
        if (ciIdMap.containsKey(ciId)) {
            return ciIdMap.get(ciId);
        }
        if (StringUtils.isNotBlank(ciName)) {
            ciVo = ciMapper.getCiByName(ciName);
            if (ciVo != null) {
                return ciVo.getId();
            }
        }
        return ciId;
    }

    /**
     * 属性id被重映射后，名称属性和唯一规则也要同步指向本地属性id。
     */
    private void remapCiAttrConfig(CiVo ciVo, Map<Long, Long> attrIdMap) {
        if (attrIdMap.isEmpty()) {
            return;
        }
        if (ciVo.getNameAttrId() != null && attrIdMap.containsKey(ciVo.getNameAttrId())) {
            ciVo.setNameAttrId(attrIdMap.get(ciVo.getNameAttrId()));
        }
        if (CollectionUtils.isNotEmpty(ciVo.getUniqueAttrIdList())) {
            ciVo.setUniqueAttrIdList(ciVo.getUniqueAttrIdList().stream().map(attrId -> attrIdMap.getOrDefault(attrId, attrId)).collect(Collectors.toList()));
        }
    }

    @Override
    public JSONArray validateImportCi(List<CiVo> newCiList) {
        JSONArray dataList = new JSONArray();
        if (CollectionUtils.isNotEmpty(newCiList)) {
            normalizeImportCiList(newCiList);
            Map<Long, CiVo> newCiMap = new HashMap<>();
            for (CiVo ciVo : newCiList) {
                newCiMap.put(ciVo.getId(), ciVo);
            }
            for (CiVo ciVo : newCiList) {
                JSONObject dataObj = new JSONObject();
                dataObj.put("name", ciVo.getName());
                dataObj.put("label", ciVo.getLabel());
                dataObj.put("error", new JSONArray());
                if (StringUtils.isNotBlank(ciVo.getTypeName()) && (ciMapper.getCiTypeByName(ciVo.getTypeName()) == null)) {
                    dataObj.getJSONArray("error").add("模型层级：" + ciVo.getTypeName() + "不存在");
                }
                if (ciVo.getParentCiId() != null && !newCiMap.containsKey(ciVo.getParentCiId())) {
                    CiVo parentCiVo = ciMapper.getCiBaseInfoById(ciVo.getParentCiId());
                    if (parentCiVo == null) {
                        dataObj.getJSONArray("error").add("父模型：" + ciVo.getParentCiId() + "不存在");
                    }
                }
                boolean hasChange = false;
                //检查关联属性是否存在
                if (CollectionUtils.isNotEmpty(ciVo.getAttrList())) {
                    JSONArray attrList = new JSONArray();
                    for (AttrVo attrVo : ciVo.getAttrList()) {
                        JSONObject attrObj = new JSONObject();
                        attrObj.put("name", attrVo.getName());
                        attrObj.put("label", attrVo.getLabel());
                        attrObj.put("error", new JSONArray());
                        AttrVo oldAttrVo = attrMapper.getAttrByCiIdAndName(ciVo.getId(), attrVo.getName());
                        if (oldAttrVo == null) {
                            attrObj.put("_action", "insert");
                            hasChange = true;
                        } else {
                            attrVo.setId(oldAttrVo.getId());
                            if (!Objects.equals(oldAttrVo.getType(), attrVo.getType())) {
                                attrObj.put("_action", "update");
                                attrObj.getJSONArray("error").add("属性类型发生变化，原类型是“" + oldAttrVo.getTypeText() + "”，新类型是“" + attrVo.getTypeText() + "”");
                            } else if (Objects.equals(oldAttrVo.getName(), attrVo.getName())
                                    && Objects.equals(oldAttrVo.getLabel(), attrVo.getLabel())
                                    && Objects.equals(oldAttrVo.getIsRequired(), attrVo.getIsRequired())
                                    && Objects.equals(oldAttrVo.getConfigStr(), attrVo.getConfigStr())) {
                                attrObj.put("_action", "same");
                            } else {
                                attrObj.put("_action", "update");
                                hasChange = true;
                            }
                        }
                        if (attrVo.getTargetCiId() != null && !newCiMap.containsKey(attrVo.getTargetCiId())) {
                            CiVo targetCiVo = ciMapper.getCiBaseInfoById(attrVo.getTargetCiId());
                            if (targetCiVo == null) {
                                attrObj.getJSONArray("error").add("目标模型：" + attrVo.getTargetCiId() + "不存在");
                            }
                        }
                        attrList.add(attrObj);
                    }
                    dataObj.put("attrList", attrList);
                }
                //检查关系对端是否存在
                if (CollectionUtils.isNotEmpty(ciVo.getRelList())) {
                    JSONArray relList = new JSONArray();
                    for (RelVo relVo : ciVo.getRelList()) {
                        JSONObject relObj = new JSONObject();
                        RelVo oldRelVo = relMapper.getRelById(relVo.getId());
                        if (oldRelVo == null) {
                            relObj.put("_action", "insert");
                            hasChange = true;
                        } else {
                            if (
                                    Objects.equals(oldRelVo.getToCiId(), relVo.getToCiId())
                                            && Objects.equals(oldRelVo.getFromCiId(), relVo.getFromCiId())
                                            && Objects.equals(oldRelVo.getToName(), relVo.getToName())
                                            && Objects.equals(oldRelVo.getToLabel(), relVo.getToLabel())
                                            && Objects.equals(oldRelVo.getFromName(), relVo.getFromName())
                                            && Objects.equals(oldRelVo.getFromLabel(), relVo.getFromLabel())
                                            && Objects.equals(oldRelVo.getToIsRequired(), relVo.getToIsRequired())
                                            && Objects.equals(oldRelVo.getFromIsRequired(), relVo.getFromIsRequired())
                                            && Objects.equals(oldRelVo.getToRule(), relVo.getToRule())
                                            && Objects.equals(oldRelVo.getFromRule(), relVo.getFromRule())
                                            && Objects.equals(oldRelVo.getToGroupId(), relVo.getToGroupId())
                                            && Objects.equals(oldRelVo.getFromGroupId(), relVo.getFromGroupId())
                            ) {
                                relObj.put("_action", "same");
                            } else {
                                relObj.put("_action", "update");
                                hasChange = true;
                            }
                        }
                        relObj.put("fromCiName", relVo.getFromCiName());
                        relObj.put("fromCiLabel", relVo.getFromCiLabel());
                        relObj.put("toCiName", relVo.getToCiName());
                        relObj.put("toCiLabel", relVo.getToCiLabel());
                        relObj.put("toName", relVo.getToName());
                        relObj.put("fromName", relVo.getFromName());
                        relObj.put("toLabel", relVo.getToLabel());
                        relObj.put("fromLabel", relVo.getFromLabel());
                        relObj.put("direction", relVo.getDirection());
                        relObj.put("error", new JSONArray());
                        if (StringUtils.isNotBlank(relVo.getTypeText()) && (relMapper.getRelTypeByName(relVo.getTypeText()) == null)) {
                            relObj.getJSONArray("error").add("类型：" + relVo.getTypeText() + "不存在");
                        }
                        if (relVo.getDirection().equals(RelDirectionType.FROM.getValue())) {
                            if (!newCiMap.containsKey(relVo.getToCiId())) {
                                CiVo toCiVo = ciMapper.getCiBaseInfoById(relVo.getToCiId());
                                if (toCiVo == null) {
                                    relObj.getJSONArray("error").add("下游模型：" + relVo.getToCiId() + "不存在");
                                }
                            }
                        } else if (relVo.getDirection().equals(RelDirectionType.TO.getValue())) {
                            if (!newCiMap.containsKey(relVo.getFromCiId())) {
                                CiVo fromCiVo = ciMapper.getCiBaseInfoById(relVo.getFromCiId());
                                if (fromCiVo == null) {
                                    relObj.getJSONArray("error").add("上游模型：" + relVo.getFromCiId() + "不存在");
                                }
                            }
                        }
                        relList.add(relObj);
                    }
                    dataObj.put("relList", relList);
                }
                if (ciMapper.getCiBaseInfoById(ciVo.getId()) == null) {
                    dataObj.put("_action", "insert");
                } else {
                    if (hasChange) {
                        dataObj.put("_action", "update");
                    } else {
                        dataObj.put("_action", "same");
                    }
                }
                dataList.add(dataObj);
            }
        }
        return dataList;
    }

    @Resource
    private CiViewMapper ciViewMapper;

    @Resource
    private CiMapper ciMapper;

    @Resource
    private CiEntityMapper ciEntityMapper;

    @Resource
    private GlobalAttrMapper globalAttrMapper;

    @Resource
    private AttrMapper attrMapper;

    @Resource
    private RelMapper relMapper;


    @Resource
    private CiSchemaMapper ciSchemaMapper;

    @Resource
    private CiEntityService ciEntityService;

    @Resource
    private SchemaMapper schemaMapper;

    @Resource
    private DataBaseViewInfoMapper dataBaseViewInfoMapper;

    @Override
    @Transactional
    public void insertCi(CiVo ciVo) {
        if (ciMapper.checkCiNameIsExists(ciVo) > 0) {
            throw new CiNameIsExistsException(ciVo.getName());
        }
        if (ciMapper.checkCiLabelIsExists(ciVo) > 0) {
            throw new CiLabelIsExistsException(ciVo.getLabel());
        }


        //int lft = LRCodeManager.beforeAddTreeNode("cmdb_ci", "id", "parent_ci_id", ciVo.getParentCiId());
        //ciVo.setLft(lft);
        //ciVo.setRht(lft + 1);
        ciMapper.insertCi(ciVo);
        //重建所有左右编码，性能差点但可靠
        LRCodeManager.rebuildLeftRightCode("cmdb_ci", "id", "parent_ci_id");
        //如果父模型有配置显示视图，则复制一份，加快配置效率
        if (ciVo.getParentCiId() != null) {
            List<CiViewVo> ciViewList = ciViewMapper.getCiViewBaseInfoByCiId(ciVo.getParentCiId());
            if (CollectionUtils.isNotEmpty(ciViewList)) {
                for (CiViewVo ciViewVo : ciViewList) {
                    ciViewVo.setCiId(ciVo.getId());
                    ciViewMapper.insertCiView(ciViewVo);
                }
            }
        }

        if (Objects.equals(ciVo.getIsVirtual(), 1)) {
            buildCiView(ciVo);
        } else if (Objects.equals(ciVo.getIsVirtual(), 0)) {
            EscapeTransactionJob.State s = buildCiSchema(ciVo);
            if (!s.isSucceed()) {
                throw new CreateCiSchemaException(ciVo.getName());
            }
        }
    }

    private EscapeTransactionJob.State buildCiSchema(CiVo ciVo) {
        return new EscapeTransactionJob(() -> {
            if (ciSchemaMapper.checkSchemaIsExists(TenantContext.get().getDataDbName()) > 0) {
                if (ciSchemaMapper.checkTableIsExists(TenantContext.get().getDataDbName(), "cmdb_" + ciVo.getId()) <= 0) {
                    //创建配置项表
                    ciSchemaMapper.insertCiTable(ciVo.getId(), ciVo.getCiTableName());
                    //补充物化视图(商业版本功能)
                    ICiSchemaViewCrossoverMapper mapper = CrossoverServiceFactory.tryToGetApi(ICiSchemaViewCrossoverMapper.class);
                    if (mapper != null) {
                        CiVo ciViewVo = new CiVo();
                        ciViewVo.setId(ciVo.getId());
                        ciViewVo.setName(ciVo.getName());
                        mapper.createCiView(ciViewVo);
                    }
                } else {
                    //如果已存在但没有数据，重建表
                    Integer rc = ciSchemaMapper.checkTableHasData(TenantContext.get().getDataDbName(), ciVo.getCiTableName(false));
                    if (rc == null || rc <= 0) {
                        ciSchemaMapper.deleteCiTable(ciVo.getId(), ciVo.getCiTableName());
                        ciSchemaMapper.insertCiTable(ciVo.getId(), ciVo.getCiTableName());
                        List<AttrVo> attrList = attrMapper.getAttrByCiId(ciVo.getId());
                        for (AttrVo attrVo : attrList) {
                            // 仅为当前模型中不使用cmdb_attr_invoke的普通属性重建动态字段。
                            if (attrVo.getCiId().equals(ciVo.getId())
                                    && attrVo.getTargetCiId() == null
                                    && !attrVo.getIsInvokeAttr()) {
                                ciSchemaMapper.insertAttrToCiTable(ciVo.getId(), ciVo.getCiTableName(), attrVo);
                                if (Objects.equals(attrVo.getIsSearchAble(), 1)) {
                                    if (ciSchemaMapper.checkIndexIsExists(TenantContext.get().getDataDbName(), attrVo.getCiId(), attrVo.getId()) == 0) {
                                        //创建hash字段的索引
                                        int indexCount = ciSchemaMapper.getIndexCount(TenantContext.get().getDataDbName(), attrVo.getCiId());
                                        if (indexCount <= 60) {
                                            ciSchemaMapper.addAttrIndex(attrVo.getCiTableName(), attrVo.getId());
                                        } else {
                                            throw new InsertAttrToSchemaException(attrVo.getName(), 60);
                                        }
                                    }
                                }
                            }
                        }
                        //补充物化视图(商业版本功能)
                        ICiSchemaViewCrossoverMapper mapper = CrossoverServiceFactory.tryToGetApi(ICiSchemaViewCrossoverMapper.class);
                        if (mapper != null) {
                            CiVo ciViewVo = new CiVo();
                            ciViewVo.setId(ciVo.getId());
                            ciViewVo.setName(ciVo.getName());
                            ciViewVo.setAttrList(attrList);
                            mapper.createCiView(ciViewVo);
                        }
                    }
                }
            } else {
                throw new DataBaseNotFoundException();
            }
        }).execute();
    }

    @Override
    public void buildCiView(CiVo ciVo) {
        VirtualCiSqlBuilder viewBuilder = new VirtualCiSqlBuilder(ciVo.getViewXml());
        viewBuilder.setCiId(ciVo.getId());

        if (viewBuilder.valid()) {
            //测试一下语句是否能正常执行
            try {
                ciSchemaMapper.testCiViewSql(viewBuilder.getTestSql());
            } catch (Exception ex) {
                throw new CiViewSqlIrregularException(ex);
            }
            List<AttrVo> attrList = viewBuilder.getAttrList();
            if (CollectionUtils.isNotEmpty(attrList)) {
                Map<String, Long> attrIdMap = new HashMap<>();
                List<AttrVo> oldAttrList = attrMapper.getAttrByCiId(ciVo.getId());
                Map<String, Long> oldAttrMap = new HashMap<>();
                Map<Long, Boolean> attrExistsMap = new HashMap<>();
                if (CollectionUtils.isNotEmpty(oldAttrList)) {
                    for (AttrVo attrVo : oldAttrList) {
                        oldAttrMap.put(attrVo.getName(), attrVo.getId());
                        attrExistsMap.put(attrVo.getId(), false);
                    }
                }
                for (AttrVo attrVo : attrList) {
                    if (!oldAttrMap.containsKey(attrVo.getName())) {
                        attrVo.setCiId(ciVo.getId());
                        attrMapper.insertAttr(attrVo);
                    } else {
                        //还原原来的id
                        attrVo.setId(oldAttrMap.get(attrVo.getName()));
                        attrMapper.updateAttr(attrVo);
                        attrExistsMap.put(attrVo.getId(), true);
                    }
                    attrIdMap.put(attrVo.getName(), attrVo.getId());
                }
                //删除没用的属性
                for (Long key : attrExistsMap.keySet()) {
                    if (!attrExistsMap.get(key)) {
                        attrMapper.deleteAttrById(key);
                    }
                }
                viewBuilder.setAttrIdMap(attrIdMap);
                String selectSql = viewBuilder.getSql();
                String md5 = Md5Util.encryptMD5(selectSql);
                String viewName = "cmdb_" + ciVo.getId();
                String tableType = schemaMapper.checkTableOrViewIsExists(TenantContext.get().getDataDbName(), viewName);
                if (tableType != null) {
                    if (Objects.equals(tableType, "SYSTEM VIEW")) {
                        return;
                    } else if (Objects.equals(tableType, "VIEW")) {
                        DataBaseViewInfoVo dataBaseViewInfoVo = dataBaseViewInfoMapper.getDataBaseViewInfoByViewName(viewName);
                        if (dataBaseViewInfoVo != null) {
                            // md5相同就不用更新视图了
                            if (Objects.equals(md5, dataBaseViewInfoVo.getMd5())) {
                                return;
                            }
                        }
                    }
                }
                EscapeTransactionJob.State s = new EscapeTransactionJob(() -> {
                    String sql = "CREATE OR REPLACE VIEW " + TenantContext.get().getDataDbName() + "." + viewName + " AS " + selectSql;
                    if (ciSchemaMapper.checkSchemaIsExists(TenantContext.get().getDataDbName()) > 0) {
                        //创建配置项表
                        ciSchemaMapper.insertCiView(sql);
                    } else {
                        throw new DataBaseNotFoundException();
                    }
                }).execute();
                if (!s.isSucceed()) {
                    throw new CreateCiSchemaException(ciVo.getName(), true);
                }
                DataBaseViewInfoVo dataBaseViewInfoVo = new DataBaseViewInfoVo();
                dataBaseViewInfoVo.setViewName(viewName);
                dataBaseViewInfoVo.setMd5(md5);
                dataBaseViewInfoVo.setLcu(UserContext.get().getUserUuid());
                dataBaseViewInfoMapper.insertDataBaseViewInfo(dataBaseViewInfoVo);
            }
        }
    }

    @Override
    @Transactional
    public void updateCiUnique(Long ciId, List<Long> attrIdList) {
        List<AttrVo> attrList = attrMapper.getAttrByCiId(ciId);
        ciMapper.deleteCiUniqueByCiId(ciId);
        if (CollectionUtils.isNotEmpty(attrIdList)) {
            for (Long attrId : attrIdList) {
                if (attrList.stream().noneMatch(attr -> attr.getId().equals(attrId))) {
                    throw new CiUniqueRuleHasNotExistsAttrException(attrId);
                }
                ciMapper.insertCiUnique(ciId, attrId);
            }
        }
    }

    @Override
    @Transactional
    public void updateCiNameAttrId(CiVo ciVo) {
        ciMapper.updateCiNameAttrId(ciVo);
        //非抽象模型才需要更新名字表达式
        if (ciVo.getIsAbstract().equals(0)) {
            AfterTransactionJob<CiVo> job = new AfterTransactionJob<>("UPDATE-CIENTITY-NAME-" + ciVo.getId());
            job.execute(ciVo, dataCiVo -> ciEntityService.updateCiEntityNameForCi(dataCiVo));
        }
    }


    @Override
    @Transactional
    public void updateCi(CiVo ciVo) {
        CiVo checkCiVo = ciMapper.getCiById(ciVo.getId());
        if (checkCiVo == null) {
            throw new CiNotFoundException(ciVo.getId());
        }
        List<AttrVo> parentAttrList = null;
        boolean needRebuildLRCode = false;
        if (!Objects.equals(checkCiVo.getParentCiId(), ciVo.getParentCiId())) {

            //如果继承发生改变需要检查是否有配置项数据，有数据不允许变更
            int ciEntityCount = ciEntityMapper.getDownwardCiEntityCountByLR(ciVo.getLft(), ciVo.getRht());
            if (ciEntityCount > 0) {
                //有关系的情况太复杂，例如父模型引用了子模型，这部分数据能否删除？想清楚之前暂时不能自动清理带关系的数据数据
                if (ciVo.getParentCiId() != null || CollectionUtils.isNotEmpty(relMapper.getRelByCiId(ciVo.getId()))) {
                    throw new CiParentCanNotBeChangedException(ciVo.getName(), ciEntityCount);
                } else {
                    List<CiVo> parentCiList = ciMapper.getUpwardCiListByLR(ciVo.getLft(), ciVo.getRht());
                    parentCiList.removeIf(d -> d.getId().equals(ciVo.getId()));
                    //清空继承需要清除父模型的数据
                    if (CollectionUtils.isNotEmpty(parentCiList)) {
                        ciEntityMapper.deleteParentCiEntity(ciVo, parentCiList);
                    }
                }
            }

            parentAttrList = attrMapper.getAttrByCiId(checkCiVo.getParentCiId());
            if (CollectionUtils.isNotEmpty(parentAttrList)) {
                //检查子模型的表达式属性是否引用了父模型的属性
                List<AttrVo> attrExpressionList = attrMapper.getExpressionAttrByValueCiIdAndAttrIdList(ciVo.getId(), parentAttrList.stream().map(AttrVo::getId).collect(Collectors.toList()));
                attrExpressionList.removeIf(attr -> !attr.getCiId().equals(ciVo.getId()));
                if (CollectionUtils.isNotEmpty(attrExpressionList)) {
                    throw new AttrIsUsedInExpressionException(attrExpressionList);
                }

                //检查唯一规则是否有被引用
                if (CollectionUtils.isNotEmpty(checkCiVo.getUniqueAttrIdList())) {
                    for (Long uaId : checkCiVo.getUniqueAttrIdList()) {
                        if (parentAttrList.stream().anyMatch(attr -> attr.getId().equals(uaId))) {
                            throw new AttrIsUsedInUniqueRuleException();
                        }
                    }
                }

                //检查名称属性是否有被引用
               /* if (checkCiVo.getNameAttrId() != null && parentAttrList.stream().anyMatch(a -> a.getId().equals(checkCiVo.getNameAttrId()))) {
                    throw new AttrIsUsedInNameAttrException();
                }*/
            }
            needRebuildLRCode = true;
            //LRCodeManager.moveTreeNode("cmdb_ci", "id", "parent_ci_id", ciVo.getId(), MoveType.INNER, ciVo.getParentCiId());
        }
        if (ciMapper.checkCiNameIsExists(ciVo) > 0) {
            throw new CiNameIsExistsException(ciVo.getName());
        }
        if (ciMapper.checkCiLabelIsExists(ciVo) > 0) {
            throw new CiLabelIsExistsException(ciVo.getLabel());
        }
        if (Objects.equals(ciVo.getIsVirtual(), 1)) {
            buildCiView(ciVo);
        } else if (Objects.equals(ciVo.getIsVirtual(), 0)) {
            EscapeTransactionJob.State s = buildCiSchema(ciVo);
            if (!s.isSucceed()) {
                throw new CreateCiSchemaException(ciVo.getName());
            }
        }
        ciMapper.updateCi(ciVo);
        if (!Objects.equals(checkCiVo.getParentCiId(), ciVo.getParentCiId())) {
            //由于名称属性一旦选中不能取消，所以直接修改，不告警
            if (checkCiVo.getNameAttrId() != null && CollectionUtils.isNotEmpty(parentAttrList) && parentAttrList.stream().anyMatch(a -> a.getId().equals(checkCiVo.getNameAttrId()))) {
                ciVo.setNameAttrId(null);
                ciMapper.updateCiNameAttrId(ciVo);
            }

            //更换父模型的视图数据
            ciViewMapper.deleteCiViewByCiId(ciVo.getId());
            if (ciVo.getParentCiId() != null) {
                List<CiViewVo> ciViewList = ciViewMapper.getCiViewBaseInfoByCiId(ciVo.getParentCiId());
                if (CollectionUtils.isNotEmpty(ciViewList)) {
                    for (CiViewVo ciViewVo : ciViewList) {
                        ciViewVo.setCiId(ciVo.getId());
                        ciViewMapper.insertCiView(ciViewVo);
                    }
                }
            }
        }
        if (needRebuildLRCode) {
            //重建所有左右编码，性能差点但可靠
            LRCodeManager.rebuildLeftRightCode("cmdb_ci", "id", "parent_ci_id");
        }
        if (Objects.equals(ciVo.getIsVirtual(), 0) && checkCiVo.getExpiredDay() != ciVo.getExpiredDay()) {
            AfterTransactionJob<CiVo> job = new AfterTransactionJob<>("REFRESH-CIENTITY-EXPIREDTIME-" + ciVo.getId());
            job.execute(ciVo, _ciVo -> {
                //修正配置项超时数据
                if (_ciVo.getExpiredDay() == 0) {
                    ciEntityMapper.deleteCiEntityExpiredTimeByCiId(_ciVo.getId());
                } else {
                    CiEntityVo pCiEntityVo = new CiEntityVo();
                    pCiEntityVo.setCiId(_ciVo.getId());
                    pCiEntityVo.setPageSize(100);
                    pCiEntityVo.setCurrentPage(1);
                    List<CiEntityVo> ciEntityList = ciEntityMapper.searchCiEntityBaseInfo(pCiEntityVo);
                    while (CollectionUtils.isNotEmpty(ciEntityList)) {
                        for (CiEntityVo cientity : ciEntityList) {
                            if (cientity.getRenewTime() != null) {
                                Calendar c = Calendar.getInstance();
                                c.setTime(cientity.getRenewTime());
                                c.add(Calendar.DAY_OF_YEAR, ciVo.getExpiredDay());
                                cientity.setExpiredTime(c.getTime());
                                cientity.setExpiredDay(ciVo.getExpiredDay());
                                ciEntityMapper.insertCiEntityExpiredTime(cientity);
                            }
                        }
                        pCiEntityVo.setCurrentPage(pCiEntityVo.getCurrentPage() + 1);
                        ciEntityList = ciEntityMapper.searchCiEntityBaseInfo(pCiEntityVo);
                    }
                }
            });
        }

        if (Objects.equals(ciVo.getIsVirtual(), 1)) {
            buildCiView(ciVo);
        }
    }

    @Override
    public int deleteCi(Long ciId) {
        CiVo ciVo = ciMapper.getCiById(ciId);
        if (ciVo == null) {
            throw new CiNotFoundException(ciId);
        }
        //检查当前模型是否被继承
        List<CiVo> childCiList = ciMapper.getDownwardCiListByLR(ciVo.getLft(), ciVo.getRht());
        if (childCiList.size() > 1) {
            throw new CiHasBeenExtendedException(ciVo.getLabel(), childCiList.subList(1, childCiList.size()));
        }
        // 检查当前模型是否有被引用
        List<CiVo> fromCiList = ciMapper.getCiByToCiId(ciId);
        if (CollectionUtils.isNotEmpty(fromCiList)) {
            throw new CiHasRelException(
                    fromCiList.stream().map(CiVo::getLabel).collect(Collectors.joining("、")));
        }
        List<CiVo> toCiList = ciMapper.getCiByFromCiId(ciId);
        if (CollectionUtils.isNotEmpty(toCiList)) {
            throw new CiHasRelException(
                    toCiList.stream().map(CiVo::getLabel).collect(Collectors.joining("、")));
        }
        //检查是否被属性引用
        List<CiVo> attrCiList = ciMapper.getCiByTargetCiId(ciId);
        if (CollectionUtils.isNotEmpty(attrCiList)) {
            throw new CiHasAttrException(
                    attrCiList.stream().map(CiVo::getLabel).collect(Collectors.joining("、")));
        }
        //检查模型以及子模型是否有数据
        int ciEntityCount = ciEntityMapper.getDownwardCiEntityCountByLR(ciVo.getLft(), ciVo.getRht());
        if (ciEntityCount > 0) {
            throw new CiIsNotEmptyException(ciId, ciEntityCount);
        }

        //检查是否被自定义视图引用
        List<CustomViewVo> viewList = ciMapper.getCustomViewByCiId(ciVo.getId());
        if (CollectionUtils.isNotEmpty(viewList)) {
            throw new CiIsUsedInCustomViewException(ciVo, viewList);
        }

        //TODO 检查是否被资源中心引用


        //清除模型数据
        LRCodeManager.beforeDeleteTreeNode("cmdb_ci", "id", "parent_ci_id", ciId);
        ciMapper.deleteCiById(ciId);

        AfterTransactionJob<CiVo> job = new AfterTransactionJob<>("DELETE-CI-" + ciVo.getId());
        job.execute(ciVo, dataCiVo -> {
            if (StringUtils.isNotBlank(dataCiVo.getCiTableName())) {
                if (dataCiVo.getIsVirtual().equals(0)) {
                    ciSchemaMapper.deleteCiTable(dataCiVo.getId(), dataCiVo.getCiTableName());
                } else {
                    ciSchemaMapper.deleteCiView(dataCiVo.getCiTableName());
                }
            }
        });

        //删除物化视图(商业版本功能)
        AfterTransactionJob<CiVo> afterTransactionJob = new AfterTransactionJob<>("DELETE-CI-VIEW");
        afterTransactionJob.execute(ciVo, ci -> {
            ICiSchemaViewCrossoverMapper mapper = CrossoverServiceFactory.tryToGetApi(ICiSchemaViewCrossoverMapper.class);
            if (mapper != null) {
                mapper.deleteCiView(ci);
            }
        });
        return 0;
    }


    @Override
    public CiVo getCiById(Long id) {
        CiVo ciVo = ciMapper.getCiById(id);
        if (ciVo == null) {
            throw new CiNotFoundException(id);
        }
        if (ciVo.getIsVirtual().equals(0)) {
            ciVo.setUpwardCiList(ciMapper.getUpwardCiListByLR(ciVo.getLft(), ciVo.getRht()));
        }
        List<AttrVo> attrList = attrMapper.getAttrByCiId(id);
        List<RelVo> relList = RelUtil.ClearRepeatRel(relMapper.getRelByCiId(id));
        List<GlobalAttrVo> globalAttrList = globalAttrMapper.getGlobalAttrByCiId(id);
        ciVo.setRelList(relList);
        ciVo.setAttrList(attrList);
        ciVo.setGlobalAttrList(globalAttrList);
        return ciVo;
    }

    @Override
    public CiVo getCiByName(String ciName) {
        CiVo ciVo = ciMapper.getCiByName(ciName);
        if (ciVo == null) {
            throw new CiNotFoundException(ciName);
        }
        if (ciVo.getIsVirtual().equals(0)) {
            ciVo.setUpwardCiList(ciMapper.getUpwardCiListByLR(ciVo.getLft(), ciVo.getRht()));
        }
        List<AttrVo> attrList = attrMapper.getAttrByCiId(ciVo.getId());
        List<RelVo> relList = RelUtil.ClearRepeatRel(relMapper.getRelByCiId(ciVo.getId()));
        ciVo.setRelList(relList);
        ciVo.setAttrList(attrList);
        return ciVo;
    }

    @Override
    public void initCiTableView() {
        List<CiVo> ciList = ciMapper.searchCi(new CiVo());
        ciList.sort(Comparator.comparing(CiVo::getLft));
        Map<Long, CiVo> ciMap = ciList.stream().collect(Collectors.toMap(CiVo::getId, e -> e));
        for (CiVo ciVo : ciList) {
            if (ciVo.getIsVirtual().equals(0)) {
                List<AttrVo> attrList = attrMapper.getAttrByCiId(ciVo.getId());
                ciVo.setAttrList(attrList);
                // 生成动态表
                ciSchemaMapper.initCiTable(ciVo.getId(), ciVo);
                //补充物化视图(商业版本功能)
                ICiSchemaViewCrossoverMapper mapper = CrossoverServiceFactory.tryToGetApi(ICiSchemaViewCrossoverMapper.class);
                if (mapper != null) {
                    CiVo ciViewVo = new CiVo();
                    ciViewVo.setId(ciVo.getId());
                    ciViewVo.setName(ciVo.getName());
                    ciViewVo.setAttrList(attrList);
                    mapper.createCiView(ciViewVo);
                }
                // 向动态表中插入数据
                CiVo tempCi = ciVo;
                List<Long> upwardIdList = new ArrayList<>();
                upwardIdList.add(tempCi.getId());
                while (tempCi.getParentCiId() != null) {
                    upwardIdList.add(tempCi.getParentCiId());
                    tempCi = ciMap.get(tempCi.getParentCiId());
                }
                CiEntityVo search = new CiEntityVo();
                search.setCiId(ciVo.getId());
                search.setPageSize(100);
                List<Long> ciEntityIdList = ciEntityMapper.getCiEntityIdByCiId(search);
                for (Long ciEntityId : ciEntityIdList) {
                    CiEntityVo ciEntityVo = new CiEntityVo();
                    ciEntityVo.setId(ciEntityId);
                    for (Long ciId : upwardIdList) {
                        ciEntityVo.setCiId(ciId);
                        ciEntityMapper.insertCiEntity(ciEntityVo);
                    }
                }
            } else {
                //创建视图
                String viewXml = ciMapper.getCiViewXmlById(ciVo.getId());
                if (StringUtils.isNotBlank(viewXml)) {
                    ciVo.setViewXml(viewXml);
                    buildCiView(ciVo);
                }
            }
        }
    }
}
