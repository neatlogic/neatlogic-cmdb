/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.matrix.handler;

import codedriver.framework.cmdb.dto.customview.CustomViewAttrVo;
import codedriver.framework.cmdb.dto.customview.CustomViewConditionVo;
import codedriver.framework.cmdb.dto.customview.CustomViewVo;
import codedriver.framework.cmdb.enums.customview.SearchMode;
import codedriver.framework.cmdb.exception.customview.CustomViewNotFoundException;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.framework.matrix.constvalue.MatrixAttributeType;
import codedriver.framework.matrix.core.MatrixDataSourceHandlerBase;
import codedriver.framework.matrix.dto.*;
import codedriver.framework.matrix.exception.MatrixCiNotFoundException;
import codedriver.framework.matrix.exception.MatrixCustomViewNotFoundException;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.cmdb.dao.mapper.customview.CustomViewMapper;
import codedriver.module.cmdb.matrix.constvalue.MatrixType;
import codedriver.module.cmdb.service.customview.CustomViewDataService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;

/**
 * @author linbq
 * @since 2021/11/15 14:35
 **/
@Component
public class CustomViewDataSourceHandler extends MatrixDataSourceHandlerBase {
    @Resource
    private CustomViewMapper customViewMapper;
    @Resource
    private CustomViewDataService customViewDataService;

    @Override
    public String getHandler() {
        return MatrixType.CMDBCICUSTOMVIEW.getValue();
    }

    @Override
    protected boolean mySaveMatrix(MatrixVo matrixVo) throws Exception {
        Long customViewId = matrixVo.getCustomViewId();
        if (customViewId == null) {
            throw new ParamNotExistsException("customViewId");
        }
        CustomViewVo customViewVo = customViewMapper.getCustomViewById(customViewId);
        if (customViewVo == null) {
            throw new CustomViewNotFoundException(customViewId);
        }
        MatrixCustomViewVo oldMatrixCustomViewVo = matrixMapper.getMatrixCustomViewByMatrixUuid(matrixVo.getUuid());
        if (oldMatrixCustomViewVo != null) {
            if (customViewId.equals(oldMatrixCustomViewVo.getCustomViewId())) {
                return false;
            }
        }
        MatrixCustomViewVo matrixCustomViewVo = new MatrixCustomViewVo(matrixVo.getUuid(), customViewId);
        matrixMapper.replaceMatrixCustomView(matrixCustomViewVo);
        return true;
    }

    @Override
    protected void myGetMatrix(MatrixVo matrixVo) {
        MatrixCustomViewVo matrixCustomViewVo = matrixMapper.getMatrixCustomViewByMatrixUuid(matrixVo.getUuid());
        if (matrixCustomViewVo == null) {
            throw new MatrixCustomViewNotFoundException(matrixVo.getUuid());
        }
        matrixVo.setCustomViewId(matrixCustomViewVo.getCustomViewId());
    }

    @Override
    protected void myDeleteMatrix(String uuid) {
        matrixMapper.deleteMatrixCustomViewByMatrixUuid(uuid);
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
        MatrixCustomViewVo matrixCustomViewVo = matrixMapper.getMatrixCustomViewByMatrixUuid(matrixUuid);
        if (matrixCustomViewVo == null) {
            throw new MatrixCustomViewNotFoundException(matrixUuid);
        }
        Long customViewId = matrixCustomViewVo.getCustomViewId();
        List<CustomViewAttrVo> customViewAttrList = customViewMapper.getCustomViewAttrByCustomViewId(new CustomViewAttrVo(customViewId));
        int sort = 0;
        List<MatrixAttributeVo> matrixAttributeList = new ArrayList<>();
        for (CustomViewAttrVo customViewAttrVo : customViewAttrList) {
            MatrixAttributeVo matrixAttributeVo = new MatrixAttributeVo();
            matrixAttributeVo.setMatrixUuid(matrixUuid);
            matrixAttributeVo.setUuid(customViewAttrVo.getUuid());
            matrixAttributeVo.setName(customViewAttrVo.getAlias());
            matrixAttributeVo.setType(MatrixAttributeType.INPUT.getValue());
            matrixAttributeVo.setIsDeletable(0);
            matrixAttributeVo.setSort(sort++);
            matrixAttributeVo.setIsRequired(0);
//            if ("date".equals(attrVo.getType())) {
//                matrixAttributeVo.setIsSearchable(0);
//            }
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
        MatrixCustomViewVo matrixCustomViewVo = matrixMapper.getMatrixCustomViewByMatrixUuid(matrixUuid);
        if (matrixCustomViewVo == null) {
            throw new MatrixCustomViewNotFoundException(matrixUuid);
        }
        CustomViewConditionVo customViewConditionVo = new CustomViewConditionVo();
        customViewConditionVo.setCustomViewId(matrixCustomViewVo.getCustomViewId());
        customViewConditionVo.setSearchMode(SearchMode.NORMAL.getValue());
        List<Map<String, Object>> dataList = customViewDataService.searchCustomViewData(customViewConditionVo);
        MatrixVo matrixVo = matrixMapper.getMatrixByUuid(matrixUuid);
        List<MatrixAttributeVo> attributeVoList = myGetAttributeList(matrixVo);
        List<Map<String, Object>> tbodyList = matrixService.matrixTableDataValueHandle(attributeVoList, dataList);
        JSONArray theadList = getTheadList(attributeVoList);
        return TableResultUtil.getResult(theadList, tbodyList, dataVo);
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
            JSONObject resultObj = new JSONObject();
            JSONArray theadList = getTheadList(matrixUuid, matrixAttributeList, dataVo.getColumnList());
            resultObj.put("theadList", theadList);
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
}
