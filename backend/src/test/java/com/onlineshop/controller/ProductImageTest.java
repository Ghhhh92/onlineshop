package com.onlineshop.controller;

import com.jayway.jsonpath.JsonPath;
import com.onlineshop.service.ProductImageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 商品图片上传（US-27）：JPG 或 PNG、不超过 5MB；上传后的地址可直接访问，也可以填进发布商品。
 * 测试上传的图片保存在 target/test-uploads，每个用例结束后清空。
 *
 * @author 李帆
 * @date 2026-10-31
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProductImageTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 13, 'I', 'H', 'D', 'R'};

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F', 0};

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductImageService productImageService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockHttpSession seller;

    @BeforeEach
    void login() throws Exception {
        jdbcTemplate.update("DELETE FROM purchase_intent");
        jdbcTemplate.update("DELETE FROM product");
        seller = (MockHttpSession) mockMvc.perform(post("/api/seller/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }

    @AfterEach
    void clearUploads() throws IOException {
        try (Stream<Path> files = Files.list(productImageService.directory())) {
            for (Path file : files.toList()) {
                Files.deleteIfExists(file);
            }
        }
    }

    @Test
    @DisplayName("上传图片需要登录")
    void requireLogin() throws Exception {
        mockMvc.perform(multipart("/api/product/image").file(image("a.png", PNG)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("请先登录"));
    }

    @Test
    @DisplayName("上传 PNG：返回图片地址，按地址能取回同样的内容")
    void uploadPng() throws Exception {
        String url = urlOf(upload(image("商品主图.png", PNG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url").value(matchesPattern("^/api/uploads/[0-9a-f]{32}\\.png$"))));

        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(PNG));
    }

    @Test
    @DisplayName("上传 JPG：保存为 .jpg")
    void uploadJpeg() throws Exception {
        String url = urlOf(upload(image("photo.jpeg", JPEG)).andExpect(status().isOk()));

        assertThat(url).endsWith(".jpg");
        mockMvc.perform(get(url))
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(content().bytes(JPEG));
    }

    @Test
    @DisplayName("格式按文件内容判断：改了扩展名的 GIF、文本文件都拒绝")
    void formatCheckedByContent() throws Exception {
        upload(image("fake.png", "GIF89a......".getBytes()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("只支持 JPG 或 PNG 格式的图片"));
        upload(image("note.jpg", "hello".getBytes()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("只支持 JPG 或 PNG 格式的图片"));
        assertThat(savedFiles()).isEmpty();
    }

    @Test
    @DisplayName("没选文件或文件为空：提示请选择要上传的图片")
    void missingFile() throws Exception {
        mockMvc.perform(multipart("/api/product/image").session(seller))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请选择要上传的图片"));
        upload(image("empty.png", new byte[0]))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请选择要上传的图片"));
    }

    @Test
    @DisplayName("正好 5MB 可以上传，超过 5MB 提示图片不能超过 5MB")
    void sizeLimit() throws Exception {
        upload(image("max.png", pngOfSize(ProductImageService.MAX_SIZE)))
                .andExpect(status().isOk());
        upload(image("big.png", pngOfSize(ProductImageService.MAX_SIZE + 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("图片不能超过 5MB"));
        assertThat(savedFiles()).hasSize(1);
    }

    @Test
    @DisplayName("不使用上传时的文件名：文件名里带路径也只会存进图片目录")
    void clientFilenameIgnored() throws Exception {
        String url = urlOf(upload(image("../../evil.png", PNG)).andExpect(status().isOk()));

        assertThat(url).doesNotContain("evil");
        assertThat(savedFiles()).hasSize(1);
    }

    @Test
    @DisplayName("上传后的地址可以直接填进发布商品，买家首页能拿到这个地址")
    void publishWithUploadedImage() throws Exception {
        String url = urlOf(upload(image("lamp.png", PNG)).andExpect(status().isOk()));

        mockMvc.perform(post("/api/product/publish").session(seller)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"台灯\",\"price\":35,\"stock\":1,\"image\":\"" + url + "\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/product/on-sale"))
                .andExpect(jsonPath("$.data.image").value(url));
    }

    @Test
    @DisplayName("图片不存在返回 404")
    void missingImage() throws Exception {
        mockMvc.perform(get("/api/uploads/00000000000000000000000000000000.png"))
                .andExpect(status().isNotFound());
    }

    private ResultActions upload(MockMultipartFile file) throws Exception {
        return mockMvc.perform(multipart("/api/product/image").file(file).session(seller));
    }

    private static String urlOf(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.data.url");
    }

    private static MockMultipartFile image(String filename, byte[] content) {
        return new MockMultipartFile("file", filename, "image/png", content);
    }

    /** 以 PNG 文件头开头、总长度为 size 的内容 */
    private static byte[] pngOfSize(long size) {
        byte[] content = Arrays.copyOf(PNG, (int) size);
        Arrays.fill(content, PNG.length, content.length, (byte) 1);
        return content;
    }

    private List<Path> savedFiles() throws IOException {
        try (Stream<Path> files = Files.list(productImageService.directory())) {
            return files.toList();
        }
    }
}
