/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.cmdb.attrvaluehandler.handler;

import neatlogic.framework.cmdb.attrvaluehandler.core.IAttrValueHandler;
import neatlogic.framework.cmdb.dto.attrexpression.RebuildAuditVo;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.enums.RelDirectionType;
import neatlogic.framework.cmdb.enums.SearchExpression;
import neatlogic.module.cmdb.attrexpression.AttrExpressionRebuildManager;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.dao.mapper.ci.RelMapper;
import neatlogic.framework.cmdb.utils.RelUtil;
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
        return 13;
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
