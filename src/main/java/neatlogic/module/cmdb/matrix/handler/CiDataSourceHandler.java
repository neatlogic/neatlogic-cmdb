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

package neatlogic.module.cmdb.matrix.handler;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiViewVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.dto.cientity.AttrFilterVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.cientity.RelEntityVo;
import neatlogic.framework.cmdb.dto.cientity.RelFilterVo;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrFilterVo;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrVo;
import neatlogic.framework.cmdb.dto.view.ViewConstVo;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.cmdb.utils.RelUtil;
import neatlogic.framework.dependency.core.DependencyManager;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.matrix.constvalue.MatrixAttributeType;
import neatlogic.framework.matrix.constvalue.SearchExpression;
import neatlogic.framework.matrix.core.MatrixDataSourceHandlerBase;
import neatlogic.framework.matrix.dto.*;
import neatlogic.framework.matrix.exception.MatrixAttributeNotFoundException;
import neatlogic.framework.matrix.exception.MatrixAttributeUniqueIdentifierIsRequiredException;
import neatlogic.framework.matrix.exception.MatrixAttributeUniqueIdentifierRepeatException;
import neatlogic.framework.matrix.exception.MatrixCiNotFoundException;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.framework.util.UuidUtil;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiViewMapper;
import neatlogic.module.cmdb.dao.mapper.ci.RelMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.RelEntityMapper;
import neatlogic.module.cmdb.dao.mapper.globalattr.GlobalAttrMapper;
import neatlogic.module.cmdb.dependency.CiAttr2MatrixAttrDependencyHandler;
import neatlogic.module.cmdb.matrix.constvalue.MatrixType;
import neatlogic.module.cmdb.matrix.dto.MatrixCiEntitySearchVo;
import neatlogic.module.cmdb.matrix.service.MatrixCiEntityService;
import neatlogic.module.cmdb.service.cientity.CiEntityService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author linbq
 * @since 2021/11/15 14:35
 **/
//@Component
public class CiDataSourceHandler extends MatrixDataSourceHandlerBase {

    private final static Logger logger = LoggerFactory.getLogger(CiDataSourceHandler.class);

    // 固化属性列表
    private final List<String> constAttrNameList = Arrays.asList("_id", "_typeName", "_ciLabel", "_inspectTime", "_inspectStatus", "_monitorTime", "_monitorStatus");

    @Resource
    private CiMapper ciMapper;

    @Resource
    private CiEntityMapper ciEntityMapper;

    @Resource
    private AttrMapper attrMapper;

    @Resource
    private RelMapper relMapper;

    @Resource
    private RelEntityMapper relEntityMapper;

    @Resource
    private CiViewMapper ciViewMapper;

    @Resource
    private GlobalAttrMapper globalAttrMapper;

    @Resource
    private CiEntityService ciEntityService;

    @Resource
    private MatrixCiEntityService matrixCiEntityService;

    @Override
    public String getHandler() {
        return "";
//        return MatrixType.CMDBCI.getValue();
    }

    @Override
    protected boolean mySaveMatrix(MatrixVo matrixVo) throws Exception {
        Long ciId = matrixVo.getCiId();
        if (ciId == null) {
            throw new ParamNotExistsException("ciId");
        }
        CiVo ciVo = ciMapper.getCiById(ciId);
        if (ciVo == null) {
            throw new CiNotFoundException(ciId);
        }
        JSONObject config = matrixVo.getConfig();
        if (MapUtils.isEmpty(config)) {
            throw new ParamNotExistsException("config");
        }
        JSONArray attributeMappingArray = config.getJSONArray("attributeMappingList");
        if (CollectionUtils.isEmpty(attributeMappingArray)) {
            throw new ParamNotExistsException("config.attributeMappingList");
        }
        Map<String, String> oldShowAttributeUuidMap = new HashMap<>();
        MatrixCiVo oldMatrixCiVo = matrixMapper.getMatrixCiByMatrixUuid(matrixVo.getUuid());
        if (oldMatrixCiVo != null) {
            if (ciId.equals(oldMatrixCiVo.getCiId())) {
                JSONObject oldConfig = oldMatrixCiVo.getConfig();
                if (MapUtils.isNotEmpty(oldConfig)) {
                    JSONArray oldAttributeMappingArray = oldConfig.getJSONArray("attributeMappingList");
                    if (CollectionUtils.isNotEmpty(oldAttributeMappingArray)) {
                        if (CollectionUtils.isEqualCollection(oldAttributeMappingArray, attributeMappingArray)) {
                            return false;
                        }
                    }
                    JSONArray showAttributeArray = oldConfig.getJSONArray("showAttributeList");
                    if (CollectionUtils.isNotEmpty(showAttributeArray)) {
                        for (int i = 0; i < showAttributeArray.size(); i++) {
                            JSONObject showAttributeObj = showAttributeArray.getJSONObject(i);
                            if (MapUtils.isNotEmpty(showAttributeObj)) {
                                String uuid = showAttributeObj.getString("uuid");
                                if (uuid != null) {
                                    oldShowAttributeUuidMap.put(showAttributeObj.getString("label"), uuid);
                                    DependencyManager.delete(CiAttr2MatrixAttrDependencyHandler.class, uuid);
                                }
                            }
                        }
                    }
                }
            }
        }
        CiViewVo searchVo = new CiViewVo();
        searchVo.setCiId(ciId);
        Map<String, CiViewVo> ciViewMap = new HashMap<>();
        List<CiViewVo> ciViewList = RelUtil.ClearCiViewRepeatRel(ciViewMapper.getCiViewByCiId(searchVo), searchVo.getCiId());
        for (CiViewVo ciview : ciViewList) {
            switch (ciview.getType()) {
                case "attr":
                    ciViewMap.put("attr_" + ciview.getItemId(), ciview);
                    break;
                case "relfrom":
                    ciViewMap.put("relfrom_" + ciview.getItemId(), ciview);
                    break;
                case "relto":
                    ciViewMap.put("relto_" + ciview.getItemId(), ciview);
                    break;
                case "const":
                    //固化属性需要特殊处理
                    ciViewMap.put("const_" + ciview.getItemName().replace("_", ""), ciview);
                    break;
                case "global":
                    ciViewMap.put("global_" + ciview.getItemId(), ciview);
                    break;
            }
        }
        JSONArray showAttributeArray = new JSONArray();
        List<String> uniqueIdentifierList = new ArrayList<>();
        boolean flag = false;
        for (int i = 0; i < attributeMappingArray.size(); i++) {
            JSONObject attributeMappingObj = attributeMappingArray.getJSONObject(i);
            if (MapUtils.isEmpty(attributeMappingObj)) {
                continue;
            }
            String label = attributeMappingObj.getString("label");
            CiViewVo ciViewVo = ciViewMap.get(label);
            if (ciViewVo == null) {
                continue;
            }
            String uniqueIdentifier = attributeMappingObj.getString("uniqueIdentifier");
            if (StringUtils.isBlank(uniqueIdentifier)) {
                throw new MatrixAttributeUniqueIdentifierIsRequiredException(matrixVo.getName(), ciViewVo.getItemLabel());
            }
            if (uniqueIdentifierList.contains(uniqueIdentifier)) {
                throw new MatrixAttributeUniqueIdentifierRepeatException(matrixVo.getName(), ciViewVo.getItemLabel());
            }
            uniqueIdentifierList.add(uniqueIdentifier);
            if (Objects.equals(attributeMappingObj.getString("label"), "const_id")) {
                flag = true;
            }
        }
        if (!flag) {
            JSONObject attributeMappingObj = new JSONObject();
            attributeMappingObj.put("label", "const_id");
            attributeMappingObj.put("uniqueIdentifier", "const_id");
            attributeMappingArray.add(0, attributeMappingObj);
        }
        Iterator<Object> iterator = attributeMappingArray.iterator();
        while (iterator.hasNext()) {
            JSONObject attributeMappingObj = (JSONObject) iterator.next();
            String uniqueIdentifier = attributeMappingObj.getString("uniqueIdentifier");
            String showAttributeLabel = attributeMappingObj.getString("label");
            JSONObject showAttributeObj = new JSONObject();
            String showAttributeUuid = oldShowAttributeUuidMap.get(showAttributeLabel);
            if (showAttributeUuid == null) {
                showAttributeUuid = UuidUtil.getCustomUUID(matrixVo.getLabel() + "_" + uniqueIdentifier);
            }
            showAttributeObj.put("uuid", showAttributeUuid);
            CiViewVo ciViewVo = ciViewMap.get(showAttributeLabel);
            if (ciViewVo == null) {
                iterator.remove();
                continue;
            }
            showAttributeObj.put("name", ciViewVo.getItemLabel());
            showAttributeObj.put("label", showAttributeLabel);
            showAttributeObj.put("uniqueIdentifier", uniqueIdentifier);
            showAttributeArray.add(showAttributeObj);
            if (showAttributeLabel.startsWith("const_")) {
                continue;
            }
            JSONObject dependencyConfig = new JSONObject();
            dependencyConfig.put("matrixUuid", matrixVo.getUuid());
            dependencyConfig.put("ciId", ciId);
            DependencyManager.insert(CiAttr2MatrixAttrDependencyHandler.class, showAttributeLabel.split("_")[1], showAttributeUuid, dependencyConfig);
        }
        config.put("showAttributeList", showAttributeArray);
        MatrixCiVo matrixCiVo = new MatrixCiVo(matrixVo.getUuid(), ciId, config);
        matrixMapper.replaceMatrixCi(matrixCiVo);
        return true;
    }

