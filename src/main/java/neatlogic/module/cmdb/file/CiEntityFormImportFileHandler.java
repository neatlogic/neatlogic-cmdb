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

package neatlogic.module.cmdb.file;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.cmdb.attrvaluehandler.core.AttrValueHandlerFactory;
import neatlogic.framework.cmdb.attrvaluehandler.core.IAttrValueHandler;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiViewVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrItemVo;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrVo;
import neatlogic.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import neatlogic.framework.cmdb.enums.EditModeType;
import neatlogic.framework.cmdb.enums.RelDirectionType;
import neatlogic.framework.cmdb.enums.TransactionActionType;
import neatlogic.framework.cmdb.exception.batchimport.BatchImportAnalyzeHeaderFailedException;
import neatlogic.framework.cmdb.exception.batchimport.BatchImportCanNotGetResourceIdException;
import neatlogic.framework.cmdb.exception.batchimport.BatchImportMouldIsMustException;
import neatlogic.framework.cmdb.exception.batchimport.BatchImportUuidLengthIsInvalidException;
import neatlogic.framework.cmdb.exception.ci.CiIsAbstractedException;
import neatlogic.framework.cmdb.exception.ci.CiIsVirtualException;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.cmdb.exception.ci.CiWithoutAttrRelException;
import neatlogic.framework.cmdb.exception.cientity.AttrEntityValueEmptyException;
import neatlogic.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import neatlogic.framework.cmdb.exception.cientity.RelEntityNotFoundException;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.file.core.FileTypeHandlerBase;
import neatlogic.framework.file.dto.FileVo;
import neatlogic.framework.util.$;
import neatlogic.framework.util.UuidUtil;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiViewMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import neatlogic.module.cmdb.dao.mapper.globalattr.GlobalAttrMapper;
import neatlogic.module.cmdb.service.ci.CiService;
import neatlogic.module.cmdb.service.cientity.CiEntityService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class CiEntityFormImportFileHandler extends FileTypeHandlerBase {
    private final Logger logger = LoggerFactory.getLogger(CiEntityFormImportFileHandler.class);
    @Resource
    private CiEntityMapper ciEntityMapper;

    @Resource
    private CiMapper ciMapper;

    @Resource
    private CiService ciService;

    @Resource
    private CiViewMapper ciViewMapper;

    @Resource
    private CiEntityService ciEntityService;

    @Resource
    private GlobalAttrMapper globalAttrMapper;

    @Override
    public boolean valid(String userUuid, FileVo fileVo, JSONObject jsonObj) {
        return false;
    }

    private List<CiEntityVo> getCiEntityBaseInfoByName(Long ciId, String name) {
        CiVo ciVo = ciMapper.getCiById(ciId);
        CiEntityVo ciEntityVo = new CiEntityVo();
        ciEntityVo.setCiId(ciId);
        ciEntityVo.setName(name);
        if (ciVo.getIsVirtual().equals(0)) {
            List<CiVo> list = ciMapper.getDownwardCiListByLR(ciVo.getLft(), ciVo.getRht());
            if (CollectionUtils.isNotEmpty(list)) {
                ciEntityVo.setIdList(list.stream().map(CiVo::getId).collect(Collectors.toList()));
                return ciEntityMapper.getCiEntityListByCiIdListAndName(ciEntityVo);
            }
            return null;
        } else {
            return ciEntityMapper.getVirtualCiEntityBaseInfoByName(ciEntityVo);
        }
    }

    private String getCellContent(Cell cell) {
        String cellContent = "";
        switch (cell.getCellType()) {
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    Date date = cell.getDateCellValue();
                    SimpleDateFormat sFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    cellContent = sFormat.format(date).replace("00:00:00", "");
                } else {
                    BigDecimal bd = new BigDecimal(Double.toString(cell.getNumericCellValue()));
                    bd = bd.setScale(4, RoundingMode.HALF_UP);
                    cellContent = bd.stripTrailingZeros().toPlainString();
                }
                break;
            case STRING:
                cellContent = cell.getStringCellValue();
                break;
            case BOOLEAN:
                cellContent = String.valueOf(cell.getBooleanCellValue());
                break;
            case BLANK:
            case ERROR:
                cellContent = "";
                break;
            case FORMULA:
                cellContent = cell.getCellFormula();
                break;
            default:
                break;
        }
        if (StringUtils.isNotBlank(cellContent)) {
            return cellContent.trim();
        } else {
            return "";
        }
    }

    private JSONObject analyseWorkbook(Workbook wb, Long ciId, String action, String editMode) {
        JSONObject resultObj = new JSONObject();
        List<CiEntityVo> successList = new ArrayList<>();
        /* 用来记录整个表格读取过程中的错误 */
        String error = "";
        InputStream in = null;

        try {
            CiVo ciVo = ciService.getCiById(ciId);
            CiViewVo ciViewVo = new CiViewVo();
            ciViewVo.setCiId(ciId);
            ciViewVo.setNeedAlias(1);
            List<CiViewVo> ciViewList = ciViewMapper.getCiViewByCiId(ciViewVo);
            if (ciVo == null) {
                throw new CiNotFoundException(ciId);
            }
            if (ciVo.getIsAbstract().equals(1)) {
                throw new CiIsAbstractedException(CiIsAbstractedException.Type.DATA, ciVo.getLabel());
            }
            if (ciVo.getIsVirtual().equals(1)) {
                throw new CiIsVirtualException(ciVo.getName());
            }
            if (CollectionUtils.isEmpty(ciVo.getAttrList()) && CollectionUtils.isEmpty(ciVo.getRelList())) {
                throw new CiWithoutAttrRelException(ciVo.getName());
            }


            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                Sheet sheet = wb.getSheetAt(i);
                if (sheet != null) {
                    Row headRow = sheet.getRow(sheet.getFirstRowNum());
                    List<Integer> cellIndex = new ArrayList<>();// 列数
                    Map<Integer, Object> typeMap = new HashMap<>();// 表头列数->表头属性
                    Set<String> checkAttrSet = new HashSet<>();
                    Set<String> requireAttrSet = new HashSet<>();//必填属性或关系
                    try {
                        COLUMN:
                        for (Iterator<Cell> cellIterator = headRow.cellIterator(); cellIterator.hasNext(); ) {
                            Cell cell = cellIterator.next();
                            if (cell != null) {
                                String content = getCellContent(cell);
                                if (content.contains("[(")) {
                                    content = content.substring(0, content.indexOf("[("));
                                }

                                if (content.equals("id")) {
                                    typeMap.put(cell.getColumnIndex(), "id");
                                    cellIndex.add(cell.getColumnIndex());
                                    continue;
                                } else if (content.equals("uuid")) {
                                    typeMap.put(cell.getColumnIndex(), "uuid");
                                    cellIndex.add(cell.getColumnIndex());
                                    continue;
                                }
                                if (CollectionUtils.isNotEmpty(ciVo.getUniqueAttrIdList())) {
                                    for (Long attrId : ciVo.getUniqueAttrIdList()) {
                                        requireAttrSet.add("attr_" + attrId);
                                    }
                                }
                                for (AttrVo attr : ciVo.getAttrList()) {
                                    if (attr.getIsRequired().equals(1)) {
                                        requireAttrSet.add("attr_" + attr.getId());
                                    }
                                }
                                for (GlobalAttrVo globalAttr : ciVo.getGlobalAttrList()) {
                                    if (CollectionUtils.isNotEmpty(ciViewList)) {
                                        Optional<CiViewVo> op = ciViewList.stream().filter(d -> d.getType().equals("global") && d.getItemId().equals(globalAttr.getId())).findFirst();
                                        op.ifPresent(viewVo -> globalAttr.setLabel(viewVo.getAlias()));
                                    }
                                    if (content.contains("[" + $.t("term.cmdb.globalattr") + "]") && (globalAttr.getLabel() + "[" + $.t("term.cmdb.globalattr") + "]").equals(content)) {
                                        checkAttrSet.add("global_" + globalAttr.getId());
                                        typeMap.put(cell.getColumnIndex(), globalAttr);
                                        cellIndex.add(cell.getColumnIndex());
                                        continue COLUMN;
                                    }
                                }
                                for (AttrVo attr : ciVo.getAttrList()) {
                                    if (CollectionUtils.isNotEmpty(ciViewList)) {
                                        Optional<CiViewVo> op = ciViewList.stream().filter(d -> d.getType().equals("attr") && d.getItemId().equals(attr.getId())).findFirst();
                                        op.ifPresent(viewVo -> attr.setLabel(viewVo.getAlias()));
                                    }
                                    if (attr.getLabel().equals(content)) {
                                        checkAttrSet.add("attr_" + attr.getId());
                                        typeMap.put(cell.getColumnIndex(), attr);
                                        cellIndex.add(cell.getColumnIndex());
                                        continue COLUMN;
                                    }
                                }
                                for (RelVo rel : ciVo.getRelList()) {
                                    if ((rel.getDirection().equals(RelDirectionType.FROM.getValue()) && rel.getToIsRequired().equals(1)) || (rel.getDirection().equals(RelDirectionType.TO.getValue()) && rel.getFromIsRequired().equals(1))) {
                                        requireAttrSet.add("rel_" + rel.getDirection() + rel.getId());
                                    }
                                }
                                for (RelVo rel : ciVo.getRelList()) {
                                    if (CollectionUtils.isNotEmpty(ciViewList)) {
                                        Optional<CiViewVo> op = ciViewList.stream().filter(d -> d.getType().startsWith("rel") && d.getItemId().equals(rel.getId())).findFirst();
                                        if (op.isPresent()) {
                                            CiViewVo view = op.get();
                                            if (view.getType().equals("relfrom")) {
                                                rel.setToLabel(view.getAlias());
                                            } else if (view.getType().equals("relto")) {
                                                rel.setFromLabel(view.getAlias());
                                            }
                                        }
                                    }
                                    if ((rel.getDirection().equals(RelDirectionType.FROM.getValue()) && rel.getToLabel().equals(content)) || (rel.getDirection().equals(RelDirectionType.TO.getValue()) && rel.getFromLabel().equals(content))) {
                                        checkAttrSet.add("rel_" + rel.getDirection() + rel.getId());
                                        typeMap.put(cell.getColumnIndex(), rel);
                                        cellIndex.add(cell.getColumnIndex());
                                        continue COLUMN;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        throw new BatchImportAnalyzeHeaderFailedException(e.getMessage());
                    }
                        /*
                          【只添加】与【添加&更新】模式下，不能缺少必填属性列和唯一属性列
                          【只更新】且【全局更新】模式下，不能缺少必填属性列
                         */
                    List<String> lostColumns = new ArrayList<>();
                    if (action.equals("all") || action.equals("append")) {
                        if (CollectionUtils.isNotEmpty(ciVo.getUniqueAttrIdList())) {
                            for (Long attrId : ciVo.getUniqueAttrIdList()) {
                                if (!checkAttrSet.contains("attr_" + attrId)) {
                                    lostColumns.add(ciVo.getAttrById(attrId).getLabel());
                                }
                            }
                        }
                    }
                    if (action.equals("all") || action.equals("append") || (action.equals("update") && editMode.equals(EditModeType.GLOBAL.getValue()))) {
                        for (AttrVo attr : ciVo.getAttrList()) {
                            if (attr.getIsRequired().equals(1) && !checkAttrSet.contains("attr_" + attr.getId())) {
                                lostColumns.add(attr.getLabel());
                            }
                        }
                        for (RelVo rel : ciVo.getRelList()) {
                            if (rel.getDirection().equals(RelDirectionType.FROM.getValue())) {
                                if (!checkAttrSet.contains("rel_" + rel.getDirection() + rel.getId()) && rel.getToIsRequired().equals(1)) {
                                    lostColumns.add(rel.getToLabel());
                                }
                            } else {
                                if (!checkAttrSet.contains("rel_" + rel.getDirection() + rel.getId()) && rel.getFromIsRequired().equals(1)) {
                                    lostColumns.add(rel.getFromLabel());
                                }
                            }
                        }
                    }
                    if (CollectionUtils.isNotEmpty(lostColumns)) {
                        error = "<b class=\"text-danger\">导入模版缺少必要的属性：" + Arrays.toString(lostColumns.toArray()) + "</b></br>";
                    }
                    if (StringUtils.isBlank(error)) {
                        for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                            /* 用来详细记录每一行每一列的错误信息，使用LinkedHashMap是为了计算列数 */
                            Map<Integer, String> errorMsgMap = new LinkedHashMap<>();
                            /* 用来记录每一行最后保存时的错误 */
                            Map<Integer, String> rowError = new LinkedHashMap<>();
                            try {
                                Row row = sheet.getRow(r);
                                if (row != null) {
                                    Long ciEntityId = null;
                                    String ciEntityUuid = null;// 记录下表格中的ciEntityId，用于判断配置项是否存在
                                    CiEntityTransactionVo ciEntityTransactionVo = new CiEntityTransactionVo();
                                    ciEntityTransactionVo.setAllowCommit(true);
                                    ciEntityTransactionVo.setCiId(ciId);
                                    ciEntityTransactionVo.setEditMode(editMode);
                                    //先遍历所有cell检查是否空行，如果是空行直接跳过
                                    boolean isEmptyRow = true;
                                    for (Integer index : cellIndex) {
                                        Cell cell = row.getCell(index);
                                        if (cell != null && StringUtils.isNotBlank(getCellContent(cell))) {
                                            isEmptyRow = false;
                                            break;
                                        }
                                    }
                                    if (isEmptyRow) {
                                        continue;
                                    }
                                    for (Integer index : cellIndex) {
                                        Cell cell = row.getCell(index);
                                        if (cell != null) {
                                            String content = getCellContent(cell);
                                            Object header = typeMap.get(index);
                                            if (header instanceof String) {
                                                if (header.equals("id")) {// 表示拿到的是ID列
                                                    if (StringUtils.isNotBlank(content)) {
                                                        try {
                                                            ciEntityId = Long.parseLong(content);
                                                            ciEntityTransactionVo.setCiEntityId(ciEntityId);
                                                        } catch (Exception e) {
                                                            throw new BatchImportCanNotGetResourceIdException(e.getMessage());
                                                        }
                                                    }
                                                } else if (header.equals("uuid")) {
                                                    if (StringUtils.isNotBlank(content)) {
                                                        ciEntityUuid = content.trim();
                                                        if (ciEntityUuid.length() != 32) {
                                                            throw new BatchImportUuidLengthIsInvalidException();
                                                        }
                                                    } else {
                                                        ciEntityUuid = UuidUtil.randomUuid();
                                                    }
                                                    ciEntityTransactionVo.setCiEntityUuid(ciEntityUuid);
                                                }
                                            } else if (header instanceof GlobalAttrVo) {
                                                GlobalAttrVo globalAttr = (GlobalAttrVo) header;
                                                JSONArray valueList = new JSONArray();
                                                for (String c : content.split(",")) {
                                                    GlobalAttrItemVo attrItem = new GlobalAttrItemVo();
                                                    attrItem.setAttrId(globalAttr.getId());
                                                    attrItem.setValue(c);
                                                    List<GlobalAttrItemVo> globalAttrItemList = globalAttrMapper.searchGlobalAttrItem(attrItem);
                                                    // Optional<GlobalAttrItemVo> op = globalAttr.getItemList().stream().filter(d -> d.getValue().equalsIgnoreCase(c)).findFirst();
                                                    //op.ifPresent(valueList::add);
                                                    valueList.addAll(globalAttrItemList);
                                                }
                                                if (CollectionUtils.isNotEmpty(valueList)) {
                                                    ciEntityTransactionVo.addGlobalAttrEntityData(globalAttr, valueList);
                                                }
                                            } else if (header instanceof AttrVo) {// 如果是属性
                                                AttrVo attr = (AttrVo) header;
                                                JSONArray valueList = new JSONArray();
                                                if (requireAttrSet.contains("attr_" + attr.getId()) && StringUtils.isBlank(content)) {
                                                    throw new AttrEntityValueEmptyException(attr.getLabel());
                                                }
                                                //如果是引用类型需要先转换成目标配置项的id
                                                //FIXME 可能需要替换使用唯一规则获取配置项信息
                                                if (attr.getTargetCiId() != null) {
                                                    for (String c : content.split(",")) {
                                                        if (StringUtils.isNotBlank(c)) {
                                                            c = c.trim();
                                                            List<CiEntityVo> targetCiEntityList = getCiEntityBaseInfoByName(attr.getTargetCiId(), c);
                                                            if (CollectionUtils.isNotEmpty(targetCiEntityList)) {
                                                                valueList.addAll(targetCiEntityList.stream().map(CiEntityVo::getId).distinct().collect(Collectors.toList()));
                                                            } else {
                                                                throw new CiEntityNotFoundException(c);
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    IAttrValueHandler handler = AttrValueHandlerFactory.getHandler(attr.getType());
                                                    if (handler != null) {
                                                        Object newContent = handler.transferValueListToInput(attr, content);
                                                        if (newContent instanceof JSONArray) {
                                                            valueList.addAll((JSONArray) newContent);
                                                        } else {
                                                            valueList.add(content);
                                                        }
                                                    } else {
                                                        valueList.add(content);
                                                    }
                                                }
                                                ciEntityTransactionVo.addAttrEntityData(attr, valueList);

                                            } else if (header instanceof RelVo) {
                                                RelVo rel = (RelVo) header;
                                                if (requireAttrSet.contains("rel_" + rel.getDirection() + rel.getId()) && StringUtils.isBlank(content)) {
                                                    throw new RelEntityNotFoundException(rel.getDirection().equals(RelDirectionType.FROM.getValue()) ? rel.getToLabel() : rel.getFromLabel());
                                                }
                                                for (String c : content.split(",")) {
                                                    if (StringUtils.isNotBlank(c)) {
                                                        c = c.trim();
                                                        List<CiEntityVo> targetCiEntityList = getCiEntityBaseInfoByName(rel.getDirection().equals(RelDirectionType.FROM.getValue()) ? rel.getToCiId() : rel.getFromCiId(), c);
                                                        if (CollectionUtils.isNotEmpty(targetCiEntityList)) {
                                                            for (CiEntityVo entity : targetCiEntityList) {
                                                                ciEntityTransactionVo.addRelEntityData(rel, rel.getDirection(), ciId, entity.getId());
                                                            }
                                                        } else {
                                                            throw new CiEntityNotFoundException(c);
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            Object header = typeMap.get(index);
                                            if (header instanceof AttrVo) {// 如果是属性
                                                AttrVo attr = (AttrVo) header;
                                                if (requireAttrSet.contains("attr_" + attr.getId())) {
                                                    throw new AttrEntityValueEmptyException(attr.getLabel());
                                                }
                                            } else if (header instanceof RelVo) {
                                                RelVo rel = (RelVo) header;
                                                if (requireAttrSet.contains("rel_" + rel.getDirection() + rel.getId())) {
                                                    throw new RelEntityNotFoundException(rel.getDirection().equals(RelDirectionType.FROM.getValue()) ? rel.getToLabel() : rel.getFromLabel());
                                                }
                                            }
                                        }
                                    }

                                    try {
                                        if (action.equals("append") && ciEntityId == null) {
                                            ciEntityTransactionVo.setAction(TransactionActionType.INSERT.getValue());
                                            ciEntityService.validateCiEntityTransaction(ciEntityTransactionVo);
                                            CiEntityVo ciEntityVo = new CiEntityVo(ciEntityTransactionVo);
                                            ciEntityVo.setCiLabel(ciVo.getLabel());
                                            ciEntityVo.setCiName(ciVo.getName());
                                            ciEntityVo.setCiIcon(ciVo.getIcon());
                                            successList.add(ciEntityVo);
                                        } /*else if (action.equals("update") && ciEntityId != null) {
                                            ciEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
                                            ciEntityService.validateCiEntityTransaction(ciEntityTransactionVo);
                                            successCount += 1;
                                        } else if (action.equals("all")) {
                                            if (ciEntityId != null) {
                                                ciEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
                                            } else {
                                                ciEntityTransactionVo.setAction(TransactionActionType.INSERT.getValue());
                                            }
                                            ciEntityService.validateCiEntityTransaction(ciEntityTransactionVo);
                                            successCount += 1;
                                        }*/ else {
                                            throw new BatchImportMouldIsMustException();
                                        }
                                    } catch (Exception e) {
                                        logger.error(e.getMessage(), e);
                                        rowError.put(r, e.getMessage());
                                    }
                                }
                            } catch (Exception e) {
                                logger.error(e.getMessage(), e);
                                rowError.put(r, e.getMessage());
                            } finally {
                                // String err = "";
                                List<Integer> columnList = new ArrayList<>();
                                List<String> errorMsgList = new ArrayList<>();
                                for (Map.Entry<Integer, String> _err : errorMsgMap.entrySet()) {
                                    columnList.add(_err.getKey());
                                    errorMsgList.add(_err.getValue());
                                }
                                String errMsg = Arrays.toString(errorMsgList.toArray());
                                String newerrMsg = errMsg.replace(',', ';');
                                if (CollectionUtils.isNotEmpty(columnList)) {
                                    error += "</br><b class=\"text-danger\">第" + r + "行第" + Arrays.toString(columnList.toArray()) + "列</b>：" + newerrMsg;
                                }

                                if (MapUtils.isNotEmpty(rowError)) {
                                    for (Map.Entry<Integer, String> entry : rowError.entrySet()) {
                                        error += "</br><b class=\"text-danger\">第" + entry.getKey() + "行：" + entry.getValue() + "</b>";
                                    }
                                }


                            }
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            if (StringUtils.isNotBlank(error) && !error.endsWith("</br>")) {
                error += "</br>";
            }
            error += "<b class='text-danger'>" + e.getMessage() + "</b>";
            logger.error(e.getMessage(), e);
        } finally {
            try {
                if (wb != null) {
                    wb.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
        resultObj.put("successList", successList);
        resultObj.put("error", error);
        return resultObj;
    }

    @Override
    public JSONObject analyze(MultipartFile multipartFile, JSONObject paramObj) throws IOException {
        Long ciId = paramObj.getLong("ciId");
        if (ciId == null) {
            throw new ParamNotExistsException("ciId");
        }
        Workbook wb = WorkbookFactory.create(multipartFile.getInputStream());
        return analyseWorkbook(wb, ciId, "append", EditModeType.GLOBAL.getValue());
    }

    @Override
    public String getDisplayName() {
        return "配置项表单上传附件";
    }

    @Override
    protected boolean myDeleteFile(FileVo fileVo, JSONObject paramObj) {
        return true;
    }

    @Override
    public boolean needSave() {
        return false;
    }

    @Override
    public void afterUpload(FileVo fileVo, JSONObject jsonObj) {
    }

    @Override
    public String getName() {
        return "CIENTITYFORMIMPORT";
    }

}
