package codedriver.module.cmdb.dot;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Title: Group
 * @Package: codedriver.module.cmdb.dot
 * @Description: 分组
 * @author: chenqiwei
 * @date: 2021/1/162:06 下午
 * Copyright(c) 2021 TechSure Co.,Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 **/
public class Cluster {
    private final List<Node> nodeList;
    private final Map<String, String> propMap = new HashMap<>();
    private final String id;

    public Cluster(Builder builder) {
        this.nodeList = builder.nodeList;
        this.id = builder.id;
        propMap.put("labeljust", "l");
        if (StringUtils.isNotBlank(builder.label)) {
            propMap.put("label", builder.label);
        }
        if (StringUtils.isNotBlank(builder.style)) {
            propMap.put("style", builder.style);
        }
        if (StringUtils.isNotBlank(builder.label)) {
            propMap.put("label", builder.label);
        }
    }

    public String toString() {
        StringBuilder str = new StringBuilder();
        if (CollectionUtils.isNotEmpty(nodeList) && nodeList.size() > 1) {
            str = new StringBuilder("subgraph " + this.id + " {");
            for (String key : propMap.keySet()) {
                str.append(key).append("=\"").append(propMap.get(key)).append("\";\n");
            }
            str.append(nodeList.stream().map(n -> "\"" + n.getId() + "\"").collect(Collectors.joining(";\n")));
            str.append("}");
        }
        return str.toString();
    }

    public static class Builder {
        private final List<Node> nodeList = new ArrayList<>();

        private final String id;
        private String label;
        private String style;

        public Builder(String _id) {
            this.id = _id;
        }

        public Cluster.Builder withLabel(String _label) {
            this.label = _label;
            return this;
        }


        public Cluster.Builder withStyle(String _style) {
            this.style = _style;
            return this;
        }

        public Cluster.Builder addNode(Node node) {
            if (node != null) {
                if (!nodeList.contains(node)) {
                    this.nodeList.add(node);
                }
            }
            return this;
        }

        public Cluster build() {
            return new Cluster(this);
        }

    }


}
