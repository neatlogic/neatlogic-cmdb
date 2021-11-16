/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.matrix.handler;

import codedriver.framework.cmdb.dto.ci.CiViewVo;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.cientity.AttrFilterVo;
import codedriver.framework.cmdb.enums.ShowType;
import codedriver.framework.cmdb.exception.ci.CiNotFoundException;
import codedriver.framework.cmdb.utils.RelUtil;
import codedriver.framework.common.constvalue.Expression;
import codedriver.framework.dto.RoleVo;
import codedriver.framework.dto.TeamVo;
import codedriver.framework.dto.UserVo;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.framework.matrix.constvalue.MatrixAttributeType;
import codedriver.framework.matrix.constvalue.MatrixType;
import codedriver.framework.matrix.core.MatrixDataSourceHandlerBase;
import codedriver.framework.matrix.dto.*;
import codedriver.framework.matrix.exception.MatrixAttributeNotFoundException;
import codedriver.framework.matrix.exception.MatrixCiNotFoundException;
import codedriver.framework.restful.core.MyApiComponent;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentFactory;
import codedriver.module.cmdb.api.cientity.SearchCiEntityApi;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dao.mapper.ci.CiViewMapper;
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
        CiViewVo ciViewVo = new CiViewVo();
        ciViewVo.setCiId(matrixCiVo.getCiId());
        ciViewVo.addShowType(ShowType.LIST.getValue());
        ciViewVo.addShowType(ShowType.ALL.getValue());
        List<CiViewVo> ciViewList = RelUtil.ClearCiViewRepeatRel(ciViewMapper.getCiViewByCiId(ciViewVo));
        List<MatrixAttributeVo> matrixAttributeList = new ArrayList<>();
        int sort = 0;
        for (CiViewVo ciView : ciViewList) {
            MatrixAttributeVo matrixAttributeVo = new MatrixAttributeVo();
            matrixAttributeVo.setMatrixUuid(matrixUuid);
            matrixAttributeVo.setUuid(ciView.getType() + "_" + ciView.getItemId());
//            matrixAttributeVo.setUuid(ciView.getItemId().toString());
//            matrixAttributeVo.setUuid(ciView.getItemId().toString());
            matrixAttributeVo.setName(ciView.getItemLabel());
            matrixAttributeVo.setType(MatrixAttributeType.INPUT.getValue());
            matrixAttributeVo.setIsDeletable(0);
            matrixAttributeVo.setSort(sort++);
            matrixAttributeVo.setIsRequired(0);
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
        paramObj.put("needCheck", false);
        paramObj.put("needAction", false);
        paramObj.put("needActionType", false);
        paramObj.put("mode", "page");
        paramObj.put("currentPage", dataVo.getCurrentPage());
        paramObj.put("pageSize", dataVo.getPageSize());
        JSONObject resultObj = (JSONObject) accessSearchCiEntityApi(paramObj);
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
//        MatrixVo matrixVo = matrixMapper.getMatrixByUuid(matrixUuid);
//        List<MatrixAttributeVo> attributeVoList = myGetAttributeList(matrixVo);
//        JSONArray theadList = getTheadList(attributeVoList);
//        resultObj.put("theadList", theadList);
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
            paramObj.put("needCheck", false);
            paramObj.put("needAction", false);
            paramObj.put("needActionType", false);
            paramObj.put("mode", "page");
            paramObj.put("currentPage", dataVo.getCurrentPage());
            paramObj.put("pageSize", dataVo.getPageSize());
            JSONArray defaultValue = dataVo.getDefaultValue();
            if (CollectionUtils.isNotEmpty(defaultValue)) {
                paramObj.put("idList", defaultValue.toJavaList(Long.class));
            } else {
                List<AttrFilterVo> attrFilterList = new ArrayList<>();
                JSONArray filterList = dataVo.getFilterList();
                for (int i = 0; i < filterList.size(); i++) {
                    JSONObject filterObj = filterList.getJSONObject(i);
                    if (MapUtils.isEmpty(filterObj)) {
                        continue;
                    }
                    String uuid = filterObj.getString("uuid");
                    if (!uuid.startsWith("attr_")) {
                        continue;
                    }
                    Long attrId = Long.parseLong(uuid.substring(5));
                    JSONArray valueArray = filterObj.getJSONArray("valueList");
                    if (CollectionUtils.isEmpty(valueArray)) {
                        continue;
                    }
                    AttrFilterVo attrFilterVo = new AttrFilterVo();
                    attrFilterVo.setAttrId(attrId);
                    attrFilterVo.setExpression("equal");
                    List<String> valueList = valueArray.toJavaList(String.class);
                    attrFilterVo.setValueList(valueList);
                    attrFilterList.add(attrFilterVo);
                }
                List<MatrixColumnVo> matrixColumnList = dataVo.getSourceColumnList();
                for (MatrixColumnVo matrixColumnVo : matrixColumnList) {
                    Object value = matrixColumnVo.getValue();
                    if (value == null) {
                        continue;
                    }
                    String uuid = matrixColumnVo.getColumn();
                    if (!uuid.startsWith("attr_")) {
                        continue;
                    }
                    Long attrId = Long.parseLong(uuid.substring(5));
                    AttrFilterVo attrFilterVo = new AttrFilterVo();
                    attrFilterVo.setAttrId(attrId);
                    attrFilterVo.setExpression(matrixColumnVo.getExpression());
                    List<String> valueList = new ArrayList<>();
                    valueList.add(value.toString());
                    attrFilterVo.setValueList(valueList);
                    attrFilterList.add(attrFilterVo);
                }
                paramObj.put("attrFilterList", attrFilterList);
            }
            JSONObject resultObj = (JSONObject) accessSearchCiEntityApi(paramObj);
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
            return resultObj;
        }
        return new JSONObject();
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
            paramObj.put("needCheck", false);
            paramObj.put("needAction", false);
            paramObj.put("needActionType", false);
            paramObj.put("mode", "page");
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
                                if (!column.startsWith("attr_")) {
                                    continue;
                                }
                                Long attrId = Long.parseLong(column.substring(5));
                                AttrFilterVo attrFilterVo = new AttrFilterVo();
                                attrFilterVo.setAttrId(attrId);
                                attrFilterVo.setExpression(Expression.EQUAL.getExpression());
                                List<String> valueList = new ArrayList<>();
                                valueList.add(splitList.get(i));
                                attrFilterVo.setValueList(valueList);
                                attrFilterList.add(attrFilterVo);
                            }
                        }
                        paramObj.put("attrFilterList", attrFilterList);
                        JSONObject resultObj = (JSONObject) accessSearchCiEntityApi(paramObj);
                        resultList.addAll(getCmdbCiDataTbodyList(resultObj, columnList));
                    }
                }
            } else {
                String keywordColumn = dataVo.getKeywordColumn();
                if (StringUtils.isNotBlank(keywordColumn) && StringUtils.isNotBlank(dataVo.getKeyword())) {
                    if (!attributeList.contains(keywordColumn)) {
                        throw new MatrixAttributeNotFoundException(dataVo.getMatrixUuid(), keywordColumn);
                    }
                    List<AttrFilterVo> attrFilterList = new ArrayList<>();
                    JSONArray filterList = dataVo.getFilterList();
                    for (int i = 0; i < filterList.size(); i++) {
                        JSONObject filterObj = filterList.getJSONObject(i);
                        if (MapUtils.isEmpty(filterObj)) {
                            continue;
                        }
                        String uuid = filterObj.getString("uuid");
                        if (!uuid.startsWith("attr_")) {
                            continue;
                        }
                        Long attrId = Long.parseLong(uuid.substring(5));
                        JSONArray valueArray = filterObj.getJSONArray("valueList");
                        if (CollectionUtils.isEmpty(valueArray)) {
                            continue;
                        }
                        AttrFilterVo attrFilterVo = new AttrFilterVo();
                        attrFilterVo.setAttrId(attrId);
                        attrFilterVo.setExpression("equal");
                        List<String> valueList = valueArray.toJavaList(String.class);
                        attrFilterVo.setValueList(valueList);
                        attrFilterList.add(attrFilterVo);
                    }
                    if (keywordColumn.startsWith("attr_")) {
                        Long attrId = Long.parseLong(keywordColumn.substring(5));
                        AttrFilterVo attrFilterVo = new AttrFilterVo();
                        attrFilterVo.setAttrId(attrId);
                        attrFilterVo.setExpression(Expression.LIKE.getExpression());
                        List<String> valueList = new ArrayList<>();
                        valueList.add(dataVo.getKeyword());
                        attrFilterVo.setValueList(valueList);
                        attrFilterList.add(attrFilterVo);
                    }
                }
//                integrationVo.getParamObj().putAll(jsonObj);
                paramObj.put("currentPage", dataVo.getCurrentPage());
                int pageSize = dataVo.getPageSize();
                paramObj.put("pageSize", pageSize);
                paramObj.put("needPage", pageSize < 100);
                JSONObject resultObj = (JSONObject) accessSearchCiEntityApi(paramObj);
                resultList = getCmdbCiDataTbodyList(resultObj, columnList);
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

    private Object accessSearchCiEntityApi(JSONObject paramObj) {
        MyApiComponent restComponent = (MyApiComponent) PrivateApiComponentFactory.getInstance(SearchCiEntityApi.class.getName());
        if (restComponent != null) {
            try {
                return restComponent.myDoService(paramObj);
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
