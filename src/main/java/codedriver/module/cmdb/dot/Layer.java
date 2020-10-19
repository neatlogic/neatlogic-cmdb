package codedriver.module.cmdb.dot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class Layer {
    List<Node> nodeList;
    private String id;
    private Map<String, String> propMap = new HashMap<>();

    private Layer(Builder builder) {
        nodeList = builder.nodeList;
        this.id = builder.id;
        propMap.put("id", builder.id);
        if (StringUtils.isNotBlank(builder.label)) {
            propMap.put("label", builder.label);
        }
        if (StringUtils.isNotBlank(builder.className)) {
           propMap.put("class", builder.className);
        }
        if (StringUtils.isNotBlank(builder.tooltip)) {
            propMap.put("tooltip", builder.tooltip);
        }
        if (StringUtils.isNotBlank(builder.fontColor)) {
            propMap.put("fontcolor", builder.fontColor);
        }
    }

    public String getId() {
        return id;
    }

    public String toString() {
        Iterator<String> itKey = propMap.keySet().iterator();
        String propString = "";
        while (itKey.hasNext()) {
            String key = itKey.next();
            if (StringUtils.isNotBlank(propString)) {
                propString += ",";
            }
            propString += key + "=\"" + propMap.get(key) + "\"";
        }
        String str = "rank=same;\n";
        str += "\"" + this.id + "\"[shape=\"none\"," + propString + "];\n";
        if (CollectionUtils.isNotEmpty(nodeList)) {
            str += nodeList.stream().map(n -> n.toString().trim()).collect(Collectors.joining(";\n"));
        }
        return str;
    }

    public static class Builder {
        private List<Node> nodeList = new ArrayList<>();
        private String id;
        private String label;
        private String className = "layer";
        private String tooltip;
        private String fontColor;

        public Builder withFontColor(String _fontcolor) {
            this.fontColor = _fontcolor;
            return this;
        }

        public Builder(String id) {
            this.id = id;
        }

        public Builder withLabel(String _label) {
            this.label = _label;
            return this;
        }

        public Builder withClass(String _class) {
            this.className = _class;
            return this;
        }

        public Builder withTooltip(String _tooltip) {
            this.tooltip = _tooltip;
            return this;
        }

        public Builder addNode(Node node) {
            if (node != null) {
                this.nodeList.add(node);
            }
            return this;
        }

        public Layer build() {
            return new Layer(this);
        }
    }
}
