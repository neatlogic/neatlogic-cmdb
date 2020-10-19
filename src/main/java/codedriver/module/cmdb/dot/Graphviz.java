package codedriver.module.cmdb.dot;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

public class Graphviz {
    List<Layer> layerList = new ArrayList<>();
    List<Link> linkList = new ArrayList<>();

    private Graphviz(Builder builder) {
        this.layerList = builder.layerList;
        this.linkList = builder.linkList;
    }

    public String toString() {
        String str = "";
        str += "digraph {\n";
        str += "bgcolor=\"transparent\";\n";
        str +=
            "Node [fontname=Arial, shape=\"ellipse\", fixedsize=\"true\", width=\"1.1\", height=\"1.1\", color=\"transparent\" ,fontsize=12];\n";
        str += "Edge [fontname=Arial, minlen=\"1\", color=\"#7f8fa6\", fontsize=10];\n";
        str += "ranksep = 1.1;\n";
        str += "nodesep=.7;\n";
        str += "size = \"11,8\";\n";
        str += "rankdir=TB";
        if (CollectionUtils.isNotEmpty(layerList)) {
            str += layerList.stream().map(d -> "{" + d.toString() + "}\n").collect(Collectors.joining(""));
            if (layerList.size() > 1) {
                str +=
                    "{node[];" + layerList.stream().map(d -> "\"" + d.getId() + "\"").collect(Collectors.joining("->"))
                        + "[style=invis]}";
            }
        }
        if (CollectionUtils.isNotEmpty(linkList)) {
            str += linkList.stream().map(d -> d.toString()).collect(Collectors.joining(";\n"));
        }
        str += "}";
        return str;
    }

    public static class Builder {
        List<Layer> layerList = new ArrayList<>();
        List<Link> linkList = new ArrayList<>();

        public Builder addLayer(Layer layer) {
            if (layer != null) {
                layerList.add(layer);
            }
            return this;
        }

        public Builder addLink(Link link) {
            if (link != null) {
                linkList.add(link);
            }
            return this;
        }

        public Graphviz build() {
            return new Graphviz(this);
        }
    }

}