    @Override
    protected void myGetMatrix(MatrixVo matrixVo) {
        MatrixCiVo matrixCiVo = matrixMapper.getMatrixCiByMatrixUuid(matrixVo.getUuid());
        if (matrixCiVo == null) {
            throw new MatrixCiNotFoundException(matrixVo.getUuid());
        }
        matrixVo.setCiId(matrixCiVo.getCiId());
        JSONObject config = matrixCiVo.getConfig();
        CiVo ciVo = ciMapper.getCiById(matrixCiVo.getCiId());
        if (ciVo != null) {
            config.put("ciName", ciVo.getName());
            config.put("ciLabel", ciVo.getLabel());
        }
        JSONArray attributeMappingList = config.getJSONArray("attributeMappingList");
        if (CollectionUtils.isEmpty(attributeMappingList)) {
            attributeMappingList = new JSONArray();
            JSONArray showAttributeList = config.getJSONArray("showAttributeList");
            if (CollectionUtils.isNotEmpty(showAttributeList)) {
                for (int i = 0; i < showAttributeList.size(); i++) {
                    JSONObject showAttributeObj = showAttributeList.getJSONObject(i);
                    if (MapUtils.isNotEmpty(showAttributeObj)) {
                        String label = showAttributeObj.getString("label");
                        if (StringUtils.isNotBlank(label)) {
                            JSONObject attributeMappingObj = new JSONObject();
                            attributeMappingObj.put("label", label);
                            attributeMappingObj.put("uniqueIdentifier", "");
                            attributeMappingList.add(attributeMappingObj);
                        }
                    }
                }
            }
            config.put("attributeMappingList", attributeMappingList);
        }
        matrixVo.setConfig(config);
    }

