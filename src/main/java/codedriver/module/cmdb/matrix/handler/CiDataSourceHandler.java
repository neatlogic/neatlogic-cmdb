/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.matrix.handler;

import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.dto.ci.CiViewVo;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.cientity.AttrFilterVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.enums.SearchExpression;
import codedriver.framework.cmdb.enums.ShowType;
import codedriver.framework.cmdb.exception.ci.CiNotFoundException;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.framework.matrix.constvalue.MatrixAttributeType;
import codedriver.framework.matrix.core.MatrixDataSourceHandlerBase;
import codedriver.framework.matrix.dto.*;
import codedriver.framework.matrix.exception.MatrixAttributeNotFoundException;
import codedriver.framework.matrix.exception.MatrixCiNotFoundException;
import codedriver.framework.restful.core.MyApiComponent;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentFactory;
import codedriver.module.cmdb.api.cientity.SearchCiEntityApi;
import codedriver.module.cmdb.dao.mapper.ci.AttrMapper;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dao.mapper.ci.CiViewMapper;
import codedriver.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import codedriver.module.cmdb.matrix.constvalue.MatrixType;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author linbq
 * @since 2021/11/15 14:35
 **/
@Component
public class CiDataSourceHandler extends MatrixDataSourceHandlerBase {

    private final static Logger logger = LoggerFactory.getLogger(CiDataSourceHandler.class);

    @Resource
    private CiMapper ciMapper;

    @Resource
    private CiEntityMapper ciEntityMapper;

    @Resource
    private AttrMapper attrMapper;

    @Resource
    private CiViewMapper ciViewMapper;

    @Override
    public String getHandler() {
        return MatrixType.CMDBCI.getValue();
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
        MatrixCiVo oldMatrixCiVo = matrixMapper.getMatrixCiByMatrixUuid(matrixVo.getUuid());
        if (oldMatrixCiVo != null) {
            if (ciId.equals(oldMatrixCiVo.getCiId())) {
                return false;
            }
        }
        MatrixCiVo matrixCiVo = new MatrixCiVo(matrixVo.getUuid(), ciId);
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
    }

    @Override
    protected void myDeleteMatrix(String uuid) {
        matrixMapper.deleteMatrixCiByMatrixUuid(uuid);
    }

    @Override
    protected void myCopyMatrix(String sourceUuid, MatrixVo matrixVo) {

    }

    @Override
    protected JSONObject myImportMatrix(MatrixVo matrixVo, MultipartFile multipartFile) throws IOException {
        return null;
    }

    @Override
    protected HSSFWorkbook myExportMatrix(MatrixVo matrixVo) {
        return null;
    }

    @Override
    protected void mySaveAttributeList(String matrixUuid, List<MatrixAttributeVo> matrixAttributeList) {

    }

