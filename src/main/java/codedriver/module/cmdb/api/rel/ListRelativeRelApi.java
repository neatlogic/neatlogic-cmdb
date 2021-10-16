/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.rel;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.ci.RelVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.ValueTextVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dao.mapper.ci.RelMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListRelativeRelApi extends PrivateApiComponentBase {

    @Autowired
    private RelMapper relMapper;

    @Autowired
    private CiMapper ciMapper;

    @Override
    public String getToken() {
        return "/cmdb/relative/rel/list";
    }

    @Override
    public String getName() {
        return "获取关联关系列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    class RelPathVo {
        private String key;
        private String path;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public RelPathVo(String key, String path) {
            /*
            记录关系和模型，数据格式:
            relId:ciId
             */
            this.key = key;
            /*
            记录完整的关系路径，数据格式：
            relId:ciId>relId:ciId<relId:ciId 箭头代表关系方向，由于不同关系可以引用相同的ciId，所以需要带上relId作为区分
             */
            this.path = path;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RelPathVo relPathVo = (RelPathVo) o;
            return key.equals(relPathVo.key);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key);
        }
    }

    private void findCiPathRecursive(String currentPath, Long currentCiId, Set<RelPathVo> pathSet, List<RelVo> relList) {
        List<RelVo> fromRelList = relList.stream().filter(d -> d.getToCiId().equals(currentCiId)).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(fromRelList)) {
            fromRelList.forEach(d -> {
                String newPath = currentPath + "<" + d.getId() + ":" + d.getFromCiId();
                RelPathVo relPathVo = new RelPathVo(d.getId() + ":" + d.getFromCiId(), newPath);
                if (!pathSet.contains(relPathVo)) {
                    pathSet.add(relPathVo);
                    findCiPathRecursive(newPath, d.getFromCiId(), pathSet, relList);
                }
            });
        }

        List<RelVo> toRelList = relList.stream().filter(d -> d.getFromCiId().equals(currentCiId)).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(toRelList)) {
            toRelList.forEach(d -> {
                String newPath = currentPath + ">" + d.getId() + ":" + d.getToCiId();
                RelPathVo relPathVo = new RelPathVo(d.getId() + ":" + d.getToCiId(), newPath);
                if (!pathSet.contains(relPathVo)) {
                    pathSet.add(relPathVo);
                    findCiPathRecursive(newPath, d.getToCiId(), pathSet, relList);
                }
            });
        }
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "模型id"),
            @Param(name = "relId", type = ApiParamType.LONG, desc = "关系id,为空代表新关系"),
            @Param(name = "fromCiId", type = ApiParamType.LONG, isRequired = true, desc = "当前关系来源模型id"),
            @Param(name = "toCiId", type = ApiParamType.LONG, isRequired = true, desc = "当前关系目标模型id")})
    @Output({@Param(explode = ValueTextVo[].class)})
    @Description(desc = "获取关联关系列表接口，配置关系的关联时使用")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long ciId = paramObj.getLong("ciId");
        Long relId = paramObj.getLong("relId");
        Long fromCiId = paramObj.getLong("fromCiId");
        Long toCiId = paramObj.getLong("toCiId");
        //List<RelVo> relList = relMapper.getRelByCiId(ciId);
        List<RelVo> relList = relMapper.getAllRelList();

        if (relId != null) {
            relList.removeIf(d -> d.getId().equals(relId));
        }

        if (CollectionUtils.isNotEmpty(relList)) {

            CiVo fromCiVo = ciMapper.getCiById(fromCiId);

            Set<RelPathVo> fromPathSet = new HashSet<>();
            findCiPathRecursive("", fromCiVo.getId(), fromPathSet, relList);

            System.out.println("from path:");
            fromPathSet.forEach(d -> {
                System.out.println(d.getPath());
            });


            CiVo toCiVo = ciMapper.getCiById(toCiId);
            Set<RelPathVo> toPathSet = new HashSet<>();
            findCiPathRecursive("", toCiVo.getId(), toPathSet, relList);

            System.out.println("to path:");
            toPathSet.forEach(d -> {
                System.out.println(d.getPath());
            });

            if (CollectionUtils.isNotEmpty(fromPathSet) && CollectionUtils.isNotEmpty(toPathSet)) {
                Set<String> possibleIdSet = new HashSet<>();
                fromPathSet.forEach(f -> {
                    toPathSet.forEach(t -> {
                        possibleIdSet.add(f.getKey().split(":")[1] + "-" + t.getKey().split(":")[1]);
                    });
                });
                List<RelVo> matchRelList = relList.stream().filter(d -> possibleIdSet.contains(d.getFromCiId() + "-" + d.getToCiId())).collect(Collectors.toList());
                List<ValueTextVo> returnList = new ArrayList<>();
                if (CollectionUtils.isNotEmpty(matchRelList)) {
                    relList.forEach(d -> returnList.add(new ValueTextVo(d.getId(), d.getFromLabel() + "->" + d.getToLabel())));
                }
                return returnList;
            }
        }
        return null;
    }
}
