package codedriver.module.cmdb.dot;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

public class Node {
    private final Map<String, String> propMap;
    private final String id;

    private Node(Builder builder) {
        this.id = builder.id;
        propMap = new HashMap<>();
        propMap.put("id", builder.id);
        if (StringUtils.isNotBlank(builder.label)) {
            propMap.put("label", builder.label);
        }
        if (StringUtils.isNotBlank(builder.tooltip)) {
            propMap.put("tooltip", builder.tooltip);
        }
        if (StringUtils.isNotBlank(builder.className)) {
            propMap.put("class", builder.className);
        }
        if (StringUtils.isNotBlank(builder.fontColor)) {
            propMap.put("fontcolor", builder.fontColor);
        }
        if (StringUtils.isNotBlank(builder.image)) {
            propMap.put("image", builder.image);
        }
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return id.equals(node.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public String toString() {
        Iterator<String> itKey = propMap.keySet().iterator();
        String propString = "";
        while (itKey.hasNext()) {
            String key = itKey.next();
            if (StringUtils.isNotBlank(propString)) {
                propString += ",";
            }
            String value = propMap.get(key);
            propString += key + "=" + (value.startsWith("<") ? "" : "\"") + propMap.get(key)
                    + (value.startsWith("<") ? "" : "\"");
        }
        // lobelloc控制label位置
        return "\"" + this.id + "\"[shape=\"none\"," + propString + ",labelloc=\"b\"]";
    }

    public static class Builder {
        private String id;
        private String label;
        private String tooltip;
        private String className = "cinode";
        private String fontColor;
        private String image;
        private String icon;

        public Builder(String _id) {
            this.id = _id;
        }

        public Builder withIcon(String _icon) {
            this.icon = _icon;
            return this;
        }

        public Builder withLabel(String _label) {
            this.label = _label;
            return this;
        }

        public Builder withTooltip(String _tooltip) {
            this.tooltip = _tooltip;
            return this;
        }

        public Builder withClass(String _class) {
            this.className = _class;
            return this;
        }

        public Builder withFontColor(String _fontcolor) {
            this.fontColor = _fontcolor;
            return this;
        }

        public Builder withImage(String _image) {
            this.image = "/resource/img/icons/" + _image + ".png";
            return this;
        }

        public Node build() {
            return new Node(this);
        }
    }
}
