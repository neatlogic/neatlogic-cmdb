/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.attrvaluehandler.handler;

import codedriver.framework.cmdb.attrvaluehandler.core.IAttrValueHandler;
import codedriver.framework.cmdb.dto.attrexpression.RebuildAuditVo;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.dto.ci.RelVo;
import codedriver.framework.cmdb.enums.RelDirectionType;
import codedriver.framework.cmdb.enums.SearchExpression;
import codedriver.module.cmdb.attrexpression.AttrExpressionRebuildManager;
import codedriver.module.cmdb.dao.mapper.ci.AttrMapper;
import codedriver.module.cmdb.dao.mapper.ci.RelMapper;
import codedriver.module.cmdb.utils.RelUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;


@Service
public class ExpressionValueHandler implements IAttrValueHandler {
    @Resource
    private RelMapper relMapper;

    @Resource
    private AttrMapper attrMapper;

    @Override
    public String getType() {
        return "expression";
    }

    @Override
    public String getName() {
        return "表达式";
    }

    @Override
    public String getIcon() {
        return "tsfont-script";
    }

    @Override
    public boolean isCanSearch() {
        return true;
    }

    @Override
    public boolean isCanInput() {
        return false;
    }

    @Override
    public boolean isCanImport() {
        return false;
    }

    @Override
    public boolean isSimple() {
        return true;
    }

    @Override
    public boolean isNeedTargetCi() {
        return false;
    }

    @Override
    public boolean isNeedConfig() {
        return true;
    }

    @Override
    public boolean isNeedWholeRow() {
        return false;
    }

    @Override
    public SearchExpression[] getSupportExpression() {
        return new SearchExpression[]{SearchExpression.EQ, SearchExpression.NE, SearchExpression.LI,
                SearchExpression.NL, SearchExpression.NOTNULL, SearchExpression.NULL};
    }

    @Override
    public int getSort() {
        return 8;
    }

    @Override
    public void afterInsert(AttrVo attrVo) {
        JSONObject config = attrVo.getConfig();
        if (config.containsKey("expression") && config.get("expression") instanceof JSONArray) {
            JSONArray expressionList = config.getJSONArray("expression");
            List<RelVo> relList = RelUtil.ClearRepeatRel(relMapper.getRelByCiId(attrVo.getCiId()));
            for (int i = 0; i < expressionList.size(); i++) {
                String expression = expressionList.getString(i);
                if (expression.startsWith("{") && expression.endsWith("}")) {
                    expression = expression.substring(1, expression.length() - 1);
                    long attrId;
                    long relId;
                    if (expression.contains(".")) {
                        //关系属性
                        relId = Long.parseLong(expression.split("\\.")[0]);
                        attrId = Long.parseLong(expression.split("\\.")[1]);
                        if (CollectionUtils.isNotEmpty(relList)) {
                            Optional<RelVo> op = relList.stream().filter(rel -> rel.getId().equals(relId)).findFirst();
                            if (op.isPresent()) {
                                RelVo relVo = op.get();
                                attrMapper.insertAttrExpressionRel(attrVo.getCiId(), attrVo.getId(), (relVo.getDirection().equals(RelDirectionType.FROM.getValue()) ? relVo.getToCiId() : relVo.getFromCiId()), attrId);
                            }
                        }
                    } else {
                        attrId = Long.parseLong(expression);
                        attrMapper.insertAttrExpressionRel(attrVo.getCiId(), attrVo.getId(), attrVo.getCiId(), attrId);
                    }
                }
            }
            //加入重建序列
            RebuildAuditVo rebuildAuditVo = new RebuildAuditVo();
            rebuildAuditVo.setCiId(attrVo.getCiId());
            rebuildAuditVo.setAttrIds(attrVo.getId().toString());
            AttrExpressionRebuildManager.rebuild(rebuildAuditVo);
        }
    }

    @Override
    public void afterUpdate(AttrVo attrVo) {
        attrMapper.deleteAttrExpressionRelByExpressionAttrId(attrVo.getId());
        afterInsert(attrVo);
    }

    @Override
    public void afterDelete(AttrVo attrVo) {
        attrMapper.deleteAttrExpressionRelByExpressionAttrId(attrVo.getId());
    }

}
