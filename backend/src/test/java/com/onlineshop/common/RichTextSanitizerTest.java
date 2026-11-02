package com.onlineshop.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 富文本描述的过滤（US-35）：保留排版、图片和视频，去掉脚本、事件属性、样式和不安全的地址。
 *
 * @author 李帆
 * @date 2026-11-02
 */
class RichTextSanitizerTest {

    @Test
    @DisplayName("标题、段落、加粗、列表原样保留")
    void keepsFormatting() {
        String html = "<h2>参数</h2><p>九成新，<strong>送充电器</strong></p><ul><li>颜色：白</li></ul>";

        assertThat(RichTextSanitizer.sanitize(html)).isEqualTo(html);
    }

    @Test
    @DisplayName("上传的图片和网络视频保留，图片仍是原来的相对地址")
    void keepsImagesAndVideos() {
        String html = "<p><img src=\"/api/uploads/0123456789abcdef0123456789abcdef.png\" alt=\"正面\"></p>"
                + "<video src=\"https://example.com/demo.mp4\" controls=\"\"></video>";

        assertThat(RichTextSanitizer.sanitize(html)).isEqualTo(html);
    }

    @Test
    @DisplayName("脚本、事件属性、样式和 iframe 被去掉，文字保留")
    void removesScriptsAndStyles() {
        String html = "<p style=\"position:fixed\" onclick=\"steal()\">便宜卖</p>"
                + "<script>alert(1)</script><iframe src=\"https://example.com\"></iframe>";

        assertThat(RichTextSanitizer.sanitize(html)).isEqualTo("<p>便宜卖</p>");
    }

    @Test
    @DisplayName("javascript: 链接、data: 内嵌图片和视频的地址被去掉")
    void removesUnsafeUrls() {
        assertThat(RichTextSanitizer.sanitize("<a href=\"javascript:alert(1)\">点我</a>")).isEqualTo("<a>点我</a>");
        assertThat(RichTextSanitizer.sanitize("<p>图<img src=\"data:image/png;base64,AAAA\"></p>"))
                .isEqualTo("<p>图<img></p>");
        assertThat(RichTextSanitizer.sanitize("<video src=\"javascript:alert(1)\" controls></video>"))
                .isEqualTo("<video controls=\"\"></video>");
        assertThat(RichTextSanitizer.sanitize("<a href=\"https://example.com/item\">详情</a>"))
                .isEqualTo("<a href=\"https://example.com/item\">详情</a>");
    }

    @Test
    @DisplayName("纯文字描述照常保存，尖括号和 & 转义")
    void plainText() {
        assertThat(RichTextSanitizer.sanitize("九成新，价格<100 & 包邮")).isEqualTo("九成新，价格&lt;100 &amp; 包邮");
    }

    @Test
    @DisplayName("没填、只有空白、只有空段落、过滤后什么都不剩：返回 null")
    void emptyBecomesNull() {
        assertThat(RichTextSanitizer.sanitize(null)).isNull();
        assertThat(RichTextSanitizer.sanitize("  ")).isNull();
        assertThat(RichTextSanitizer.sanitize("<p><br></p>")).isNull();
        assertThat(RichTextSanitizer.sanitize("<script>alert(1)</script>")).isNull();
    }
}
