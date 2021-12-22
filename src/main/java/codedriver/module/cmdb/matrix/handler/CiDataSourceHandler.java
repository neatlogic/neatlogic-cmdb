/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.matrix.handler;

import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.dto.ci.CiViewVo;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.ci.RelVo;
import codedriver.framework.cmdb.dto.cientity.AttrFilterVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.dto.cientity.RelEntityVo;
import codedriver.framework.cmdb.dto.cientity.RelFilterVo;
import codedriver.framework.cmdb.enums.SearchExpression;
import codedriver.framework.cmdb.exception.ci.CiNotFoundException;
import codedriver.framework.cmdb.utils.RelUtil;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.framework.matrix.constvalue.MatrixAttributeType;
import codedriver.framework.matrix.core.MatrixDataSourceHandlerBase;
import codedriver.framework.matrix.dto.*;
import codedriver.framework.matrix.exception.MatrixAttributeNotFoundException;
import codedriver.framework.matrix.exception.MatrixCiNotFoundException;
import codedriver.module.cmdb.api.cientity.SearchCiEntityApi;
import codedriver.module.cmdb.dao.mapper.ci.AttrMapper;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dao.mapper.ci.CiViewMapper;
import codedriver.module.cmdb.dao.mapper.ci.RelMapper;
import codedriver.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import codedriver.module.cmdb.dao.mapper.cientity.RelEntityMapper;
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
    private RelMapper relMapper;

    @Resource
    private RelEntityMapper relEntityMapper;

    @Resource
    private CiViewMapper ciViewMapper;

    @Resource
    private SearchCiEntityApi searchCiEntityApi;

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
        int sort = 0;
        List<MatrixAttributeVo> matrixAttributeList = new ArrayList<>();
        CiViewVo ciViewVo = new CiViewVo();
        ciViewVo.setCiId(ciId);
        List<CiViewVo> ciViewList = RelUtil.ClearCiViewRepeatRel(ciViewMapper.getCiViewByCiId(ciViewVo));
        if (CollectionUtils.isNotEmpty(ciViewList)) {
            List<AttrVo> attrList = attrMapper.getAttrByCiId(ciId);
            Map<Long, AttrVo> attrMap = attrList.stream().collect(Collectors.toMap(e -> e.getId(), e -> e));
            for (CiViewVo ciview : ciViewList) {
                MatrixAttributeVo matrixAttributeVo = new MatrixAttributeVo();
                matrixAttributeVo.setMatrixUuid(matrixUuid);
                matrixAttributeVo.setName(ciview.getItemLabel());
                matrixAttributeVo.setType(MatrixAttributeType.INPUT.getValue());
                matrixAttributeVo.setIsDeletable(0);
                matrixAttributeVo.setSort(sort++);
                matrixAttributeVo.setIsRequired(0);
                switch (ciview.getType()) {
                    case "attr":
                        matrixAttributeVo.setUuid(ciview.getItemName());
                        AttrVo attrVo = attrMap.get(ciview.getItemId());
                        if ("date".equals(attrVo.getType())) {
                            matrixAttributeVo.setIsSearchable(0);
                        }
                        break;
                    case "relfrom":
                        matrixAttributeVo.setUuid(ciview.getItemName());
                        break;
                    case "relto":
                        matrixAttributeVo.setUuid(ciview.getItemName());
                        break;
                    case "const":
                        //固化属性需要特殊处理
                        matrixAttributeVo.setUuid(ciview.getItemName().replace("_", ""));
                        break;
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
            boolean needAccessApi = true;
            JSONObject paramObj = new JSONObject();
            paramObj.put("ciId", matrixCiVo.getCiId());
            paramObj.put("currentPage", dataVo.getCurrentPage());
            paramObj.put("pageSize", dataVo.getPageSize());
            JSONArray defaultValue = dataVo.getDefaultValue();
            if (CollectionUtils.isNotEmpty(defaultValue)) {
                paramObj.put("idList", defaultValue.toJavaList(Long.class));
            } else {
                Long ciId = matrixCiVo.getCiId();
                List<AttrVo> attrList = attrMapper.getAttrByCiId(ciId);
                Map<String, AttrVo> attrMap = attrList.stream().collect(Collectors.toMap(e -> e.getName(), e -> e));
                Map<String, RelVo> relMap = new HashMap<>();
                List<RelVo> relList = relMapper.getRelByCiId(ciId);
                for (RelVo relVo : relList) {
                    if ("from".equals(relVo.getDirection())) {
                        relMap.put(relVo.getToName(), relVo);
                    } else if ("to".equals(relVo.getDirection())) {
                        relMap.put(relVo.getFromName(), relVo);
                    }
                }
                CiViewVo ciViewVo = new CiViewVo();
                ciViewVo.setCiId(ciId);
                List<CiViewVo> ciViewList = RelUtil.ClearCiViewRepeatRel(ciViewMapper.getCiViewByCiId(ciViewVo));
                Map<String, CiViewVo> ciViewMap = ciViewList.stream().collect(Collectors.toMap(e -> e.getItemName(), e -> e));
                List<AttrFilterVo> attrFilterList = new ArrayList<>();
                List<RelFilterVo> relFilterList = new ArrayList<>();
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

                        CiViewVo ciView = ciViewMap.get(uuid);
                        if (ciView == null) {
                            continue;
                        }
                        if (!conversionFilter(uuid, valueList, attrMap, relMap, ciView, attrFilterList, relFilterList, paramObj)) {
                            needAccessApi = false;
                            break;
                        }
                    }
                }
                if (needAccessApi) {
                    List<MatrixColumnVo> matrixColumnList = dataVo.getSourceColumnList();
                    if (CollectionUtils.isNotEmpty(matrixColumnList)) {
                        for (MatrixColumnVo matrixColumnVo : matrixColumnList) {
                            Object value = matrixColumnVo.getValue();
                            if (value == null) {
                                continue;
                            }
                            String valueStr = value.toString();
                            if (StringUtils.isBlank(valueStr)) {
                                continue;
                            }
                            String uuid = matrixColumnVo.getColumn();
                            CiViewVo ciView = ciViewMap.get(uuid);
                            if (ciView == null) {
                                continue;
                            }
                            List<String> valueList = new ArrayList<>();
                            valueList.add(valueStr);
                            if (!conversionFilter(uuid, valueList, attrMap, relMap, ciView, attrFilterList, relFilterList, paramObj)) {
                                needAccessApi = false;
                                break;
                            }
                        }
                    }
                }
                if (needAccessApi) {
                    paramObj.put("attrFilterList", attrFilterList);
                    paramObj.put("relFilterList", relFilterList);
                }
            }
            JSONObject resultObj = new JSONObject();
            if (needAccessApi) {
//                System.out.println(paramObj);
                JSONObject result = accessSearchCiEntityApi(paramObj);
//                System.out.println(result);
                if (MapUtils.isNotEmpty(result)) {
                    List<Map<String, Object>> tbodyList = new ArrayList<>();
                    JSONArray tbodyArray = result.getJSONArray("tbodyList");
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
            }
            JSONArray theadList = getTheadList(matrixUuid, matrixAttributeList, dataVo.getColumnList());
            resultObj.put("theadList", theadList);
            return resultObj;
        }
        return new JSONObject();
    }

    private boolean conversionFilter(String uuid, List<String> valueList, Map<String, AttrVo> attrMap, Map<String, RelVo> relMap, CiViewVo ciView, List<AttrFilterVo> attrFilterList, List<RelFilterVo> relFilterList, JSONObject paramObj) {
        RelVo relVo = relMap.get(uuid);
        switch (ciView.getType()) {
            case "attr":
                AttrVo attrVo = attrMap.get(uuid);
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
                    paramObj.put("filterCiEntityId", valueList.get(0));
                } else if ("ciLabel".equals(uuid)) {
                    String ciLabel = valueList.get(0);
                    CiVo ciVo = ciMapper.getCiByLabel(ciLabel);
                    if (ciVo == null) {
                        return false;
                    }
                    paramObj.put("filterCiId", ciVo.getId());
                }
                break;
        }
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
            Long ciId = matrixCiVo.getCiId();
            paramObj.put("ciId", ciId);
            List<AttrVo> attrList = attrMapper.getAttrByCiId(ciId);
            Map<String, AttrVo> attrMap = attrList.stream().collect(Collectors.toMap(e -> e.getName(), e -> e));
            Map<String, RelVo> relMap = new HashMap<>();
            List<RelVo> relList = relMapper.getRelByCiId(ciId);
            for (RelVo relVo : relList) {
                if ("from".equals(relVo.getDirection())) {
                    relMap.put(relVo.getToName(), relVo);
                } else if ("to".equals(relVo.getDirection())) {
                    relMap.put(relVo.getFromName(), relVo);
                }
            }
            CiViewVo ciViewVo = new CiViewVo();
            ciViewVo.setCiId(ciId);
            List<CiViewVo> ciViewList = RelUtil.ClearCiViewRepeatRel(ciViewMapper.getCiViewByCiId(ciViewVo));
            Map<String, CiViewVo> ciViewMap = ciViewList.stream().collect(Collectors.toMap(e -> e.getItemName(), e -> e));
            List<AttrFilterVo> attrFilterList = new ArrayList<>();
            List<RelFilterVo> relFilterList = new ArrayList<>();
            JSONArray defaultValue = dataVo.getDefaultValue();
            if (CollectionUtils.isNotEmpty(defaultValue)) {
                for (String value : defaultValue.toJavaList(String.class)) {
                    if (value.contains(SELECT_COMPOSE_JOINER)) {
                        String[] split = value.split(SELECT_COMPOSE_JOINER);
                        //当下拉框配置的值和显示文字列为同一列时，value值是这样的20210101&=&20210101，split数组第一和第二个元素相同，这时需要去重
                        List<String> splitList = new ArrayList<>();
                        for (String str : split) {
                            if (!splitList.contains(str)) {
                                splitList.add(str);
                            }
                        }
                        boolean needAccessApi = true;
                        int min = Math.min(splitList.size(), columnList.size());
                        for (int i = 0; i < min; i++) {
                            String column = columnList.get(i);
                            if (StringUtils.isNotBlank(column)) {
                                CiViewVo ciView = ciViewMap.get(column);
                                if (ciView == null) {
                                    continue;
                                }
                                List<String> valueList = new ArrayList<>();
                                valueList.add(splitList.get(i));
                                if (!conversionFilter(column, valueList, attrMap, relMap, ciView, attrFilterList, relFilterList, paramObj)) {
                                    needAccessApi = false;
                                    break;
                                }
                            }
                        }
                        if (needAccessApi) {
                            paramObj.put("attrFilterList", attrFilterList);
                            paramObj.put("relFilterList", relFilterList);
                            JSONObject resultObj = accessSearchCiEntityApi(paramObj);
                            resultList.addAll(getCmdbCiDataTbodyList(resultObj, columnList));
                        }
                    }
                }
            } else {
                boolean needAccessApi = true;
                String keywordColumn = dataVo.getKeywordColumn();
                if (StringUtils.isNotBlank(keywordColumn) && StringUtils.isNotBlank(dataVo.getKeyword())) {
                    if (!attributeList.contains(keywordColumn)) {
                        throw new MatrixAttributeNotFoundException(dataVo.getMatrixUuid(), keywordColumn);
                    }
                    CiViewVo ciView = ciViewMap.get(keywordColumn);
                    if (ciView != null) {
                        List<String> valueList = new ArrayList<>();
                        valueList.add(dataVo.getKeyword());
                        if (!conversionFilter(keywordColumn, valueList, attrMap, relMap, ciView, attrFilterList, relFilterList, paramObj)) {
                            needAccessApi = false;
                        }
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
                        CiViewVo ciView = ciViewMap.get(uuid);
                        if (ciView != null) {
                            continue;
                        }
                        if (!conversionFilter(uuid, valueList, attrMap, relMap, ciView, attrFilterList, relFilterList, paramObj)) {
                            needAccessApi = false;
                            break;
                        }
                    }
                }
                if (needAccessApi) {
                    paramObj.put("attrFilterList", attrFilterList);
                    paramObj.put("relFilterList", relFilterList);
                    paramObj.put("currentPage", dataVo.getCurrentPage());
                    int pageSize = dataVo.getPageSize();
                    paramObj.put("pageSize", pageSize);
                    paramObj.put("needPage", pageSize < 100);
                    JSONObject result = accessSearchCiEntityApi(paramObj);
                    resultList = getCmdbCiDataTbodyList(result, columnList);
                }

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
        try {
            paramObj.put("needCheck", false);
            paramObj.put("needAction", false);
            paramObj.put("needActionType", false);
            paramObj.put("mode", "dialog");
            //避免性能太差
            //paramObj.put("isAllColumn", 1);
            JSONObject resultObj = (JSONObject) searchCiEntityApi.myDoService(paramObj);
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
                        JSONObject relEntityData = tbodyObj.getJSONObject("relEntityData");
                        if (MapUtils.isNotEmpty(relEntityData)) {
                            for (Map.Entry<String, Object> entry : relEntityData.entrySet()) {
                                JSONObject relObj = (JSONObject) entry.getValue();
                                String name = relObj.getString("name");
                                if (StringUtils.isNotBlank(name)) {
                                    JSONArray valueArray = relObj.getJSONArray("valueList");
                                    if (CollectionUtils.isNotEmpty(valueArray)) {
                                        List<String> ciEntityNameList = new ArrayList<>();
                                        for (int j = 0; j < valueArray.size(); j++) {
                                            JSONObject valueObj = valueArray.getJSONObject(j);
                                            String ciEntityName = valueObj.getString("ciEntityName");
                                            if (StringUtils.isNotBlank(ciEntityName)) {
                                                ciEntityNameList.add(ciEntityName);
                                            }
                                        }
                                        tbody.put(name, String.join(",", ciEntityNameList));
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
