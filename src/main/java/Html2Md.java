import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

/**
 * HTML转MD工具类
 * create by zhangtao
 */
public class Html2Md {
    private Html2Md() {
    }

    public static String getMarkDownText(String html) {
        StringBuilder result = new StringBuilder();

        Document document = Jsoup.parseBodyFragment(html.replace("&nbsp;", ""));
        // 遍历所有直接子节点
        for (Node node : document.body().childNodes()) {
            result.append(handleNode(node));
        }
        return result.toString();
    }

    public static String getMarkDownText(Element element) {
        StringBuilder result = new StringBuilder();
        // 遍历所有直接子节点
        for (Node node : element.childNodes()) {
            result.append(handleNode(node));
        }
        return result.toString();
    }

    /**
     * 处理Node，目前支持处理p、pre、ul和ol四种节点
     *
     * @param node
     * @return
     */
    private static String handleNode(Node node) {
        String nodeName = node.nodeName();
        String nodeStr = node.toString();
        if ("p".equals(nodeName)) {
            Element pElement = Jsoup.parseBodyFragment(nodeStr).body().getElementsByTag("p").first();
            String pStr = pElement.html();
            for (Element child : pElement.children()) {
                pStr = handleInnerHtml(pStr, child);
            }
            return pStr + "\n";
        } else if ("pre".equals(nodeName)) {
            return "```java\n" + Jsoup.parseBodyFragment(nodeStr).body().text() + "\n```\n";
        } else if ("ul".equals(nodeName)) {
            Element ulElement = Jsoup.parseBodyFragment(nodeStr).body().getElementsByTag("ul").first();
            String ulStr = ulElement.html().replace("<li>", "- ").replace("</li>", "");
            for (Element li : ulElement.getElementsByTag("li")) {
                for (Element child : li.children()) {
                    ulStr = handleInnerHtml(ulStr, child);
                }
            }
            return ulStr + "\n";
        } else if ("ol".equals(nodeName)) {
            Element olElement = Jsoup.parseBodyFragment(nodeStr).body().getElementsByTag("ol").first();
            String olStr = olElement.html();

            Elements liElements = olElement.getElementsByTag("li");
            for (int i = 1; i <= liElements.size(); i++) {
                Element li = liElements.get(i - 1);
                olStr = olStr.replace(li.toString(), li.toString().replace("<li>", i + ". ").replace("</li>", ""));

                for (Element child : li.children()) {
                    olStr = handleInnerHtml(olStr, child);
                }
            }
            return olStr + "\n";
        } else if ("blockquote".equals(nodeName)) {
            Element blockquoteElement = Jsoup.parseBodyFragment(nodeStr).body().getElementsByTag("blockquote").first();
            Element pElement = blockquoteElement.getElementsByTag("p").first();
            String pStr = pElement.html();
            pStr = ">" + handleInnerHtml(pStr, pElement);
            return pStr + "\n";
        } else if ("h2".equals(nodeName)) {
            Element h2Element = Jsoup.parseBodyFragment(nodeStr).body().getElementsByTag("h2").first();
            String h2Str = "## " + h2Element.text();

            return h2Str + "\n";
        } else if ("h3".equals(nodeName)) {
            Element h3Element = Jsoup.parseBodyFragment(nodeStr).body().getElementsByTag("h3").first();
            String h3Str = "### " + h3Element.text();

            return h3Str + "\n";
        } else if ("h4".equals(nodeName)) {
            Element h4Element = Jsoup.parseBodyFragment(nodeStr).body().getElementsByTag("h4").first();
            String h2Str = "#### " + h4Element.text();

            return h2Str + "\n";
        } else if ("h5".equals(nodeName)) {
            Element h5Element = Jsoup.parseBodyFragment(nodeStr).body().getElementsByTag("h5").first();
            String h5Str = "##### " + h5Element.text();

            return h5Str + "\n";
        } else if ("#text".equals(nodeName)) {
            return "\n";
        }
        return "";
    }

    /**
     * 处理innerHTML中的HTML元素，目前支持处理的子元素包括strong、img、em
     *
     * @param innerHTML
     * @param child
     * @return
     */
    private static String handleInnerHtml(String innerHTML, Element child) {
        String s = child.tag().toString();
        if ("strong".equals(s)) {
            innerHTML = innerHTML.replace(child.toString(), "**" + child.text() + "**");
        } else if ("img".equals(s)) {
            String src = child.attr("src");
            if (src.charAt(0) == '/') {
                src = "http://img-blog" + src;
            }

            innerHTML = "\n" + innerHTML.replace(child.toString(), "![img](" + src + ")");
        } else if ("em".equals(s)) {
            innerHTML = innerHTML.replace(child.toString(), " *" + child.text() + "* ");
        } else if ("a".equals(s)) {
            String href = child.attr("href");
            innerHTML = innerHTML.replace(child.toString(), " [" + child.text() + "]" + "(" + href + ")");
        } else {
            innerHTML = innerHTML.replace(child.toString(), child.text());
        }
        return innerHTML;
    }


}