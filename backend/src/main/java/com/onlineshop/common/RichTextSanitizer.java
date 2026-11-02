package com.onlineshop.common;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;

/**
 * 富文本（HTML）过滤：按白名单只保留段落、标题、列表、表格、图片、视频、链接等标签，
 * 去掉脚本、事件属性（onclick 等）、样式和 javascript: 这类链接，防止商品描述里的脚本在买家浏览时执行（XSS）。
 *
 * @author 李帆
 * @date 2026-11-02
 */
public final class RichTextSanitizer {

    /** 判断相对地址的协议时用的基准地址；保存的仍是原来的相对地址，例如 /api/uploads/xxx.png */
    private static final String BASE_URI = "http://localhost/";

    /** 图片和视频只允许 http、https 和相对地址，不允许 data: 内嵌 */
    private static final Safelist SAFELIST = Safelist.relaxed()
            .addTags("video", "source", "s", "hr")
            .addAttributes("video", "src", "poster", "controls", "width", "height")
            .addAttributes("source", "src", "type")
            .addProtocols("video", "src", "http", "https")
            .addProtocols("video", "poster", "http", "https")
            .addProtocols("source", "src", "http", "https")
            .preserveRelativeLinks(true);

    private RichTextSanitizer() {
    }

    /**
     * 过滤后的 HTML；没填，或者过滤后既没有文字也没有图片、视频时返回 null
     */
    public static String sanitize(String html) {
        if (html == null || html.isBlank()) {
            return null;
        }
        Document clean = new Cleaner(SAFELIST).clean(Jsoup.parseBodyFragment(html, BASE_URI));
        clean.outputSettings().prettyPrint(false);
        Element body = clean.body();
        if (!body.hasText() && body.select("img, video").isEmpty()) {
            return null;
        }
        return body.html().trim();
    }
}