    @Override
    protected void myDeleteMatrix(String uuid) {
        MatrixCiVo matrixCiVo = matrixMapper.getMatrixCiByMatrixUuid(uuid);
        if (matrixCiVo != null) {
            matrixMapper.deleteMatrixCiByMatrixUuid(uuid);
            JSONObject config = matrixCiVo.getConfig();
            if (MapUtils.isNotEmpty(config)) {
                JSONArray showAttributeArray = config.getJSONArray("showAttributeList");
                if (CollectionUtils.isNotEmpty(showAttributeArray)) {
                    for (int i = 0; i < showAttributeArray.size(); i++) {
                        JSONObject showAttributeObj = showAttributeArray.getJSONObject(i);
                        if (MapUtils.isNotEmpty(showAttributeObj)) {
                            Long id = showAttributeObj.getLong("id");
                            if (id != null) {
                                DependencyManager.delete(CiAttr2MatrixAttrDependencyHandler.class, id);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void myCopyMatrix(String sourceUuid, MatrixVo matrixVo) {

    }

    @Override
    protected JSONObject myImportMatrix(MatrixVo matrixVo, MultipartFile multipartFile) throws IOException {
        return null;
    }

    @Override
    protected void myExportMatrix2CSV(MatrixVo matrixVo, OutputStream os) throws IOException {
        String matrixUuid = matrixVo.getUuid();
        MatrixCiVo matrixCiVo = matrixMapper.getMatrixCiByMatrixUuid(matrixUuid);
        if (matrixCiVo == null) {
            throw new MatrixCiNotFoundException(matrixUuid);
        }
        List<MatrixAttributeVo> attributeVoList = myGetAttributeList(matrixVo);
        JSONArray theadList = getTheadList(attributeVoList);
        StringBuilder header = new StringBuilder();
        List<String> headList = new ArrayList<>();
        for (MatrixAttributeVo attributeVo : attributeVoList) {
            String title = attributeVo.getName();
            String key = attributeVo.getLabel();
            if (StringUtils.isNotBlank(title) && StringUtils.isNotBlank(key)) {
                header.append(title).append(",");
                headList.add(key);
            }
        }
        header.append("\n");
        os.write(header.toString().getBytes("GBK"));
        os.flush();
        CiEntityVo ciEntityVo = new CiEntityVo();
        ciEntityVo.setCiId(matrixCiVo.getCiId());
        ciEntityVo.setCurrentPage(1);
        ciEntityVo.setPageSize(1000);
        setAttrIdListAndRelIdListFromMatrixConfig(matrixCiVo, ciEntityVo);
        List<CiEntityVo> ciEntityList = ciEntityService.searchCiEntity(ciEntityVo);
        Integer rowNum = ciEntityVo.getRowNum();
        if (rowNum > 0) {
            List<String> viewConstNameList = new ArrayList<>();
            List<ViewConstVo> ciViewConstList = ciViewMapper.getAllCiViewConstList();
            for (ViewConstVo viewConstVo : ciViewConstList) {
                viewConstNameList.add(viewConstVo.getName());
            }
            int currentPage = 1;
            ciEntityVo.setPageSize(1000);
            Integer pageCount = ciEntityVo.getPageCount();
            List<CiEntityVo> list;
            while (currentPage <= pageCount) {
                if (currentPage == 1) {
                    list = ciEntityList;
                } else {
                    ciEntityVo.setCurrentPage(currentPage);
                    ciEntityVo.setGlobalAttrStrictMode(true);
                    list = ciEntityService.searchCiEntity(ciEntityVo);
                }
                if (CollectionUtils.isNotEmpty(list)) {
                    StringBuilder content = new StringBuilder();
                    for (CiEntityVo ciEntity : list) {
                        JSONObject rowData = ciEntityService.getTbodyRowData(viewConstNameList, ciEntity);
                        for (String head : headList) {
                            String value = rowData.getString(head);
                            content.append(value != null ? value.replaceAll("\n", "").replaceAll(",", "，") : StringUtils.EMPTY).append(",");
                        }
                        content.append("\n");
                    }
                    os.write(content.toString().getBytes("GBK"));
                    os.flush();
                }
                list.clear();
                currentPage++;
            }
        }
    }

    @Override
    protected MatrixVo myExportMatrix(MatrixVo matrixVo) {
        myGetMatrix(matrixVo);
        return matrixVo;
    }

    @Override
    protected void myImportMatrix(MatrixVo matrixVo) {
        matrixMapper.deleteMatrixCiByMatrixUuid(matrixVo.getUuid());
        MatrixCiVo matrixCiVo = new MatrixCiVo(matrixVo.getUuid(), matrixVo.getCiId(), matrixVo.getConfig());
        matrixMapper.replaceMatrixCi(matrixCiVo);
    }

    @Override
    protected void mySaveAttributeList(String matrixUuid, List<MatrixAttributeVo> matrixAttributeList) {

    }

    @Override
    protected List<MatrixAttributeVo> myGetAttributeList(MatrixVo matrixVo) {
        Long ciId = null;
        Map<String, String> showAttributeUuidMap = new HashMap<>();
        Map<String, String> showAttributeUniqueIdentifierMap = new HashMap<>();
        String matrixUuid = matrixVo.getUuid();
        if (StringUtils.isNotBlank(matrixUuid)) {
            MatrixCiVo matrixCiVo = matrixMapper.getMatrixCiByMatrixUuid(matrixUuid);
            if (matrixCiVo == null) {
                throw new MatrixCiNotFoundException(matrixUuid);
            }
            ciId = matrixCiVo.getCiId();
            JSONObject config = matrixCiVo.getConfig();
            JSONArray showAttributeArray = config.getJSONArray("showAttributeList");
            for (int i = 0; i < showAttributeArray.size(); i++) {
                JSONObject showAttributeObj = showAttributeArray.getJSONObject(i);
                showAttributeUuidMap.put(showAttributeObj.getString("label"), showAttributeObj.getString("uuid"));
                showAttributeUniqueIdentifierMap.put(showAttributeObj.getString("label"), showAttributeObj.getString("uniqueIdentifier"));
            }
        } else {
            ciId = matrixVo.getCiId();
        }
        CiVo ciVo = ciMapper.getCiById(ciId);
        if (ciVo == null) {
            throw new CiNotFoundException(ciId);
        }
        int sort = 0;
        List<MatrixAttributeVo> matrixAttributeList = new ArrayList<>();
        CiViewVo ciViewVo = new CiViewVo();
        ciViewVo.setCiId(ciId);
        List<CiViewVo> ciViewList = RelUtil.ClearCiViewRepeatRel(ciViewMapper.getCiViewByCiId(ciViewVo), ciViewVo.getCiId());
        if (CollectionUtils.isNotEmpty(ciViewList)) {
            List<AttrVo> attrList = attrMapper.getAttrByCiId(ciId);
            Map<Long, AttrVo> attrMap = attrList.stream().collect(Collectors.toMap(AttrVo::getId, e -> e));
            List<RelVo> relList = RelUtil.ClearRepeatRel(relMapper.getRelByCiId(ciId));
            Map<Long, RelVo> fromRelMap = relList.stream().filter(rel -> rel.getDirection().equals("from")).collect(Collectors.toMap(RelVo::getId, e -> e));
            Map<Long, RelVo> toRelMap = relList.stream().filter(rel -> rel.getDirection().equals("to")).collect(Collectors.toMap(RelVo::getId, e -> e));

            GlobalAttrVo searchVo = new GlobalAttrVo();
            searchVo.setIsActive(1);
            List<GlobalAttrVo> globalAttrList = globalAttrMapper.searchGlobalAttr(searchVo);
            Map<Long, GlobalAttrVo> globalAttrMap = globalAttrList.stream().collect(Collectors.toMap(GlobalAttrVo::getId, e -> e));
            for (CiViewVo ciview : ciViewList) {
                MatrixAttributeVo matrixAttributeVo = new MatrixAttributeVo();
                matrixAttributeVo.setMatrixUuid(matrixUuid);
                matrixAttributeVo.setName(ciview.getItemLabel());
                matrixAttributeVo.setDefaultUniqueIdentifier(ciview.getItemName());
                matrixAttributeVo.setType(MatrixAttributeType.INPUT.getValue());
                matrixAttributeVo.setIsDeletable(0);
                matrixAttributeVo.setSort(sort++);
                matrixAttributeVo.setIsRequired(0);
                switch (ciview.getType()) {
                    case "attr":
                        AttrVo attrVo = attrMap.get(ciview.getItemId());
                        if (attrVo == null
                                || Objects.equals(attrVo.getType(), "password")
                                || Objects.equals(attrVo.getType(), "file")
                                || Objects.equals(attrVo.getType(), "table")) {
                            break;
                        }
                        if (!Objects.equals(attrVo.getIsSearchAble(), 1)) {
                            matrixAttributeVo.setIsSearchable(0);
                        }
                        matrixAttributeVo.setLabel("attr_" + ciview.getItemId());
                        JSONObject attrConfig = new JSONObject();
                        attrConfig.put("attr", attrVo);
                        matrixAttributeVo.setConfig(attrConfig);
                        break;
                    case "relfrom":
                        matrixAttributeVo.setLabel("relfrom_" + ciview.getItemId());
                        RelVo fromRelVo = fromRelMap.get(ciview.getItemId());
                        JSONObject fromRelConfig = new JSONObject();
                        fromRelConfig.put("rel", fromRelVo);
                        matrixAttributeVo.setConfig(fromRelConfig);
                        break;
                    case "relto":
                        matrixAttributeVo.setLabel("relto_" + ciview.getItemId());
                        RelVo toRelVo = toRelMap.get(ciview.getItemId());
                        JSONObject toRelConfig = new JSONObject();
                        toRelConfig.put("rel", toRelVo);
                        matrixAttributeVo.setConfig(toRelConfig);
                        break;
                    case "const":
                        //固化属性需要特殊处理
                        String itemName = ciview.getItemName();
                        if (constAttrNameList.contains(itemName)) {
                            matrixAttributeVo.setLabel("const" + itemName);
                            if ("_id".equals(itemName)) {
                                matrixAttributeVo.setPrimaryKey(1);
                            } else if ("_ciLabel".equals(itemName)) {
                                // 不是抽象模型的模型属性不能搜索
                                if (Objects.equals(ciVo.getIsAbstract(), 0)) {
                                    matrixAttributeVo.setIsSearchable(0);
                                }
                                JSONObject config = new JSONObject();
                                config.put("ciId", ciId);
                                matrixAttributeVo.setConfig(config);
                            } else {
                                matrixAttributeVo.setIsSearchable(0);
                            }
                        }
                        break;
                    case "global":
                        // 全局属性
                        matrixAttributeVo.setLabel("global_" + ciview.getItemId());
                        GlobalAttrVo globalAttrVo = globalAttrMap.get(ciview.getItemId());
                        JSONObject globalConfig = new JSONObject();
                        globalConfig.put("global", globalAttrVo);
                        matrixAttributeVo.setConfig(globalConfig);
                        break;
                    default:
                        break;
                }
                if (StringUtils.isBlank(matrixAttributeVo.getLabel())) {
                    continue;
                }
                if (MapUtils.isNotEmpty(showAttributeUuidMap)) {
                    String uniqueIdentifier = showAttributeUniqueIdentifierMap.get(matrixAttributeVo.getLabel());
                    if (StringUtils.isNotBlank(uniqueIdentifier)) {
                        matrixAttributeVo.setUniqueIdentifier(uniqueIdentifier);
                    } else {
                        matrixAttributeVo.setUniqueIdentifier(StringUtils.EMPTY);
                    }
                    String uuid = showAttributeUuidMap.get(matrixAttributeVo.getLabel());
                    if (uuid == null && Objects.equals(matrixAttributeVo.getPrimaryKey(), 0)) {
                        continue;
                    }
                    matrixAttributeVo.setUuid(uuid);
                }
                matrixAttributeList.add(matrixAttributeVo);
            }
        }
        return matrixAttributeList;
    }

    @Override
    protected JSONObject myExportAttribute(MatrixVo matrixVo) {
        return null;
    }

//    @Override
//    protected JSONObject myGetTableData(MatrixDataVo dataVo) {
//        String matrixUuid = dataVo.getMatrixUuid();
//        MatrixCiVo matrixCiVo = matrixMapper.getMatrixCiByMatrixUuid(matrixUuid);
//        if (matrixCiVo == null) {
//            throw new MatrixCiNotFoundException(matrixUuid);
//        }
//        MatrixVo matrixVo = matrixMapper.getMatrixByUuid(matrixUuid);
//        List<MatrixAttributeVo> attributeVoList = myGetAttributeList(matrixVo);
//        CiEntityVo ciEntityVo = new CiEntityVo();
//        ciEntityVo.setCiId(matrixCiVo.getCiId());
//        ciEntityVo.setCurrentPage(dataVo.getCurrentPage());
//        ciEntityVo.setPageSize(dataVo.getPageSize());
//        JSONArray tbodyArray = accessSearchCiEntity(matrixUuid, ciEntityVo);
//        List<Map<String, Object>> tbodyList = new ArrayList<>();
//        if (CollectionUtils.isNotEmpty(tbodyArray)) {
//            Map<String, String> attributeUuidMap = attributeVoList.stream().collect(Collectors.toMap(e -> e.getLabel(), e -> e.getUuid()));
//            for (int i = 0; i < tbodyArray.size(); i++) {
//                JSONObject rowData = tbodyArray.getJSONObject(i);
//                if (MapUtils.isNotEmpty(rowData)) {
//                    Map<String, Object> rowDataMap = new HashMap<>();
//                    for (Map.Entry<String, Object> entry : rowData.entrySet()) {
//                        String uuid = attributeUuidMap.get(entry.getKey());
//                        if (StringUtils.isNotBlank(uuid)) {
//                            rowDataMap.put(uuid, matrixAttributeValueHandle(null, entry.getValue()));
//                        }
//                        if ("const_id".equals(entry.getKey())) {
//                            rowDataMap.put("uuid", matrixAttributeValueHandle(null, entry.getValue()));
//                        }
//                    }
//                    tbodyList.add(rowDataMap);
//                }
//            }
//        }
//        JSONArray theadList = getTheadList(attributeVoList);
//        return TableResultUtil.getResult(theadList, tbodyList, ciEntityVo);
//    }

    @Override
    protected JSONObject myTableDataSearch(MatrixDataVo dataVo) {
        String matrixUuid = dataVo.getMatrixUuid();
        MatrixCiVo matrixCiVo = matrixMapper.getMatrixCiByMatrixUuid(matrixUuid);
        if (matrixCiVo == null) {
            throw new MatrixCiNotFoundException(matrixUuid);
        }
        MatrixVo matrixVo = matrixMapper.getMatrixByUuid(matrixUuid);
        List<MatrixAttributeVo> matrixAttributeList = myGetAttributeList(matrixVo);
        if (CollectionUtils.isNotEmpty(matrixAttributeList)) {
            boolean needAccessApi = true;
            Long ciId = matrixCiVo.getCiId();
            CiEntityVo ciEntityVo = new CiEntityVo();
            ciEntityVo.setCiId(ciId);
            ciEntityVo.setCurrentPage(dataVo.getCurrentPage());
            ciEntityVo.setPageSize(dataVo.getPageSize());
            JSONArray defaultValue = dataVo.getDefaultValue();
            if (CollectionUtils.isNotEmpty(defaultValue)) {
                ciEntityVo.setIdList(defaultValue.toJavaList(Long.class));
            } else {
                List<AttrFilterVo> attrFilterList = new ArrayList<>();
                List<RelFilterVo> relFilterList = new ArrayList<>();
                JSONArray attrFilterArray = dataVo.getAttrFilterList();
                if (CollectionUtils.isNotEmpty(attrFilterArray)) {
                    attrFilterList = attrFilterArray.toJavaList(AttrFilterVo.class);
                }
                JSONArray relFilterArray = dataVo.getRelFilterList();
                if (CollectionUtils.isNotEmpty(relFilterArray)) {
                    relFilterList = relFilterArray.toJavaList(RelFilterVo.class);
                }
                List<MatrixFilterVo> filterList = dataVo.getFilterList();
                if (CollectionUtils.isNotEmpty(filterList)) {
                    Map<String, String> attributeLabelMap = matrixAttributeList.stream().collect(Collectors.toMap(e -> e.getUuid(), e -> e.getLabel()));
                    List<String> attributeList = matrixAttributeList.stream().map(MatrixAttributeVo::getUuid).collect(Collectors.toList());
                    Map<Long, AttrVo> attrMap = new HashMap<>();
                    Map<Long, RelVo> relMap = new HashMap<>();
                    Map<String, CiViewVo> ciViewMap = new HashMap<>();
                    Map<Long, GlobalAttrVo> globalAttrMap = new HashMap<>();
                    ciEntityService.getCiViewMapAndAttrMapAndRelMap(ciId, attrMap, relMap, ciViewMap, globalAttrMap);
                    for (MatrixFilterVo matrixFilterVo : filterList) {
                        if (matrixFilterVo == null) {
                            continue;
                        }
                        String uuid = matrixFilterVo.getUuid();
                        if (StringUtils.isBlank(uuid)) {
                            continue;
                        }
                        if (!attributeList.contains(uuid)) {
                            throw new MatrixAttributeNotFoundException(dataVo.getMatrixUuid(), uuid);
                        }
                        String label = attributeLabelMap.get(uuid);
                        CiViewVo ciView = ciViewMap.get(label);
                        if (ciView != null) {
                            List<String> valueList = matrixFilterVo.getValueList();
                            if (CollectionUtils.isEmpty(valueList)) {
                                continue;
                            }
                            if (!conversionFilter(label, valueList, attrMap, relMap, ciView, attrFilterList, relFilterList, ciEntityVo)) {
                                needAccessApi = false;
                            }
                        }
                    }
                }
                ciEntityVo.setRelFilterList(relFilterList);
                ciEntityVo.setAttrFilterList(attrFilterList);
                ciEntityVo.setFilterCiEntityId(dataVo.getFilterCiEntityId());
                ciEntityVo.setFilterCiId(dataVo.getFilterCiId());
            }
            List<Map<String, Object>> tbodyList = new ArrayList<>();
            if (needAccessApi) {
                JSONArray tbodyArray = accessSearchCiEntity(matrixUuid, ciEntityVo);
                if (CollectionUtils.isNotEmpty(tbodyArray)) {
                    Map<String, String> attributeUuidMap = matrixAttributeList.stream().collect(Collectors.toMap(e -> e.getLabel(), e -> e.getUuid()));
                    for (int i = 0; i < tbodyArray.size(); i++) {
                        JSONObject rowData = tbodyArray.getJSONObject(i);
                        if (MapUtils.isNotEmpty(rowData)) {
                            Map<String, Object> rowDataMap = new HashMap<>();
                            for (Map.Entry<String, Object> entry : rowData.entrySet()) {
                                String uuid = attributeUuidMap.get(entry.getKey());
                                if (StringUtils.isNotBlank(uuid)) {
                                    rowDataMap.put(uuid, matrixAttributeValueHandle(null, entry.getValue()));
                                }
                                if ("const_id".equals(entry.getKey())) {
                                    rowDataMap.put("uuid", matrixAttributeValueHandle(null, entry.getValue()));
                                }
                            }
                            tbodyList.add(rowDataMap);
                        }
                    }
                }
            }
            JSONArray theadList = getTheadList(matrixUuid, matrixAttributeList, dataVo.getColumnList());
            return TableResultUtil.getResult(theadList, tbodyList, ciEntityVo);
        }
        return new JSONObject();
    }

    private boolean conversionFilter(String uuid, List<String> valueList, Map<Long, AttrVo> attrMap, Map<Long, RelVo> relMap, CiViewVo ciView, List<AttrFilterVo> attrFilterList, List<RelFilterVo> relFilterList, CiEntityVo pCiEntityVo) {
        switch (ciView.getType()) {
            case "attr":
                Long attrId = Long.valueOf(uuid.substring(5));
                AttrVo attrVo = attrMap.get(attrId);
                if (attrVo == null) {
                    return true;
                }
                if ("select".equals(attrVo.getType())) {
                    CiVo targetCiVo = ciMapper.getCiById(attrVo.getTargetCiId());
                    if (targetCiVo == null) {
                        return false;
                    }
                    List<String> newValueList = new ArrayList<>();
                    for (String value : valueList) {
                        if (Objects.equals(targetCiVo.getIsVirtual(), 1)) {
                            CiEntityVo ciEntityVo = new CiEntityVo();
                            ciEntityVo.setCiId(targetCiVo.getId());
                            ciEntityVo.setName(value);
                            List<CiEntityVo> ciEntityList = ciEntityMapper.getVirtualCiEntityBaseInfoByName(ciEntityVo);
                            if (CollectionUtils.isEmpty(ciEntityList)) {
                                return false;
                            }
                            for (CiEntityVo ciEntity : ciEntityList) {
                                newValueList.add(ciEntity.getId().toString());
                            }
                        } else {
                            Long ciEntityId = ciEntityMapper.getIdByCiIdAndName(targetCiVo.getId(), value);
                            if (ciEntityId == null) {
                                return false;
                            }
                            newValueList.add(ciEntityId.toString());
                        }
                    }
                    valueList = newValueList;
                }
                AttrFilterVo attrFilterVo = new AttrFilterVo();
                attrFilterVo.setAttrId(attrVo.getId());
                attrFilterVo.setExpression(SearchExpression.LI.getExpression());
                attrFilterVo.setValueList(valueList);
                attrFilterList.add(attrFilterVo);
                break;
            case "relfrom":
                Long relId = Long.valueOf(uuid.substring(8));
                RelVo relVo = relMap.get(relId);
                if (relVo == null) {
                    return true;
                }
                CiVo toCiVo = ciMapper.getCiById(relVo.getToCiId());
                if (toCiVo == null) {
                    return false;
                }
                List<Long> toCiEntityIdList = new ArrayList<>();
                for (String value : valueList) {
                    if (Objects.equals(toCiVo.getIsVirtual(), 1)) {
                        CiEntityVo ciEntityVo = new CiEntityVo();
                        ciEntityVo.setCiId(toCiVo.getId());
                        ciEntityVo.setName(value);
                        List<CiEntityVo> ciEntityList = ciEntityMapper.getVirtualCiEntityBaseInfoByName(ciEntityVo);
                        if (CollectionUtils.isEmpty(ciEntityList)) {
                            return false;
                        }
                        for (CiEntityVo ciEntity : ciEntityList) {
                            toCiEntityIdList.add(ciEntity.getId());
                        }
                    } else {
                        RelEntityVo relEntityVo = new RelEntityVo();
                        relEntityVo.setRelId(relVo.getId());
                        relEntityVo.setPageSize(100);
                        List<RelEntityVo> relEntityList = relEntityMapper.getRelEntityByRelId(relEntityVo);
                        if (CollectionUtils.isEmpty(relEntityList)) {
                            return false;
                        }
                        for (RelEntityVo relEntity : relEntityList) {
                            if (value.equals(relEntity.getToCiEntityName())) {
                                toCiEntityIdList.add(relEntity.getToCiEntityId());
                                break;
                            }
                        }
                    }
                }
                if (CollectionUtils.isEmpty(toCiEntityIdList)) {
                    return false;
                }
                RelFilterVo toRelFilterVo = new RelFilterVo();
                toRelFilterVo.setRelId(relVo.getId());
                toRelFilterVo.setExpression(SearchExpression.LI.getExpression());
                toRelFilterVo.setValueList(toCiEntityIdList);
                toRelFilterVo.setDirection("from");
                relFilterList.add(toRelFilterVo);
                break;
            case "relto":
                relId = Long.valueOf(uuid.substring(6));
                relVo = relMap.get(relId);
                if (relVo == null) {
                    return true;
                }
                CiVo fromCiVo = ciMapper.getCiById(relVo.getFromCiId());
                if (fromCiVo == null) {
                    return false;
                }
                List<Long> fromCiEntityIdList = new ArrayList<>();
                for (String value : valueList) {
                    if (Objects.equals(fromCiVo.getIsVirtual(), 1)) {
                        CiEntityVo ciEntityVo = new CiEntityVo();
                        ciEntityVo.setCiId(fromCiVo.getId());
                        ciEntityVo.setName(value);
                        List<CiEntityVo> ciEntityList = ciEntityMapper.getVirtualCiEntityBaseInfoByName(ciEntityVo);
                        if (CollectionUtils.isEmpty(ciEntityList)) {
                            return false;
                        }
                        for (CiEntityVo ciEntity : ciEntityList) {
                            fromCiEntityIdList.add(ciEntity.getId());
                        }
                    } else {
                        RelEntityVo relEntityVo = new RelEntityVo();
                        relEntityVo.setRelId(relVo.getId());
                        relEntityVo.setPageSize(100);
                        List<RelEntityVo> relEntityList = relEntityMapper.getRelEntityByRelId(relEntityVo);
                        if (CollectionUtils.isEmpty(relEntityList)) {
                            return false;
                        }
                        for (RelEntityVo relEntity : relEntityList) {
                            if (value.equals(relEntity.getFromCiEntityName())) {
                                fromCiEntityIdList.add(relEntity.getFromCiEntityId());
                                break;
                            }
                        }
                    }
                }
                if (CollectionUtils.isEmpty(fromCiEntityIdList)) {
                    return false;
                }
                RelFilterVo fromRelFilterVo = new RelFilterVo();
                fromRelFilterVo.setRelId(relVo.getId());
                fromRelFilterVo.setExpression(SearchExpression.LI.getExpression());
                fromRelFilterVo.setValueList(fromCiEntityIdList);
                fromRelFilterVo.setDirection("to");
                relFilterList.add(fromRelFilterVo);
                break;
            case "const":
                //固化属性需要特殊处理
                if ("id".equals(uuid)) {
                    pCiEntityVo.setFilterCiEntityId(Long.valueOf(valueList.get(0)));
                } else if ("ciLabel".equals(uuid)) {
                    String ciLabel = valueList.get(0);
                    CiVo ciVo = ciMapper.getCiByLabel(ciLabel);
                    if (ciVo == null) {
                        return false;
                    }
                    pCiEntityVo.setFilterCiId(ciVo.getId());
                }
                break;
        }
        return true;
    }

    @Override
    protected List<Map<String, JSONObject>> mySearchTableDataNew(MatrixDataVo dataVo) {
        String matrixUuid = dataVo.getMatrixUuid();
        MatrixCiVo matrixCiVo = matrixMapper.getMatrixCiByMatrixUuid(matrixUuid);
        if (matrixCiVo == null) {
            throw new MatrixCiNotFoundException(matrixUuid);
        }
        List<Map<String, JSONObject>> resultList = new ArrayList<>();
        MatrixVo matrixVo = matrixMapper.getMatrixByUuid(matrixUuid);
        List<MatrixAttributeVo> matrixAttributeList = myGetAttributeList(matrixVo);
        if (CollectionUtils.isEmpty(matrixAttributeList)) {
            return resultList;
        }
        CiEntityVo ciEntityVo = new CiEntityVo();
        Long ciId = matrixCiVo.getCiId();
        ciEntityVo.setCiId(ciId);
        Map<Long, AttrVo> attrMap = new HashMap<>();
        Map<Long, RelVo> relMap = new HashMap<>();
        Map<Long, GlobalAttrVo> globalAttrMap = new HashMap<>();
        Map<String, CiViewVo> ciViewMap = new HashMap<>();
        ciEntityService.getCiViewMapAndAttrMapAndRelMap(ciId, attrMap, relMap, ciViewMap, globalAttrMap);
        JSONArray tbodyArray = new JSONArray();
        JSONArray defaultValue = dataVo.getDefaultValue();
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            ciEntityVo.setIdList(defaultValue.toJavaList(Long.class));
            tbodyArray = accessSearchCiEntityList(matrixUuid, ciEntityVo, dataVo.getColumnList());
        } else if (CollectionUtils.isNotEmpty(dataVo.getDefaultValueFilterList())) {
            List<MatrixFilterVo> initFilterList = dataVo.getFilterList();
            for (MatrixDefaultValueFilterVo defaultValueFilterVo : dataVo.getDefaultValueFilterList()) {
                List<Long> idList = new ArrayList<>();
                List<Long> filterCiIdList = new ArrayList<>();
                List<AttrFilterVo> attrFilters = new ArrayList<>();
                List<RelFilterVo> relFilters = new ArrayList<>();
                List<GlobalAttrFilterVo> globalAttrFilters = new ArrayList<>();
                List<MatrixFilterVo> filterList = new ArrayList<>(initFilterList);
                MatrixKeywordFilterVo valueFieldFilter = defaultValueFilterVo.getValueFieldFilter();
                if (valueFieldFilter != null) {
                    filterList.add(new MatrixFilterVo(valueFieldFilter.getUuid(), valueFieldFilter.getExpression(), List.of(valueFieldFilter.getValue())));
                }
                MatrixKeywordFilterVo textFieldFilter = defaultValueFilterVo.getTextFieldFilter();
                if (textFieldFilter != null && (valueFieldFilter == null || !Objects.equals(valueFieldFilter.getUuid(), textFieldFilter.getUuid()))) {
                    filterList.add(new MatrixFilterVo(textFieldFilter.getUuid(), textFieldFilter.getExpression(), List.of(textFieldFilter.getValue())));
                }
                boolean flag = handleFilterList(
                        dataVo.getMatrixUuid(),
                        filterList,
                        matrixAttributeList,
                        attrMap,
                        relMap,
                        globalAttrMap,
                        ciViewMap,
                        idList,
                        filterCiIdList,
                        attrFilters,
                        relFilters,
                        globalAttrFilters
                );
                if (flag) {
                    ciEntityVo.setIdList(idList);
                    ciEntityVo.setFilterCiIdList(filterCiIdList);
                    ciEntityVo.setAttrFilterList(attrFilters);
                    ciEntityVo.setRelFilterList(relFilters);
                    ciEntityVo.setGlobalAttrFilterList(globalAttrFilters);
                    tbodyArray.addAll(accessSearchCiEntityList(matrixUuid, ciEntityVo, dataVo.getColumnList()));
                }
            }
        } else {
            List<Long> idList = new ArrayList<>();
            List<Long> filterCiIdList = new ArrayList<>();
            List<AttrFilterVo> attrFilters = new ArrayList<>();
            List<RelFilterVo> relFilters = new ArrayList<>();
            List<GlobalAttrFilterVo> globalAttrFilters = new ArrayList<>();
            List<MatrixFilterVo> filterList = dataVo.getFilterList();
            String keywordColumn = dataVo.getKeywordColumn();
            if (StringUtils.isNotBlank(keywordColumn) && StringUtils.isNotBlank(dataVo.getKeyword())) {
                MatrixFilterVo matrixFilterVo = new MatrixFilterVo(keywordColumn, SearchExpression.LI.getExpression(), List.of(dataVo.getKeyword()));
                filterList.add(matrixFilterVo);
            }
            boolean flag = handleFilterList(
                    dataVo.getMatrixUuid(),
                    filterList,
                    matrixAttributeList,
                    attrMap,
                    relMap,
                    globalAttrMap,
                    ciViewMap,
                    idList,
                    filterCiIdList,
                    attrFilters,
                    relFilters,
                    globalAttrFilters
            );
            if (flag) {
                ciEntityVo.setIdList(idList);
                ciEntityVo.setFilterCiIdList(filterCiIdList);
                ciEntityVo.setAttrFilterList(attrFilters);
                ciEntityVo.setRelFilterList(relFilters);
                ciEntityVo.setGlobalAttrFilterList(globalAttrFilters);
                //下面逻辑适用于下拉框滚动加载，也可以搜索，但是一页返回的数据量可能会小于pageSize，因为做了去重处理
                ciEntityVo.setCurrentPage(dataVo.getCurrentPage());
                ciEntityVo.setPageSize(dataVo.getPageSize());
                tbodyArray = accessSearchCiEntityList(matrixUuid, ciEntityVo, dataVo.getColumnList());
                dataVo.setRowNum(ciEntityVo.getRowNum());
            }
        }
        List<Map<String, JSONObject>> tbodyList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(tbodyArray)) {
            Map<String, String> attributeUuidMap = matrixAttributeList.stream().collect(Collectors.toMap(MatrixAttributeVo::getLabel, MatrixAttributeVo::getUuid));
            for (int i = 0; i < tbodyArray.size(); i++) {
                JSONObject rowData = tbodyArray.getJSONObject(i);
                if (MapUtils.isNotEmpty(rowData)) {
                    Map<String, JSONObject> rowDataMap = new HashMap<>();
                    for (Map.Entry<String, Object> entry : rowData.entrySet()) {
                        String uuid = attributeUuidMap.get(entry.getKey());
                        if (StringUtils.isNotBlank(uuid)) {
                            rowDataMap.put(uuid, matrixAttributeValueHandle(null, entry.getValue()));
                        }
                        if ("const_id".equals(entry.getKey())) {
                            rowDataMap.put("uuid", matrixAttributeValueHandle(null, entry.getValue()));
                        }
                    }
                    tbodyList.add(rowDataMap);
                }
            }
        }
        resultList.addAll(tbodyList);
        return resultList;
    }

    private boolean handleFilterList(
            String matrixUuid,
            List<MatrixFilterVo> filterList,
            List<MatrixAttributeVo> matrixAttributeList,
            Map<Long, AttrVo> attrMap,
            Map<Long, RelVo> relMap,
            Map<Long, GlobalAttrVo> globalAttrMap,
            Map<String, CiViewVo> ciViewMap,
            List<Long> idList,
            List<Long> filterCiIdList,
            List<AttrFilterVo> attrFilters,
            List<RelFilterVo> relFilters,
            List<GlobalAttrFilterVo> globalAttrFilters) {

        boolean flag = true;
        if (CollectionUtils.isNotEmpty(filterList)) {
            Map<String, String> attributeLabelMap = matrixAttributeList.stream().collect(Collectors.toMap(MatrixAttributeVo::getUuid, MatrixAttributeVo::getLabel));
            List<String> attributeList = matrixAttributeList.stream().map(MatrixAttributeVo::getUuid).toList();
            for (MatrixFilterVo matrixFilterVo : filterList) {
                if (matrixFilterVo == null) {
                    continue;
                }
                String uuid = matrixFilterVo.getUuid();
                if (StringUtils.isBlank(uuid)) {
                    continue;
                }
                if (!attributeList.contains(uuid)) {
                    throw new MatrixAttributeNotFoundException(matrixUuid, uuid);
                }
                String label = attributeLabelMap.get(uuid);
                CiViewVo ciView = ciViewMap.get(label);
                if (ciView == null) {
                    continue;
                }
                List<String> valueList = matrixFilterVo.getValueList();
                if (CollectionUtils.isEmpty(valueList)) {
                    if (!Objects.equals(matrixFilterVo.getExpression(), SearchExpression.NULL.getExpression())
                            && !Objects.equals(matrixFilterVo.getExpression(), SearchExpression.NOTNULL.getExpression())) {
                        continue;
                    }
                }
                switch (ciView.getType()) {
                    case "attr":
                        Long attrId = Long.valueOf(label.substring("attr_".length()));
                        AttrVo attrVo = attrMap.get(attrId);
                        if (attrVo != null) {
                            AttrFilterVo attrFilterVo = matrixCiEntityService.convertAttrFilter(attrVo, matrixFilterVo.getExpression(), valueList);
                            if (attrFilterVo != null) {
                                attrFilters.add(attrFilterVo);
                            } else {
                                flag = false;
                            }
                        }
                        break;
                    case "relfrom":
                        Long relFromId = Long.valueOf(label.substring("relfrom_".length()));
                        RelVo relFromVo = relMap.get(relFromId);
                        if (relFromVo != null) {
                            RelFilterVo relFilterVo = matrixCiEntityService.convertFromRelFilter(relFromVo, matrixFilterVo.getExpression(), valueList, "from");
                            if (relFilterVo != null) {
                                relFilters.add(relFilterVo);
                            } else {
                                flag = false;
                            }
                        }
                        break;
                    case "relto":
                        Long relToId = Long.valueOf(label.substring("relto_".length()));
                        RelVo relToVo = relMap.get(relToId);
                        if (relToVo != null) {
                            RelFilterVo relFilterVo = matrixCiEntityService.convertFromRelFilter(relToVo, matrixFilterVo.getExpression(), valueList, "to");
                            if (relFilterVo != null) {
                                relFilters.add(relFilterVo);
                            } else {
                                flag = false;
                            }
                        }
                        break;
                    case "const":
                        //固化属性需要特殊处理
                        if ("const_id".equals(label)) {
                            for (String value : new HashSet<>(valueList)) {
                                idList.add(Long.valueOf(value));
                            }
                        } else if ("const_ciLabel".equals(label)) {
                            List<CiVo> ciList = ciMapper.getCiListByLabelList(valueList);
                            for (CiVo ci : ciList) {
                                if (!filterCiIdList.contains(ci.getId())) {
                                    filterCiIdList.add(ci.getId());
                                }
                            }
                        }
                        break;
                    case "global":
                        Long globalId = Long.valueOf(label.substring("global_".length()));
                        GlobalAttrVo globalAttrVo = globalAttrMap.get(globalId);
                        if (globalAttrVo != null) {
                            GlobalAttrFilterVo globalAttrFilterVo = matrixCiEntityService.convertGlobalAttrFilter(globalAttrVo, matrixFilterVo.getExpression(), valueList);
                            if (globalAttrFilterVo != null) {
                                globalAttrFilters.add(globalAttrFilterVo);
                            } else {
                                flag = false;
                            }
                        }
                        break;
                }
            }
        }
        return flag;
    }

    @Override
    protected JSONObject mySaveTableRowData(String matrixUuid, JSONObject rowData) {
        return null;
    }

    @Override
    protected Map<String, String> myGetTableRowData(MatrixDataVo matrixDataVo) {
        return null;
    }

    @Override
    protected void myDeleteTableRowData(String matrixUuid, List<String> uuidList) {

    }

    /**
     * 从matrixCiVo中提取showAttributeUuidList为CiEntityVo的attrIdList与relIdList赋值
     *
     * @param matrixCiVo
     * @param ciEntityVo
     */
    private void setAttrIdListAndRelIdListFromMatrixConfig(MatrixCiVo matrixCiVo, CiEntityVo ciEntityVo) {
        List<Long> attrIdList = new ArrayList<>();
        List<Long> relIdList = new ArrayList<>();
        if (matrixCiVo == null) {
            throw new MatrixCiNotFoundException(matrixCiVo.getMatrixUuid());
        }
        JSONObject config = matrixCiVo.getConfig();
        JSONArray showAttributeList = config.getJSONArray("showAttributeList");
        if (CollectionUtils.isNotEmpty(showAttributeList)) {
            for (int i = 0; i < showAttributeList.size(); i++) {
                JSONObject showAttributeObj = showAttributeList.getJSONObject(i);
                if (MapUtils.isNotEmpty(showAttributeObj)) {
                    String label = showAttributeObj.getString("label");
                    if (StringUtils.isNotBlank(label)) {
                        if (label.startsWith("attr_")) {
                            attrIdList.add(Long.valueOf(label.substring(5)));
                        } else if (label.startsWith("relfrom_")) {
                            relIdList.add(Long.valueOf(label.substring(8)));
                        } else if (label.startsWith("relto_")) {
                            relIdList.add(Long.valueOf(label.substring(6)));
                        }
                    }
                }
            }
            ciEntityVo.setAttrIdList(attrIdList);
            ciEntityVo.setRelIdList(relIdList);
        }
    }

    private JSONArray accessSearchCiEntity(String matrixUuid, CiEntityVo ciEntityVo) {
        try {
            MatrixCiVo matrixCiVo = matrixMapper.getMatrixCiByMatrixUuid(matrixUuid);
            setAttrIdListAndRelIdListFromMatrixConfig(matrixCiVo, ciEntityVo);
            ciEntityVo.setGlobalAttrStrictMode(true);
            List<CiEntityVo> ciEntityList = ciEntityService.searchCiEntity(ciEntityVo);
            if (CollectionUtils.isNotEmpty(ciEntityList)) {
                List<String> viewConstNameList = new ArrayList<>();
                List<ViewConstVo> ciViewConstList = ciViewMapper.getAllCiViewConstList();
                for (ViewConstVo viewConstVo : ciViewConstList) {
                    viewConstNameList.add(viewConstVo.getName());
                }
                JSONArray tbodyList = new JSONArray();
                for (CiEntityVo ciEntity : ciEntityList) {
                    JSONObject tbody = ciEntityService.getTbodyRowData(viewConstNameList, ciEntity);
                    tbodyList.add(tbody);
                }
                return tbodyList;
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return new JSONArray();
    }

    /**
     * 从matrixCiVo中提取showAttributeUuidList为CiEntitySearchVo的attrIdList、relIdList、globalAttrIdList、showConstList赋值
     *
     * @param matrixCiVo
     * @param matrixCiEntitySearchVo
     * @param columnList
     */
    private void setAttrIdListAndRelIdListFromMatrixConfig(MatrixCiVo matrixCiVo, MatrixCiEntitySearchVo matrixCiEntitySearchVo, List<String> columnList) {
        JSONObject config = matrixCiVo.getConfig();
        JSONArray showAttributeList = config.getJSONArray("showAttributeList");
        if (CollectionUtils.isNotEmpty(showAttributeList)) {
            List<Long> attrIdList = new ArrayList<>();
            List<Long> relIdList = new ArrayList<>();
            List<Long> globalAttrIdList = new ArrayList<>();
            List<String> showConstList = new ArrayList<>();
            for (int i = 0; i < showAttributeList.size(); i++) {
                JSONObject showAttributeObj = showAttributeList.getJSONObject(i);
                if (MapUtils.isNotEmpty(showAttributeObj)) {
                    String uuid = showAttributeObj.getString("uuid");
                    String label = showAttributeObj.getString("label");
                    if (columnList.contains(uuid) && StringUtils.isNotBlank(label)) {
                        if (label.startsWith("const_")) {
                            showConstList.add(label);
                        } else if (label.startsWith("attr_")) {
                            attrIdList.add(Long.valueOf(label.substring("attr_".length())));
                        } else if (label.startsWith("relfrom_")) {
                            relIdList.add(Long.valueOf(label.substring("relfrom_".length())));
                        } else if (label.startsWith("relto_")) {
                            relIdList.add(Long.valueOf(label.substring("relto_".length())));
                        } else if (label.startsWith("global_")) {
                            globalAttrIdList.add(Long.valueOf(label.substring("global_".length())));
                        }
                    }
                }
            }
            matrixCiEntitySearchVo.setAttrIdList(attrIdList);
            matrixCiEntitySearchVo.setRelIdList(relIdList);
            matrixCiEntitySearchVo.setGlobalAttrIdList(globalAttrIdList);
            matrixCiEntitySearchVo.setShowConstList(showConstList);
        }
    }

    private JSONArray accessSearchCiEntityList(String matrixUuid, CiEntityVo ciEntityVo, List<String> columnList) {
        JSONArray resultList = new JSONArray();
        try {
            MatrixCiVo matrixCiVo = matrixMapper.getMatrixCiByMatrixUuid(matrixUuid);
            if (matrixCiVo == null) {
                throw new MatrixCiNotFoundException(matrixUuid);
            }
            MatrixCiEntitySearchVo matrixCiEntitySearchVo = new MatrixCiEntitySearchVo();
            setAttrIdListAndRelIdListFromMatrixConfig(matrixCiVo, matrixCiEntitySearchVo, columnList);
            matrixCiEntitySearchVo.setCiId(ciEntityVo.getCiId());
            matrixCiEntitySearchVo.setAttrFilterList(ciEntityVo.getAttrFilterList());
            matrixCiEntitySearchVo.setGlobalAttrFilterList(ciEntityVo.getGlobalAttrFilterList());
            matrixCiEntitySearchVo.setRelFilterList(ciEntityVo.getRelFilterList());
            matrixCiEntitySearchVo.setFilterCiIdList(ciEntityVo.getFilterCiIdList());
            matrixCiEntitySearchVo.setCurrentPage(ciEntityVo.getCurrentPage());
            matrixCiEntitySearchVo.setPageSize(ciEntityVo.getPageSize());
            matrixCiEntitySearchVo.setIdList(ciEntityVo.getIdList());
            matrixCiEntitySearchVo.setDistinct(true);
            List<Map<String, Object>> ciEntityList = matrixCiEntityService.searchCiEntityList(matrixCiEntitySearchVo);
            resultList.addAll(ciEntityList);
            ciEntityVo.setRowNum(matrixCiEntitySearchVo.getRowNum());
            return resultList;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return resultList;
    }

    private List<Map<String, JSONObject>> getCmdbCiDataTbodyList(JSONArray tbodyArray, List<String> columnList, String matrixUuid) {
        List<Map<String, JSONObject>> resultList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(tbodyArray)) {
            MatrixVo matrixVo = matrixMapper.getMatrixByUuid(matrixUuid);
            List<MatrixAttributeVo> attributeVoList = myGetAttributeList(matrixVo);
            Map<String, String> attributeLabelMap = attributeVoList.stream().collect(Collectors.toMap(e -> e.getUuid(), e -> e.getLabel()));
            for (int i = 0; i < tbodyArray.size(); i++) {
                JSONObject rowData = tbodyArray.getJSONObject(i);
                if (MapUtils.isNotEmpty(rowData)) {
                    Map<String, JSONObject> resultMap = new HashMap<>(columnList.size());
                    for (String column : columnList) {
                        String label = attributeLabelMap.get(column);
                        String columnValue = rowData.getString(label);
                        resultMap.put(column, matrixAttributeValueHandle(null, columnValue));
                    }
                    resultList.add(resultMap);
                }
            }
        }
        return resultList;
    }

    public JSONObject matrixAttributeValueHandle(MatrixAttributeVo matrixAttribute, Object valueObj) {
        JSONObject resultObj = new JSONObject();
        String type = MatrixAttributeType.INPUT.getValue();
        if (matrixAttribute != null) {
            type = matrixAttribute.getType();
        }
        resultObj.put("type", type);
        if (valueObj == null) {
            resultObj.put("value", null);
            resultObj.put("text", null);
            return resultObj;
        }
        String value = valueObj.toString();
        resultObj.put("value", value);
        resultObj.put("text", value);
        if (MatrixAttributeType.SELECT.getValue().equals(type)) {
            if (matrixAttribute != null) {
                JSONObject config = matrixAttribute.getConfig();
                if (MapUtils.isNotEmpty(config)) {
                    JSONArray dataList = config.getJSONArray("dataList");
                    if (CollectionUtils.isNotEmpty(dataList)) {
                        for (int i = 0; i < dataList.size(); i++) {
                            JSONObject data = dataList.getJSONObject(i);
                            if (Objects.equals(value, data.getString("value"))) {
                                resultObj.put("text", data.getString("text"));
                            }
                        }
                    }
                }
            }
        }
        return resultObj;
    }
}
