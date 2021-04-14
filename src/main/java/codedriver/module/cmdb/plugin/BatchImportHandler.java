/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.plugin;

import codedriver.framework.asynchronization.thread.CodeDriverThread;
import codedriver.framework.cmdb.constvalue.EditModeType;
import codedriver.framework.cmdb.constvalue.RelActionType;
import codedriver.framework.cmdb.constvalue.RelDirectionType;
import codedriver.framework.cmdb.constvalue.TransactionActionType;
import codedriver.framework.common.util.FileUtil;
import codedriver.framework.file.dto.FileVo;
import codedriver.module.cmdb.dao.mapper.batchimport.ImportMapper;
import codedriver.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import codedriver.framework.cmdb.dto.batchimport.ImportAuditVo;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.ci.RelVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.dto.transaction.AttrEntityTransactionVo;
import codedriver.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import codedriver.framework.cmdb.dto.transaction.RelEntityTransactionVo;
import codedriver.framework.cmdb.enums.BatchImportStatus;
import codedriver.module.cmdb.exception.ci.CiNotFoundException;
import codedriver.module.cmdb.service.ci.CiService;
import codedriver.module.cmdb.service.cientity.CiEntityService;
import com.alibaba.fastjson.JSONArray;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

@Service
public class BatchImportHandler {

    public static final Map<Long, String> importMap = new HashMap<>();
    static Logger logger = LoggerFactory.getLogger(BatchImportHandler.class);

    private static ImportMapper importMapper;

    private static CiEntityMapper ciEntityMapper;

    private static CiService ciService;

    private static CiEntityService ciEntityService;

    @Autowired
    public void setImportMapper(ImportMapper _importMapper) {
        importMapper = _importMapper;
    }

    @Autowired
    public void setCiEntityMapper(CiEntityMapper _ciEntityMapper) {
        ciEntityMapper = _ciEntityMapper;
    }

    @Autowired
    public void setCiService(CiService _ciService) {
        ciService = _ciService;
    }

    @Autowired
    public void setCiEntityService(CiEntityService _ciEntityService) {
        ciEntityService = _ciEntityService;
    }

    private static String getCellContent(Cell cell) {
        String cellContent = "";
        switch (cell.getCellType()) {
            case NUMERIC:
                if (HSSFDateUtil.isCellDateFormatted(cell)) {
                    Date date = cell.getDateCellValue();
                    SimpleDateFormat sFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    cellContent = sFormat.format(date).replace("00:00:00", "");
                } else {
                    cellContent = String.format("%.0f", cell.getNumericCellValue());
                }
                break;
            case STRING:
                cellContent = cell.getStringCellValue() + "";
                break;
            case BOOLEAN:
                cellContent = cell.getBooleanCellValue() + "";
                break;
            case BLANK:
                cellContent = "";
                break;
            case FORMULA:
                cellContent = cell.getCellFormula() + "";
                break;
            case ERROR:
                cellContent = "";
                break;
        }
        return cellContent;
    }

    public static class Importer extends CodeDriverThread {
        private Long ciId;
        private Integer editMode;
        private FileVo fileVo;
        private String importUser;
        private String action;
        private Boolean checkProp;

        public Importer(Long ciId, String action, Integer editMode, FileVo fileVo, String importUser,
                        Boolean checkProp) {
            this.ciId = ciId;
            this.fileVo = fileVo;
            this.action = action;
            this.editMode = editMode;
            this.importUser = importUser;
            this.checkProp = checkProp;
        }