    @Override
    protected List<MatrixAttributeVo> myGetAttributeList(MatrixVo matrixVo) {
        String matrixUuid = matrixVo.getUuid();
        MatrixCiVo matrixCiVo = matrixMapper.getMatrixCiByMatrixUuid(matrixUuid);
        if (matrixCiVo == null) {
            throw new MatrixCiNotFoundException(matrixUuid);
        }

        Long ciId = matrixCiVo.getCiId();
        String showType = ShowType.LIST.getValue();
        List<AttrVo> attrList = attrMapper.getAttrByCiId(ciId);
        if (StringUtils.isNotBlank(showType)) {
            CiViewVo ciViewVo = new CiViewVo();
            ciViewVo.setCiId(ciId);
            ciViewVo.addShowType(showType);
            ciViewVo.addShowType(ShowType.ALL.getValue());
            List<CiViewVo> ciViewList = ciViewMapper.getCiViewByCiId(ciViewVo);
            Set<Long> attrSet = new HashSet<>();
            for (CiViewVo ciView : ciViewList) {
                if (ciView.getType().equals("attr")) {
                    attrSet.add(ciView.getItemId());
                }
            }
            attrList.removeIf(attr -> !attrSet.contains(attr.getId()));
        }

        int sort = 0;
        List<MatrixAttributeVo> matrixAttributeList = new ArrayList<>();
        MatrixAttributeVo matrixAttributeVo = new MatrixAttributeVo();
        matrixAttributeVo.setMatrixUuid(matrixUuid);
        matrixAttributeVo.setUuid("id");
        matrixAttributeVo.setName("ID#");
        matrixAttributeVo.setType(MatrixAttributeType.INPUT.getValue());
        matrixAttributeVo.setIsDeletable(0);
        matrixAttributeVo.setSort(sort++);
        matrixAttributeVo.setIsRequired(0);
        matrixAttributeVo.setPrimaryKey(1);
        matrixAttributeList.add(matrixAttributeVo);
        matrixAttributeVo = new MatrixAttributeVo();
        matrixAttributeVo.setMatrixUuid(matrixUuid);
        matrixAttributeVo.setUuid("ciLabel");
        matrixAttributeVo.setName("模型");
        matrixAttributeVo.setType(MatrixAttributeType.INPUT.getValue());
        matrixAttributeVo.setIsDeletable(0);
        matrixAttributeVo.setSort(sort++);
        matrixAttributeVo.setIsRequired(0);
        matrixAttributeList.add(matrixAttributeVo);
        for (AttrVo attrVo : attrList) {
            matrixAttributeVo = new MatrixAttributeVo();
            matrixAttributeVo.setMatrixUuid(matrixUuid);
            matrixAttributeVo.setUuid(attrVo.getName());
            matrixAttributeVo.setName(attrVo.getLabel());
            matrixAttributeVo.setType(MatrixAttributeType.INPUT.getValue());
            matrixAttributeVo.setIsDeletable(0);
            matrixAttributeVo.setSort(sort++);
            matrixAttributeVo.setIsRequired(0);
            if ("date".equals(attrVo.getType())) {
                matrixAttributeVo.setIsSearchable(0);
            }
            matrixAttributeList.add(matrixAttributeVo);
        }
        return matrixAttributeList;
    }

    @Override
    protected JSONObject myExportAttribute(MatrixVo matrixVo) {
        return null;
    }

