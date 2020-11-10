package codedriver.module.cmdb.api.batchimport;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import codedriver.module.cmdb.dto.ci.AttrVo;
import codedriver.module.cmdb.dto.ci.CiVo;
import codedriver.module.cmdb.dto.ci.RelVo;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.List;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetImportTemplateApi extends PrivateBinaryStreamApiComponentBase {

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

    // TODO 参数描述
    @Input({
            @Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "模型id"),
            @Param(name = "attrIdList", type = ApiParamType.JSONARRAY, desc = ""),
            @Param(name = "relIdList", type = ApiParamType.JSONARRAY, desc = ""),
    })
    @Output({})
    @Description(desc = "下载配置项导入模板")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        HSSFWorkbook wb = null;
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


            if ((attrIdList.size() + relIdList.size()) < 1) {
                throw new RuntimeException("模型属性不能为空");
            }

            // TODO 无法确定relList是否获取正确
            CiVo ciVo = ciService.getCiById(ciId);

            wb = new HSSFWorkbook();
            HSSFSheet sheet = wb.createSheet("data");
            String excelTitle = ciVo.getId() + "_" + ciVo.getName();

            String fileNameEncode = "";
            String userAgent = request.getHeader("User-Agent").toUpperCase();

            if (userAgent.indexOf("MSIE") > 0) {
                fileNameEncode = URLEncoder.encode(excelTitle, "UTF-8");// IE浏览器
            } else {
                fileNameEncode = new String(excelTitle.getBytes("UTF-8"), "ISO8859-1");// 非IE浏览器
            }
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment;fileName=" + fileNameEncode + ".xls");

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

            for (RelVo rel : ciVo.getRelList()) {
                if (relIdList.contains(rel.getId())) {
                    // TODO label与isRequired无法确定取值
                    String label = rel.getToLabel();
//                    Integer isRequired = rel.getIsRequired();
                    StringBuilder ciTypeAttrNameDesc = new StringBuilder();
//                    if (isRequired.equals(1)) {
//                        if (ciTypeAttrNameDesc.length() > 0) {
//                            ciTypeAttrNameDesc.append("|");
//                        }
//                        ciTypeAttrNameDesc.append("必填");
//                    }

                    if (ciTypeAttrNameDesc.length() > 0) {
                        label = label + "[(" + ciTypeAttrNameDesc.toString() + ")]";
                    }
                    HSSFCell cell = row.createCell(i);
                    cell.setCellStyle(style);
                    cell.setCellValue(label);
                    i++;
                }
            }

            OutputStream os = response.getOutputStream();
            wb.write(os);
            os.close();
        } catch (IOException ex) {
//            logger.error(ex.getMessage(), ex);
        } finally {
            if (wb != null) {
                try {
                    wb.close();
                } catch (IOException ex) {
//                    logger.error(ex.getMessage(), ex);
                }
            }
        }
        return null;
    }
}
