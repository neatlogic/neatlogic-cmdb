import static guru.nidi.graphviz.model.Factory.graph;
import static guru.nidi.graphviz.model.Factory.node;
import static guru.nidi.graphviz.model.Factory.to;
import static guru.nidi.graphviz.attribute.Attributes.*;

import java.io.File;
import java.io.IOException;

import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.Font;
import guru.nidi.graphviz.attribute.Rank;
import guru.nidi.graphviz.attribute.Rank.RankDir;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.Graph;

public class GraphvizTest {
    public static void main(String[] arg) {
        Graph g = graph("example1").directed().graphAttr().with(Rank.dir(RankDir.LEFT_TO_RIGHT)).nodeAttr().with(Font.name("arial"))
            .linkAttr().with("class", "link-class").with(node("a").with(Color.RED).link(node("b")),
                node("b").link(to(node("c")).with(attr("weight", 5), Style.DASHED)));
            //Graphviz.fromGraph(g).height(100).render(Format.PNG).toFile(new File("/Users/chenqiwei/Downloads/a.png"));
        System.out.println(Graphviz.fromGraph(g).height(100).render(Format.SVG).toString());
    }
}