        @Override
        public void execute() {
            ImportAuditVo importAuditVo = new ImportAuditVo();
            importAuditVo.setCiId(ciId);
            importAuditVo.setImportUser(importUser);
            importAuditVo.setAction(action);
            importAuditVo.setFileId(fileVo.getId());
            importMapper.insertImportAudit(importAuditVo);

            importMap.put(importAuditVo.getId(), BatchImportStatus.RUNNING.getValue());
            int successCount = 0;
            int failedCount = 0;
            int totalCount = 0;
            /** 用来记录整个表格读取过程中的错误 */
            String error = "";
            Workbook wb = null;
            InputStream in = null;

            try {
                CiVo ciVo = ciService.getCiById(ciId);
                if (ciVo == null) {
                    throw new CiNotFoundException(ciId);
                }
                if ((ciVo.getAttrList() == null || ciVo.getAttrList().size() == 0)
                        && (ciVo.getRelList() == null || ciVo.getRelList().size() == 0)) {
                    throw new RuntimeException("ID为：" + ciVo.getId() + "的配置项类型没有配置属性信息或关系信息");
                }

                try {
                    in = FileUtil.getData(fileVo.getPath());
                    wb = WorkbookFactory.create(in);
                } catch (Exception e) {
                    throw new RuntimeException("读取文件失败，" + e.getMessage());
                }

                for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                    Sheet sheet = wb.getSheetAt(i);
                    if (sheet == null) {
                        continue;
                    } else {
                        Row headRow = sheet.getRow(sheet.getFirstRowNum());
                        List<Integer> cellIndex = new ArrayList<>();// 列数
                        Map<Integer, Object> typeMap = new HashMap<>();// 表头列数->表头属性
                        Map<String, Boolean> checkAttrMap = new HashMap<>();
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
                                        continue COLUMN;
                                    }

                                    for (AttrVo attr : ciVo.getAttrList()) {
                                        if (attr.getLabel().equals(content)) {
                                            checkAttrMap.put("attr_" + attr.getId(), true);
                                            typeMap.put(cell.getColumnIndex(), attr);
                                            cellIndex.add(cell.getColumnIndex());
                                            continue COLUMN;
                                        }
                                    }
                                    for (RelVo rel : ciVo.getRelList()) {
                                        if (rel.getFromLabel().equals(content) || rel.getToLabel().equals(content)) {
                                            checkAttrMap.put("rel_" + rel.getId(), true);
                                            typeMap.put(cell.getColumnIndex(), rel);
                                            cellIndex.add(cell.getColumnIndex());
                                            continue COLUMN;
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            throw new RuntimeException("表头为空，" + e.getMessage());
                        }
                        /*
                          【只添加】与【添加&更新】模式下，不能缺少必填属性列 【只更新】且【全局更新】模式下，不能缺少必填属性列
                         */
                        List<String> lostColumns = new ArrayList<>();
                        if (action.equals("all") || action.equals("append")
                                || (action.equals("update") && editMode == 1)) {
                            for (AttrVo attr : ciVo.getAttrList()) {
                                if (attr.getIsRequired().equals(1)
                                        && !checkAttrMap.containsKey("attr_" + attr.getId())) {
                                    lostColumns.add(attr.getLabel());
                                }
                            }
                            for (RelVo rel : ciVo.getRelList()) {
                                if (rel.getFromCiId().equals(ciVo.getId())) {
                                    if (!checkAttrMap.containsKey("rel_" + rel.getId())
                                            && rel.getToIsRequired().equals(1)) {
                                        lostColumns.add(rel.getToLabel());
                                    }
                                } else if (rel.getToCiId().equals(ciVo.getId())) {
                                    if (!checkAttrMap.containsKey("rel_" + rel.getId())
                                            && rel.getFromIsRequired().equals(1)) {
                                        lostColumns.add(rel.getFromLabel());
                                    }
                                }
                            }
                        }
                        if (CollectionUtils.isNotEmpty(lostColumns)) {
                            error = "<b class=\"text-danger\">导入模版缺少：" + Arrays.toString(lostColumns.toArray())
                                    + "</b></br>";
                            totalCount = sheet.getPhysicalNumberOfRows() - 1;
                            failedCount = totalCount;
                        }
                        if (StringUtils.isBlank(error)) {
                            totalCount += sheet.getLastRowNum();
                            for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                                /** 用来详细记录每一行每一列的错误信息，使用LinkedHashMap是为了计算列数 */
                                Map<Integer, String> errorMsgMap = new LinkedHashMap<>();
                                /** 用来记录每一行最后保存时的错误 */
                                Map<Integer, String> rowError = new LinkedHashMap<>();
                                try {
                                    Row row = sheet.getRow(r);
                                    if (row != null) {
                                        Long ciEntityId = null;// 记录下表格中的ciEntityId，用于判断配置项是否存在
                                        CiEntityTransactionVo ciEntityTransactionVo = new CiEntityTransactionVo();
                                        ciEntityTransactionVo.setCiId(ciId);
                                        if (editMode.intValue() == 1) {
                                            ciEntityTransactionVo.setEditMode(EditModeType.GLOBAL.getValue());
                                        } else if (editMode.intValue() == 0) {
                                            ciEntityTransactionVo.setEditMode(EditModeType.PARTIAL.getValue());
                                        }
                                        //List<AttrEntityTransactionVo> attrList = new ArrayList<>();
                                        // List<RelEntityTransactionVo> relList = new ArrayList<>();
                                        //ciEntityTransactionVo.setAttrEntityTransactionList(attrList);
                                        //ciEntityTransactionVo.setRelEntityTransactionList(relList);
                                        for (int ci = 0; ci < cellIndex.size(); ci++) {
                                            Cell cell = row.getCell(cellIndex.get(ci));
                                            if (cell != null) {
                                                String content = getCellContent(cell);
                                                if (StringUtils.isNotBlank(content)) {
                                                    content = content.trim();
                                                }
                                                // content = content == null ? "" : content;
                                                // content = content.trim();
                                                Object header = typeMap.get(cellIndex.get(ci));
                                                if (header instanceof String) { // 表示拿到的是ID列
                                                    if (StringUtils.isNotBlank(content)) {
                                                        try {
                                                            ciEntityId = Long.parseLong(content);
                                                            ciEntityTransactionVo
                                                                    .setCiEntityId(Long.parseLong(content));
                                                        } catch (Exception e) {
                                                            throw new RuntimeException("无法获取到ID，" + e.getMessage());
                                                        }
                                                    }
                                                } else if (header instanceof AttrVo) {// 如果是属性
                                                    AttrVo attr = (AttrVo) header;

                                                    /** 不支持导入表格、文件 */
                                                  /*  if (attr.getType().equals(AttrType.PROPERTY.getValue()) && (attr
                                                            .getPropHandler().equals(PropHandlerType.FILE.getValue())
                                                            || attr.getPropHandler()
                                                            .equals(PropHandlerType.TABLE.getValue()))) {
                                                        continue;
                                                    }*/
                                                    /** 不支持导入表达式 */
                                                  /*  if (attr.getType().equals(AttrType.EXPRESSION.getValue())) {
                                                        continue;
                                                    }*/

                                                    JSONArray valueList = new JSONArray();
                                                   /* if (StringUtils.isNotBlank(content)
                                                            && attr.getType().equals(AttrType.PROPERTY.getValue())
                                                            && StringUtils.isNotBlank(attr.getPropHandler())) {
                                                        IPropertyHandler handler =
                                                                PropertyHandlerFactory.getHandler(attr.getPropHandler());
                                                        List<String> values = new ArrayList<>();
                                                        if (content.contains(",")) {
                                                            values = Arrays.asList(content.split(","));
                                                        } else {
                                                            values.add(content);
                                                        }
                                                        JSONArray valueArray = null;
                                                        try {
                                                            valueArray = (JSONArray) handler.getActualValue(values,
                                                                    attr.getPropConfig());
                                                        } catch (Exception e) {
                                                            errorMsgMap.put(ci + 1, e.getMessage());
                                                        }
                                                        if (CollectionUtils.isNotEmpty(valueArray)) {
                                                            valueList = valueArray.stream().map(v -> v.toString())
                                                                    .collect(Collectors.toList());
                                                        }
                                                    } else if (StringUtils.isNotBlank(content)) {
                                                        valueList.add(content);
                                                    }*/

                                                    AttrEntityTransactionVo attrEntity = new AttrEntityTransactionVo();
                                                    attrEntity.setAttrId(attr.getId());
                                                    attrEntity.setAttrName(attr.getName());
                                                    attrEntity.setValueList(valueList);
                                                    /**
                                                     * 没有取到值且没有采集到异常，说明单元格内容为空 如果是【只添加】，那所有必填属性都不能为空
                                                     * 如果是【只更新】且【全局更新】：所有必填属性不能为空 如果是【添加&更新】，没有ID的，必填属性不能为空；
                                                     * 有ID的，且选择了【全局更新】，则必填属性不能为空
                                                     */
                                                    if (Objects.equals(attr.getIsRequired(), 1)
                                                            && CollectionUtils.isEmpty(attrEntity.getValueList())
                                                            && MapUtils.isEmpty(errorMsgMap)) {
                                                        checkAttrIsRequired(errorMsgMap, ciEntityId,
                                                                ciEntityTransactionVo, ci, attr);
                                                    } else {
                                                        //attrList.add(attrEntity);
                                                    }
                                                    // if(attr.getIsRequired().equals(1) &&
                                                    // ciEntityTransactionVo.getEditMode().equals(EditModeType.GLOBAL.getValue())
                                                    // && CollectionUtils.isEmpty(attrEntity.getActualValueList())) {
                                                    // errorMsgMap.put(ci+1, "请补充“" + attr.getLabel() + "”信息");
                                                    // } else if (attrEntity.getActualValueList().size() > 0) {
                                                    // attrList.add(attrEntity);
                                                    // }

                                                } else if (header instanceof RelVo) {
                                                    RelVo rel = (RelVo) header;
                                                    List<String> valueList = null;
                                                    if (StringUtils.isNotBlank(content)) {
                                                        if (content.contains(",")) {
                                                            String[] split = content.split(",");
                                                            valueList = Arrays.asList(split);
                                                        } else {
                                                            valueList = new ArrayList();
                                                            valueList.add(content);
                                                        }
                                                    }

                                                    if (CollectionUtils.isEmpty(valueList)) {
                                                        checkRelIsRequired(ciVo, errorMsgMap, ciEntityId,
                                                                ciEntityTransactionVo, ci, rel);
                                                    } else {
                                                        if (rel.getFromCiId().equals(ciVo.getId())) { // 当前配置项处于from位置
                                                            Long toCiId = rel.getToCiId();
                                                            // 根据content查询配置项ID
                                                            for (String o : valueList) {
                                                                Long id = ciEntityMapper.getIdByCiIdAndName(toCiId, o);
                                                                if (id != null) {
                                                                    RelEntityTransactionVo relEntity =
                                                                            new RelEntityTransactionVo();
                                                                    relEntity.setToCiEntityId(id);
                                                                    relEntity.setRelId(rel.getId());
                                                                    relEntity
                                                                            .setDirection(RelDirectionType.FROM.getValue());
                                                                    relEntity.setFromCiEntityId(
                                                                            ciEntityTransactionVo.getCiEntityId());
                                                                    relEntity
                                                                            .setAction(RelActionType.INSERT.getValue());
                                                                    //relList.add(relEntity);
                                                                } else {
                                                                    errorMsgMap.put(ci + 1, "配置项：" + o + "不存在");
                                                                }
                                                            }
                                                        } else if (rel.getToCiId().equals(ciVo.getId())) { // 当前配置项处于to位置
                                                            Long fromCiId = rel.getFromCiId();
                                                            // 根据content查询配置项ID
                                                            for (String o : valueList) {
                                                                Long id =
                                                                        ciEntityMapper.getIdByCiIdAndName(fromCiId, o);
                                                                if (id != null) {
                                                                    RelEntityTransactionVo relEntity =
                                                                            new RelEntityTransactionVo();
                                                                    relEntity.setFromCiEntityId(id);
                                                                    relEntity.setRelId(rel.getId());
                                                                    relEntity
                                                                            .setDirection(RelDirectionType.TO.getValue());
                                                                    relEntity.setToCiEntityId(
                                                                            ciEntityTransactionVo.getCiEntityId());
                                                                    relEntity
                                                                            .setAction(RelActionType.INSERT.getValue());
                                                                    //relList.add(relEntity);
                                                                } else {
                                                                    errorMsgMap.put(ci + 1, "配置项：" + o + "不存在");
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            } else {
                                                Object header = typeMap.get(cellIndex.get(ci));
                                                if (header instanceof AttrVo) {
                                                    AttrVo attr = (AttrVo) header;
                                                    if (Objects.equals(attr.getIsRequired(), 1)) {
                                                        checkAttrIsRequired(errorMsgMap, ciEntityId,
                                                                ciEntityTransactionVo, ci, attr);
                                                    }
                                                    // if (attr.getIsRequired().equals(1) &&
                                                    // ciEntityTransactionVo.getEditMode().equals(EditModeType.GLOBAL.getValue()))
                                                    // {
                                                    // errorMsgMap.put(ci+1,"请补充“" + attr.getLabel() + "”信息");
                                                    // }
                                                } else if (header instanceof RelVo) {
                                                    RelVo rel = (RelVo) header;
                                                    /** 校验关系必填 */
                                                    checkRelIsRequired(ciVo, errorMsgMap, ciEntityId,
                                                            ciEntityTransactionVo, ci, rel);
                                                }
                                            }
                                        }

                                        try {
                                            /** 没有采集到异常才执行保存，保存过程中发生异常再put到errMsgMap中 */
                                            if (action.equals("append") && ciEntityId == null) {
                                                if (MapUtils.isEmpty(errorMsgMap)) {
                                                    ciEntityTransactionVo
                                                            .setTransactionMode(TransactionActionType.INSERT);
                                                    ciEntityService.saveCiEntity(ciEntityTransactionVo);
                                                    successCount += 1;
                                                } else {
                                                    failedCount += 1;
                                                }
                                            } else if (action.equals("update") && ciEntityId != null) {
                                                //FIXME 需补充新逻辑
                                                CiEntityVo entity = ciEntityService.getCiEntityById(ciEntityId);
                                                if (entity == null) {
                                                    throw new RuntimeException("配置项：" + ciEntityId + "不存在");
                                                }
                                                if (MapUtils.isEmpty(errorMsgMap)) {
                                                    ciEntityTransactionVo
                                                            .setTransactionMode(TransactionActionType.UPDATE);
                                                    ciEntityService.saveCiEntity(ciEntityTransactionVo);
                                                    successCount += 1;
                                                } else {
                                                    failedCount += 1;
                                                }
                                            } else if (action.equals("all")) {
                                                if (ciEntityId != null) {
                                                    CiEntityVo entity = ciEntityService.getCiEntityById(ciEntityId);
                                                    if (entity == null) {
                                                        throw new RuntimeException("配置项：" + ciEntityId + "不存在");
                                                    }
                                                    if (MapUtils.isEmpty(errorMsgMap)) {
                                                        ciEntityTransactionVo
                                                                .setTransactionMode(TransactionActionType.UPDATE);
                                                        ciEntityService.saveCiEntity(ciEntityTransactionVo);
                                                        successCount += 1;
                                                    } else {
                                                        failedCount += 1;
                                                    }
                                                } else {
                                                    if (MapUtils.isEmpty(errorMsgMap)) {
                                                        ciEntityTransactionVo
                                                                .setTransactionMode(TransactionActionType.INSERT);
                                                        ciEntityService.saveCiEntity(ciEntityTransactionVo);
                                                        successCount += 1;
                                                    } else {
                                                        failedCount += 1;
                                                    }
                                                }
                                            } else {
                                                throw new RuntimeException("请正确填写模版与选择导入模式，【只添加】不需要填写ID；【只更新】必须填写ID");
                                            }
                                        } catch (Exception e) {
                                            failedCount += 1;
                                            rowError.put(r, e.getMessage());
                                        }
                                    }
                                } catch (Exception e) {
                                    failedCount += 1;
                                    rowError.put(r, e.getMessage());
                                } finally {
                                    // String err = "";
                                    List<Integer> columnList = new ArrayList<>();
                                    List<String> errorMsgList = new ArrayList<>();
                                    for (Entry<Integer, String> _err : errorMsgMap.entrySet()) {
                                        columnList.add(_err.getKey());
                                        errorMsgList.add(_err.getValue());
                                    }
                                    String errMsg = Arrays.toString(errorMsgList.toArray());
                                    String newerrMsg = errMsg.replace(',', ';');
                                    if (CollectionUtils.isNotEmpty(columnList)) {
                                        error += "</br><b class=\"text-danger\">第" + r + "行第"
                                                + Arrays.toString(columnList.toArray()) + "列</b>：" + newerrMsg;
                                    }

                                    if (MapUtils.isNotEmpty(rowError)) {
                                        for (Map.Entry<Integer, String> entry : rowError.entrySet()) {
                                            error += "</br><b class=\"text-danger\">第" + entry.getKey() + "行："
                                                    + entry.getValue() + "</b>";
                                        }
                                    }

                                    importAuditVo.setSuccessCount(successCount);
                                    importAuditVo.setFailedCount(failedCount);
                                    importAuditVo.setTotalCount(totalCount);
                                    importAuditVo.setError(StringUtils.isNotBlank(error) ? error : null);
                                    importMapper.updateImportAuditTemporary(importAuditVo);

                                    // if (failedCount == 1) {
                                    // importAuditVo.setError(err);
                                    // } else if(failedCount > 1){
                                    // importAuditVo.setError(importAuditVo.getError() + "<br>" + err);
                                    // }
                                    // importAuditVo.setSuccessCount(successCount);
                                    // importAuditVo.setFailedCount(failedCount);
                                    // importAuditVo.setTotalCount(totalCount);
                                    // importMapper.updateImportAuditTemporary(importAuditVo);
                                }
                            }
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                importAuditVo.setError((("".equals(importAuditVo.getError()) || importAuditVo.getError() == null) ? ""
                        : importAuditVo.getError() + "<br>") + e.getMessage());
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
                if (StringUtils.isNotBlank(error)) {
                    importAuditVo.setError(error);
                }
                if (BatchImportStatus.STOPPED.getValue().equals(importMap.get(importAuditVo.getId()))) {
                    importAuditVo.setError((("".equals(importAuditVo.getError()) || importAuditVo.getError() == null)
                            ? "" : importAuditVo.getError() + "<br>") + "<b class=\"text-danger\">导入已停止</b>。");
                }
                importAuditVo.setSuccessCount(successCount);
                importAuditVo.setFailedCount(failedCount);
                importAuditVo.setTotalCount(totalCount);
                importMapper.updateImportAudit(importAuditVo);
                importMap.replace(importAuditVo.getId(), BatchImportStatus.SUCCEED.getValue());
                importMap.remove(importAuditVo.getId());
            }
        }

        private void checkAttrIsRequired(Map<Integer, String> errorMsgMap, Long ciEntityId,
                                         CiEntityTransactionVo ciEntityTransactionVo, int ci, AttrVo attr) {
            if ("append".equals(action)
                    || "update".equals(action) && ciEntityTransactionVo.getEditMode().equals(EditModeType.GLOBAL.getValue())
                    || "all".equals(action) && ciEntityId == null
                    || "all".equals(action) && ciEntityTransactionVo.getEditMode().equals(EditModeType.GLOBAL.getValue())) {
                errorMsgMap.put(ci + 1, "请补充“" + attr.getLabel() + "”信息");
            }
        }

        private void checkRelIsRequired(CiVo ciVo, Map<Integer, String> errorMsgMap, Long ciEntityId,
                                        CiEntityTransactionVo ciEntityTransactionVo, int ci, RelVo rel) {
            if ("append".equals(action)
                    || "update".equals(action) && ciEntityTransactionVo.getEditMode().equals(EditModeType.GLOBAL.getValue())
                    || "all".equals(action) && ciEntityId == null
                    || "all".equals(action) && ciEntityTransactionVo.getEditMode().equals(EditModeType.GLOBAL.getValue())) {
                if (rel.getFromCiId().equals(ciVo.getId())) { // 当前CI处于from
                    if (rel.getToIsRequired().equals(1)) {
                        errorMsgMap.put(ci + 1, "缺少" + rel.getToLabel());
                    }
                } else if (rel.getToCiId().equals(ciVo.getId())) {// 当前CI处于to
                    if (rel.getFromIsRequired().equals(1)) {
                        errorMsgMap.put(ci + 1, "缺少" + rel.getFromLabel());
                    }
                }
            }
        }
    }

    public static ImportAuditVo getStatusById(Long id) {
        ImportAuditVo importAuditVo = importMapper.getImportAuditById(id);
        if (importAuditVo.getStatus() == 0 && importMap.get(id) == null) {
            importAuditVo.setStatus(-1);
            importAuditVo
                    .setError((StringUtils.isNotBlank(importAuditVo.getError()) ? "" : importAuditVo.getError() + "<br>")
                            + "<b class=\"text-danger\">发生异常，导入中断</b>。");
            importMapper.updateImportAuditStatus(importAuditVo);
        }
        return importAuditVo;
    }

    public static void stopImportById(Long id) {
        importMap.replace(id, BatchImportStatus.STOPPED.getValue());
    }
}
