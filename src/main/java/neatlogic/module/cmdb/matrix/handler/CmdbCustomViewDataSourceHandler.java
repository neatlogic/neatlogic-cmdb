/*
 * Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package neatlogic.module.cmdb.matrix.handler;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.cmdb.dto.customview.*;
import neatlogic.framework.cmdb.exception.customview.CustomViewNotFoundException;
import neatlogic.framework.common.constvalue.Expression;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.matrix.constvalue.MatrixAttributeType;
import neatlogic.framework.matrix.constvalue.SearchExpression;
import neatlogic.framework.matrix.core.MatrixDataSourceHandlerBase;
import neatlogic.framework.matrix.dto.*;
import neatlogic.framework.matrix.exception.MatrixAttributeNotFoundException;
import neatlogic.framework.matrix.exception.MatrixCmdbCustomViewNotFoundException;
import neatlogic.framework.util.UuidUtil;
import neatlogic.module.cmdb.dao.mapper.customview.CustomViewMapper;
import neatlogic.module.cmdb.matrix.constvalue.MatrixType;
import neatlogic.module.cmdb.service.customview.CustomViewDataService;
import neatlogic.module.cmdb.workerdispatcher.exception.CmdbDispatcherDispatchFailedException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class CmdbCustomViewDataSourceHandler extends MatrixDataSourceHandlerBase {

    private final static Logger logger = LoggerFactory.getLogger(CmdbCustomViewDataSourceHandler.class);

    @Resource
    private CustomViewMapper customViewMapper;

    @Resource
    private CustomViewDataService customViewDataService;

    @Override
    public String getHandler() {
        return MatrixType.CMDBCUSTOMVIEW.getValue();
    }

    @Override
    protected boolean mySaveMatrix(MatrixVo matrixVo) throws Exception {
        Long customViewId = matrixVo.getCustomViewId();
        if (customViewId == null) {
            throw new ParamNotExistsException("customViewId");
        }
        CustomViewVo customView = customViewMapper.getCustomViewById(customViewId);
        if (customView == null) {
            throw new CmdbDispatcherDispatchFailedException(customViewId);
        }
        JSONObject config = matrixVo.getConfig();
        if (MapUtils.isEmpty(config)) {
            throw new ParamNotExistsException("config");
        }
        JSONArray showAttributeLabelArray = config.getJSONArray("showAttributeLabelList");
        if (CollectionUtils.isEmpty(showAttributeLabelArray)) {
            throw new ParamNotExistsException("config.showAttributeLabelList");
        }
        Map<String, String> oldShowAttributeUuidMap = new HashMap<>();
        MatrixCmdbCustomViewVo oldMatrixCmdbCustomViewVo = matrixMapper.getMatrixCmdbCustomViewByMatrixUuid(matrixVo.getUuid());
        if (oldMatrixCmdbCustomViewVo != null) {
            if (customViewId.equals(oldMatrixCmdbCustomViewVo.getCustomViewId())) {
                JSONObject oldConfig = oldMatrixCmdbCustomViewVo.getConfig();
                if (MapUtils.isNotEmpty(oldConfig)) {
                    JSONArray oldShowAttributeUuidArray = oldConfig.getJSONArray("showAttributeLabelList");
                    if (CollectionUtils.isNotEmpty(oldShowAttributeUuidArray)) {
                        if (CollectionUtils.isEqualCollection(oldShowAttributeUuidArray, showAttributeLabelArray)) {
                            return false;
                        }
                        JSONArray showAttributeArray = oldConfig.getJSONArray("showAttributeList");
                        if (CollectionUtils.isNotEmpty(showAttributeArray)) {
                            for (int i = 0; i < showAttributeArray.size(); i++) {
                                JSONObject showAttributeObj = showAttributeArray.getJSONObject(i);
                                if (MapUtils.isNotEmpty(showAttributeObj)) {
                                    String uuid = showAttributeObj.getString("uuid");
                                    if (uuid != null) {
                                        oldShowAttributeUuidMap.put(showAttributeObj.getString("label"), uuid);
//                                        DependencyManager.delete(CiAttr2MatrixAttrDependencyHandler.class, uuid);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        JSONArray showAttributeArray = new JSONArray();
        Iterator<Object> iterator = showAttributeLabelArray.iterator();
        while (iterator.hasNext()) {
            String showAttributeLabel = (String) iterator.next();
            JSONObject showAttributeObj = new JSONObject();
            String showAttributeUuid = oldShowAttributeUuidMap.get(showAttributeLabel);
            if (showAttributeUuid == null) {
                showAttributeUuid = UuidUtil.randomUuid();
            }
            showAttributeObj.put("uuid", showAttributeUuid);
            showAttributeObj.put("label", showAttributeLabel);
            showAttributeArray.add(showAttributeObj);
        }
        config.put("showAttributeList", showAttributeArray);
        MatrixCmdbCustomViewVo matrixCmdbCustomViewVo = new MatrixCmdbCustomViewVo(matrixVo.getUuid(), customViewId, config);
        matrixMapper.replaceMatrixCmdbCustomView(matrixCmdbCustomViewVo);
        return true;
    }

    @Override
    protected void myGetMatrix(MatrixVo matrixVo) {
        MatrixCmdbCustomViewVo matrixCmdbCustomViewVo = matrixMapper.getMatrixCmdbCustomViewByMatrixUuid(matrixVo.getUuid());
        if (matrixCmdbCustomViewVo == null) {
            throw new MatrixCmdbCustomViewNotFoundException(matrixVo.getUuid());
        }
        matrixVo.setCustomViewId(matrixCmdbCustomViewVo.getCustomViewId());
        JSONObject config = matrixCmdbCustomViewVo.getConfig();
        CustomViewVo customView = customViewMapper.getCustomViewById(matrixCmdbCustomViewVo.getCustomViewId());
        if (customView != null) {
            config.put("customViewName", customView.getName());
        }
        matrixVo.setConfig(config);
    }

    @Override
    protected void myDeleteMatrix(String uuid) {
        MatrixCmdbCustomViewVo matrixCmdbCustomViewVo = matrixMapper.getMatrixCmdbCustomViewByMatrixUuid(uuid);
        if (matrixCmdbCustomViewVo != null) {
            matrixMapper.deleteMatrixCmdbCustomViewByMatrixUuid(uuid);
//            JSONObject config = matrixCmdbCustomViewVo.getConfig();
//            if (MapUtils.isNotEmpty(config)) {
//                JSONArray showAttributeArray = config.getJSONArray("showAttributeList");
//                if (CollectionUtils.isNotEmpty(showAttributeArray)) {
//                    for (int i = 0; i < showAttributeArray.size(); i++) {
//                        JSONObject showAttributeObj = showAttributeArray.getJSONObject(i);
//                        if (MapUtils.isNotEmpty(showAttributeObj)) {
//                            Long id = showAttributeObj.getLong("id");
//                            if (id != null) {
//                                DependencyManager.delete(CiAttr2MatrixAttrDependencyHandler.class, id);
//                            }
//                        }
//                    }
//                }
//            }
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
    protected MatrixVo myExportMatrix(MatrixVo matrixVo) {
        myGetMatrix(matrixVo);
        return matrixVo;
    }

    @Override
    protected void myImportMatrix(MatrixVo matrixVo) {
        matrixMapper.deleteMatrixCmdbCustomViewByMatrixUuid(matrixVo.getUuid());
        MatrixCmdbCustomViewVo matrixCmdbCustomViewVo = new MatrixCmdbCustomViewVo(matrixVo.getUuid(), matrixVo.getCustomViewId(), matrixVo.getConfig());
        matrixMapper.replaceMatrixCmdbCustomView(matrixCmdbCustomViewVo);
    }

    @Override
    protected void mySaveAttributeList(String matrixUuid, List<MatrixAttributeVo> matrixAttributeList) {

    }

    @Override
    protected List<MatrixAttributeVo> myGetAttributeList(MatrixVo matrixVo) {
        Long customViewId = null;
        Map<String, String> showAttributeUuidMap = new HashMap<>();
        String matrixUuid = matrixVo.getUuid();
        if (StringUtils.isNotBlank(matrixUuid)) {
            MatrixCmdbCustomViewVo matrixCmdbCustomViewVo = matrixMapper.getMatrixCmdbCustomViewByMatrixUuid(matrixUuid);
            if (matrixCmdbCustomViewVo == null) {
                throw new MatrixCmdbCustomViewNotFoundException(matrixUuid);
            }
            customViewId = matrixCmdbCustomViewVo.getCustomViewId();
            JSONObject config = matrixCmdbCustomViewVo.getConfig();
            JSONArray showAttributeArray = config.getJSONArray("showAttributeList");
            for (int i = 0; i < showAttributeArray.size(); i++) {
                JSONObject showAttributeObj = showAttributeArray.getJSONObject(i);
                showAttributeUuidMap.put(showAttributeObj.getString("label"), showAttributeObj.getString("uuid"));
            }
        } else {
            customViewId = matrixVo.getCustomViewId();
        }
        CustomViewVo customView = customViewMapper.getCustomViewById(customViewId);
        if (customView == null) {
            throw new CustomViewNotFoundException(customViewId);
        }
        int sort = 0;
        List<MatrixAttributeVo> matrixAttributeList = new ArrayList<>();
        CustomViewConstAttrVo customViewConstAttrVo = new CustomViewConstAttrVo(customViewId);
        CustomViewAttrVo customViewAttrVo = new CustomViewAttrVo(customViewId);
        List<CustomViewConstAttrVo> constAttrList = customViewMapper.getCustomViewConstAttrByCustomViewId(customViewConstAttrVo);
        for (CustomViewConstAttrVo constAttrVo : constAttrList) {
            MatrixAttributeVo matrixAttributeVo = new MatrixAttributeVo();
            if (MapUtils.isNotEmpty(showAttributeUuidMap)) {
                String uuid = showAttributeUuidMap.get(constAttrVo.getUuid());
                if (uuid == null && Objects.equals(constAttrVo.getIsPrimary(), 0)) {
                    continue;
                }
                matrixAttributeVo.setUuid(uuid);
            }
            matrixAttributeVo.setLabel(constAttrVo.getUuid());
            matrixAttributeVo.setName(constAttrVo.getAlias());
            matrixAttributeVo.setMatrixUuid(matrixUuid);
            matrixAttributeVo.setType(MatrixAttributeType.INPUT.getValue());
            matrixAttributeVo.setIsDeletable(0);
            matrixAttributeVo.setSort(sort++);
            matrixAttributeVo.setIsRequired(0);
            matrixAttributeVo.setPrimaryKey(constAttrVo.getIsPrimary());
            matrixAttributeList.add(matrixAttributeVo);
        }
        List<CustomViewAttrVo> attrList = customViewMapper.getCustomViewAttrByCustomViewId(customViewAttrVo);
        for (CustomViewAttrVo attrVo : attrList) {
            MatrixAttributeVo matrixAttributeVo = new MatrixAttributeVo();
            if (MapUtils.isNotEmpty(showAttributeUuidMap)) {
                String uuid = showAttributeUuidMap.get(attrVo.getUuid());
                if (uuid == null && Objects.equals(attrVo.getIsPrimary(), 0)) {
                    continue;
                }
                matrixAttributeVo.setUuid(uuid);
            }
            matrixAttributeVo.setLabel(attrVo.getUuid());
            matrixAttributeVo.setName(attrVo.getAlias());
            matrixAttributeVo.setMatrixUuid(matrixUuid);
            matrixAttributeVo.setType(MatrixAttributeType.INPUT.getValue());
            matrixAttributeVo.setIsDeletable(0);
            matrixAttributeVo.setSort(sort++);
            matrixAttributeVo.setIsRequired(0);
            matrixAttributeVo.setPrimaryKey(attrVo.getIsPrimary());
            matrixAttributeList.add(matrixAttributeVo);
        }
        return matrixAttributeList;
    }

    @Override
    protected JSONObject myExportAttribute(MatrixVo matrixVo) {
        return null;
    }

    @Override
    protected JSONObject myTableDataSearch(MatrixDataVo dataVo) {
        return null;
    }

    @Override
    protected List<Map<String, JSONObject>> mySearchTableDataNew(MatrixDataVo dataVo) {
        String matrixUuid = dataVo.getMatrixUuid();
        MatrixCmdbCustomViewVo matrixCmdbCustomViewVo = matrixMapper.getMatrixCmdbCustomViewByMatrixUuid(matrixUuid);
        if (matrixCmdbCustomViewVo == null) {
            throw new MatrixCmdbCustomViewNotFoundException(matrixUuid);
        }
        List<Map<String, JSONObject>> resultList = new ArrayList<>();
        MatrixVo matrixVo = matrixMapper.getMatrixByUuid(matrixUuid);
        List<MatrixAttributeVo> matrixAttributeList = myGetAttributeList(matrixVo);
        if (CollectionUtils.isEmpty(matrixAttributeList)) {
            return resultList;
        }
        List<Map<String, Object>> dataList = new ArrayList<>();
        Map<String, MatrixAttributeVo> label2AttributeMap = matrixAttributeList.stream().collect(Collectors.toMap(e -> e.getLabel(), e -> e));
        Map<String, String> attributeUuidMap = matrixAttributeList.stream().collect(Collectors.toMap(e -> e.getLabel(), e -> e.getUuid()));
        if (CollectionUtils.isNotEmpty(dataVo.getDefaultValue())) {
            List<String> defaultValue = dataVo.getDefaultValue().toJavaList(String.class);
            for (String primaryKeyAttrUuidAndValueListStr : defaultValue) {
                List<String> primaryKeyAttrUuidAndValueList = new ArrayList<>();
                if (primaryKeyAttrUuidAndValueListStr.contains("(&&)")) {
                    String[] split = primaryKeyAttrUuidAndValueListStr.split("(&&)");
                    for (String e : split) {
                        primaryKeyAttrUuidAndValueList.add(e);
                    }
                } else {
                    primaryKeyAttrUuidAndValueList.add(primaryKeyAttrUuidAndValueListStr);
                }
                List<MatrixFilterVo> filterList = new ArrayList<>();
                for (String primaryKeyAttrUuidAndValue : primaryKeyAttrUuidAndValueList) {
                    String[] split = primaryKeyAttrUuidAndValue.split("#");
                    String uuid = split[0];
                    String value = split[1];
                    filterList.add(new MatrixFilterVo(uuid, SearchExpression.EQ.getExpression(), Arrays.asList(value)));
                }
                CustomViewConditionVo customViewConditionVo = new CustomViewConditionVo();
                customViewConditionVo.setCustomViewId(matrixCmdbCustomViewVo.getCustomViewId());
                List<CustomViewConditionFilterVo> attrFilterList = convertAttrFilter(matrixCmdbCustomViewVo.getCustomViewId(), matrixAttributeList, filterList, matrixUuid);
                customViewConditionVo.setAttrFilterList(attrFilterList);
                customViewConditionVo.setCurrentPage(dataVo.getCurrentPage());
                customViewConditionVo.setPageSize(dataVo.getPageSize());
                List<Map<String, Object>> mapList = customViewDataService.searchCustomViewData(customViewConditionVo);
                dataList.add(mapList.get(0));
            }
        } else if (CollectionUtils.isNotEmpty(dataVo.getDefaultValueFilterList())) {
            for (MatrixDefaultValueFilterVo defaultValueFilterVo : dataVo.getDefaultValueFilterList()) {
                List<MatrixFilterVo> filterList = new ArrayList<>();
                MatrixKeywordFilterVo valueFieldFilter = defaultValueFilterVo.getValueFieldFilter();
                if (valueFieldFilter != null) {
                    filterList.add(new MatrixFilterVo(valueFieldFilter.getUuid(), valueFieldFilter.getExpression(), Arrays.asList(valueFieldFilter.getValue())));
                }
                MatrixKeywordFilterVo textFieldFilter = defaultValueFilterVo.getTextFieldFilter();
                if (textFieldFilter != null && (valueFieldFilter == null || !Objects.equals(valueFieldFilter.getUuid(), textFieldFilter.getUuid()))) {
                    filterList.add(new MatrixFilterVo(textFieldFilter.getUuid(), textFieldFilter.getExpression(), Arrays.asList(textFieldFilter.getValue())));
                }
                CustomViewConditionVo customViewConditionVo = new CustomViewConditionVo();
                customViewConditionVo.setCustomViewId(matrixCmdbCustomViewVo.getCustomViewId());
                List<CustomViewConditionFilterVo> attrFilterList = convertAttrFilter(matrixCmdbCustomViewVo.getCustomViewId(), matrixAttributeList, filterList, matrixUuid);
                customViewConditionVo.setAttrFilterList(attrFilterList);
                customViewConditionVo.setCurrentPage(dataVo.getCurrentPage());
                customViewConditionVo.setPageSize(dataVo.getPageSize());
                List<Map<String, Object>> mapList = customViewDataService.searchCustomViewData(customViewConditionVo);
                dataList.addAll(mapList);
            }
        } else {
            CustomViewConditionVo customViewConditionVo = new CustomViewConditionVo();
            customViewConditionVo.setCustomViewId(matrixCmdbCustomViewVo.getCustomViewId());
            List<CustomViewConditionFilterVo> attrFilterList = convertAttrFilter(matrixCmdbCustomViewVo.getCustomViewId(), matrixAttributeList, dataVo.getFilterList(), matrixUuid);
            customViewConditionVo.setAttrFilterList(attrFilterList);
            customViewConditionVo.setCurrentPage(dataVo.getCurrentPage());
            customViewConditionVo.setPageSize(dataVo.getPageSize());
            dataList = customViewDataService.searchCustomViewData(customViewConditionVo);
            dataVo.setRowNum(customViewConditionVo.getRowNum());
        }

        for (Map<String, Object> map : dataList) {
            if (MapUtils.isEmpty(map)) {
                continue;
            }
            List<String> primaryKeyAttrUuidAndValueList = new ArrayList<>();
            Map<String, JSONObject> rowDataMap = new HashMap<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                MatrixAttributeVo attributeVo = label2AttributeMap.get(entry.getKey());
                String uuid = attributeUuidMap.get(entry.getKey());
                if (StringUtils.isBlank(uuid)) {
                    continue;
                }
                JSONObject resultObj = new JSONObject();
                resultObj.put("type", MatrixAttributeType.INPUT.getValue());
                resultObj.put("value", entry.getValue());
                resultObj.put("text", entry.getValue());
                rowDataMap.put(uuid, resultObj);
                if (Objects.equals(attributeVo.getPrimaryKey(), 1)) {
                    primaryKeyAttrUuidAndValueList.add(uuid + "#" + entry.getValue());
                }
            }
            if (CollectionUtils.isNotEmpty(primaryKeyAttrUuidAndValueList)) {
                String value = String.join("&&", primaryKeyAttrUuidAndValueList);
                JSONObject resultObj = new JSONObject();
                resultObj.put("type", MatrixAttributeType.INPUT.getValue());
                resultObj.put("value", value);
                resultObj.put("text", value);
                rowDataMap.put("uuid", resultObj);
            } else {
                // 没有设置主键时取第一个属性作为主键列
                MatrixAttributeVo attributeVo = matrixAttributeList.get(0);
                Object value = map.get(attributeVo.getLabel());
                if (value == null) {
                    value = "";
                }
                value = attributeVo.getUuid() + "#" + value;
                JSONObject resultObj = new JSONObject();
                resultObj.put("type", MatrixAttributeType.INPUT.getValue());
                resultObj.put("value", value);
                resultObj.put("text", value);
                rowDataMap.put("uuid", resultObj);
            }
            resultList.add(rowDataMap);
        }
        return resultList;
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

    private List<CustomViewConditionFilterVo> convertAttrFilter(Long customViewId, List<MatrixAttributeVo> matrixAttributeList, List<MatrixFilterVo> filterList, String matrixUuid) {
        List<CustomViewConditionFilterVo> customViewConditionFilterList = new ArrayList<>();
        Map<String, String> attributeLabelMap = matrixAttributeList.stream().collect(Collectors.toMap(e -> e.getUuid(), e -> e.getLabel()));
        List<String> attributeList = matrixAttributeList.stream().map(MatrixAttributeVo::getUuid).collect(Collectors.toList());
        Map<String, String> uuid2TypeMap = new HashMap<>();
        CustomViewConstAttrVo customViewConstAttrVo = new CustomViewConstAttrVo(customViewId);
        CustomViewAttrVo customViewAttrVo = new CustomViewAttrVo(customViewId);
        List<CustomViewConstAttrVo> constAttrList = customViewMapper.getCustomViewConstAttrByCustomViewId(customViewConstAttrVo);
        for (CustomViewConstAttrVo constAttrVo : constAttrList) {
            uuid2TypeMap.put(constAttrVo.getUuid(), "constattr");
        }
        List<CustomViewAttrVo> attrList = customViewMapper.getCustomViewAttrByCustomViewId(customViewAttrVo);
        for (CustomViewAttrVo attrVo : attrList) {
            uuid2TypeMap.put(attrVo.getUuid(), "attr");
        }
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
            String attrUuid = attributeLabelMap.get(uuid);
            List<String> valueList = matrixFilterVo.getValueList();
            if (CollectionUtils.isEmpty(valueList)) {
                if (!Objects.equals(matrixFilterVo.getExpression(), SearchExpression.NULL.getExpression())
                        && !Objects.equals(matrixFilterVo.getExpression(), SearchExpression.NOTNULL.getExpression())) {
                    continue;
                }
            }
            JSONArray valueArray = new JSONArray();
            for (String value : valueList) {
                if (StringUtils.isNotBlank(value)) {
                    valueArray.add(value);
                }
            }
            String expression = matrixFilterVo.getExpression();
            if (StringUtils.isBlank(expression)) {
                expression = Expression.EQUAL.getExpression();
            }
            CustomViewConditionFilterVo customViewConditionFilterVo = new CustomViewConditionFilterVo();
            customViewConditionFilterVo.setAttrUuid(attrUuid);
            customViewConditionFilterVo.setType(uuid2TypeMap.get(attrUuid));
            customViewConditionFilterVo.setExpression(expression);
            customViewConditionFilterVo.setValueList(valueArray);
            customViewConditionFilterList.add(customViewConditionFilterVo);
        }
        return customViewConditionFilterList;
    }
}
