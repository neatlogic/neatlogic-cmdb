/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.cmdb.attrvaluehandler.handler;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.cmdb.attrvaluehandler.core.IAttrValueHandler;
import neatlogic.framework.cmdb.dto.attrexpression.RebuildAuditVo;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.enums.RelDirectionType;
import neatlogic.framework.cmdb.enums.SearchExpression;
import neatlogic.framework.cmdb.utils.RelUtil;
import neatlogic.module.cmdb.attrexpression.AttrExpressionRebuildManager;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.dao.mapper.ci.RelMapper;
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
    public boolean isCanSort() {
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
        return 14;
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
                    String direction;
                    if (expression.contains(".")) {
                        //关系属性
                        relId = Long.parseLong(expression.split("\\.")[0]);
                        attrId = Long.parseLong(expression.split("\\.")[1]);
                        direction = expression.split("\\.")[2];
                        if (CollectionUtils.isNotEmpty(relList)) {
                            Optional<RelVo> op = relList.stream().filter(rel -> rel.getId().equals(relId) && rel.getDirection().equals(direction)).findFirst();
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
