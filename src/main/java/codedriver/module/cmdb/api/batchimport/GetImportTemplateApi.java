package codedriver.module.cmdb.api.batchimport;

import codedriver.framework.cmdb.constvalue.RelRuleType;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.core.constvalue.OperationTypeEnum;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import codedriver.module.cmdb.dto.ci.AttrVo;
import codedriver.module.cmdb.dto.ci.CiVo;
import codedriver.module.cmdb.dto.ci.RelVo;
import codedriver.module.cmdb.exception.ci.CiNotFoundException;
import codedriver.module.cmdb.service.ci.CiService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetImportTemplateApi extends PrivateBinaryStreamApiComponentBase {

    static Logger logger = LoggerFactory.getLogger(GetImportTemplateApi.class);

    @Autowired
    private CiService ciService;


    @Override
    public String getToken() {
        return "/cmdb/import/template/get";
    }

    @Override
    public String getName() {
        return "下载配置项导入模板";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "模型id"),
            @Param(name = "attrIdList", type = ApiParamType.JSONARRAY, desc = "模型属性ID列表"),
            @Param(name = "relIdList", type = ApiParamType.JSONARRAY, desc = "模型关系ID列表"),
    })
    @Output({})
    @Description(desc = "下载配置项导入模板")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        HSSFWorkbook wb = null;
        OutputStream os = null;
        try {
            Long ciId = paramObj.getLong("ciId");
            JSONArray attrIdArray = paramObj.getJSONArray("attrIdList");
            JSONArray relIdArray = paramObj.getJSONArray("relIdList");
            List<Long> attrIdList = null;
            List<Long> relIdList = null;
            if(CollectionUtils.isNotEmpty(attrIdArray)){
                attrIdList = attrIdArray.toJavaList(Long.class);
            }
            if(CollectionUtils.isNotEmpty(relIdArray)){
                relIdList = relIdArray.toJavaList(Long.class);
            }

            if (CollectionUtils.isEmpty(attrIdList) && CollectionUtils.isEmpty(relIdList)) {
                throw new RuntimeException("模型属性不能为空");
            }

            CiVo ciVo = ciService.getCiById(ciId);
            if(ciVo == null){
                throw new CiNotFoundException(ciId);
            }

            wb = new HSSFWorkbook();
            HSSFSheet sheet = wb.createSheet("data");
            String fileName = ciVo.getId() + "_" + ciVo.getLabel() + ".xls";

            int ciTypeAttrListSize = ciVo.getAttrList().size() + ciVo.getRelList().size() + 1;
            // 设置excel每列宽度
            for (int i = 0; i < ciTypeAttrListSize; i++) {
                sheet.setColumnWidth(i, 5000);
            }

            // 创建字体样式
            HSSFFont font = wb.createFont();
            font.setFontName("宋体");
            font.setFontHeightInPoints((short) 10);
            font.setColor(HSSFColor.HSSFColorPredefined.WHITE.getIndex());

            // 创建单元格样式
            HSSFPalette palette = wb.getCustomPalette();
            palette.setColorAtIndex((short) 8, (byte) (0xff & 2), (byte) (0xff & 159), (byte) (0xff & 228));
            HSSFCellStyle style = wb.createCellStyle();
            style.setAlignment(HorizontalAlignment.CENTER);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            style.setFillForegroundColor((short) 8);
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // 设置边框
            style.setBottomBorderColor(HSSFColor.HSSFColorPredefined.BLACK.getIndex());
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            style.setBorderTop(BorderStyle.THIN);
            style.setFont(font);// 设置字体

            // 创建Excel的sheet的一行
            int i = 0;
            HSSFRow row = sheet.createRow(0);
            row.setHeight((short) 300);// 设定行的高度
            HSSFCell idcell = row.createCell(i);
            idcell.setCellStyle(style);
            idcell.setCellValue("id");
            i++;
            /** 属性 */
            if(CollectionUtils.isNotEmpty(ciVo.getAttrList()) && CollectionUtils.isNotEmpty(attrIdList)){
                for (AttrVo attr : ciVo.getAttrList()) {
                    if (attrIdList.contains(attr.getId())) {
                        String label = attr.getLabel();
                        Integer isRequired = attr.getIsRequired();
                        Integer isUnique = attr.getIsUnique();
                        StringBuilder ciTypeAttrNameDesc = new StringBuilder();
                        if (isUnique.equals(1)) {
                            ciTypeAttrNameDesc.append("唯一");
                        }
                        if (isRequired.equals(1)) {
                            if (ciTypeAttrNameDesc.length() > 0) {
                                ciTypeAttrNameDesc.append("|");
                            }
                            ciTypeAttrNameDesc.append("必填");
                        }

                        if (ciTypeAttrNameDesc.length() > 0) {
                            label = label + "[(" + ciTypeAttrNameDesc.toString() + ")]";
                        }
                        HSSFCell cell = row.createCell(i);
                        cell.setCellStyle(style);
                        cell.setCellValue(label);
                        i++;
                    }
                }
            }
            /** 关系 */
            if(CollectionUtils.isNotEmpty(ciVo.getRelList()) && CollectionUtils.isNotEmpty(relIdList)){
                for (RelVo rel : ciVo.getRelList()) {
                    if (relIdList.contains(rel.getId())) {
                        if(rel.getFromCiId().equals(ciVo.getId())){ //当前CI处于from
                            String label = rel.getToLabel();
                            if(rel.getToRule().equals(RelRuleType.OO.getValue()) || rel.getToRule().equals(RelRuleType.ON.getValue())){
                                label = label + "[(必填)]";
                            }
                            HSSFCell cell = row.createCell(i);
                            cell.setCellStyle(style);
                            cell.setCellValue(label);
                            i++;
                        }else if(rel.getToCiId().equals(ciVo.getId())){//当前CI处于to
                            String label = rel.getFromLabel();
                            if(rel.getFromRule().equals(RelRuleType.OO.getValue()) || rel.getFromRule().equals(RelRuleType.ON.getValue())){
                                label = label + "[(必填)]";
                            }
                            HSSFCell cell = row.createCell(i);
                            cell.setCellStyle(style);
                            cell.setCellValue(label);
                            i++;
                        }
                    }
                }
            }

            Boolean flag = request.getHeader("User-Agent").indexOf("Gecko") > 0;
            if (request.getHeader("User-Agent").toLowerCase().indexOf("msie") > 0 || flag) {
                fileName = URLEncoder.encode(fileName, "UTF-8");// IE浏览器
            } else {
                fileName = new String(fileName.replace(" ", "").getBytes(StandardCharsets.UTF_8), "ISO8859-1");
            }
            response.setContentType("application/vnd.ms-excel;charset=utf-8");
            response.setHeader("Content-Disposition", "attachment;fileName=\"" + fileName + "\"");
            os = response.getOutputStream();
            wb.write(os);
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        } finally {
            if (wb != null) {
                try {
                    wb.close();
                } catch (IOException ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
            if (os != null) {
                os.flush();
                os.close();
            }
        }
        return null;
    }
}