    @Override
    protected JSONObject myGetTableData(MatrixDataVo dataVo) {
        String matrixUuid = dataVo.getMatrixUuid();
        MatrixCiVo matrixCiVo = matrixMapper.getMatrixCiByMatrixUuid(matrixUuid);
        if (matrixCiVo == null) {
            throw new MatrixCiNotFoundException(matrixUuid);
        }
        JSONObject paramObj = new JSONObject();
        paramObj.put("ciId", matrixCiVo.getCiId());
        paramObj.put("currentPage", dataVo.getCurrentPage());
        paramObj.put("pageSize", dataVo.getPageSize());
        JSONObject resultObj = accessSearchCiEntityApi(paramObj);
        if (MapUtils.isNotEmpty(resultObj)) {
            List<Map<String, Object>> tbodyList = new ArrayList<>();
            JSONArray tbodyArray = resultObj.getJSONArray("tbodyList");
            if (CollectionUtils.isNotEmpty(tbodyArray)) {
                for (int i = 0; i < tbodyArray.size(); i++) {
                    JSONObject rowData = tbodyArray.getJSONObject(i);
                    if (MapUtils.isNotEmpty(rowData)) {
                        Map<String, Object> rowDataMap = new HashMap<>();
                        for (Map.Entry<String, Object> entry : rowData.entrySet()) {
                            rowDataMap.put(entry.getKey(), matrixAttributeValueHandle(null, entry.getValue()));
                        }
                        tbodyList.add(rowDataMap);
                    }
                }
            }
            resultObj.put("tbodyList", tbodyList);
        }
        MatrixVo matrixVo = matrixMapper.getMatrixByUuid(matrixUuid);
        List<MatrixAttributeVo> attributeVoList = myGetAttributeList(matrixVo);
        JSONArray theadList = getTheadList(attributeVoList);
        resultObj.put("theadList", theadList);
        return resultObj;
    }

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
            JSONObject paramObj = new JSONObject();
            paramObj.put("ciId", matrixCiVo.getCiId());
            paramObj.put("currentPage", dataVo.getCurrentPage());
            paramObj.put("pageSize", dataVo.getPageSize());
            JSONArray defaultValue = dataVo.getDefaultValue();
            if (CollectionUtils.isNotEmpty(defaultValue)) {
                paramObj.put("idList", defaultValue.toJavaList(Long.class));
            } else {
                if (!setAttrFilterList(dataVo, matrixCiVo.getCiId(), paramObj)) {
                    return new JSONObject();
                }
            }
            JSONObject resultObj = accessSearchCiEntityApi(paramObj);
            if (MapUtils.isNotEmpty(resultObj)) {
                List<Map<String, Object>> tbodyList = new ArrayList<>();
                JSONArray tbodyArray = resultObj.getJSONArray("tbodyList");
                if (CollectionUtils.isNotEmpty(tbodyArray)) {
                    for (int i = 0; i < tbodyArray.size(); i++) {
                        JSONObject rowData = tbodyArray.getJSONObject(i);
                        if (MapUtils.isNotEmpty(rowData)) {
                            Map<String, Object> rowDataMap = new HashMap<>();
                            for (Map.Entry<String, Object> entry : rowData.entrySet()) {
                                rowDataMap.put(entry.getKey(), matrixAttributeValueHandle(null, entry.getValue()));
                            }
                            tbodyList.add(rowDataMap);
                        }
                    }
                }
                resultObj.put("tbodyList", tbodyList);
            }
            JSONArray theadList = getTheadList(matrixUuid, matrixAttributeList, dataVo.getColumnList());
            resultObj.put("theadList", theadList);
            return resultObj;
        }
        return new JSONObject();
    }

    private boolean setAttrFilterList(MatrixDataVo dataVo, Long ciId, JSONObject paramObj) {
        Map<String, AttrVo> attrMap = new HashMap<>();
        List<AttrVo> attrList = attrMapper.getAttrByCiId(ciId);
        for (AttrVo attrVo : attrList) {
            attrMap.put(attrVo.getName(), attrVo);
        }
        List<AttrFilterVo> attrFilterList = new ArrayList<>();
        JSONArray filterList = dataVo.getFilterList();
        if (CollectionUtils.isNotEmpty(filterList)) {
            for (int i = 0; i < filterList.size(); i++) {
                JSONObject filterObj = filterList.getJSONObject(i);
                if (MapUtils.isEmpty(filterObj)) {
                    continue;
                }
                JSONArray valueArray = filterObj.getJSONArray("valueList");
                if (CollectionUtils.isEmpty(valueArray)) {
                    continue;
                }
                List<String> valueList = new ArrayList<>();
                for (String value : valueArray.toJavaList(String.class)) {
                    if (StringUtils.isNotBlank(value)) {
                        valueList.add(value);
                    }
                }
                if (CollectionUtils.isEmpty(valueList)) {
                    continue;
                }
                String uuid = filterObj.getString("uuid");
                if ("id".equals(uuid)) {
                    paramObj.put("filterCiEntityId", valueList.get(0));
                } else  if ("ciLabel".equals(uuid)) {
                    String ciLabel = valueList.get(0);
                    CiVo ciVo = ciMapper.getCiByLabel(ciLabel);
                    if (ciVo == null) {
                        return false;
                    }
                    paramObj.put("filterCiId", ciVo.getId());
                } else {
                    AttrVo attrVo = attrMap.get(uuid);
                    if (attrVo == null) {
                        continue;
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
                    attrFilterVo.setExpression(SearchExpression.EQ.getExpression());
                    attrFilterVo.setValueList(valueList);
                    attrFilterList.add(attrFilterVo);
                }
            }
        }

        List<MatrixColumnVo> matrixColumnList = dataVo.getSourceColumnList();
        if (CollectionUtils.isNotEmpty(matrixColumnList)) {
            for (MatrixColumnVo matrixColumnVo : matrixColumnList) {
                Object value = matrixColumnVo.getValue();
                if (value == null) {
                    continue;
                }
                String uuid = matrixColumnVo.getColumn();
                if ("id".equals(uuid)) {
                    paramObj.put("filterCiEntityId", value);
                } else if ("ciLabel".equals(uuid)) {
                    CiVo ciVo = ciMapper.getCiByLabel(value.toString());
                    if (ciVo == null) {
                        return false;
                    }
                    paramObj.put("filterCiId", ciVo.getId());
                } else {
                    AttrVo attrVo = attrMap.get(uuid);
                    if (attrVo == null) {
                        continue;
                    }
                    List<String> valueList = new ArrayList<>();
                    if ("select".equals(attrVo.getType())) {
                        CiVo targetCiVo = ciMapper.getCiById(attrVo.getTargetCiId());
                        if (targetCiVo == null) {
                            return false;
                        }
                        if (Objects.equals(targetCiVo.getIsVirtual(), 1)) {
                            CiEntityVo ciEntityVo = new CiEntityVo();
                            ciEntityVo.setCiId(targetCiVo.getId());
                            ciEntityVo.setName(value.toString());
                            List<CiEntityVo> ciEntityList = ciEntityMapper.getVirtualCiEntityBaseInfoByName(ciEntityVo);
                            if (CollectionUtils.isEmpty(ciEntityList)) {
                                return false;
                            }
                            for (CiEntityVo ciEntity : ciEntityList) {
                                valueList.add(ciEntity.getId().toString());
                            }
                        } else {
                            Long ciEntityId = ciEntityMapper.getIdByCiIdAndName(attrVo.getTargetCiId(), value.toString());
                            if (ciEntityId == null) {
                                return false;
                            }
                            valueList.add(ciEntityId.toString());
                        }
                    } else {
                        valueList.add(value.toString());
                    }
                    AttrFilterVo attrFilterVo = new AttrFilterVo();
                    attrFilterVo.setAttrId(attrVo.getId());
                    String expression = matrixColumnVo.getExpression();
                    expression = SearchExpression.checkExpressionIsExists(expression) ? expression : SearchExpression.EQ.getExpression();
                    attrFilterVo.setExpression(expression);
                    attrFilterVo.setValueList(valueList);
                    attrFilterList.add(attrFilterVo);
                }
            }
        }
        paramObj.put("attrFilterList", attrFilterList);
        return true;
    }
    @Override
    protected List<Map<String, JSONObject>> myTableColumnDataSearch(MatrixDataVo dataVo) {
        String matrixUuid = dataVo.getMatrixUuid();
        MatrixCiVo matrixCiVo = matrixMapper.getMatrixCiByMatrixUuid(matrixUuid);
        if (matrixCiVo == null) {
            throw new MatrixCiNotFoundException(matrixUuid);
        }
        List<Map<String, JSONObject>> resultList = new ArrayList<>();
        MatrixVo matrixVo = matrixMapper.getMatrixByUuid(matrixUuid);
        List<MatrixAttributeVo> matrixAttributeList = myGetAttributeList(matrixVo);
        if (CollectionUtils.isNotEmpty(matrixAttributeList)) {
            List<String> attributeList = matrixAttributeList.stream().map(MatrixAttributeVo::getUuid).collect(Collectors.toList());
            List<String> columnList = dataVo.getColumnList();
            for (String column : columnList) {
                if (!attributeList.contains(column)) {
                    throw new MatrixAttributeNotFoundException(dataVo.getMatrixUuid(), column);
                }
            }
            JSONObject paramObj = new JSONObject();
            paramObj.put("ciId", matrixCiVo.getCiId());

            Map<String, AttrVo> attrMap = new HashMap<>();
            List<AttrVo> attrList = attrMapper.getAttrByCiId(matrixCiVo.getCiId());
            for (AttrVo attrVo : attrList) {
                attrMap.put(attrVo.getName(), attrVo);
            }
            JSONArray defaultValue = dataVo.getDefaultValue();
            if (CollectionUtils.isNotEmpty(defaultValue)) {
                for (String value : defaultValue.toJavaList(String.class)) {
                    if (value.contains(SELECT_COMPOSE_JOINER)) {
                        List<AttrFilterVo> attrFilterList = new ArrayList<>();
                        String[] split = value.split(SELECT_COMPOSE_JOINER);
                        //当下拉框配置的值和显示文字列为同一列时，value值是这样的20210101&=&20210101，split数组第一和第二个元素相同，这时需要去重
                        List<String> splitList = new ArrayList<>();
                        for (String str : split) {
                            if (!splitList.contains(str)) {
                                splitList.add(str);
                            }
                        }
                        int min = Math.min(splitList.size(), columnList.size());
                        for (int i = 0; i < min; i++) {
                            String column = columnList.get(i);
                            if (StringUtils.isNotBlank(column)) {
                                AttrVo attrVo = attrMap.get(column);
                                if (attrVo == null) {
                                    continue;
                                }
                                AttrFilterVo attrFilterVo = new AttrFilterVo();
                                attrFilterVo.setAttrId(attrVo.getId());
                                attrFilterVo.setExpression(SearchExpression.EQ.getExpression());
                                List<String> valueList = new ArrayList<>();
                                valueList.add(splitList.get(i));
                                attrFilterVo.setValueList(valueList);
                                attrFilterList.add(attrFilterVo);
                            }
                        }
                        paramObj.put("attrFilterList", attrFilterList);
                        JSONObject resultObj = accessSearchCiEntityApi(paramObj);
                        resultList.addAll(getCmdbCiDataTbodyList(resultObj, columnList));
                    }
                }
            } else {
                List<AttrFilterVo> attrFilterList = new ArrayList<>();
                String keywordColumn = dataVo.getKeywordColumn();
                if (StringUtils.isNotBlank(keywordColumn) && StringUtils.isNotBlank(dataVo.getKeyword())) {
                    if (!attributeList.contains(keywordColumn)) {
                        throw new MatrixAttributeNotFoundException(dataVo.getMatrixUuid(), keywordColumn);
                    }
                    AttrVo attrVo = attrMap.get(keywordColumn);
                    if (attrVo != null) {
                        AttrFilterVo attrFilterVo = new AttrFilterVo();
                        attrFilterVo.setAttrId(attrVo.getId());
                        attrFilterVo.setExpression(SearchExpression.LI.getExpression());
                        List<String> valueList = new ArrayList<>();
                        valueList.add(dataVo.getKeyword());
                        attrFilterVo.setValueList(valueList);
                        attrFilterList.add(attrFilterVo);
                    }
                }
                JSONArray filterList = dataVo.getFilterList();
                if (CollectionUtils.isNotEmpty(filterList)) {
                    for (int i = 0; i < filterList.size(); i++) {
                        JSONObject filterObj = filterList.getJSONObject(i);
                        if (MapUtils.isEmpty(filterObj)) {
                            continue;
                        }
                        JSONArray valueArray = filterObj.getJSONArray("valueList");
                        if (CollectionUtils.isEmpty(valueArray)) {
                            continue;
                        }
                        List<String> valueList = new ArrayList<>();
                        for (String value : valueArray.toJavaList(String.class)) {
                            if (StringUtils.isNotBlank(value)) {
                                valueList.add(value);
                            }
                        }
                        if (CollectionUtils.isEmpty(valueList)) {
                            continue;
                        }
                        String uuid = filterObj.getString("uuid");
                        if ("id".equals(uuid)) {
                            paramObj.put("filterCiEntityId", valueList.get(0));
                        } else  if ("ciLabel".equals(uuid)) {
                            String ciLabel = valueList.get(0);
                            CiVo ciVo = ciMapper.getCiByLabel(ciLabel);
                            if (ciVo == null) {
                                return resultList;
                            }
                            paramObj.put("filterCiId", ciVo.getId());
                        } else {
                            AttrVo attrVo = attrMap.get(uuid);
                            if (attrVo == null) {
                                continue;
                            }
                            if ("select".equals(attrVo.getType())) {
                                CiVo targetCiVo = ciMapper.getCiById(attrVo.getTargetCiId());
                                if (targetCiVo == null) {
                                    return resultList;
                                }
                                List<String> newValueList = new ArrayList<>();
                                for (String value : valueList) {
                                    if (Objects.equals(targetCiVo.getIsVirtual(), 1)) {
                                        CiEntityVo ciEntityVo = new CiEntityVo();
                                        ciEntityVo.setCiId(targetCiVo.getId());
                                        ciEntityVo.setName(value);
                                        List<CiEntityVo> ciEntityList = ciEntityMapper.getVirtualCiEntityBaseInfoByName(ciEntityVo);
                                        if (CollectionUtils.isEmpty(ciEntityList)) {
                                            return resultList;
                                        }
                                        for (CiEntityVo ciEntity : ciEntityList) {
                                            newValueList.add(ciEntity.getId().toString());
                                        }
                                    } else {
                                        Long ciEntityId = ciEntityMapper.getIdByCiIdAndName(targetCiVo.getId(), value);
                                        if (ciEntityId == null) {
                                            return resultList;
                                        }
                                        newValueList.add(ciEntityId.toString());
                                    }
                                }
                                valueList = newValueList;
                            }
                            AttrFilterVo attrFilterVo = new AttrFilterVo();
                            attrFilterVo.setAttrId(attrVo.getId());
                            attrFilterVo.setExpression(SearchExpression.EQ.getExpression());
                            attrFilterVo.setValueList(valueList);
                            attrFilterList.add(attrFilterVo);
                        }
                    }
                }
                paramObj.put("attrFilterList", attrFilterList);
                paramObj.put("currentPage", dataVo.getCurrentPage());
                int pageSize = dataVo.getPageSize();
                paramObj.put("pageSize", pageSize);
                paramObj.put("needPage", pageSize < 100);
                JSONObject result = accessSearchCiEntityApi(paramObj);
                resultList = getCmdbCiDataTbodyList(result, columnList);

                //去重
                String firstColumn = columnList.get(0);
                String secondColumn = columnList.get(0);
                if (columnList.size() >= 2) {
                    secondColumn = columnList.get(1);
                }
                List<String> exsited = new ArrayList<>();
                Iterator<Map<String, JSONObject>> iterator = resultList.iterator();
                while (iterator.hasNext()) {
                    Map<String, JSONObject> resultObj = iterator.next();
                    JSONObject firstObj = resultObj.get(firstColumn);
                    JSONObject secondObj = resultObj.get(secondColumn);
                    String firstValue = firstObj.getString("value");
                    String secondText = secondObj.getString("text");
                    String compose = firstValue + SELECT_COMPOSE_JOINER + secondText;
                    if (exsited.contains(compose)) {
                        iterator.remove();
                    } else {
                        exsited.add(compose);
                    }
                }
            }
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

    private JSONObject accessSearchCiEntityApi(JSONObject paramObj) {
        MyApiComponent restComponent = (MyApiComponent) PrivateApiComponentFactory.getInstance(SearchCiEntityApi.class.getName());
        if (restComponent != null) {
            try {
                paramObj.put("needCheck", false);
                paramObj.put("needAction", false);
                paramObj.put("needActionType", false);
                paramObj.put("mode", "dialog");
                JSONObject resultObj = (JSONObject) restComponent.myDoService(paramObj);
                JSONArray tbodyArray = resultObj.getJSONArray("tbodyList");
                if (CollectionUtils.isNotEmpty(tbodyArray)) {
                    JSONArray tbodyList = new JSONArray();
                    for (int i = 0; i < tbodyArray.size(); i++) {
                        JSONObject tbodyObj = tbodyArray.getJSONObject(i);
                        if (MapUtils.isNotEmpty(tbodyObj)) {
                            JSONObject tbody = new JSONObject();
                            tbody.put("id", tbodyObj.getLong("id"));
                            tbody.put("ciLabel", tbodyObj.getString("ciLabel"));
                            JSONObject attrEntityData = tbodyObj.getJSONObject("attrEntityData");
                            if (MapUtils.isNotEmpty(attrEntityData)) {
                                for (Map.Entry<String, Object> entry : attrEntityData.entrySet()) {
                                    JSONObject valueObj = (JSONObject) entry.getValue();
                                    String name = valueObj.getString("name");
                                    if (StringUtils.isNotBlank(name)) {
                                        JSONArray actualValueArray = valueObj.getJSONArray("actualValueList");
                                        if (CollectionUtils.isNotEmpty(actualValueArray)) {
                                            List<String> actualValueList = actualValueArray.toJavaList(String.class);
                                            tbody.put(name, String.join(",", actualValueList));
                                        }
                                    }
                                }
                            }
                            tbodyList.add(tbody);
                        }
                    }
                    resultObj.put("tbodyList", tbodyList);
                }
                return resultObj;
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
        return new JSONObject();
    }

    public List<Map<String, JSONObject>> getCmdbCiDataTbodyList(JSONObject resultObj, List<String> columnList) {
        List<Map<String, JSONObject>> resultList = new ArrayList<>();
        if (MapUtils.isNotEmpty(resultObj)) {
            JSONArray tbodyArray = resultObj.getJSONArray("tbodyList");
            if (CollectionUtils.isNotEmpty(tbodyArray)) {
                for (int i = 0; i < tbodyArray.size(); i++) {
                    JSONObject rowData = tbodyArray.getJSONObject(i);
                    if (MapUtils.isNotEmpty(rowData)) {
                        Map<String, JSONObject> resultMap = new HashMap<>(columnList.size());
                        for (String column : columnList) {
                            String columnValue = rowData.getString(column);
                            resultMap.put(column, matrixAttributeValueHandle(null, columnValue));
                        }
                        resultList.add(resultMap);
                    }
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
                String config = matrixAttribute.getConfig();
                if (StringUtils.isNotBlank(config)) {
                    JSONObject configObj = JSON.parseObject(config);
                    JSONArray dataList = configObj.getJSONArray("dataList");
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
