package com.onlineshop.service;

import com.onlineshop.common.BizException;
import com.onlineshop.config.ShopProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 商品图片上传（US-27）：只收 JPG、PNG，单张不超过 5MB，保存到本地目录，返回图片的访问地址。
 * <p>
 * 格式按文件开头的几个字节判断，不看文件名和浏览器报的类型；保存时用随机文件名，不用上传时的文件名。
 *
 * @author 李帆
 * @date 2026-10-31
 */
@Service
public class ProductImageService {

    /** 图片访问地址的前缀。放在 /api 下面，开发时的 Vite 代理和部署时的 nginx 都已经转发 /api */
    public static final String URL_PREFIX = "/api/uploads/";

    /** 单张图片的大小上限：5MB */
    public static final long MAX_SIZE = 5L * 1024 * 1024;

    public static final String TOO_LARGE_MESSAGE = "图片不能超过 5MB";

    private static final byte[] JPEG_HEADER = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    private static final byte[] PNG_HEADER = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

    /** 上传后返回的地址：前缀 + 32 位随机文件名 + .jpg 或 .png */
    private static final Pattern STORED_URL = Pattern.compile(Pattern.quote(URL_PREFIX) + "([0-9a-f]{32}\\.(?:jpg|png))");

    private final Path directory;

    public ProductImageService(ShopProperties shopProperties) throws IOException {
        this.directory = Path.of(shopProperties.upload().dir()).toAbsolutePath().normalize();
        Files.createDirectories(directory);
    }

    /** 图片保存目录（绝对路径） */
    public Path directory() {
        return directory;
    }

    /**
     * 保存上传的图片，返回访问地址，例如 /api/uploads/3f2a….png
     */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择要上传的图片");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BizException(TOO_LARGE_MESSAGE);
        }
        try {
            byte[] content = file.getBytes();
            String extension = extensionOf(content);
            if (extension == null) {
                throw new BizException("只支持 JPG 或 PNG 格式的图片");
            }
            String filename = UUID.randomUUID().toString().replace("-", "") + extension;
            Files.write(directory.resolve(filename), content);
            return URL_PREFIX + filename;
        } catch (IOException e) {
            throw new UncheckedIOException("保存商品图片失败", e);
        }
    }

    /**
     * 是不是上传接口返回过、而且文件还在的图片地址。发布商品时只接受这样的地址。
     */
    public boolean isStored(String url) {
        if (url == null) {
            return false;
        }
        Matcher matcher = STORED_URL.matcher(url);
        return matcher.matches() && Files.isRegularFile(directory.resolve(matcher.group(1)));
    }

    /** 按文件开头的字节识别格式：JPG 返回 .jpg，PNG 返回 .png，其他返回 null */
    private static String extensionOf(byte[] content) {
        if (startsWith(content, JPEG_HEADER)) {
            return ".jpg";
        }
        if (startsWith(content, PNG_HEADER)) {
            return ".png";
        }
        return null;
    }

    private static boolean startsWith(byte[] content, byte[] header) {
        return content.length >= header.length
                && Arrays.equals(content, 0, header.length, header, 0, header.length);
    }
}
